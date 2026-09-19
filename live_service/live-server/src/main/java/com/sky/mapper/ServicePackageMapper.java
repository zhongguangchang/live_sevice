package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.ServicePackagePageQueryDTO;
import com.sky.entity.ServicePackage;
import com.sky.enumeration.OperationType;
import com.sky.vo.ServicePackageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 服务套餐 Mapper
 */
@Mapper
public interface ServicePackageMapper {

    @AutoFill(value = OperationType.INSERT)
    void insert(ServicePackage servicePackage);

    @AutoFill(value = OperationType.UPDATE)
    void update(ServicePackage servicePackage);

    void deleteByIds(@Param("ids") List<Long> ids);

    @Select("select * from service_package where id = #{id}")
    ServicePackage getById(Long id);

    Page<ServicePackageVO> pageQuery(ServicePackagePageQueryDTO servicePackagePageQueryDTO);

    List<ServicePackage> list(ServicePackage servicePackage);

    /**
     * 统计某分类下的套餐数量（删除分类前校验用）
     */
    @Select("select count(id) from service_package where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    Integer countByMap(Map<String, Object> map);

    @Update("update service_package set sales = sales + #{count} where id = #{packageId}")
    void increaseSales(@Param("packageId") Long packageId, @Param("count") Integer count);

    @Update("<script>" +
            "update service_package set status = #{status} where id in " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    void updateStatusByIds(@Param("ids") List<Long> ids, @Param("status") Integer status);
}
