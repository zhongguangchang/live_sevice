package com.sky.vo;

import com.sky.entity.ServiceSpec;
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
 * 服务项目视图对象
 * <p>
 * 管理端列表和服务详情页共用。比实体多带了分类名称和规格列表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceItemVO implements Serializable {

    private Long id;

    private String name;

    private Long categoryId;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private String image;

    private String description;

    private String detail;

    private String unit;

    //服务时长（分钟）
    private Integer duration;

    //服务方式 1上门 2到店 3都支持
    private Integer serviceMode;

    //是否需要预约 0否 1是
    private Integer needAppoint;

    //累计销量
    private Integer sales;

    //综合评分
    private BigDecimal score;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    //分类名称（联表查询得到）
    private String categoryName;

    //服务规格列表
    private List<ServiceSpec> specs = new ArrayList<>();
}
