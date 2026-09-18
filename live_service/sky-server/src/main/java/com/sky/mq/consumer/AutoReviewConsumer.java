package com.sky.mq.consumer;

import com.rabbitmq.client.Channel;
import com.sky.constant.MqConstant;
import com.sky.dto.OrderMessageDTO;
import com.sky.service.ServiceOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 服务完成 7 天后仍未评价，自动结单
 * <p>
 * 服务完成时（状态流转到「待评价」）发一条 7 天的延迟消息，
 * 到点后如果用户还是没评价，就把订单推进到「已完成」。
 * <p>
 * 为什么要有这一步：订单如果永远停在「待评价」，统计口径就乱了
 * ——「待评价」既是业务上的待办，也是数据上的未闭环状态。
 * <p>
 * 和订单超时取消一样，这里的幂等靠数据库 CAS：
 * {@code autoComplete} 内部要求当前状态必须是「待评价」，
 * 用户恰好在这一刻提交了评价时，影响行数为 0，自动结单自动作废。
 */
@Component
@Slf4j
public class AutoReviewConsumer {

    @Autowired
    private ServiceOrderService serviceOrderService;

    @RabbitListener(queues = MqConstant.AUTO_REVIEW_QUEUE)
    public void handleAutoReview(OrderMessageDTO message, Channel channel,
                                 @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("收到自动结单消息：orderId={}", message.getOrderId());

        try {
            boolean completed = serviceOrderService.autoComplete(message.getOrderId());
            if (!completed) {
                // 用户已经评价过、或者订单被人工处理过，都属于正常情况
                log.info("订单无需自动结单（已评价或状态已变更）：orderId={}", message.getOrderId());
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理自动结单消息失败：orderId={}", message.getOrderId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
