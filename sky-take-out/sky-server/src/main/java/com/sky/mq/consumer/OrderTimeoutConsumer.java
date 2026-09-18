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
 * 订单超时未支付消费者
 * <p>
 * 这是 MQ 延迟队列的核心消费者，取代了原项目每分钟扫全表轮询的定时任务。
 * <p>
 * <b>为什么必须做幂等：</b>
 * RabbitMQ 的投递语义是「至少一次」，同一条消息可能被投递多次
 * （消费者处理完但 ACK 丢失、网络抖动导致重投等）。
 * 如果没有幂等保护，同一笔订单会被反复取消、名额被反复回补，
 * 最后凭空多出一堆可售名额。具体的幂等实现在
 * {@code ServiceOrderServiceImpl#timeoutCancel} 里，
 * 靠数据库的 CAS 更新（where status = 待付款）保证只生效一次。
 * <p>
 * <b>关于 ACK 方式：</b>
 * application.yml 里配置的是手动 ACK（acknowledge-mode: manual），
 * 所以这里必须自己调用 basicAck / basicNack。
 * 忘了调用的话消息会一直挂在 unacked 状态，堆积在 RabbitMQ 里。
 */
@Component
@Slf4j
public class OrderTimeoutConsumer {

    @Autowired
    private ServiceOrderService serviceOrderService;

    @RabbitListener(queues = MqConstant.ORDER_TIMEOUT_QUEUE)
    public void handleOrderTimeout(OrderMessageDTO message, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("收到订单超时延迟消息：orderId={}, orderNumber={}",
                message.getOrderId(), message.getOrderNumber());

        try {
            boolean cancelled = serviceOrderService.timeoutCancel(message.getOrderId());
            if (cancelled) {
                log.info("订单超时取消完成：orderId={}", message.getOrderId());
            } else {
                // 没取消不是错误：说明订单已经付过款、或者已被别的消息处理过。
                // 这正是幂等生效的表现，正常 ACK 掉即可。
                log.info("订单无需取消（已支付或已处理）：orderId={}", message.getOrderId());
            }
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("处理订单超时消息失败：orderId={}", message.getOrderId(), e);
            // requeue=false：不重新入队。
            // 原因是这类消息一旦失败往往是数据问题，重新入队只会无限循环刷日志。
            // 真正的兜底交给 ServiceOrderTask 里每 5 分钟扫一次超时订单的任务，
            // 这也正是「MQ 之外还要保留定时对账」的意义所在。
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
