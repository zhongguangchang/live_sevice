package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 服务规格（服务项目的可选项）
 * <p>
 * 对应原项目的 DishFlavor 菜品口味。区别在于新增了 priceDelta——
 * 外卖的「微辣/中辣」不影响价格，但生活服务的「60平方米以下/90平方米以上」
 * 是要加价的，所以规格必须能带价差。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceSpec implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //所属服务项目id
    private Long serviceId;

    //规格名（如：房屋面积）
    private String name;

    //规格值（如：60-90平方米）
    private String value;

    //加价金额（正数加价，0为不加价）
    private BigDecimal priceDelta;

    //同规格下的排序
    private Integer sort;
}
