package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.ProviderDTO;
import com.sky.dto.ProviderPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Provider;
import com.sky.entity.ProviderSkill;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.ProviderMapper;
import com.sky.mapper.ProviderSkillMapper;
import com.sky.result.PageResult;
import com.sky.service.ProviderService;
import com.sky.vo.ProviderVO;
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
 * 服务人员业务实现
 */
@Service
@Slf4j
public class ProviderServiceImpl implements ProviderService {

    @Autowired
    private ProviderMapper providerMapper;
    @Autowired
    private ProviderSkillMapper providerSkillMapper;
    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    @Transactional
    public void saveWithSkills(ProviderDTO dto) {
        Provider provider = new Provider();
        BeanUtils.copyProperties(dto, provider);
        provider.setMerchantId(1L);
        providerMapper.insert(provider);
        saveSkills(provider.getId(), dto.getCategoryIds());
    }

    @Override
    @Transactional
    public void updateWithSkills(ProviderDTO dto) {
        Provider provider = new Provider();
        BeanUtils.copyProperties(dto, provider);
        providerMapper.update(provider);

        providerSkillMapper.deleteByProviderId(provider.getId());
        saveSkills(provider.getId(), dto.getCategoryIds());
    }

    @Override
    public PageResult pageQuery(ProviderPageQueryDTO dto) {
        PageHelper.startPage(dto.getPage(), dto.getPageSize());
        Page<ProviderVO> page = providerMapper.pageQuery(dto);

        List<ProviderVO> records = page.getResult();
        List<ProviderVO> result = new ArrayList<>();
        for (ProviderVO vo : records) {
            fillSkills(vo);
            result.add(vo);
        }
        return new PageResult(page.getTotal(), result);
    }

    @Override
    public ProviderVO getByIdWithSkills(Long id) {
        Provider provider = providerMapper.getById(id);
        if (provider == null) {
            return null;
        }
        ProviderVO vo = new ProviderVO();
        BeanUtils.copyProperties(provider, vo);
        fillSkills(vo);
        return vo;
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        providerMapper.updateStatus(id, status);
    }

    @Override
    public List<ProviderVO> listAvailable(Long categoryId) {
        List<Provider> providers = providerMapper.listAvailableByCategoryId(categoryId);
        List<ProviderVO> result = new ArrayList<>();
        for (Provider provider : providers) {
            ProviderVO vo = new ProviderVO();
            BeanUtils.copyProperties(provider, vo);
            fillSkills(vo);
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<Provider> list(Provider provider) {
        return providerMapper.list(provider);
    }

    // ========================================================================
    //  私有辅助方法
    // ========================================================================

    private void saveSkills(Long providerId, List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }
        List<ProviderSkill> skills = new ArrayList<>();
        for (Long categoryId : categoryIds) {
            skills.add(ProviderSkill.builder()
                    .providerId(providerId)
                    .categoryId(categoryId)
                    .level(1)
                    .build());
        }
        providerSkillMapper.insertBatch(skills);
    }

    /**
     * 把技能的分类 id 和名称填进 VO
     */
    private void fillSkills(ProviderVO vo) {
        List<ProviderSkill> skills = providerSkillMapper.listByProviderId(vo.getId());
        if (skills == null || skills.isEmpty()) {
            return;
        }
        // 分类名称做一次全量查询再映射，分类数量很少，比逐个查划算
        List<Category> categories = categoryMapper.list(null);
        Map<Long, String> nameMap = new HashMap<>();
        for (Category c : categories) {
            nameMap.put(c.getId(), c.getName());
        }

        List<Long> ids = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (ProviderSkill skill : skills) {
            ids.add(skill.getCategoryId());
            String name = nameMap.get(skill.getCategoryId());
            if (name != null) {
                names.add(name);
            }
        }
        vo.setCategoryIds(ids);
        vo.setCategoryNames(names);
    }
}
