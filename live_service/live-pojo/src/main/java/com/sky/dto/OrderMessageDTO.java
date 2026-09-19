package com.sky.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单相关消息的统一消息体（走 MQ 传递）
 * <p>
 * 四种延迟场景共用这一个结构，靠 type 字段区分：
 * <ul>
 *   <li>ORDER_TIMEOUT —— 订单超时未支付，要取消</li>
 *   <li>SERVICE_REMIND —— 服务开始前 1 小时提醒</li>
 *   <li>AUTO_REVIEW —— 服务完成 7 天后用户仍未评价，自动结单</li>
 *   <li>DISPATCH_TIMEOUT —— 派单后师傅 5 分钟未接，转派</li>
 * </ul>
 * <p>
 * 为什么不让消息体直接传订单对象：消息里应该只放「让消费者能定位到
 * 唯一一条数据」的最小信息，真正处理时消费者自己去数据库查最新状态。
 * 否则消息发出时的快照和消费时的实际状态可能不一致，
 * 引发「明明已经付过款了却被判超时」这类问题。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    //消息类型
    private String type;

    //订单id
    private Long orderId;

    //订单号
    private String orderNumber;

    //预约的时段id
    private Long slotId;

    //下单用户id
    private Long userId;

    //服务人员id
    private Long providerId;

    //预约的服务时间
    private LocalDateTime serviceTime;

    //消息发出时间
    private LocalDateTime sendTime;
}
