package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.constant.RedisKeyConstant;
import com.sky.dto.SlotBatchCreateDTO;
import com.sky.dto.SlotQueryDTO;
import com.sky.entity.Slot;
import com.sky.exception.DuplicateBookingException;
import com.sky.exception.SlotNotAvailableException;
import com.sky.exception.SlotSoldOutException;
import com.sky.mapper.SlotMapper;
import com.sky.service.SlotService;
import com.sky.vo.SlotVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 服务时段排期业务实现
 * <p>
 * <b>本类是整个 Redis 并发亮点的落点。</b>
 */
@Service
@Slf4j
public class SlotServiceImpl implements SlotService {

    /** Lua 脚本返回值：库存未初始化 */
    private static final long RESULT_NOT_INITIALIZED = -1L;
    /** Lua 脚本返回值：名额已抢完 */
    private static final long RESULT_SOLD_OUT = -2L;
    /** Lua 脚本返回值：该用户已约过此时段 */
    private static final long RESULT_DUPLICATED = -3L;

    /**
     * 预扣脚本，类加载时读一次，后续复用
     * <p>
     * 注意这里必须配 {@link StringRedisTemplate} 使用。
     * 如果把脚本交给配了 JSON 序列化器的 RedisTemplate，
     * Lua 里 tonumber() 拿到的会是带引号的字符串，直接报错。
     */
    private static final DefaultRedisScript<Long> SLOT_DEDUCT_SCRIPT = new DefaultRedisScript<>();
    private static final DefaultRedisScript<Long> SLOT_ROLLBACK_SCRIPT = new DefaultRedisScript<>();

    static {
        SLOT_DEDUCT_SCRIPT.setLocation(new ClassPathResource("lua/slot_deduct.lua"));
        SLOT_DEDUCT_SCRIPT.setResultType(Long.class);
        SLOT_ROLLBACK_SCRIPT.setLocation(new ClassPathResource("lua/slot_rollback.lua"));
        SLOT_ROLLBACK_SCRIPT.setResultType(Long.class);
    }

    @Autowired
    private SlotMapper slotMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // ========================================================================
    //  查询
    // ========================================================================

    @Override
    public List<SlotVO> listAvailable(SlotQueryDTO slotQueryDTO) {
        List<SlotVO> slots = slotMapper.listAvailable(slotQueryDTO);
        for (SlotVO slot : slots) {
            slot.setRemainStock(currentRemainStock(slot));
        }
        return slots;
    }

    @Override
    public List<SlotVO> listByCondition(SlotQueryDTO slotQueryDTO) {
        return slotMapper.listByCondition(slotQueryDTO);
    }

    // ========================================================================
    //  排期生成
    // ========================================================================

    @Override
    @Transactional
    public void batchCreate(SlotBatchCreateDTO dto) {
        if (dto.getStartDate() == null || dto.getEndDate() == null
                || dto.getProviderIds() == null || dto.getProviderIds().isEmpty()
                || dto.getServiceIds() == null || dto.getServiceIds().isEmpty()
                || dto.getStartTimes() == null || dto.getStartTimes().isEmpty()) {
            throw new SlotNotAvailableException("排期参数不完整");
        }

        int duration = dto.getSlotDuration() == null ? 60 : dto.getSlotDuration();
        int stock = dto.getTotalStock() == null ? 1 : dto.getTotalStock();
        LocalDateTime now = LocalDateTime.now();

        // 笛卡尔积：日期 × 师傅 × 服务 × 时段
        List<Slot> slots = new ArrayList<>();
        for (LocalDate date = dto.getStartDate(); !date.isAfter(dto.getEndDate()); date = date.plusDays(1)) {
            for (Long providerId : dto.getProviderIds()) {
                for (Long serviceId : dto.getServiceIds()) {
                    for (LocalTime startTime : dto.getStartTimes()) {
                        slots.add(Slot.builder()
                                .providerId(providerId)
                                .serviceId(serviceId)
                                .serviceDate(date)
                                .startTime(startTime)
                                .endTime(startTime.plusMinutes(duration))
                                .totalStock(stock)
                                .bookedCount(0)
                                .status(Slot.STATUS_OPEN)
                                .version(0)
                                .merchantId(1L)
                                .createTime(now)
                                .updateTime(now)
                                .build());
                    }
                }
            }
        }

        if (slots.isEmpty()) {
            return;
        }
        slotMapper.insertBatch(slots);
        log.info("批量生成排期 {} 条：{} ~ {}", slots.size(), dto.getStartDate(), dto.getEndDate());

        // 生成完顺手预热，避免第一个下单的用户撞上"库存未初始化"
        warmUpStock(dto.getStartDate(), dto.getEndDate());
    }

