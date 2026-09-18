package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 服务套餐分页查询入参
 */
@Data
public class ServicePackagePageQueryDTO implements Serializable {

    //页码
    private int page;

    //每页记录数
    private int pageSize;

    //套餐名称（模糊匹配）
    private String name;

    //服务套餐分类id
    private Long categoryId;

    //状态 0停用 1启用
    private Integer status;
}
