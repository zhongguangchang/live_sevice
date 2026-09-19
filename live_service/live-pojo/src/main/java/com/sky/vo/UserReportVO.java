package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 用户统计结果
 * <p>
 * 新增用户和累计用户是两个不同的量：
 * 新增看的是拉新能力，累计看的是盘子有多大。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 日期列表，格式 yyyy-MM-dd */
    private List<String> dateList;

    /** 每天的新增用户数 */
    private List<Integer> newUserList;

    /** 截至当天的累计用户数 */
    private List<Integer> totalUserList;
}
