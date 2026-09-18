package com.sky.service;

import com.sky.dto.ServiceOrderDispatchDTO;
import com.sky.dto.ServiceOrderPageQueryDTO;
import com.sky.dto.ServiceOrderRejectionDTO;
import com.sky.dto.ServiceOrderSubmitDTO;
import com.sky.result.PageResult;
import com.sky.vo.ServiceOrderStatisticsVO;
import com.sky.vo.ServiceOrderSubmitVO;
import com.sky.vo.ServiceOrderVO;

/**
 * 服务订单业务接口
 * <p>
 * 这个接口是整个系统的主动脉，把 Redis 预扣、MySQL 落账、
 * MQ 延迟消息三件事串成了一条线。
 */
public interface ServiceOrderService {

    /**
     * 提交订单
     * <p>
     * 执行顺序（顺序不能调换）：
     * <pre>
     *   校验时段/地址/服务区域
     *   Redis Lua 预扣名额       <- 先抢名额，抢不到直接返回，不产生任何数据库写入
     *   落库订单 + 订单明细
     *   清空服务清单
     *   发 15 分钟延迟消息        <- 到期未支付自动取消
     * </pre>
     * 任何一步抛异常，都会回滚 Redis 名额，避免名额被白白占用。
     */
    ServiceOrderSubmitVO submitOrder(ServiceOrderSubmitDTO serviceOrderSubmitDTO);

    /**
     * 支付成功处理（供支付回调调用）
     * <p>
     * 幂等：只有「待付款」状态的订单才会被处理，重复回调直接跳过。
     */
    void paySuccess(String orderNumber, Integer payMethod);

    /**
     * 超时未支付自动取消（供 MQ 消费者调用）
     * <p>
     * 幂等：靠 updateStatusIfMatch 的 CAS 保证重复消息不会重复处理。
     *
     * @return true 表示本次确实执行了取消
     */
    boolean timeoutCancel(Long orderId);

    /**
     * 用户主动取消订单
     */
    void userCancel(Long orderId);

    /**
     * 订单分页查询
     */
    PageResult pageQuery(ServiceOrderPageQueryDTO serviceOrderPageQueryDTO);

    /**
     * 查询订单详情
     */
    ServiceOrderVO getDetail(Long orderId);

    /**
     * 派单：把订单指派给服务人员
     * <p>
     * providerId 为空时走自动派单算法：
     * 技能过滤 -> 状态过滤 -> 档期过滤 -> 按评分和负载排序。
     */
    void dispatch(ServiceOrderDispatchDTO serviceOrderDispatchDTO);

    /**
     * 师傅接单
     */
    void accept(Long orderId, Long providerId);

    /**
     * 师傅拒单（会重新进入待派单状态）
     */
    void reject(ServiceOrderRejectionDTO serviceOrderRejectionDTO);

    /**
     * 开始服务
     */
    void startService(Long orderId, Long providerId);

    /**
     * 服务完成（进入待评价）
     */
    void completeService(Long orderId, Long providerId);

    /**
     * 用户长期未评价，自动结单（供 MQ 消费者调用）
     */
    boolean autoComplete(Long orderId);

    /**
     * 到店核销
     */
    void verify(Long orderId, String verifyCode);

    /**
     * 各状态订单数量统计（管理端工作台用）
     */
    ServiceOrderStatisticsVO statistics();
}
