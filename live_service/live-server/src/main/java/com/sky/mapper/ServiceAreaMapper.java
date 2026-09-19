package com.sky.mapper;

import com.sky.entity.ServiceArea;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 服务区域 Mapper
 */
@Mapper
public interface ServiceAreaMapper {

    void insert(ServiceArea serviceArea);

    void update(ServiceArea serviceArea);

    @Delete("delete from service_area where id = #{id}")
    void deleteById(Long id);

    @Select("select * from service_area where id = #{id}")
    ServiceArea getById(Long id);

    /**
     * 按区划编号查区域
     * <p>
     * 下单时用它做服务范围校验：查不到或状态不是已开通，
     * 就抛「该区域暂未开通服务」。这是生活服务网比外卖多出的一道校验。
     */
    @Select("select * from service_area where code = #{code}")
    ServiceArea getByCode(String code);

    List<ServiceArea> list(ServiceArea serviceArea);

    @Update("update service_area set status = #{status} where id = #{id}")
    void updateStatus(ServiceArea serviceArea);
}
