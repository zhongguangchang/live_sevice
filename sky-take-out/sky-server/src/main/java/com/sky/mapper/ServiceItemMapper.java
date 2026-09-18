package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.ServiceItemPageQueryDTO;
import com.sky.entity.ServiceItem;
import com.sky.enumeration.OperationType;
import com.sky.vo.ServiceItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 服务项目 Mapper
 * <p>
 * 对应原项目的 DishMapper。公共字段（create_time/update_time/create_user/update_user）
 * 由 {@link com.sky.aspect.AutoFillAspect} 自动填充。
 */
@Mapper
public interface ServiceItemMapper {

    /**
     * 新增服务项目
     */
    @AutoFill(value = OperationType.INSERT)
    void insert(ServiceItem serviceItem);

    /**
     * 动态修改服务项目
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(ServiceItem serviceItem);

    /**
     * 批量删除服务项目
     */
    void deleteByIds(@Param("ids") List<Long> ids);

    /**
     * 根据主键查询
     */
    @Select("select * from service_item where id = #{id}")
    ServiceItem getById(Long id);

    /**
     * 分页条件查询，联表带出分类名称
     */
    Page<ServiceItemVO> pageQuery(ServiceItemPageQueryDTO serviceItemPageQueryDTO);

    /**
     * 动态条件查询列表
     */
    List<ServiceItem> list(ServiceItem serviceItem);

    /**
     * 统计某分类下的服务项目数量（删除分类前校验用）
     */
    @Select("select count(id) from service_item where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * 按条件统计数量，map 可传 status、categoryId
     */
    Integer countByMap(Map<String, Object> map);

    /**
     * 查询某分类下已起售的服务项目（用户端用），按销量倒序
     */
    @Select("select * from service_item where category_id = #{categoryId} and status = 1 order by sales desc")
    List<ServiceItem> listOnSaleByCategoryId(Long categoryId);

    /**
     * 增加销量（订单完成时回写）
     * <p>
     * 用 sales = sales + n 而不是先查再写，避免并发下丢失更新
     */
    @Update("update service_item set sales = sales + #{count} where id = #{serviceId}")
    void increaseSales(@Param("serviceId") Long serviceId, @Param("count") Integer count);

    /**
     * 回写综合评分（评价提交时调用）
     */
    @Update("update service_item set score = #{score} where id = #{serviceId}")
    void updateScore(@Param("serviceId") Long serviceId, @Param("score") BigDecimal score);

    /**
     * 批量修改状态（起售/停售）
     */
    @Update("<script>" +
            "update service_item set status = #{status} where id in " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    void updateStatusByIds(@Param("ids") List<Long> ids, @Param("status") Integer status);
}
