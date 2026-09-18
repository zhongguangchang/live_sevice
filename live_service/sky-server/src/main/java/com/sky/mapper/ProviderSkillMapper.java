package com.sky.mapper;

import com.sky.entity.ProviderSkill;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 服务人员技能 Mapper
 */
@Mapper
public interface ProviderSkillMapper {

    void insertBatch(@Param("skills") List<ProviderSkill> skills);

    @Delete("delete from provider_skill where provider_id = #{providerId}")
    void deleteByProviderId(Long providerId);

    @Select("select * from provider_skill where provider_id = #{providerId}")
    List<ProviderSkill> listByProviderId(Long providerId);

    /**
     * 批量查询多个师傅的技能（列表页避免 N+1 查询）
     */
    List<ProviderSkill> listByProviderIds(@Param("providerIds") List<Long> providerIds);
}
