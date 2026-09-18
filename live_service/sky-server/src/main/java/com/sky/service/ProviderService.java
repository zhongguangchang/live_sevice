package com.sky.service;

import com.sky.dto.ProviderDTO;
import com.sky.dto.ProviderPageQueryDTO;
import com.sky.entity.Provider;
import com.sky.result.PageResult;
import com.sky.vo.ProviderVO;

import java.util.List;

/**
 * 服务人员（师傅）业务接口
 */
public interface ProviderService {

    /**
     * 新增师傅，同时保存技能
     */
    void saveWithSkills(ProviderDTO providerDTO);

    /**
     * 修改师傅（技能全删重插）
     */
    void updateWithSkills(ProviderDTO providerDTO);

    /**
     * 分页查询
     */
    PageResult pageQuery(ProviderPageQueryDTO providerPageQueryDTO);

    /**
     * 查询详情（含技能）
     */
    ProviderVO getByIdWithSkills(Long id);

    /**
     * 修改接单状态（可接单 / 忙碌 / 休息 / 离职）
     */
    void updateStatus(Long id, Integer status);

    /**
     * 按技能分类查询可接单的师傅（派单和用户端选师傅用）
     */
    List<ProviderVO> listAvailable(Long categoryId);

    /**
     * 动态条件查询
     */
    List<Provider> list(Provider provider);
}
