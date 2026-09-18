package com.sky.mapper;

import com.sky.entity.ServiceOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 服务订单明细 Mapper
 */
@Mapper
public interface ServiceOrderItemMapper {

    /**
     * 批量插入订单明细
     */
    void insertBatch(@Param("items") List<ServiceOrderItem> items);

    /**
     * 根据订单id查询明细
     */
    @Select("select * from service_order_item where order_id = #{orderId}")
    List<ServiceOrderItem> listByOrderId(Long orderId);

    /**
     * 批量查询多个订单的明细（管理端列表页避免 N+1 查询）
     */
    List<ServiceOrderItem> listByOrderIds(@Param("orderIds") List<Long> orderIds);
}
