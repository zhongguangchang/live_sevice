package com.sky.mapper;

import com.sky.entity.ServiceSpec;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 服务规格 Mapper
 * <p>
 * 对应原项目的 DishFlavorMapper。编辑服务项目时采用「先按 serviceId 全删、
 * 再批量插入」的策略，和原项目处理口味的思路一致，简单可靠。
 */
@Mapper
public interface ServiceSpecMapper {

    /**
     * 批量插入规格
     */
    void insertBatch(@Param("specs") List<ServiceSpec> specs);

    /**
     * 根据服务项目id删除其全部规格
     */
    @Delete("delete from service_spec where service_id = #{serviceId}")
    void deleteByServiceId(Long serviceId);

    /**
     * 批量删除多个服务项目的规格
     */
    void deleteByServiceIds(@Param("serviceIds") List<Long> serviceIds);

    /**
     * 根据服务项目id查询规格
     */
    @Select("select * from service_spec where service_id = #{serviceId} order by sort")
    List<ServiceSpec> listByServiceId(Long serviceId);

    /**
     * 根据多个服务项目id批量查询规格（列表页避免 N+1 查询）
     */
    List<ServiceSpec> listByServiceIds(@Param("serviceIds") List<Long> serviceIds);
}
