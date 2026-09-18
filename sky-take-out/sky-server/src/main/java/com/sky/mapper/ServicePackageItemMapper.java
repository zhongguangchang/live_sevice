package com.sky.mapper;

import com.sky.entity.ServicePackageItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 服务套餐明细 Mapper
 */
@Mapper
public interface ServicePackageItemMapper {

    void insertBatch(@Param("items") List<ServicePackageItem> items);

    @Delete("delete from service_package_item where service_package_id = #{packageId}")
    void deleteByPackageId(Long packageId);

    void deleteByPackageIds(@Param("packageIds") List<Long> packageIds);

    @Select("select * from service_package_item where service_package_id = #{packageId}")
    List<ServicePackageItem> listByPackageId(Long packageId);

    List<ServicePackageItem> listByPackageIds(@Param("packageIds") List<Long> packageIds);

    /**
     * 统计某个服务项目被多少个套餐引用
     * <p>
     * 删除服务项目前的校验：被套餐引用的不允许删，对应原项目的
     * DeletionNotAllowedException 逻辑。
     */
    @Select("select count(id) from service_package_item where service_id = #{serviceId}")
    Integer countByServiceId(Long serviceId);

    /**
     * 查询引用了指定服务项目的所有套餐id（批量删除时用）
     */
    @Select("<script>" +
            "select distinct service_package_id from service_package_item where service_id in " +
            "<foreach collection='serviceIds' item='sid' open='(' separator=',' close=')'>#{sid}</foreach>" +
            "</script>")
    List<Long> listPackageIdsByServiceIds(@Param("serviceIds") List<Long> serviceIds);
}
