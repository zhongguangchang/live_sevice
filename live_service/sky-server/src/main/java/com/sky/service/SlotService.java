package com.sky.service;

import com.sky.dto.SlotBatchCreateDTO;
import com.sky.dto.SlotQueryDTO;
import com.sky.vo.SlotVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 服务时段排期业务接口
 * <p>
 * 这个接口承载了本项目的核心亮点——Redis 原子预扣防超卖，
 * 以及 Redis 与 MySQL 的双层库存一致性维护。
 * <p>
 * <b>库存两层结构：</b>
 * <ul>
 *   <li>Redis（预扣层）：承接并发抢名额，走 Lua 脚本原子扣减，完全不碰数据库</li>
 *   <li>MySQL（权威层）：真正落账，作为最终事实</li>
 * </ul>
 * <p>
 * <b>完整生命周期：</b>
 * <pre>
 *   用户下单      preDeduct        Redis 扣名额，记录用户
 *   支付成功      confirmBooked    MySQL booked_count + 1
 *   取消/超时     rollback         Redis 还名额
 *                 releaseBooked    MySQL booked_count - 1
 *   定时对账      reconcile        以 MySQL 为准修正 Redis
 * </pre>
 */
public interface SlotService {

    /**
     * 查询可预约时段（用户端下单页用）
     * <p>
     * 返回的 SlotVO 里会填充实时剩余名额，注意这个值读的是 Redis
     * 而不是数据库——因为数据库那边还没落账，只有 Redis 才知道
     * 此刻真实还剩多少。
     */
    List<SlotVO> listAvailable(SlotQueryDTO slotQueryDTO);

    /**
     * 管理端条件查询排期
     */
    List<SlotVO> listByCondition(SlotQueryDTO slotQueryDTO);

    /**
     * 批量生成排期
     * <p>
     * 运营选一个日期区间、一组时段、一批师傅和服务，
     * 系统按笛卡尔积批量生成 slot 记录。
     * 重复执行不会产生重复数据（依赖唯一索引 + ON DUPLICATE KEY UPDATE）。
     */
    void batchCreate(SlotBatchCreateDTO slotBatchCreateDTO);

    /**
     * 关闭 / 开放时段
     */
    void updateStatus(Long slotId, Integer status);

    /**
     * 预扣名额（用户下单时调用）
     * <p>
     * 走 Lua 脚本原子完成「校验名额 -> 扣减 -> 记录用户」三步。
     * 如果 Redis 里还没预热该时段，会先现场加载一次再重试。
     *
     * @throws com.sky.exception.SlotSoldOutException     名额已抢完
     * @throws com.sky.exception.DuplicateBookingException 该用户已约过此时段
     * @throws com.sky.exception.SlotNotAvailableException 时段不存在或已关闭
     */
    void preDeduct(Long slotId, Long userId);

    /**
     * 回补名额（取消订单 / 超时未支付 / 师傅拒单时调用）
     * <p>
     * 幂等：重复调用不会把名额越加越多。
     */
    void rollback(Long slotId, Long userId);

    /**
     * 把预扣结果落到 MySQL（支付成功时调用）
     * <p>
     * 用 CAS 更新保证不会超卖：{@code where booked_count < total_stock}。
     *
     * @return true 落账成功；false 说明已被抢完，此时应当回滚整笔订单
     */
    boolean confirmBooked(Long slotId);

    /**
     * 释放 MySQL 里的占用（取消订单时调用）
     */
    boolean releaseBooked(Long slotId);

    /**
     * 缓存预热：把指定日期区间内的时段库存写入 Redis
     * <p>
     * 应该在时段开售前（或服务端启动时）执行，避免第一个下单的用户
     * 撞上"库存未初始化"。
     *
     * @return 实际预热的时段数量
     */
    int warmUpStock(LocalDate begin, LocalDate end);

    /**
     * 对账：以 MySQL 为准修正 Redis
     * <p>
     * 由定时任务调用，用于兜住 Redis 挂掉、数据过期、网络分区等
     * 导致两层数据不一致的情况。
     *
     * @return 修正过的时段数量
     */
    int reconcile();
}
