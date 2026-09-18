package com.sky.dto;

import com.sky.entity.ServicePackageItem;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务套餐新增/编辑入参
 */
@Data
public class ServicePackageDTO implements Serializable {

    private Long id;

    //服务套餐分类id
    private Long categoryId;

    //套餐名称
    private String name;

    //套餐价格
    private BigDecimal price;

    //套餐原价（各项单买之和）
    private BigDecimal originalPrice;

    //套餐图片url
    private String image;

    //套餐描述
    private String description;

    //套餐总时长（分钟）
    private Integer duration;

    //服务方式 1上门 2到店 3都支持
    private Integer serviceMode;

    //状态 0停用 1启用
    private Integer status;

    //套餐包含的服务项目
    private List<ServicePackageItem> items = new ArrayList<>();
}
