package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 服务套餐明细（套餐里包含哪些服务项目、各几份）
 * <p>
 * 对应原项目的 SetmealDish。name 和 price 是冗余快照，
 * 防止服务改名或调价后，历史套餐的展示内容跟着错乱。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicePackageItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //套餐id
    private Long servicePackageId;

    //服务项目id
    private Long serviceId;

    //服务名称（冗余快照）
    private String name;

    //服务单价（冗余快照）
    private BigDecimal price;

    //份数
    private Integer copies;
}
