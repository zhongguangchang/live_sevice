package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.ServiceOrderPageQueryDTO;
import com.sky.entity.ServiceOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 服务订单 Mapper
 * <p>
 * 这个 Mapper 里最重要的方法是 updateStatusIfMatch，
 * 它是整个订单系统幂等性的基础，详见方法注释。
 */
@Mapper
public interface ServiceOrderMapper {

    /**
     * 插入订单，主键回填
     */
    void insert(ServiceOrder serviceOrder);

    /**
     * 动态修改订单（不带状态校验，仅用于内部确定安全的地方）
     */
    void update(ServiceOrder serviceOrder);

    /**
     * 【幂等核心】带前置状态校验的状态更新
     * <p>
     * 为什么必须有这个方法：本项目多个入口都会推送同一个订单的状态，
     * 包括用户主动支付、微信支付回调、MQ 延迟消息、定时对账任务。
     * 这些入口可能重复触发（RabbitMQ 是至少一次投递，微信回调也会重试），
     * 如果用普通的 update，同一个订单会被反复处理，比如名额被重复回补。
     * <p>
     * 这里的做法是把期望的当前状态写进 where 条件：
     * {@code where id = ? and status = ?}
     * 只有订单确实处于预期状态时才更新，否则影响行数为 0。
     * 调用方通过判断返回值来决定要不要继续后续动作（回补名额、发消息等）。
     * <p>
     * 这就是用数据库的行锁做 CAS，比分布式锁更轻量，也更可靠。
     *
     * @param order        要更新的目标状态与时间戳
     * @param expectStatus 期望的当前状态
     * @return 影响行数，1 表示更新成功可以继续，0 表示已被其他线程处理过
     */
    int updateStatusIfMatch(@Param("order") ServiceOrder order,
                            @Param("expectStatus") Integer expectStatus);

    @Select("select * from service_order where id = #{id}")
    ServiceOrder getById(Long id);

    @Select("select * from service_order where number = #{number}")
    ServiceOrder getByNumber(String number);

    /**
     * 分页条件查询
     */
    Page<ServiceOrder> pageQuery(ServiceOrderPageQueryDTO serviceOrderPageQueryDTO);

    /**
     * 按条件统计订单数量，map 可传 status、begin、end
     */
    Integer countByMap(Map<String, Object> map);

    /**
     * 按条件统计营业额，map 可传 status、begin、end
     */
    Double sumByMap(Map<String, Object> map);

    /**
     * 按状态统计订单数（管理端工作台的四宫格）
     */
    @Select("select count(id) from service_order where status = #{status}")
    Integer countByStatus(Integer status);

    /**
     * 查询指定状态且下单时间早于某时刻的订单
     * <p>
     * 给定时对账任务做兜底扫尾用：防止 MQ 消息丢失导致订单永远停在待付款。
     */
    @Select("select * from service_order where status = #{status} and order_time < #{orderTime}")
    List<ServiceOrder> getByStatusAndOrderTimeLT(@Param("status") Integer status,
                                                 @Param("orderTime") LocalDateTime orderTime);

    /**
     * 查询指定状态且某时间字段早于某时刻的订单（自动好评兜底用）
     */
    @Select("select * from service_order where status = #{status} and finish_time < #{time}")
    List<ServiceOrder> getByStatusAndFinishTimeLT(@Param("status") Integer status,
                                                  @Param("time") LocalDateTime time);

    /**
     * 查询某师傅在指定时间段内是否已经有在身的订单（派单时判断档期冲突）
     * <p>
     * <b>为什么不能按 slot_id 判重：</b>一个 slot 只属于一个师傅，
     * 拿「师傅 + slot」当条件时，同一个师傅几乎不可能出现两条记录，
     * 这个过滤等于失效——订单会被派给那个时间段其实正在别人家干活的师傅。
     * <p>
     * 真正要判断的是<b>时间是否重叠</b>：只要该师傅 09:00-11:00 已经有单，
     * 那么 10:00-12:00 的单就不能再派给他。
     * <p>
     * 只统计 2~5（待接单、已接单、服务中、待评价）的活跃订单，
     * 已完成和已取消的不占用师傅的时间。
     */
    @Select("select count(1) from service_order o " +
            "inner join slot s on o.slot_id = s.id " +
            "where o.provider_id = #{providerId} and o.status in (2, 3, 4, 5) " +
            "and s.service_date = #{serviceDate} " +
            "and s.start_time < #{endTime} and s.end_time > #{startTime}")
    Integer countProviderBusy(@Param("providerId") Long providerId,
                              @Param("serviceDate") LocalDate serviceDate,
                              @Param("startTime") LocalTime startTime,
                              @Param("endTime") LocalTime endTime);

    /**
     * 统计销量前 N 的服务项目（报表用）
     * <p>
     * 注意这里的统计口径：只算已完成和待评价的订单，
     * 未付款和已取消的不计入销量。
     */
    List<Map<String, Object>> getServiceSalesTopN(@Param("begin") LocalDateTime begin,
                                                  @Param("end") LocalDateTime end,
                                                  @Param("limit") Integer limit);

    /**
     * 强制修正订单状态（对账兜底专用，慎用）
     */
    @Update("update service_order set status = #{status} where id = #{orderId}")
    void forceUpdateStatus(@Param("orderId") Long orderId, @Param("status") Integer status);
}
