package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.sky.context.BaseContext;
import com.sky.dto.ServiceCartDTO;
import com.sky.entity.ServiceCart;
import com.sky.entity.ServiceItem;
import com.sky.entity.ServicePackage;
import com.sky.entity.ServiceSpec;
import com.sky.exception.SlotNotAvailableException;
import com.sky.mapper.ServiceCartMapper;
import com.sky.mapper.ServiceItemMapper;
import com.sky.mapper.ServicePackageMapper;
import com.sky.mapper.ServiceSpecMapper;
import com.sky.service.ServiceCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 服务清单业务实现
 */
@Service
@Slf4j
public class ServiceCartServiceImpl implements ServiceCartService {

    @Autowired
    private ServiceCartMapper serviceCartMapper;
    @Autowired
    private ServiceItemMapper serviceItemMapper;
    @Autowired
    private ServicePackageMapper servicePackageMapper;
    @Autowired
    private ServiceSpecMapper serviceSpecMapper;

    @Override
    public void add(ServiceCartDTO serviceCartDTO) {
        Long userId = BaseContext.getCurrentId();

        // 先看清单里有没有「同一服务 + 同一规格」的记录
        // 规格不同要算两条，因为价格不一样（60 平和 120 平的保洁是两回事）
        ServiceCart probe = ServiceCart.builder()
                .userId(userId)
                .serviceId(serviceCartDTO.getServiceId())
                .servicePackageId(serviceCartDTO.getServicePackageId())
                .spec(serviceCartDTO.getSpec())
                .build();
        List<ServiceCart> existing = serviceCartMapper.list(probe);
        if (!existing.isEmpty()) {
            ServiceCart cart = existing.get(0);
            cart.setNumber(cart.getNumber() + 1);
            serviceCartMapper.updateNumberById(cart);
            return;
        }

        ServiceCart cart = ServiceCart.builder()
                .userId(userId)
                .serviceId(serviceCartDTO.getServiceId())
                .servicePackageId(serviceCartDTO.getServicePackageId())
                .spec(serviceCartDTO.getSpec())
                .number(1)
                .createTime(LocalDateTime.now())
                .build();

        if (serviceCartDTO.getServiceId() != null) {
            ServiceItem item = serviceItemMapper.getById(serviceCartDTO.getServiceId());
            if (item == null) {
                throw new SlotNotAvailableException("服务项目不存在");
            }
            cart.setName(item.getName());
            cart.setImage(item.getImage());
            // 价格以服务端算的为准，不信任前端传的值
            cart.setAmount(calculateAmount(item.getPrice(), item.getId(), serviceCartDTO.getSpec()));
        } else if (serviceCartDTO.getServicePackageId() != null) {
            ServicePackage pkg = servicePackageMapper.getById(serviceCartDTO.getServicePackageId());
            if (pkg == null) {
                throw new SlotNotAvailableException("服务套餐不存在");
            }
            cart.setName(pkg.getName());
            cart.setImage(pkg.getImage());
            cart.setAmount(pkg.getPrice());
        } else {
            throw new SlotNotAvailableException("请选择服务项目或服务套餐");
        }

        serviceCartMapper.insert(cart);
        log.info("加入服务清单：userId={}, name={}, amount={}", userId, cart.getName(), cart.getAmount());
    }

    @Override
    public List<ServiceCart> list() {
        return serviceCartMapper.list(ServiceCart.builder()
                .userId(BaseContext.getCurrentId())
                .build());
    }

    @Override
    public void sub(ServiceCartDTO serviceCartDTO) {
        Long userId = BaseContext.getCurrentId();
        ServiceCart probe = ServiceCart.builder()
                .userId(userId)
                .serviceId(serviceCartDTO.getServiceId())
                .servicePackageId(serviceCartDTO.getServicePackageId())
                .spec(serviceCartDTO.getSpec())
                .build();
        List<ServiceCart> existing = serviceCartMapper.list(probe);
        if (existing.isEmpty()) {
            return;
        }
        ServiceCart cart = existing.get(0);
        if (cart.getNumber() > 1) {
            cart.setNumber(cart.getNumber() - 1);
            serviceCartMapper.updateNumberById(cart);
        } else {
            serviceCartMapper.deleteById(cart.getId());
        }
    }

    @Override
    public void clean() {
        serviceCartMapper.deleteByUserId(BaseContext.getCurrentId());
    }

    /**
     * 计算含规格加价后的实际单价
     * <p>
     * 前端传过来的 spec 形如 {"房屋面积":"60-90平方米"}。
     * 这里拿它去 service_spec 表里匹配，匹配上的加价累加。
     * <p>
     * 之所以要在服务端算而不是直接用前端传的价格：
     * 前端传的价格是可以被篡改的，金额必须以服务端数据源为准。
     */
    private BigDecimal calculateAmount(BigDecimal basePrice, Long serviceId, String specJson) {
        if (basePrice == null) {
            return BigDecimal.ZERO;
        }
        if (specJson == null || specJson.trim().isEmpty()) {
            return basePrice;
        }
        Map<String, String> chosen;
        try {
            chosen = JSON.parseObject(specJson, new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {
            log.warn("规格 JSON 解析失败，按基础价计算：serviceId={}, spec={}", serviceId, specJson);
            return basePrice;
        }
        if (chosen == null || chosen.isEmpty()) {
            return basePrice;
        }

        BigDecimal amount = basePrice;
        List<ServiceSpec> specs = serviceSpecMapper.listByServiceId(serviceId);
        for (ServiceSpec spec : specs) {
            String selected = chosen.get(spec.getName());
            if (selected != null && selected.equals(spec.getValue()) && spec.getPriceDelta() != null) {
                amount = amount.add(spec.getPriceDelta());
            }
        }
        return amount;
    }
}
