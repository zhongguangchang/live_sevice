package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 订单统计结果
 * <p>
 * 「有效订单」的口径：服务真的做完了（状态是待评价或已完成）。
 * 待付款、待接单、服务中、已取消都不算 ——
 * 只看下了多少单没意义，要看有多少单真的交付了。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 日期列表，格式 yyyy-MM-dd */
    private List<String> dateList;

    /** 每天的订单总数 */
    private List<Integer> orderCountList;

    /** 每天的有效订单数 */
    private List<Integer> validOrderCountList;

    /** 区间内订单总数 */
    private Integer totalOrderCount;

    /** 区间内有效订单数 */
    private Integer validOrderCount;

    /** 订单完成率（有效订单 / 订单总数） */
    private Double orderCompletionRate;
}
