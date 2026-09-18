package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务人员视图对象
 * <p>
 * 比实体多带技能分类名称，管理端列表和用户端师傅详情页共用。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderVO implements Serializable {

    private Long id;

    private String name;

    private String phone;

    private String avatar;

    private String sex;

    private String certImage;

    //从业年限
    private Integer workYears;

    //个人简介
    private String intro;

    //可提供的服务方式
    private Integer serviceMode;

    //接单状态 1可接单 2忙碌 3休息中 4已离职
    private Integer status;

    //综合评分
    private BigDecimal score;

    //好评率
    private BigDecimal goodRate;

    //累计完成订单数
    private Integer orderCount;

    //擅长的服务分类id列表
    private List<Long> categoryIds = new ArrayList<>();

    //擅长的服务分类名称列表
    private List<String> categoryNames = new ArrayList<>();
}
