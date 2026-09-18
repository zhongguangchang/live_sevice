package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 各状态订单数量统计（管理端工作台用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrderStatisticsVO implements Serializable {

    //待接单数量（已支付，等待派单）
    private Integer toBeAccepted;

    //已接单数量（师傅已接，等待上门）
    private Integer accepted;

    //服务中数量
    private Integer inService;

    //待评价数量
    private Integer toBeReviewed;
}
