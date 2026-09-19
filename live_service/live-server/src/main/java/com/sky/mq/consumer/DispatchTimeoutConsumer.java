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
 * 派单超时未接单消费者
 * <p>
 * 订单支付成功后进入「待接单」，同时发一条 5 分钟的延迟消息。
 * 如果师傅没在这个时间内接单，这里就把订单转派给另一个师傅。
 * <p>
 * <b>为什么订单在下单时就已经有师傅了，还需要转派：</b>
 * 本项目是「按师傅排班」模型，用户预约时段时选中的 slot 本身就带
 * provider_id，所以下单那一刻师傅就定了。但师傅可能临时有事、没看手机、
 * 或者同时在忙别的单，这时需要系统自动换人，
 * 而不是让订单一直挂在那里等人来处理。
 * <p>
 * <b>幂等性：</b>和订单超时取消一样，这里的处理靠数据库的 CAS 保证
 * 只生效一次 —— 师傅在延迟消息到期的同一瞬间接了单，
 * status 已经变成 3，更新条件不满足，转派自然作废。
 */
@Component
@Slf4j
public class DispatchTimeoutConsumer {

    @Autowired
    private ServiceOrderService serviceOrderService;

    @RabbitListener(queues = MqConstant.DISPATCH_TIMEOUT_QUEUE)
    public void handleDispatchTimeout(OrderMessageDTO message, Channel channel,
                                      @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("收到派单超时消息：orderId={}, 原师傅={}",
                message.getOrderId(), message.getProviderId());

        try {
            boolean reassigned = serviceOrderService.reassign(
                    message.getOrderId(), message.getProviderId());
            if (reassigned) {
                log.info("订单已转派给其他师傅：orderId={}", message.getOrderId());
            } else {
                // 没转派不是错误：订单可能已经被接了、被取消了，
                // 或者当前没有别的师傅能接。正常 ACK 即可。
                log.info("订单无需转派：orderId={}", message.getOrderId());
            }
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("处理派单超时消息失败：orderId={}", message.getOrderId(), e);
            // requeue=false，避免毒消息无限循环。
            // 兜底由 ServiceOrderTask 的定时扫描负责。
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
