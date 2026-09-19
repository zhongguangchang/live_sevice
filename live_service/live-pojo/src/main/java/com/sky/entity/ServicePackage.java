package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务套餐（多个服务项目打包优惠售卖）
 * <p>
 * 对应原项目的 Setmeal。例如「新居开荒保洁套餐 = 深度保洁 + 玻璃清洗 + 甲醛检测」。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicePackage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //服务套餐分类id
    private Long categoryId;

    //套餐名称
    private String name;

    //套餐价格
    private BigDecimal price;

    //套餐原价（各项单买之和，用于展示优惠力度）
    private BigDecimal originalPrice;

    //套餐图片url
    private String image;

    //套餐描述
    private String description;

    //套餐总时长（分钟）
    private Integer duration;

    //服务方式 1上门 2到店 3都支持
    private Integer serviceMode;

    //累计销量
    private Integer sales;

    //状态 0停用 1启用
    private Integer status;

    //商户id（预留多商户）
    private Long merchantId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;
}
