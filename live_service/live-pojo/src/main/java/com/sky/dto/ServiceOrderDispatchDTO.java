package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 派单入参（管理端把订单指派给师傅）
 * <p>
 * providerId 可以为空，为空表示走自动派单算法：
 * 技能过滤 -> 状态过滤 -> 档期过滤 -> 按评分和负载排序。
 */
@Data
public class ServiceOrderDispatchDTO implements Serializable {

    //订单id
    private Long orderId;

    //指定的服务人员id，为空则自动派单
    private Long providerId;
}
