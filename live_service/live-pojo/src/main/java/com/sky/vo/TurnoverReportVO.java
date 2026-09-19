package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 营业额统计结果
 * <p>
 * 两个 List 的长度一一对应，下标 i 表示同一天：
 * {@code dateList[i]} 这天的营业额是 {@code turnoverList[i]}。
 * <p>
 * 为什么不用「日期 - 金额」的 Map：前端图表（ECharts）需要的是两个平行数组，
 * 而且没有营业额的那天必须补 0 —— 补 0 的逻辑放在服务层统一做，
 * 前端拿到就能直接画，不用自己对齐日期。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurnoverReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 日期列表，格式 yyyy-MM-dd */
    private List<String> dateList;

    /** 每天的营业额 */
    private List<BigDecimal> turnoverList;
}
