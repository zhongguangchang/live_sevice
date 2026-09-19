package com.sky.task;

import com.sky.entity.ServiceOrder;
import com.sky.mapper.ServiceOrderMapper;
import com.sky.service.ServiceOrderService;
import com.sky.service.SlotService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 订单兜底对账任务
 * <p>
 * <b>这个类存在的意义：MQ 不是 100% 可靠。</b>
 * 消息可能因为 RabbitMQ 重启、磁盘故障、消费者异常而丢失，
 * 光靠延迟队列不足以保证「超时订单一定会被取消」。
 * 所以这里保留一组低频的定时扫描作为最后一道防线。
 * <p>
 * 这和原项目的 OrderTask 有本质区别：原项目是「每分钟扫全表」
 * 作为主要手段，空转严重、精度差；这里是「每 5 分钟兜底扫一次」，
 * 主力已经交给 MQ 延迟队列了。两者不是替代关系，是主备关系。
 */
@Component
@Slf4j
public class ServiceOrderTask {

    /** 兜底扫描时，超过这个时长还没付款就认定该取消 */
    private static final int TIMEOUT_MINUTES = 20;

    /** 服务完成后超过这么多天还没评价，自动结单 */
    private static final int AUTO_COMPLETE_DAYS = 7;

    @Autowired
    private ServiceOrderMapper serviceOrderMapper;
    @Autowired
    private ServiceOrderService serviceOrderService;
    @Autowired
    private SlotService slotService;

    /**
     * Redisson 客户端，用于给定时任务加分布式锁
     * <p>
     * <b>为什么定时任务需要分布式锁：</b>@Scheduled 是「每个实例各自跑」的。
     * 项目部署两台机器时，同一时刻两个实例都会去扫同一批超时订单、
     * 跑同一轮库存对账 —— 虽然 CAS 和幂等能保证数据不出错，
     * 但会白白多一倍数据库压力和日志噪音。
     * <p>
     * 加锁之后同一时刻只有一个实例真正执行，另一个直接跳过。
     * 用 required = false：万一没接 Redisson（比如单元测试里），
     * 退化成单机模式照常执行，不会因为缺个 Bean 就起不来。
     */
    @Autowired(required = false)
    private RedissonClient redissonClient;

    /** 锁的持有时长（秒）。必须大于任务最长执行时间，否则任务没跑完锁就自动释放了 */
    private static final long LOCK_LEASE_SECONDS = 60;

    /**
     * 尝试获取分布式锁。拿不到说明别的实例正在跑，本次直接跳过
     */
    private boolean tryLock(String lockKey) {
        if (redissonClient == null) {
            return true;
        }
        try {
            return redissonClient.getLock(lockKey).tryLock(0, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            // Redis 抖动不应该让兜底任务彻底停摆：这里选择「继续执行」，
            // 交给业务层的幂等（CAS）去保证重复执行不会出错
            log.warn("获取定时任务分布式锁失败，本次直接执行：lockKey={}, 原因={}", lockKey, e.getMessage());
            return true;
        }
    }

    private void unlock(String lockKey) {
        if (redissonClient == null) {
            return;
        }
        try {
            RLock lock = redissonClient.getLock(lockKey);
            // 只释放自己持有的锁。Redisson 的锁是可重入的，
            // 不判断 isHeldByCurrentThread 时可能把别人的锁释放掉
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (Exception e) {
            log.warn("释放定时任务分布式锁失败：lockKey={}, 原因={}", lockKey, e.getMessage());
        }
    }

    /**
     * 兜底取消超时未支付订单
     * <p>
     * 正常情况这批订单早被 MQ 延迟消息处理掉了（15 分钟），
     * 这里用 20 分钟更宽松的阈值，只捞那些「消息丢了」的漏网之鱼。
     * <p>
     * 之所以是 20 分钟而不是 15 分钟：如果两个机制用同样的时间，
     * 它们会在同一时刻争抢同一批订单，虽然 CAS 能保证不出错，
     * 但会产生大量无意义的并发冲突和日志。
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void cancelTimeoutOrders() {
        String lockKey = "lock:task:order:timeout";
        if (!tryLock(lockKey)) {
            log.debug("兜底取消任务已在其他实例执行，本次跳过");
            return;
        }
        try {
            LocalDateTime deadline = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);
            List<ServiceOrder> orders = serviceOrderMapper.getByStatusAndOrderTimeLT(
                    ServiceOrder.PENDING_PAYMENT, deadline);
            if (orders == null || orders.isEmpty()) {
                return;
            }
            log.warn("兜底任务发现 {} 笔疑似丢失消息的超时订单", orders.size());
            for (ServiceOrder order : orders) {
                try {
                    serviceOrderService.timeoutCancel(order.getId());
                } catch (Exception e) {
                    log.error("兜底取消订单失败：orderId={}", order.getId(), e);
                }
            }
        } finally {
            unlock(lockKey);
        }
    }

    /**
     * 兜底自动结单
     * <p>
     * 服务完成 7 天后用户仍未评价，自动推进到已完成。
     * 不这么做的话订单会永远停在「待评价」，统计口径就乱了。
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void autoCompleteStaleOrders() {
        String lockKey = "lock:task:order:auto-complete";
        if (!tryLock(lockKey)) {
            log.debug("自动结单任务已在其他实例执行，本次跳过");
            return;
        }
        try {
            LocalDateTime deadline = LocalDateTime.now().minusDays(AUTO_COMPLETE_DAYS);
            List<ServiceOrder> orders = serviceOrderMapper.getByStatusAndFinishTimeLT(
                    ServiceOrder.TO_BE_REVIEWED, deadline);
            if (orders == null || orders.isEmpty()) {
                return;
            }
            log.info("兜底任务发现 {} 笔超期未评价的订单", orders.size());
            for (ServiceOrder order : orders) {
                try {
                    serviceOrderService.autoComplete(order.getId());
                } catch (Exception e) {
                    log.error("兜底自动结单失败：orderId={}", order.getId(), e);
                }
            }
        } finally {
            unlock(lockKey);
        }
    }

    /**
     * Redis 与 MySQL 库存对账
     * <p>
     * 修正两层数据不一致的情况：Redis 重启丢数据、key 过期、
     * 网络分区导致回补失败等等。以 MySQL 为权威。
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void reconcileSlotStock() {
        String lockKey = "lock:task:slot:reconcile";
        if (!tryLock(lockKey)) {
            log.debug("库存对账任务已在其他实例执行，本次跳过");
            return;
        }
        try {
            int fixed = slotService.reconcile();
            if (fixed > 0) {
                log.warn("库存对账任务修正了 {} 个时段", fixed);
            }
        } catch (Exception e) {
            log.error("库存对账任务执行失败", e);
        } finally {
            unlock(lockKey);
        }
    }
}
