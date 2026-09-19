package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.BusinessOverviewVO;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

/**
 * 统计报表（管理端）
 * <p>
 * 日期参数统一用 ISO 格式（yyyy-MM-dd），并且显式标注
 * {@code @DateTimeFormat} —— 不标的话 Spring 会用本地化格式解析，
 * 前端传 2026-09-19 会直接 400。
 */
@RestController("adminReportController")
@RequestMapping("/admin/report")
@Api(tags = "统计报表接口")
@Slf4j
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 工作台首屏的经营概览
     */
    @GetMapping("/overview")
    @ApiOperation("经营数据概览")
    public Result<BusinessOverviewVO> overview() {
        return Result.success(reportService.overview());
    }

    @GetMapping("/turnoverStatistics")
    @ApiOperation("营业额统计")
    public Result<TurnoverReportVO> turnoverStatistics(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        log.info("营业额统计：{} ~ {}", begin, end);
        return Result.success(reportService.turnoverStatistics(begin, end));
    }

    @GetMapping("/userStatistics")
    @ApiOperation("用户统计")
    public Result<UserReportVO> userStatistics(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        log.info("用户统计：{} ~ {}", begin, end);
        return Result.success(reportService.userStatistics(begin, end));
    }

    @GetMapping("/ordersStatistics")
    @ApiOperation("订单统计")
    public Result<OrderReportVO> ordersStatistics(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        log.info("订单统计：{} ~ {}", begin, end);
        return Result.success(reportService.orderStatistics(begin, end));
    }

    @GetMapping("/top10")
    @ApiOperation("服务销量排名 Top10")
    public Result<SalesTop10ReportVO> top10(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        log.info("销量排名统计：{} ~ {}", begin, end);
        return Result.success(reportService.salesTop10(begin, end));
    }

    /**
     * 导出运营数据报表。
     * <p>
     * 这个接口返回的是 Excel 二进制流，不是 Result 包装的 JSON，
     * 所以用 void 作为返回值，直接往 response 里写。
     */
    @GetMapping("/export")
    @ApiOperation("导出运营数据报表")
    public void export(HttpServletResponse response,
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        log.info("导出运营数据报表：{} ~ {}", begin, end);
        reportService.exportBusinessData(response, begin, end);
    }
}
