package com.sky.service;

import com.sky.vo.BusinessOverviewVO;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

/**
 * 统计报表业务接口
 */
public interface ReportService {

    /**
     * 营业额统计（按天）
     */
    TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end);

    /**
     * 用户统计（新增 + 累计）
     */
    UserReportVO userStatistics(LocalDate begin, LocalDate end);

    /**
     * 订单统计（总量、有效量、完成率）
     */
    OrderReportVO orderStatistics(LocalDate begin, LocalDate end);

    /**
     * 服务销量 Top10
     */
    SalesTop10ReportVO salesTop10(LocalDate begin, LocalDate end);

    /**
     * 经营概览（工作台首屏）
     */
    BusinessOverviewVO overview();

    /**
     * 导出运营数据报表（Excel）
     * <p>
     * 直接往 response 里写二进制流，不走 Result 包装 ——
     * 前端拿到的应该是文件下载，而不是一段 JSON。
     */
    void exportBusinessData(HttpServletResponse response, LocalDate begin, LocalDate end);
}
