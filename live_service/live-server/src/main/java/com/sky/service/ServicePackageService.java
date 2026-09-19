package com.sky.service;

import com.sky.dto.ServicePackageDTO;
import com.sky.dto.ServicePackagePageQueryDTO;
import com.sky.entity.ServicePackage;
import com.sky.result.PageResult;
import com.sky.vo.ServicePackageVO;

import java.util.List;

/**
 * 服务套餐业务接口
 */
public interface ServicePackageService {

    /**
     * 新增套餐，同时保存套餐明细
     */
    void saveWithItems(ServicePackageDTO servicePackageDTO);

    /**
     * 修改套餐（明细全删重插）
     */
    void updateWithItems(ServicePackageDTO servicePackageDTO);

    /**
     * 分页查询
     */
    PageResult pageQuery(ServicePackagePageQueryDTO servicePackagePageQueryDTO);

    /**
     * 查询详情（含明细）
     */
    ServicePackageVO getByIdWithItems(Long id);

    /**
     * 批量删除，起售中的不能删
     */
    void deleteBatch(List<Long> ids);

    /**
     * 启用 / 停用。
     * <p>
     * 启用前要校验套餐内所有服务项目都是起售状态，
     * 否则用户会买到一个包含已停售服务的套餐。
     */
    void startOrStop(Integer status, Long id);

    /**
     * 动态条件查询
     */
    List<ServicePackage> list(ServicePackage servicePackage);
}
