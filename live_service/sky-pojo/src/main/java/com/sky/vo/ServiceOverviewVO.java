package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 服务资源概览（管理端工作台用）
 * <p>
 * 对应原项目的 DishOverViewVO / SetmealOverViewVO，
 * 这里合并成一个，避免 VO 类膨胀。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOverviewVO implements Serializable {

    //已起售服务项目数
    private Integer soldServiceCount;

    //已停售服务项目数
    private Integer discontinuedServiceCount;

    //已启用套餐数
    private Integer enabledPackageCount;

    //已停用套餐数
    private Integer disabledPackageCount;

    //可接单师傅数
    private Integer availableProviderCount;

    //师傅总数
    private Integer totalProviderCount;
}
