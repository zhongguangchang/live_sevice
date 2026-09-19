package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 统计报表 Mapper
 * <p>
 * <b>为什么单独建一个 Mapper，而不是塞进 ServiceOrderMapper：</b>
 * 报表用的都是「按日期分组聚合」的 SQL，和业务查询的形态完全不同；
 * 而且报表的口径（什么算营业额、什么算有效订单）是业务决策，
 * 集中放在一个文件里，改口径时只改一处，不会漏。
 * <p>
 * <b>口径统一说明（全平台只有这一套定义）：</b>
 * <ul>
 *   <li><b>营业额</b>：已付款、且服务确实完成了的订单（状态 5 待评价 / 6 已完成），
 *       按支付日期归集。未付款、已取消、还在服务中的都不算</li>
 *   <li><b>有效订单</b>：同上，即状态 5 或 6 的订单</li>
 *   <li><b>订单完成率</b>：有效订单数 / 订单总数</li>
 * </ul>
 * 为什么营业额要用「支付日期」而不是「下单日期」：
 * 下单不代表收到钱，跨天支付时按下单日统计会让报表和实际到账对不上。
 */
@Mapper
public interface ReportMapper {

    /**
     * 按天统计营业额
     *
     * @return 每行两个键：date（yyyy-MM-dd）、value（当天营业额）
     */
    List<Map<String, Object>> turnoverByDate(@Param("begin") LocalDateTime begin,
                                             @Param("end") LocalDateTime end);

    /**
     * 按天统计新增用户数
     *
     * @return 每行两个键：date、value
     */
    List<Map<String, Object>> newUserByDate(@Param("begin") LocalDateTime begin,
                                            @Param("end") LocalDateTime end);

    /**
     * 按天统计订单总数与有效订单数
     *
     * @return 每行三个键：date、total（总单量）、valid（有效单量）
     */
    List<Map<String, Object>> orderCountByDate(@Param("begin") LocalDateTime begin,
                                               @Param("end") LocalDateTime end);

    /**
     * 销量 Top N（按服务项目聚合）
     *
     * @return 每行两个键：name（服务名）、value（销量）
     */
    List<Map<String, Object>> salesTopN(@Param("begin") LocalDateTime begin,
                                        @Param("end") LocalDateTime end,
                                        @Param("limit") Integer limit);

    /**
     * 统计区间开始之前的总用户数
     * <p>
     * 累计用户数不能只统计区间内的，否则「累计」两个字就没有意义了。
     * 用「区间前的存量 + 区间内逐日累加」才能得到真实的累计曲线。
     */
    Integer countUserBefore(@Param("before") LocalDateTime before);

    /**
     * 统计区间开始之前的总订单数（数据看板的累计口径用）
     */
    Integer countOrderBefore(@Param("before") LocalDateTime before);

    /**
     * 有效订单数（状态 5 待评价 / 6 已完成）
     * <p>
     * 传 null 表示不限时间。之所以单独写一条 SQL，而不是用
     * ServiceOrderMapper.countByMap —— 后者只支持「单个状态」，
     * 而有效订单是两个状态的并集，用两次查询相加既啰嗦又容易漏。
     */
    Integer countValidOrder(@Param("begin") LocalDateTime begin,
                            @Param("end") LocalDateTime end);

    /**
     * 营业额合计（已付款且服务完成），传 null 表示不限时间
     */
    java.math.BigDecimal sumTurnover(@Param("begin") LocalDateTime begin,
                                     @Param("end") LocalDateTime end);
}
