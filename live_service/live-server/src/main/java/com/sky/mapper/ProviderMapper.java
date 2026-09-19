package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.ProviderPageQueryDTO;
import com.sky.entity.Provider;
import com.sky.enumeration.OperationType;
import com.sky.vo.ProviderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 服务人员（师傅）Mapper
 */
@Mapper
public interface ProviderMapper {

    @AutoFill(value = OperationType.INSERT)
    void insert(Provider provider);

    @AutoFill(value = OperationType.UPDATE)
    void update(Provider provider);

    @Select("select * from provider where id = #{id}")
    Provider getById(Long id);

    @Select("select * from provider where phone = #{phone}")
    Provider getByPhone(String phone);

    Page<ProviderVO> pageQuery(ProviderPageQueryDTO providerPageQueryDTO);

    List<Provider> list(Provider provider);

    Integer countByMap(Map<String, Object> map);

    /**
     * 派单核心：查询能做指定分类、且当前可接单的师傅，按评分倒序、接单数升序
     * <p>
     * 这是派单算法里「技能过滤 + 状态过滤 + 排序」合并成的一条 SQL。
     * 档期过滤需要在 Service 层用 SlotMapper.countConflict 完成，
     * 因为那一步依赖具体订单的时段，属于行级判断，不适合塞进这条 SQL。
     *
     * @param categoryId 服务项目所属分类
     * @return 候选师傅列表，已按优先级排好序
     */
    @Select("select p.* from provider p " +
            "inner join provider_skill ps on p.id = ps.provider_id " +
            "where ps.category_id = #{categoryId} and p.status = 1 " +
            "order by p.score desc, p.order_count asc")
    List<Provider> listAvailableByCategoryId(Long categoryId);

    /**
     * 回写评分与好评率（评价提交时调用）
     */
    @Update("update provider set score = #{score}, good_rate = #{goodRate} where id = #{providerId}")
    void updateScore(@Param("providerId") Long providerId,
                     @Param("score") BigDecimal score,
                     @Param("goodRate") BigDecimal goodRate);

    /**
     * 完成订单后累加接单数
     */
    @Update("update provider set order_count = order_count + 1 where id = #{providerId}")
    void increaseOrderCount(Long providerId);

    /**
     * 修改接单状态
     */
    @Update("update provider set status = #{status} where id = #{providerId}")
    void updateStatus(@Param("providerId") Long providerId, @Param("status") Integer status);
}