    @Override
    public void updateStatus(Long slotId, Integer status) {
        slotMapper.updateStatus(slotId, status);
    }

    // ========================================================================
    //  库存预扣 / 回补 / 落账
    // ========================================================================

    @Override
    public void preDeduct(Long slotId, Long userId) {
        Long result = executeDeduct(slotId, userId);

        // -1 表示 Redis 里还没有这个时段的库存，现场加载一次再重试
        if (result != null && result == RESULT_NOT_INITIALIZED) {
            if (ensureStockLoaded(slotId)) {
                result = executeDeduct(slotId, userId);
            }
        }

        if (result == null || result == RESULT_NOT_INITIALIZED) {
            throw new SlotNotAvailableException(MessageConstant.SLOT_NOT_FOUND);
        }
        if (result == RESULT_SOLD_OUT) {
            throw new SlotSoldOutException(MessageConstant.SLOT_SOLD_OUT);
        }
        if (result == RESULT_DUPLICATED) {
            throw new DuplicateBookingException(MessageConstant.SLOT_ALREADY_BOOKED);
        }

        log.info("时段名额预扣成功：slotId={}, userId={}, 剩余名额={}", slotId, userId, result);
    }

    @Override
    public void rollback(Long slotId, Long userId) {
        if (slotId == null || userId == null) {
            return;
        }
        try {
            Long result = stringRedisTemplate.execute(
                    SLOT_ROLLBACK_SCRIPT,
                    Arrays.asList(RedisKeyConstant.slotStockKey(slotId),
                            RedisKeyConstant.slotUsersKey(slotId)),
                    String.valueOf(userId));
            log.info("时段名额回补：slotId={}, userId={}, 结果={}", slotId, userId, result);
        } catch (Exception e) {
            // 回补失败不应该让「取消订单」这个主流程失败，
            // 记日志后交给定时对账任务修正即可
            log.error("时段名额回补失败，等待定时对账修正：slotId={}, userId={}", slotId, userId, e);
        }
    }

    @Override
    public boolean confirmBooked(Long slotId) {
        int rows = slotMapper.deductStock(slotId);
        if (rows != 1) {
            log.warn("MySQL 库存落账失败，名额可能已被抢完：slotId={}", slotId);
            return false;
        }
        return true;
    }

    @Override
    public boolean releaseBooked(Long slotId) {
        return slotMapper.releaseStock(slotId) == 1;
    }

    // ========================================================================
    //  缓存预热与对账
    // ========================================================================

    @Override
    public int warmUpStock(LocalDate begin, LocalDate end) {
        List<Slot> slots = slotMapper.listByDateRange(begin, end);
        int count = 0;
        for (Slot slot : slots) {
            long ttl = ttlSeconds(slot);
            if (ttl <= 0) {
                // 已经过了追溯期的排期没必要占 Redis 内存
                continue;
            }
            int remain = Math.max(0, slot.getTotalStock() - slot.getBookedCount());
            stringRedisTemplate.opsForValue().set(
                    RedisKeyConstant.slotStockKey(slot.getId()),
                    String.valueOf(remain),
                    Duration.ofSeconds(ttl));
            count++;
        }
        log.info("排期缓存预热完成：{} ~ {}，共 {} 个时段", begin, end, count);
        return count;
    }

