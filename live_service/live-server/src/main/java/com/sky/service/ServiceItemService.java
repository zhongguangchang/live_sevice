package com.sky.service;

import com.sky.dto.ServiceItemDTO;
import com.sky.dto.ServiceItemPageQueryDTO;
import com.sky.entity.ServiceItem;
import com.sky.result.PageResult;
import com.sky.vo.ServiceItemVO;

import java.util.List;

/**
 * 服务项目业务接口
 */
public interface ServiceItemService {

    /**
     * 新增服务项目，同时保存规格
     */
    void saveWithSpecs(ServiceItemDTO serviceItemDTO);

    /**
     * 修改服务项目。
     * <p>
     * 规格采用「先按 serviceId 全删、再批量插入」的策略，
     * 和原项目处理菜品口味的思路一致，简单可靠。
     */
    void updateWithSpecs(ServiceItemDTO serviceItemDTO);

    /**
     * 分页查询
     */
    PageResult pageQuery(ServiceItemPageQueryDTO serviceItemPageQueryDTO);

    /**
     * 查询详情（含规格）
     */
    ServiceItemVO getByIdWithSpecs(Long id);

    /**
     * 批量删除。
     * <p>
     * 两条校验：起售中的不能删；被套餐引用的不能删。
     */
    void deleteBatch(List<Long> ids);

    /**
     * 起售 / 停售
     */
    void startOrStop(Integer status, Long id);

    /**
     * 按分类查询已起售的服务项目（用户端用）
     */
    List<ServiceItemVO> listByCategoryId(Long categoryId);

    /**
     * 动态条件查询
     */
    List<ServiceItem> list(ServiceItem serviceItem);
}
