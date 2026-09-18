package com.sky.vo;

import com.sky.entity.ServicePackageItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务套餐视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicePackageVO implements Serializable {

    private Long id;

    private Long categoryId;

    private String name;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private String image;

    private String description;

    private Integer duration;

    private Integer serviceMode;

    private Integer sales;

    private Integer status;

    private LocalDateTime updateTime;

    //分类名称
    private String categoryName;

    //套餐包含的服务项目
    private List<ServicePackageItem> items = new ArrayList<>();
}
