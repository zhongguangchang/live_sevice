package com.sky.service;

import com.sky.dto.ServiceCartDTO;
import com.sky.entity.ServiceCart;

import java.util.List;

/**
 * 服务清单（购物车）业务接口
 */
public interface ServiceCartService {

    /**
     * 加入清单。同一服务同一规格重复添加时只累加数量。
     */
    void add(ServiceCartDTO serviceCartDTO);

    /**
     * 查看当前用户的清单
     */
    List<ServiceCart> list();

    /**
     * 从清单中减掉一个
     */
    void sub(ServiceCartDTO serviceCartDTO);

    /**
     * 清空当前用户的清单
     */
    void clean();
}
