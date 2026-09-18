package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.ServicePackageDTO;
import com.sky.dto.ServicePackagePageQueryDTO;
import com.sky.entity.ServiceItem;
import com.sky.entity.ServicePackage;
import com.sky.entity.ServicePackageItem;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.ServiceItemMapper;
import com.sky.mapper.ServicePackageItemMapper;
import com.sky.mapper.ServicePackageMapper;
import com.sky.result.PageResult;
import com.sky.service.ServicePackageService;
import com.sky.vo.ServicePackageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务套餐业务实现
 */
@Service
@Slf4j
public class ServicePackageServiceImpl implements ServicePackageService {

    @Autowired
    private ServicePackageMapper servicePackageMapper;
    @Autowired
    private ServicePackageItemMapper servicePackageItemMapper;
    @Autowired
    private ServiceItemMapper serviceItemMapper;

    @Override
    @Transactional
    public void saveWithItems(ServicePackageDTO dto) {
        ServicePackage pkg = new ServicePackage();
        BeanUtils.copyProperties(dto, pkg);
        pkg.setSales(0);
        pkg.setMerchantId(1L);
        servicePackageMapper.insert(pkg);

        saveItems(pkg.getId(), dto.getItems());
    }

    @Override
    @Transactional
    public void updateWithItems(ServicePackageDTO dto) {
        ServicePackage pkg = new ServicePackage();
        BeanUtils.copyProperties(dto, pkg);
        servicePackageMapper.update(pkg);

        servicePackageItemMapper.deleteByPackageId(pkg.getId());
        saveItems(pkg.getId(), dto.getItems());
    }

    @Override
    public PageResult pageQuery(ServicePackagePageQueryDTO dto) {
        PageHelper.startPage(dto.getPage(), dto.getPageSize());
        Page<ServicePackageVO> page = servicePackageMapper.pageQuery(dto);

        // 和菜品列表一样，套餐列表也要显示包含哪些服务，
        // 用一次批量查询避免 N+1
        List<ServicePackageVO> records = page.getResult();
        if (!records.isEmpty()) {
            List<Long> ids = new ArrayList<>();
            for (ServicePackageVO vo : records) {
                ids.add(vo.getId());
            }
            List<ServicePackageItem> allItems = servicePackageItemMapper.listByPackageIds(ids);
            Map<Long, List<ServicePackageItem>> itemMap = new HashMap<>();
            for (ServicePackageItem item : allItems) {
                itemMap.computeIfAbsent(item.getServicePackageId(), k -> new ArrayList<>()).add(item);
            }
            for (ServicePackageVO vo : records) {
                List<ServicePackageItem> items = itemMap.get(vo.getId());
                if (items != null) {
                    vo.setItems(items);
                }
            }
        }
        return new PageResult(page.getTotal(), records);
    }

    @Override
    public ServicePackageVO getByIdWithItems(Long id) {
        ServicePackage pkg = servicePackageMapper.getById(id);
        if (pkg == null) {
            return null;
        }
        ServicePackageVO vo = new ServicePackageVO();
        BeanUtils.copyProperties(pkg, vo);
        vo.setItems(servicePackageItemMapper.listByPackageId(id));
        return vo;
    }

    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        for (Long id : ids) {
            ServicePackage pkg = servicePackageMapper.getById(id);
            if (pkg != null && StatusConstant.ENABLE.equals(pkg.getStatus())) {
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        servicePackageItemMapper.deleteByPackageIds(ids);
        servicePackageMapper.deleteByIds(ids);
    }

    @Override
    @Transactional
    public void startOrStop(Integer status, Long id) {
        if (StatusConstant.ENABLE.equals(status)) {
            // 启用前检查套餐内每个服务项目都还在售
            List<ServicePackageItem> items = servicePackageItemMapper.listByPackageId(id);
            for (ServicePackageItem item : items) {
                ServiceItem serviceItem = serviceItemMapper.getById(item.getServiceId());
                if (serviceItem == null || !StatusConstant.ENABLE.equals(serviceItem.getStatus())) {
                    throw new SetmealEnableFailedException(MessageConstant.SERVICE_PACKAGE_ENABLE_FAILED);
                }
            }
        }
        ServicePackage pkg = ServicePackage.builder().id(id).status(status).build();
        servicePackageMapper.update(pkg);
    }

    @Override
    public List<ServicePackage> list(ServicePackage servicePackage) {
        return servicePackageMapper.list(servicePackage);
    }

    private void saveItems(Long packageId, List<ServicePackageItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (ServicePackageItem item : items) {
            item.setServicePackageId(packageId);
            if (item.getCopies() == null) {
                item.setCopies(1);
            }
        }
        servicePackageItemMapper.insertBatch(items);
    }
}