    /**
     * 对账：以 MySQL 为权威，修正 Redis 里的库存
     * <p>
     * 推导过程：设 Redis 用户集合大小为 holds，MySQL 已落账数为 committed。
     * 已付款的用户既在集合里、也被计入了 committed，所以
     * <pre>
     *   pending（已预扣未付款）= holds - committed
     *   理论剩余名额 = total_stock - committed - pending
     * </pre>
     * 拿这个理论值和 Redis 里的实际值比对，不一致就修正。
     * <p>
     * 如果库存 key 整个消失了（Redis 重启或过期），说明丢失的
     * 都是「已预扣但没付款」的占用，直接按
     * {@code total_stock - committed} 重建是安全的。
     */
    @Override
    public int reconcile() {
        List<Slot> slots = slotMapper.listForReconcile(LocalDate.now());
        int fixed = 0;

        for (Slot slot : slots) {
            String stockKey = RedisKeyConstant.slotStockKey(slot.getId());
            String usersKey = RedisKeyConstant.slotUsersKey(slot.getId());

            Long holds = stringRedisTemplate.opsForSet().size(usersKey);
            int holdCount = holds == null ? 0 : holds.intValue();
            int committed = slot.getBookedCount();
            int pending = Math.max(0, holdCount - committed);
            int expectedStock = Math.max(0, slot.getTotalStock() - committed - pending);

            long ttl = ttlSeconds(slot);
            if (ttl <= 0) {
                continue;
            }

            String actual = stringRedisTemplate.opsForValue().get(stockKey);
            if (actual == null) {
                // key 不存在：按「只有已落账的部分被占用」重建
                int rebuild = Math.max(0, slot.getTotalStock() - committed);
                stringRedisTemplate.opsForValue()
                        .set(stockKey, String.valueOf(rebuild), Duration.ofSeconds(ttl));
                fixed++;
                log.info("对账重建库存：slotId={}, 重建值={}", slot.getId(), rebuild);
            } else if (!String.valueOf(expectedStock).equals(actual)) {
                stringRedisTemplate.opsForValue()
                        .set(stockKey, String.valueOf(expectedStock), Duration.ofSeconds(ttl));
                fixed++;
                log.warn("对账修正库存：slotId={}, Redis原值={}, 修正为={}",
                        slot.getId(), actual, expectedStock);
            }
        }

        if (fixed > 0) {
            log.info("库存对账完成，共修正 {} 个时段", fixed);
        }
        return fixed;
    }

    // ========================================================================
    //  私有辅助方法
    // ========================================================================

    /**
     * 执行预扣 Lua 脚本
     */
    private Long executeDeduct(Long slotId, Long userId) {
        return stringRedisTemplate.execute(
                SLOT_DEDUCT_SCRIPT,
                Arrays.asList(RedisKeyConstant.slotStockKey(slotId),
                        RedisKeyConstant.slotUsersKey(slotId)),
                String.valueOf(userId));
    }

    /**
     * 把某个时段的库存从数据库加载到 Redis
     * <p>
     * 用 setIfAbsent 而不是 set：并发场景下多个线程可能同时发现 key 不存在，
     * setIfAbsent 保证只有第一个写入生效，不会互相覆盖成不同的值。
     *
     * @return true 表示加载后 Redis 里确实有库存了
     */
    private boolean ensureStockLoaded(Long slotId) {
        Slot slot = slotMapper.getById(slotId);
        if (slot == null || !Slot.STATUS_OPEN.equals(slot.getStatus())) {
            return false;
        }
        long ttl = ttlSeconds(slot);
        if (ttl <= 0) {
            return false;
        }
        int remain = Math.max(0, slot.getTotalStock() - slot.getBookedCount());
        stringRedisTemplate.opsForValue().setIfAbsent(
                RedisKeyConstant.slotStockKey(slotId),
                String.valueOf(remain),
                Duration.ofSeconds(ttl));
        return true;
    }

    /**
     * 读取时段当前剩余名额
     * <p>
     * 优先读 Redis（含未落账的预扣），Redis 没数据时退回数据库计算，
     * 保证页面不会显示空白。
     */
    private int currentRemainStock(SlotVO slot) {
        String value = stringRedisTemplate.opsForValue()
                .get(RedisKeyConstant.slotStockKey(slot.getId()));
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("Redis 中的库存值格式异常：slotId={}, value={}", slot.getId(), value);
            }
        }
        return Math.max(0, slot.getTotalStock() - slot.getBookedCount());
    }

    /**
     * 计算库存 key 的存活时长
     * <p>
     * 服务日期之后再保留 7 天作为纠纷追溯期，过期后 key 自动清理，
     * 不会让 Redis 里堆积大量历史时段的库存数据。
     */
    private long ttlSeconds(Slot slot) {
        LocalDateTime expireAt = slot.getServiceDate().plusDays(7).atStartOfDay();
        return Duration.between(LocalDateTime.now(), expireAt).getSeconds();
    }
}
