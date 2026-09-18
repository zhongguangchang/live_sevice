package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 服务项目分页查询入参
 */
@Data
public class ServiceItemPageQueryDTO implements Serializable {

    //页码
    private int page;

    //每页记录数
    private int pageSize;

    //服务名称（模糊匹配）
    private String name;

    //服务分类id
    private Long categoryId;

    //状态 0停售 1起售
    private Integer status;

    //服务方式 1上门 2到店 3都支持
    private Integer serviceMode;
}
