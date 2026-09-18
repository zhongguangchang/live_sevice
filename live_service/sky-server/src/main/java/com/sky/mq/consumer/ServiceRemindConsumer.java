package com.sky.mq.consumer;

import com.rabbitmq.client.Channel;
import com.sky.constant.MqConstant;
import com.sky.dto.OrderMessageDTO;
import com.sky.websocket.AdminNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * 服务前 1 小时提醒消费者
 * <p>
 * 订单支付成功时发一条延迟消息，延迟时长按「服务时间 - 1 小时」算出来，
 * 由生产者在消息上单独设置 expiration。到点后消息变成死信转发到
 * {@link MqConstant#SERVICE_REMIND_QUEUE}，由这里消费。
 * <p>
 * <b>这个消费者曾经是整个 MQ 链路里唯一缺的一环：</b>
 * 队列、交换机、绑定、生产者都写好了，就是没有人监听消费队列，
 * 结果消息全部堆在 RabbitMQ 里没人取，业务上表现为「提醒永远不发」，
 * 而且队列积压会一直涨。打开管理台看到
 * {@code life.service.remind.queue} 的 consumers 是 0 就能定位到这个问题。
 */
@Component
@Slf4j
public class ServiceRemindConsumer {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    @Autowired
    private AdminNotifier adminNotifier;

    @RabbitListener(queues = MqConstant.SERVICE_REMIND_QUEUE)
    public void handleServiceRemind(OrderMessageDTO message, Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("收到服务提醒消息：orderId={}, 预约时间={}",
                message.getOrderId(), message.getServiceTime());

        try {
            String content = String.format("订单 %d 预约的上门服务将在 1 小时后开始（%s），请提醒师傅准时出发",
                    message.getOrderId(),
                    message.getServiceTime() == null ? "-" : TIME_FORMAT.format(message.getServiceTime()));

            // 真实项目里这里会同时发短信 / 小程序订阅消息，
            // 毕设环境用管理端实时提醒代替，效果上运营能看到弹窗和提示音
            adminNotifier.serviceRemind(message.getOrderId(), content);
            log.info("服务前提醒已完成：orderId={}", message.getOrderId());

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理服务提醒消息失败：orderId={}", message.getOrderId(), e);
            // requeue=false，避免同一条消息无限重投刷日志
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
