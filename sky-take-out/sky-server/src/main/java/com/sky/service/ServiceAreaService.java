package com.sky.service;

import com.sky.dto.ServiceAreaDTO;
import com.sky.entity.ServiceArea;

import java.util.List;

/**
 * 服务区域业务接口
 */
public interface ServiceAreaService {

    void save(ServiceAreaDTO serviceAreaDTO);

    void update(ServiceAreaDTO serviceAreaDTO);

    void delete(Long id);

    List<ServiceArea> list(ServiceArea serviceArea);

    /**
     * 开通 / 停用某个区域
     */
    void updateStatus(Long id, Integer status);
}
