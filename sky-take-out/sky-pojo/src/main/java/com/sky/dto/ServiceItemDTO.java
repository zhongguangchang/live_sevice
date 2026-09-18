package com.sky.dto;

import com.sky.entity.ServiceSpec;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务项目新增/编辑入参
 * <p>
 * 对应原项目的 DishDTO，额外带了规格列表（ServiceSpec）。
 * 编辑时采用「先删后插」策略处理规格，和原项目处理口味的方式一致。
 */
@Data
public class ServiceItemDTO implements Serializable {

    private Long id;

    //服务名称
    private String name;

    //服务分类id
    private Long categoryId;

    //服务价格
    private BigDecimal price;

    //原价（划线价）
    private BigDecimal originalPrice;

    //服务图片url
    private String image;

    //服务描述
    private String description;

    //服务详情（富文本）
    private String detail;

    //计价单位 次/小时/平方米/台
    private String unit;

    //服务时长（分钟）
    private Integer duration;

    //服务方式 1上门 2到店 3都支持
    private Integer serviceMode;

    //是否需要预约 0否 1是
    private Integer needAppoint;

    //状态 0停售 1起售
    private Integer status;

    //服务规格（如：房屋面积 60-90平方米 +80元）
    private List<ServiceSpec> specs = new ArrayList<>();
}
