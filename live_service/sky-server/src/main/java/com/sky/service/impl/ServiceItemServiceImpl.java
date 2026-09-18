package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.ServiceItemDTO;
import com.sky.dto.ServiceItemPageQueryDTO;
import com.sky.entity.ServiceItem;
import com.sky.entity.ServiceSpec;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.ServiceItemMapper;
import com.sky.mapper.ServicePackageItemMapper;
import com.sky.mapper.ServiceSpecMapper;
import com.sky.result.PageResult;
import com.sky.service.ServiceItemService;
import com.sky.vo.ServiceItemVO;
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
 * 服务项目业务实现
 */
@Service
@Slf4j
public class ServiceItemServiceImpl implements ServiceItemService {

    @Autowired
    private ServiceItemMapper serviceItemMapper;
    @Autowired
    private ServiceSpecMapper serviceSpecMapper;
    @Autowired
    private ServicePackageItemMapper servicePackageItemMapper;

    @Override
    @Transactional
    public void saveWithSpecs(ServiceItemDTO dto) {
        ServiceItem item = new ServiceItem();
        BeanUtils.copyProperties(dto, item);
        applyInsertDefaults(item);

        serviceItemMapper.insert(item);
        saveSpecs(item.getId(), dto.getSpecs());
    }

    /**
     * 补齐新增时的默认值
     * <p>
     * <b>为什么不能依赖数据库的 DEFAULT：</b>这些列在 DDL 里确实写了默认值，
     * 但 MyBatis 的 insert 语句是固定列清单，未传的字段会显式插入 NULL，
     * 而「显式 NULL」不会触发默认值，MySQL 直接报
     * {@code Column 'score' cannot be null}。
     * <p>
     * 结果就是管理端点「新增服务项目」必然失败——这是改造过程中真实存在的问题，
     * 因为默认值只在服务层补了 sales 和 merchantId，漏掉了 score。
     * <p>
     * 取值对齐 DDL 里的默认值：单位空串、时长 60 分钟、上门服务、
     * 需要预约、销量 0、评分 5.00（新项目先给满分，之后由评价回写覆盖）。
     */
    private void applyInsertDefaults(ServiceItem item) {
        if (item.getUnit() == null) {
            item.setUnit("");
        }
        if (item.getDuration() == null) {
            item.setDuration(60);
        }
        if (item.getServiceMode() == null) {
            item.setServiceMode(ServiceItem.MODE_HOME);
        }
        if (item.getNeedAppoint() == null) {
            item.setNeedAppoint(1);
        }
        if (item.getSales() == null) {
            item.setSales(0);
        }
        if (item.getScore() == null) {
            item.setScore(new java.math.BigDecimal("5.00"));
        }
        if (item.getStatus() == null) {
            item.setStatus(StatusConstant.DISABLE);
        }
        item.setMerchantId(1L);
    }

    @Override
    @Transactional
    public void updateWithSpecs(ServiceItemDTO dto) {
        ServiceItem item = new ServiceItem();
        BeanUtils.copyProperties(dto, item);
        serviceItemMapper.update(item);

        // 规格全删重插
        serviceSpecMapper.deleteByServiceId(item.getId());
        saveSpecs(item.getId(), dto.getSpecs());
    }

    @Override
    public PageResult pageQuery(ServiceItemPageQueryDTO dto) {
        PageHelper.startPage(dto.getPage(), dto.getPageSize());
        Page<ServiceItemVO> page = serviceItemMapper.pageQuery(dto);

        // 列表页也要显示规格，否则运营看不出这个服务有哪些档次可选。
        // 这里用一次批量查询代替逐条查询，避免 N+1。
        List<ServiceItemVO> records = page.getResult();
        if (!records.isEmpty()) {
            List<Long> ids = new ArrayList<>();
            for (ServiceItemVO vo : records) {
                ids.add(vo.getId());
            }
            List<ServiceSpec> allSpecs = serviceSpecMapper.listByServiceIds(ids);
            Map<Long, List<ServiceSpec>> specMap = new HashMap<>();
            for (ServiceSpec spec : allSpecs) {
                specMap.computeIfAbsent(spec.getServiceId(), k -> new ArrayList<>()).add(spec);
            }
            for (ServiceItemVO vo : records) {
                List<ServiceSpec> specs = specMap.get(vo.getId());
                if (specs != null) {
                    vo.setSpecs(specs);
                }
            }
        }
        return new PageResult(page.getTotal(), records);
    }

    @Override
    public ServiceItemVO getByIdWithSpecs(Long id) {
        ServiceItem item = serviceItemMapper.getById(id);
        if (item == null) {
            return null;
        }
        ServiceItemVO vo = new ServiceItemVO();
        BeanUtils.copyProperties(item, vo);
        vo.setSpecs(serviceSpecMapper.listByServiceId(id));
        return vo;
    }

    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        for (Long id : ids) {
            ServiceItem item = serviceItemMapper.getById(id);
            if (item == null) {
                continue;
            }
            // 起售中的不能删，得先停售
            if (StatusConstant.ENABLE.equals(item.getStatus())) {
                throw new DeletionNotAllowedException(MessageConstant.SERVICE_ITEM_ON_SALE);
            }
            // 被套餐引用的不能删，否则套餐里会出现空项
            Integer refCount = servicePackageItemMapper.countByServiceId(id);
            if (refCount != null && refCount > 0) {
                throw new DeletionNotAllowedException(MessageConstant.SERVICE_ITEM_BE_RELATED_BY_PACKAGE);
            }
        }
        serviceSpecMapper.deleteByServiceIds(ids);
        serviceItemMapper.deleteByIds(ids);
    }

    @Override
    public void startOrStop(Integer status, Long id) {
        ServiceItem item = ServiceItem.builder().id(id).status(status).build();
        serviceItemMapper.update(item);
    }

    @Override
    public List<ServiceItemVO> listByCategoryId(Long categoryId) {
        List<ServiceItem> items = serviceItemMapper.listOnSaleByCategoryId(categoryId);
        List<ServiceItemVO> result = new ArrayList<>();
        for (ServiceItem item : items) {
            ServiceItemVO vo = new ServiceItemVO();
            BeanUtils.copyProperties(item, vo);
            vo.setSpecs(serviceSpecMapper.listByServiceId(item.getId()));
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<ServiceItem> list(ServiceItem serviceItem) {
        return serviceItemMapper.list(serviceItem);
    }

    /**
     * 保存规格列表，顺便回填 serviceId
     */
    private void saveSpecs(Long serviceId, List<ServiceSpec> specs) {
        if (specs == null || specs.isEmpty()) {
            return;
        }
        for (ServiceSpec spec : specs) {
            spec.setServiceId(serviceId);
            if (spec.getPriceDelta() == null) {
                spec.setPriceDelta(java.math.BigDecimal.ZERO);
            }
            if (spec.getSort() == null) {
                spec.setSort(0);
            }
        }
        serviceSpecMapper.insertBatch(specs);
    }
}
