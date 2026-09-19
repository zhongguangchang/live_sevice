package com.sky.task;

import com.sky.entity.ServiceOrder;
import com.sky.mapper.ServiceOrderMapper;
import com.sky.service.ServiceOrderService;
import com.sky.service.SlotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

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
    }

    /**
     * 兜底自动结单
     * <p>
     * 服务完成 7 天后用户仍未评价，自动推进到已完成。
     * 不这么做的话订单会永远停在「待评价」，统计口径就乱了。
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void autoCompleteStaleOrders() {
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
    }

    /**
     * Redis 与 MySQL 库存对账
     * <p>
     * 修正两层数据不一致的情况：Redis 重启丢数据、key 过期、
     * 网络分区导致回补失败等等。以 MySQL 为权威。
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void reconcileSlotStock() {
        try {
            int fixed = slotService.reconcile();
            if (fixed > 0) {
                log.warn("库存对账任务修正了 {} 个时段", fixed);
            }
        } catch (Exception e) {
            log.error("库存对账任务执行失败", e);
        }
    }
}
