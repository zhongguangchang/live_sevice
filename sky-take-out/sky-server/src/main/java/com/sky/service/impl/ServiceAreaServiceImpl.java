package com.sky.service.impl;

import com.sky.dto.ServiceAreaDTO;
import com.sky.entity.ServiceArea;
import com.sky.mapper.ServiceAreaMapper;
import com.sky.service.ServiceAreaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 服务区域业务实现
 * <p>
 * 注意这张表没有 create_user / update_user 字段，
 * 所以不能用 @AutoFill 自动填充，时间戳在这里手工设置。
 */
@Service
@Slf4j
public class ServiceAreaServiceImpl implements ServiceAreaService {

    @Autowired
    private ServiceAreaMapper serviceAreaMapper;

    @Override
    public void save(ServiceAreaDTO dto) {
        ServiceArea area = new ServiceArea();
        BeanUtils.copyProperties(dto, area);
        if (area.getStatus() == null) {
            area.setStatus(ServiceArea.STATUS_OPEN);
        }
        if (area.getSort() == null) {
            area.setSort(0);
        }
        area.setCreateTime(LocalDateTime.now());
        area.setUpdateTime(LocalDateTime.now());
        serviceAreaMapper.insert(area);
    }

    @Override
    public void update(ServiceAreaDTO dto) {
        ServiceArea area = new ServiceArea();
        BeanUtils.copyProperties(dto, area);
        area.setUpdateTime(LocalDateTime.now());
        serviceAreaMapper.update(area);
    }

    @Override
    public void delete(Long id) {
        serviceAreaMapper.deleteById(id);
    }

    @Override
    public List<ServiceArea> list(ServiceArea serviceArea) {
        return serviceAreaMapper.list(serviceArea);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        ServiceArea area = ServiceArea.builder()
                .id(id)
                .status(status)
                .updateTime(LocalDateTime.now())
                .build();
        serviceAreaMapper.update(area);
    }
}
