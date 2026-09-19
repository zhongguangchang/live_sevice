package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务项目（平台售卖的最小服务单元）
 * <p>
 * 对应原项目的 Dish 菜品，语义从「一份外卖」变成「一次上门/到店服务」。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 服务方式：上门服务 */
    public static final Integer MODE_HOME = 1;
    /** 服务方式：到店服务 */
    public static final Integer MODE_STORE = 2;
    /** 服务方式：两者都支持 */
    public static final Integer MODE_BOTH = 3;

    private Long id;

    //服务名称（如：深度保洁3小时）
    private String name;

    //服务分类id
    private Long categoryId;

    //服务价格
    private BigDecimal price;

    //原价（划线价，用于展示优惠）
    private BigDecimal originalPrice;

    //服务图片url
    private String image;

    //服务描述
    private String description;

    //服务详情（富文本，说明服务内容/注意事项）
    private String detail;

    //计价单位 次/小时/平方米/台
    private String unit;

    //服务时长（分钟），用于排期时段占用计算
    private Integer duration;

    //服务方式 1上门服务 2到店服务 3两者都支持
    private Integer serviceMode;

    //是否需要预约 0否 1是
    private Integer needAppoint;

    //累计销量（冗余字段，列表展示与排序用）
    private Integer sales;

    //综合评分（冗余字段，由评价模块回写）
    private BigDecimal score;

    //状态 0停售 1起售
    private Integer status;

    //商户id（预留多商户，单商户固定为1）
    private Long merchantId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;
}
