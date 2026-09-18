package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 评价分页查询入参
 */
@Data
public class ReviewPageQueryDTO implements Serializable {

    //页码
    private int page;

    //每页记录数
    private int pageSize;

    //服务项目id
    private Long serviceId;

    //服务人员id
    private Long providerId;

    //用户id
    private Long userId;

    //按评分筛选（如只看差评传 1）
    private Integer score;

    //状态 0隐藏 1显示
    private Integer status;
}
