package com.sky.mapper;

import com.sky.entity.ServiceCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 服务清单 Mapper
 */
@Mapper
public interface ServiceCartMapper {

    void insert(ServiceCart serviceCart);

    /**
     * 动态条件查询
     */
    List<ServiceCart> list(ServiceCart serviceCart);

    @Update("update shopping_cart set number = #{number} where id = #{id}")
    void updateNumberById(ServiceCart serviceCart);

    @Delete("delete from shopping_cart where id = #{id}")
    void deleteById(Long id);

    /**
     * 清空某用户的清单（下单成功后调用）
     */
    @Delete("delete from shopping_cart where user_id = #{userId}")
    void deleteByUserId(Long userId);
}
