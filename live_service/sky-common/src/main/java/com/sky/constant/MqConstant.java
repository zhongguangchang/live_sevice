package com.sky.constant;

/**
 * RabbitMQ 常量类
 * <p>
 * 统一管理交换机、队列、路由键名称。
 * <p>
 * 延迟队列实现原理（TTL + 死信队列）：
 * <pre>
 *   生产者 --&gt; 延迟交换机 --&gt; 延迟队列（无消费者，消息在这里干等 TTL）
 *                                    |
 *                           TTL 到期变成死信
 *                                    |
 *                                    v
 *                             死信交换机 --&gt; 消费队列 --&gt; 消费者
 * </pre>
 */
public class MqConstant {

    // ==================== 消息类型 ====================

    /** 订单超时未支付，需要取消 */
    public static final String MSG_ORDER_TIMEOUT = "ORDER_TIMEOUT";

    /** 服务开始前提醒 */
    public static final String MSG_SERVICE_REMIND = "SERVICE_REMIND";

    /** 服务完成 7 天后仍未评价，自动结单 */
    public static final String MSG_AUTO_REVIEW = "AUTO_REVIEW";

    /** 派单后师傅未及时接单，需要转派 */
    public static final String MSG_DISPATCH_TIMEOUT = "DISPATCH_TIMEOUT";

    // ==================== 订单超时未支付自动取消 ====================

    /** 延迟交换机：生产者把延迟消息发到这里 */
    public static final String ORDER_DELAY_EXCHANGE = "life.order.delay.exchange";

    /** 延迟队列：没有消费者，消息在这里等待 TTL 到期 */
    public static final String ORDER_DELAY_QUEUE = "life.order.delay.queue";

    /** 延迟路由键 */
    public static final String ORDER_DELAY_ROUTING_KEY = "order.delay";

    /** 死信交换机：延迟队列里的消息过期后被转发到这里 */
    public static final String ORDER_DLX_EXCHANGE = "life.order.dlx.exchange";

    /** 订单超时消费队列：真正被监听的队列 */
    public static final String ORDER_TIMEOUT_QUEUE = "life.order.timeout.queue";

    /** 订单超时路由键 */
    public static final String ORDER_TIMEOUT_ROUTING_KEY = "order.timeout";

    /** 订单支付超时时间（毫秒），15 分钟 */
    public static final long ORDER_TIMEOUT_TTL = 15 * 60 * 1000L;

    // ==================== 服务前提醒 ====================

    /** 服务提醒延迟交换机 */
    public static final String SERVICE_REMIND_EXCHANGE = "life.service.remind.exchange";

    /** 服务提醒延迟队列 */
    public static final String SERVICE_REMIND_DELAY_QUEUE = "life.service.remind.delay.queue";

    /** 服务提醒路由键 */
    public static final String SERVICE_REMIND_ROUTING_KEY = "service.remind";

    /** 服务提醒死信交换机 */
    public static final String SERVICE_REMIND_DLX_EXCHANGE = "life.service.remind.dlx.exchange";

    /** 服务提醒消费队列 */
    public static final String SERVICE_REMIND_QUEUE = "life.service.remind.queue";

    /** 服务提醒路由键 */
    public static final String SERVICE_REMIND_CONSUME_ROUTING_KEY = "service.remind.consume";

    /** 提前多久提醒（毫秒），1 小时 */
    public static final long SERVICE_REMIND_AHEAD_TTL = 60 * 60 * 1000L;

    // ==================== 完成后自动好评 ====================

    /** 自动评价延迟交换机 */
    public static final String AUTO_REVIEW_EXCHANGE = "life.auto.review.exchange";

    /** 自动评价延迟队列 */
    public static final String AUTO_REVIEW_DELAY_QUEUE = "life.auto.review.delay.queue";

    /** 自动评价路由键 */
    public static final String AUTO_REVIEW_ROUTING_KEY = "auto.review";

    /** 自动评价死信交换机 */
    public static final String AUTO_REVIEW_DLX_EXCHANGE = "life.auto.review.dlx.exchange";

    /** 自动评价消费队列 */
    public static final String AUTO_REVIEW_QUEUE = "life.auto.review.queue";

    /** 自动评价消费路由键 */
    public static final String AUTO_REVIEW_CONSUME_ROUTING_KEY = "auto.review.consume";

    /** 完成后多少天自动好评（毫秒），7 天 */
    public static final long AUTO_REVIEW_TTL = 7 * 24 * 60 * 60 * 1000L;

    // ==================== 派单超时转派 ====================

    /** 派单延迟交换机 */
    public static final String DISPATCH_DELAY_EXCHANGE = "life.dispatch.delay.exchange";

    /** 派单超时延迟队列 */
    public static final String DISPATCH_DELAY_QUEUE = "life.dispatch.delay.queue";

    /** 派单延迟路由键 */
    public static final String DISPATCH_DELAY_ROUTING_KEY = "dispatch.delay";

    /** 派单超时死信交换机 */
    public static final String DISPATCH_DLX_EXCHANGE = "life.dispatch.dlx.exchange";

    /** 派单超时消费队列 */
    public static final String DISPATCH_TIMEOUT_QUEUE = "life.dispatch.timeout.queue";

    /** 派单超时路由键 */
    public static final String DISPATCH_TIMEOUT_ROUTING_KEY = "dispatch.timeout";

    /** 师傅接单超时时间（毫秒），5 分钟 */
    public static final long DISPATCH_TIMEOUT_TTL = 5 * 60 * 1000L;
}
