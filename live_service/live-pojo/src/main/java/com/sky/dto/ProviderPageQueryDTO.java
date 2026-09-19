package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 服务人员分页查询入参
 */
@Data
public class ProviderPageQueryDTO implements Serializable {

    //页码
    private int page;

    //每页记录数
    private int pageSize;

    //师傅姓名（模糊匹配）
    private String name;

    //接单状态 1可接单 2忙碌 3休息中 4已离职
    private Integer status;

    //按擅长的服务分类筛选
    private Long categoryId;
}
