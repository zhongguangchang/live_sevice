package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务清单（对应表 shopping_cart）
 * <p>
 * 替代原项目的 ShoppingCart。严格说生活服务不叫「购物车」，
 * 叫「待预约清单」更贴切——用户先把几个服务放进来，最后一起选时段提交。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCart implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //服务名称（冗余快照）
    private String name;

    //用户id
    private Long userId;

    //服务项目id（与套餐id二选一）
    private Long serviceId;

    //服务套餐id（与项目id二选一）
    private Long servicePackageId;

    //选中的规格，JSON串，如 {"房屋面积":"60-90平方米"}
    private String spec;

    //数量
    private Integer number;

    //金额（含规格加价后的实际单价）
    private BigDecimal amount;

    //图片
    private String image;

    private LocalDateTime createTime;
}
