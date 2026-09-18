package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 服务订单明细
 * <p>
 * 对应原项目的 OrderDetail。name / price / image 全部是下单瞬间的快照，
 * 这样运营改价、换图、改名字都不会影响历史订单的展示。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //订单id
    private Long orderId;

    //服务项目id
    private Long serviceId;

    //服务套餐id
    private Long servicePackageId;

    //服务名称快照
    private String name;

    //选中的规格快照，JSON串
    private String spec;

    //单价快照（含规格加价）
    private BigDecimal price;

    //数量
    private Integer number;

    //小计金额 = price * number
    private BigDecimal amount;

    //图片快照
    private String image;

    //单项服务时长（分钟）
    private Integer duration;
}
