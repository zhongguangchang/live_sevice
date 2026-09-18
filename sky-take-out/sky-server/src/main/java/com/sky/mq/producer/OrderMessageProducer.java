package com.sky.mq.producer;

import com.sky.constant.MqConstant;
import com.sky.dto.OrderMessageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 订单消息生产者
 * <p>
 * 四个延迟场景的消息都从这里发出。前三类延迟时长固定，靠队列上的
 * TTL 控制；第四类（服务前提醒）延迟时长不固定，靠每条消息自己的
 * expiration 控制。
 */
@Component
@Slf4j
public class OrderMessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送「订单超时未支付」延迟消息，15 分钟后被消费
     * <p>
     * 这是本项目最核心的一个延迟消息：下单时发，到期后消费者检查
     * 订单是否还是待付款，是的话就关单并把 Redis 名额还回去。
     * 它取代了原项目每分钟扫全表轮询的定时任务。
     */
    public void sendOrderTimeout(Long orderId, String orderNumber, Long slotId, Long userId) {
        OrderMessageDTO message = OrderMessageDTO.builder()
                .type(MqConstant.MSG_ORDER_TIMEOUT)
                .orderId(orderId)
                .orderNumber(orderNumber)
                .slotId(slotId)
                .userId(userId)
                .sendTime(LocalDateTime.now())
                .build();
        sendAfterCommit(MqConstant.ORDER_DELAY_EXCHANGE, MqConstant.ORDER_DELAY_ROUTING_KEY,
                message, "timeout:" + orderId, null);
    }

    /**
     * 发送「服务前 1 小时提醒」
     * <p>
     * 这个场景的延迟时长是按预约时间算出来的，所以不能靠队列的固定 TTL，
     * 必须给每条消息单独设 expiration。
     * <p>
     * 已知限制：同一个队列里的消息是先进先出排队过期的，如果队头是
     * 一条延迟很久的消息，后面短期消息也得等——也就是队头阻塞。
     * 彻底解决要装 rabbitmq_delayed_message_exchange 插件。
     * 这里业务上可接受，因为预约时间通常都在几天内，偏差有限。
     */
    public void sendServiceRemind(Long orderId, Long userId, Long providerId, LocalDateTime serviceTime) {
        LocalDateTime remindAt = serviceTime.minusHours(1);
        long delayMillis = Duration.between(LocalDateTime.now(), remindAt).toMillis();
        if (delayMillis <= 0) {
            log.info("距离服务时间已不足 1 小时，跳过提醒：orderId={}", orderId);
            return;
        }

        OrderMessageDTO message = OrderMessageDTO.builder()
                .type(MqConstant.MSG_SERVICE_REMIND)
                .orderId(orderId)
                .userId(userId)
                .providerId(providerId)
                .serviceTime(serviceTime)
                .sendTime(LocalDateTime.now())
                .build();
        sendAfterCommit(MqConstant.SERVICE_REMIND_EXCHANGE, MqConstant.SERVICE_REMIND_ROUTING_KEY,
                message, "remind:" + orderId, delayMillis);
    }

    /**
     * 发送「服务完成 7 天后自动结单」延迟消息
     */
    public void sendAutoReview(Long orderId, Long userId, Long providerId) {
        OrderMessageDTO message = OrderMessageDTO.builder()
                .type(MqConstant.MSG_AUTO_REVIEW)
                .orderId(orderId)
                .userId(userId)
                .providerId(providerId)
                .sendTime(LocalDateTime.now())
                .build();
        sendAfterCommit(MqConstant.AUTO_REVIEW_EXCHANGE, MqConstant.AUTO_REVIEW_ROUTING_KEY,
                message, "review:" + orderId, null);
    }

    /**
     * 发送「派单后 5 分钟未接单」延迟消息
     */
    public void sendDispatchTimeout(Long orderId, Long providerId) {
        OrderMessageDTO message = OrderMessageDTO.builder()
                .type(MqConstant.MSG_DISPATCH_TIMEOUT)
                .orderId(orderId)
                .providerId(providerId)
                .sendTime(LocalDateTime.now())
                .build();
        sendAfterCommit(MqConstant.DISPATCH_DELAY_EXCHANGE, MqConstant.DISPATCH_DELAY_ROUTING_KEY,
                message, "dispatch:" + orderId, null);
    }

    // ========================================================================
    //  私有辅助方法
    // ========================================================================

    /**
     * 在数据库事务提交之后再发消息
     * <p>
     * 为什么必须这样：如果直接在事务里发消息，可能出现
     * 「消息发出去了、但事务最后回滚了」的情况——消费者收到消息去查订单，
     * 发现订单根本不存在。
     * <p>
     * 注册一个事务同步回调，等 afterCommit 再真正发送，就避免了这个问题。
     * 这是「本地事务 + 消息」的经典处理方式，比引入分布式事务轻量得多。
     * <p>
     * 注意：提交之后发送如果失败，已经没办法回滚订单了，只能记错误日志，
     * 靠定时对账任务兜底。这也是为什么对账任务不是可有可无的。
     *
     * @param expireMillis 消息级 TTL，null 表示用队列级 TTL
     */
    private void sendAfterCommit(String exchange, String routingKey,
                                 OrderMessageDTO message, String messageId, Long expireMillis) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doSend(exchange, routingKey, message, messageId, expireMillis);
                }
            });
        } else {
            doSend(exchange, routingKey, message, messageId, expireMillis);
        }
    }

    private void doSend(String exchange, String routingKey,
                        OrderMessageDTO message, String messageId, Long expireMillis) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, message, m -> {
                MessageProperties props = m.getMessageProperties();
                props.setMessageId(messageId);
                // 消息持久化：RabbitMQ 重启后消息不丢
                props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                if (expireMillis != null) {
                    props.setExpiration(String.valueOf(expireMillis));
                }
                return m;
            });
            log.info("MQ 消息已发送：type={}, orderId={}, 目标队列={}",
                    message.getType(), message.getOrderId(), routingKey);
        } catch (Exception e) {
            log.error("MQ 消息发送失败，等待定时对账任务兜底：type={}, orderId={}",
                    message.getType(), message.getOrderId(), e);
        }
    }
}
