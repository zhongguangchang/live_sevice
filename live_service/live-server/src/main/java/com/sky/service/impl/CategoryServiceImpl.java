package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.RedisKeyConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.ServiceItemMapper;
import com.sky.mapper.ServicePackageMapper;
import com.sky.result.PageResult;
import com.sky.service.CategoryService;
import com.sky.utils.CacheHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 分类业务层
 */
@Service
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    /** 分类列表缓存时长（秒） */
    private static final int LIST_CACHE_SECONDS = 30 * 60;

    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private ServiceItemMapper serviceItemMapper;
    @Autowired
    private ServicePackageMapper servicePackageMapper;
    @Autowired
    private CacheHelper cacheHelper;

    /**
     * 新增分类
     * @param categoryDTO
     */
    public void save(CategoryDTO categoryDTO) {
        Category category = new Category();
        //属性拷贝
        BeanUtils.copyProperties(categoryDTO, category);

        //分类状态默认为禁用状态0
        category.setStatus(StatusConstant.DISABLE);

        //设置创建时间、修改时间、创建人、修改人
        //category.setCreateTime(LocalDateTime.now());
        //category.setUpdateTime(LocalDateTime.now());
        //category.setCreateUser(BaseContext.getCurrentId());
        //category.setUpdateUser(BaseContext.getCurrentId());

        categoryMapper.insert(category);
        evictCategoryCache();
    }

    /**
     * 分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        PageHelper.startPage(categoryPageQueryDTO.getPage(),categoryPageQueryDTO.getPageSize());
        //下一条sql进行分页，自动加入limit关键字分页
        Page<Category> page = categoryMapper.pageQuery(categoryPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 根据id删除分类
     * @param id
     */
    public void deleteById(Long id) {
        //查询当前分类下是否还有服务项目，有就不能删
        Integer count = serviceItemMapper.countByCategoryId(id);
        if(count > 0){
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SERVICE_ITEM);
        }

        //再查是否还有服务套餐
        count = servicePackageMapper.countByCategoryId(id);
        if(count > 0){
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SERVICE_PACKAGE);
        }

        //删除分类数据
        categoryMapper.deleteById(id);
        evictCategoryCache();
    }

    /**
     * 修改分类
     * @param categoryDTO
     */
    public void update(CategoryDTO categoryDTO) {
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO,category);

        //设置修改时间、修改人
        //category.setUpdateTime(LocalDateTime.now());
        //category.setUpdateUser(BaseContext.getCurrentId());

        categoryMapper.update(category);
        evictCategoryCache();
    }

    /**
     * 启用、禁用分类
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        Category category = Category.builder()
                .id(id)
                .status(status)
                //.updateTime(LocalDateTime.now())
                //.updateUser(BaseContext.getCurrentId())
                .build();
        categoryMapper.update(category);
        evictCategoryCache();
    }

    /**
     * 根据类型查询分类
     * @param type
     * @return
     */
    public List<Category> list(Integer type) {
        // 分类列表是「读多写极少」的典型：运营改一次、用户读上万次，
        // 最适合缓存。key 里带上 type（1 服务分类 / 2 套餐分类），
        // 两类分类互不影响
        return cacheHelper.getOrLoad(
                RedisKeyConstant.CACHE_CATEGORY_LIST + ":" + type,
                new com.alibaba.fastjson.TypeReference<List<Category>>() {
                }.getType(),
                () -> categoryMapper.list(type),
                LIST_CACHE_SECONDS);
    }

    /**
     * 分类变更后清掉分类缓存
     * <p>
     * 注意这里同时清了「服务列表」缓存：分类被停用后，
     * 用户端不应该再看到这个分类下的服务，而服务列表是按分类缓存的，
     * 只清分类本身会出现「分类没了、服务还在」的中间态
     */
    private void evictCategoryCache() {
        cacheHelper.evictByPrefix(RedisKeyConstant.CACHE_CATEGORY_LIST);
        cacheHelper.evictByPrefix(RedisKeyConstant.CACHE_SERVICE_LIST_PREFIX);
    }
}
