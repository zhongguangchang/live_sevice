package com.sky.service.impl;

import com.sky.constant.StatusConstant;
import com.sky.entity.ServiceOrder;
import com.sky.mapper.ProviderMapper;
import com.sky.mapper.ReportMapper;
import com.sky.mapper.ServiceItemMapper;
import com.sky.mapper.ServiceOrderMapper;
import com.sky.mapper.ServicePackageMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.BusinessOverviewVO;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计报表业务实现
 * <p>
 * <b>这个类里最容易被忽略、但最影响体验的一点：补零。</b>
 * 数据库的 group by 只会返回「有数据的那几天」。如果 3 天里只有 2 天有订单，
 * 直接丢给前端画折线图，日期和金额就对不上了（前端会把这 2 个点按顺序
 * 摆在 3 个日期标签前面，看着像有一条莫名其妙的斜线）。
 * 所以这里统一按日期区间补齐，没有数据的那天补 0，两个数组严格等长。
 */
@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd");

    /** 销量排名的条数 */
    private static final int TOP_N = 10;

    @Autowired
    private ReportMapper reportMapper;
    @Autowired
    private ServiceOrderMapper serviceOrderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ProviderMapper providerMapper;
    @Autowired
    private ServiceItemMapper serviceItemMapper;
    @Autowired
    private ServicePackageMapper servicePackageMapper;

    // ========================================================================
    //  营业额
    // ========================================================================

    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        List<String> dateList = buildDateList(begin, end);

        Map<String, BigDecimal> turnoverMap = toMap(
                reportMapper.turnoverByDate(toBeginTime(begin), toEndTime(end)));

        List<BigDecimal> turnoverList = new ArrayList<>();
        for (String date : dateList) {
            turnoverList.add(turnoverMap.getOrDefault(date, BigDecimal.ZERO));
        }
        return TurnoverReportVO.builder()
                .dateList(dateList)
                .turnoverList(turnoverList)
                .build();
    }

    // ========================================================================
    //  用户
    // ========================================================================

    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        List<String> dateList = buildDateList(begin, end);

        Map<String, BigDecimal> newUserMap = toMap(
                reportMapper.newUserByDate(toBeginTime(begin), toEndTime(end)));

        // 累计用户 = 区间开始之前的存量 + 区间内逐日累加
        Integer before = reportMapper.countUserBefore(toBeginTime(begin));
        int cumulative = before == null ? 0 : before;

        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();
        for (String date : dateList) {
            int daily = newUserMap.getOrDefault(date, BigDecimal.ZERO).intValue();
            newUserList.add(daily);
            cumulative += daily;
            totalUserList.add(cumulative);
        }

        return UserReportVO.builder()
                .dateList(dateList)
                .newUserList(newUserList)
                .totalUserList(totalUserList)
                .build();
    }

    // ========================================================================
    //  订单
    // ========================================================================

    @Override
    public OrderReportVO orderStatistics(LocalDate begin, LocalDate end) {
        List<String> dateList = buildDateList(begin, end);

        Map<String, Map<String, Object>> rowMap = new HashMap<>();
        for (Map<String, Object> row : reportMapper.orderCountByDate(toBeginTime(begin), toEndTime(end))) {
            rowMap.put(String.valueOf(row.get("date")), row);
        }

        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        int totalOrderCount = 0;
        int validOrderCount = 0;

        for (String date : dateList) {
            Map<String, Object> row = rowMap.get(date);
            int total = row == null ? 0 : toInt(row.get("total"));
            int valid = row == null ? 0 : toInt(row.get("valid"));
            orderCountList.add(total);
            validOrderCountList.add(valid);
            totalOrderCount += total;
            validOrderCount += valid;
        }

        // 完成率保留两位小数。除数为 0 时直接给 0，不要让它抛异常
        double rate = totalOrderCount == 0 ? 0D
                : BigDecimal.valueOf(validOrderCount * 100.0 / totalOrderCount)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        return OrderReportVO.builder()
                .dateList(dateList)
                .orderCountList(orderCountList)
                .validOrderCountList(validOrderCountList)
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(rate)
                .build();
    }

    // ========================================================================
    //  销量排行
    // ========================================================================

    @Override
    public SalesTop10ReportVO salesTop10(LocalDate begin, LocalDate end) {
        List<Map<String, Object>> rows = reportMapper.salesTopN(
                toBeginTime(begin), toEndTime(end), TOP_N);

        List<String> nameList = new ArrayList<>();
        List<Integer> numberList = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            nameList.add(String.valueOf(row.get("name")));
            numberList.add(toInt(row.get("value")));
        }
        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

    // ========================================================================
    //  经营概览
    // ========================================================================

    @Override
    public BusinessOverviewVO overview() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayBegin = LocalDateTime.of(today, LocalTime.MIN);
        LocalDateTime now = LocalDateTime.now();

        // 今日营业额：只算已付款且服务完成的
        BigDecimal todayTurnover = zeroIfNull(reportMapper.sumTurnover(todayBegin, now));

        Integer todayOrderCount = serviceOrderMapper.countByMap(
                rangeMap(todayBegin, now));
        Integer todayUserCount = userMapper.countByMap(beginMap(todayBegin));

        BigDecimal totalTurnover = sumTurnover(null, null);
        Integer totalOrderCount = serviceOrderMapper.countByMap(new HashMap<>());
        Integer totalUserCount = userMapper.countByMap(new HashMap<>());

        Integer validOrderCount = reportMapper.countValidOrder(null, null);
        double completionRate = totalOrderCount == null || totalOrderCount == 0 ? 0D
                : BigDecimal.valueOf((validOrderCount == null ? 0 : validOrderCount) * 100.0 / totalOrderCount)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        // 近 7 天营业额趋势
        LocalDate weekBegin = today.minusDays(6);
        Map<String, BigDecimal> weekMap = toMap(
                reportMapper.turnoverByDate(toBeginTime(weekBegin), toEndTime(today)));
        List<String> recentDateList = new ArrayList<>();
        List<BigDecimal> recentTurnoverList = new ArrayList<>();
        for (LocalDate date = weekBegin; !date.isAfter(today); date = date.plusDays(1)) {
            String key = DATE_FORMAT.format(date);
            recentDateList.add(SHORT_DATE_FORMAT.format(date));
            recentTurnoverList.add(weekMap.getOrDefault(key, BigDecimal.ZERO));
        }

        Map<String, Object> providerQuery = new HashMap<>();
        providerQuery.put("status", StatusConstant.ENABLE);
        Map<String, Object> itemQuery = new HashMap<>();
        itemQuery.put("status", StatusConstant.ENABLE);
        Map<String, Object> packageQuery = new HashMap<>();
        packageQuery.put("status", StatusConstant.ENABLE);

        return BusinessOverviewVO.builder()
                .todayTurnover(todayTurnover)
                .todayOrderCount(todayOrderCount == null ? 0 : todayOrderCount)
                .todayUserCount(todayUserCount == null ? 0 : todayUserCount)
                .totalTurnover(totalTurnover)
                .totalOrderCount(totalOrderCount == null ? 0 : totalOrderCount)
                .totalUserCount(totalUserCount == null ? 0 : totalUserCount)
                .orderCompletionRate(completionRate)
                .toBeAccepted(countStatus(ServiceOrder.TO_BE_ACCEPTED))
                .accepted(countStatus(ServiceOrder.ACCEPTED))
                .inService(countStatus(ServiceOrder.IN_SERVICE))
                .toBeReviewed(countStatus(ServiceOrder.TO_BE_REVIEWED))
                .completed(countStatus(ServiceOrder.COMPLETED))
                .cancelled(countStatus(ServiceOrder.CANCELLED))
                .providerCount(providerMapper.countByMap(providerQuery))
                .serviceItemCount(serviceItemMapper.countByMap(itemQuery))
                .servicePackageCount(servicePackageMapper.countByMap(packageQuery))
                .recentDateList(recentDateList)
                .recentTurnoverList(recentTurnoverList)
                .build();
    }

    // ========================================================================
    //  Excel 导出
    // ========================================================================

    @Override
    public void exportBusinessData(HttpServletResponse response, LocalDate begin, LocalDate end) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("运营数据报表");
            sheet.setColumnWidth(0, 26 * 256);
            sheet.setColumnWidth(1, 16 * 256);
            sheet.setColumnWidth(2, 16 * 256);
            sheet.setColumnWidth(3, 16 * 256);
            sheet.setColumnWidth(4, 16 * 256);

            Font titleFont = workbook.createFont();
            titleFont.setFontName("黑体");
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setBold(true);
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);

            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(30);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("生活服务网 · 运营数据报表");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 4));

            Row rangeRow = sheet.createRow(1);
            rangeRow.createCell(0).setCellValue(
                    "统计时间：" + DATE_FORMAT.format(begin) + " 至 " + DATE_FORMAT.format(end));
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 4));

            // ---- 汇总 ----
            OrderReportVO order = orderStatistics(begin, end);
            UserReportVO users = userStatistics(begin, end);
            TurnoverReportVO turnover = turnoverStatistics(begin, end);
            BigDecimal totalTurnover = turnover.getTurnoverList().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int rowIndex = 3;
            rowIndex = writeSummary(sheet, rowIndex, "概览", new String[][]{
                    {"统计周期订单总数", String.valueOf(order.getTotalOrderCount())},
                    {"其中有效订单（服务已完成）", String.valueOf(order.getValidOrderCount())},
                    {"订单完成率", order.getOrderCompletionRate() + "%"},
                    {"营业额合计", totalTurnover.toPlainString() + " 元"},
                    {"新增用户数", String.valueOf(users.getNewUserList().stream().mapToInt(Integer::intValue).sum())}
            });

            // ---- 按天明细 ----
            rowIndex++;
            Row header = sheet.createRow(rowIndex++);
            String[] headers = {"日期", "营业额（元）", "订单数", "有效订单数", "新增用户数"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            for (int i = 0; i < order.getDateList().size(); i++) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(order.getDateList().get(i));
                row.createCell(1).setCellValue(turnover.getTurnoverList().get(i).doubleValue());
                row.createCell(2).setCellValue(order.getOrderCountList().get(i));
                row.createCell(3).setCellValue(order.getValidOrderCountList().get(i));
                row.createCell(4).setCellValue(users.getNewUserList().get(i));
            }

            // ---- 销量排行 ----
            rowIndex++;
            SalesTop10ReportVO top = salesTop10(begin, end);
            rowIndex = writeSummary(sheet, rowIndex, "服务销量 Top10",
                    toTopRows(top));

            String fileName = "life-service-report-"
                    + DATE_FORMAT.format(begin) + "_" + DATE_FORMAT.format(end) + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            // 文件名里有中文/特殊字符时，用 filename* 告诉浏览器按 UTF-8 解析
            response.setHeader("Content-Disposition",
                    "attachment; filename=" + fileName
                            + "; filename*=UTF-8''" + java.net.URLEncoder.encode(fileName, "UTF-8"));

            ServletOutputStream out = response.getOutputStream();
            workbook.write(out);
            out.flush();
            log.info("运营数据报表已导出：{} ~ {}", begin, end);
        } catch (IOException e) {
            log.error("导出运营数据报表失败：{} ~ {}", begin, end, e);
            throw new RuntimeException("导出报表失败：" + e.getMessage(), e);
        }
    }

    private int writeSummary(Sheet sheet, int rowIndex, String title, String[][] rows) {
        Row titleRow = sheet.createRow(rowIndex++);
        titleRow.createCell(0).setCellValue(title);
        for (String[] pair : rows) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(pair[0]);
            row.createCell(1).setCellValue(pair[1]);
        }
        return rowIndex;
    }

    private String[][] toTopRows(SalesTop10ReportVO top) {
        String[][] rows = new String[top.getNameList().size()][];
        for (int i = 0; i < top.getNameList().size(); i++) {
            rows[i] = new String[]{
                    "第 " + (i + 1) + " 名：" + top.getNameList().get(i),
                    String.valueOf(top.getNumberList().get(i))
            };
        }
        return rows;
    }

    // ========================================================================
    //  私有辅助
    // ========================================================================

    /**
     * 生成区间内的每一天（含首尾），保证前端拿到的两个数组严格等长
     */
    private List<String> buildDateList(LocalDate begin, LocalDate end) {
        List<String> list = new ArrayList<>();
        for (LocalDate date = begin; !date.isAfter(end); date = date.plusDays(1)) {
            list.add(DATE_FORMAT.format(date));
        }
        return list;
    }

    /**
     * 把「date -> 数值」的查询结果转成 Map
     * <p>
     * 用 LinkedHashMap 保持数据库返回的日期顺序，
     * 方便调试时一眼看出聚合结果；取值时按日期 key 查，不依赖顺序
     */
    private Map<String, BigDecimal> toMap(List<Map<String, Object>> rows) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        if (rows == null) {
            return map;
        }
        for (Map<String, Object> row : rows) {
            Object date = row.get("date");
            Object value = row.get("value");
            if (date == null) {
                continue;
            }
            map.put(String.valueOf(date), value == null ? BigDecimal.ZERO : new BigDecimal(value.toString()));
        }
        return map;
    }

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        return new BigDecimal(value.toString()).intValue();
    }

    private int countStatus(Integer status) {
        Integer count = serviceOrderMapper.countByStatus(status);
        return count == null ? 0 : count;
    }

    /**
     * 统计某段时间内「已付款且服务完成」的营业额
     * <p>
     * 传 null 表示不限时间（累计营业额）。
     * 口径统一走 ReportMapper.sumTurnover：时间是 checkout_time（支付时间）、
     * 状态是 5/6 的并集 —— 这样日报和累计报表永远对得上，
     * 不会出现「每天加起来不等于总数」这种最容易被当场问住的问题。
     */
    private BigDecimal sumTurnover(LocalDateTime begin, LocalDateTime end) {
        return zeroIfNull(reportMapper.sumTurnover(begin, end));
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Object> rangeMap(LocalDateTime begin, LocalDateTime end) {
        Map<String, Object> map = new HashMap<>();
        map.put("begin", begin);
        map.put("end", end);
        return map;
    }

    private Map<String, Object> beginMap(LocalDateTime begin) {
        Map<String, Object> map = new HashMap<>();
        map.put("begin", begin);
        return map;
    }

    /**
     * 区间起点：包含当天 00:00:00
     */
    private LocalDateTime toBeginTime(LocalDate date) {
        return LocalDateTime.of(date, LocalTime.MIN);
    }

    /**
     * 区间终点：用「次日 00:00:00」而不是「当天 23:59:59」
     * <p>
     * SQL 里统一用 {@code < 次日零点}，这样就不会漏掉 23:59:59.9 这种毫秒级数据
     */
    private LocalDateTime toEndTime(LocalDate date) {
        return LocalDateTime.of(date.plusDays(1), LocalTime.MIN);
    }
}
