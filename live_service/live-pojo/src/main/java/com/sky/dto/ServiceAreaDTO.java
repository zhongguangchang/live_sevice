package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 服务区域新增/编辑入参
 */
@Data
public class ServiceAreaDTO implements Serializable {

    private Long id;

    //区域名称（如：海淀区）
    private String name;

    //区级区划编号
    private String code;

    //所属市级区划编号
    private String cityCode;

    //所属市级名称
    private String cityName;

    //排序
    private Integer sort;

    //状态 0未开通 1已开通
    private Integer status;
}
