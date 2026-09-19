package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 经营概览（工作台首屏用）
 * <p>
 * 工作台原来要调四个接口、在前端各取一个字段拼出来，
 * 现在收口成一个接口：一次请求拿到今日经营数据、待处理订单、
 * 平台资源数量，以及近 7 天的营业额趋势。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessOverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ---------- 今日 ----------

    /** 今日营业额 */
    private BigDecimal todayTurnover;

    /** 今日新增订单数 */
    private Integer todayOrderCount;

    /** 今日新增用户数 */
    private Integer todayUserCount;

    // ---------- 累计 ----------

    /** 累计营业额 */
    private BigDecimal totalTurnover;

    /** 累计订单数 */
    private Integer totalOrderCount;

    /** 累计用户数 */
    private Integer totalUserCount;

    /** 整体订单完成率（有效订单 / 总订单） */
    private Double orderCompletionRate;

    // ---------- 待处理 ----------

    /** 待接单 */
    private Integer toBeAccepted;

    /** 已接单 */
    private Integer accepted;

    /** 服务中 */
    private Integer inService;

    /** 待评价 */
    private Integer toBeReviewed;

    /** 已完成 */
    private Integer completed;

    /** 已取消 */
    private Integer cancelled;

    // ---------- 平台资源 ----------

    /** 可接单的服务人员数 */
    private Integer providerCount;

    /** 在售服务项目数 */
    private Integer serviceItemCount;

    /** 已启用的服务套餐数 */
    private Integer servicePackageCount;

    // ---------- 近 7 天营业额趋势 ----------

    /** 近 7 天日期，格式 MM-dd */
    private List<String> recentDateList;

    /** 近 7 天每天的营业额 */
    private List<BigDecimal> recentTurnoverList;
}
