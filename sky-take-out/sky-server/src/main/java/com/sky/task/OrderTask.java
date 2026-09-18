package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务类，定时处理订单状态
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时订单的方法
     * <p>
     * 【已停用】这两个 @Scheduled 目前被注释掉了，原因有二：
     * <p>
     * 1. 本项目把超时取消交给了 MQ 延迟队列（见 RabbitMQConfiguration 的
     *    life.order.delay.queue），不再需要每分钟扫全表轮询。
     *    这个类后续会降级成「对账兜底」任务，而不是主力。
     * <p>
     * 2. 更直接的原因是：它查的是老项目的 orders 表，而改造后数据库换成了
     *    life_service，里面没有 orders 表，所以每分钟都会刷一次
     *    "Table 'life_service.orders' doesn't exist" 的异常堆栈，
     *    把真正有用的日志淹掉。
     * <p>
     * 等 Controller 层替换完成、老的 Orders 相关代码删除后，
     * 这里会按新表 service_order 重写成对账逻辑，再放开调度。
     */
    //@Scheduled(cron = "0 * * * * ? ") //每分钟触发一次
    public void processTimeoutOrder(){
        log.info("定时处理超时订单：{}", LocalDateTime.now());

        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);

        // select * from orders where status = ? and order_time < (当前时间 - 15分钟)
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, time);

        if(ordersList != null && ordersList.size() > 0){
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时，自动取消");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }

    /**
     * 处理一直处于派送中状态的订单
     * <p>
     * 【已停用】原因同上。另外生活服务场景下「派送中」这个状态已经不存在了，
     * 对应的语义变成了「服务中」，这段逻辑会在重写时一并调整。
     */
    //@Scheduled(cron = "0 0 1 * * ?") //每天凌晨1点触发一次
    public void processDeliveryOrder(){
        log.info("定时处理处于派送中的订单：{}",LocalDateTime.now());

        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);

        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, time);

        if(ordersList != null && ordersList.size() > 0){
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.COMPLETED);
                orderMapper.update(orders);
            }
        }
    }
}
