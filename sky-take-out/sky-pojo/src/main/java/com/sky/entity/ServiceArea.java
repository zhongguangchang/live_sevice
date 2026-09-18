package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 服务区域（平台业务覆盖的行政区划）
 * <p>
 * 平台不是哪儿都上门。用户下单时用地址的 districtCode 匹配这张表，
 * 匹配不到就提示「该区域暂未开通服务」。
 * 这是生活服务网比外卖多出来的一道业务校验。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceArea implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 区域状态：已开通 */
    public static final Integer STATUS_OPEN = 1;
    /** 区域状态：未开通 */
    public static final Integer STATUS_CLOSED = 0;

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

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
