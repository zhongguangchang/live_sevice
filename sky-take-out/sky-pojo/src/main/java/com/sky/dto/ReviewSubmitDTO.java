package com.sky.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 提交评价入参
 * <p>
 * 三维打分：服务态度、响应速度、服务质量。只给总分的话，
 * 平台分不清「态度好但来晚了」和「来得快但活干糙」。
 */
@Data
public class ReviewSubmitDTO implements Serializable {

    //订单id
    private Long orderId;

    //综合评分 1-5
    private Integer score;

    //服务态度评分 1-5
    private Integer serviceScore;

    //响应速度评分 1-5
    private Integer speedScore;

    //服务质量评分 1-5
    private Integer qualityScore;

    //评价文字内容
    private String content;

    //评价图片url列表
    private List<String> images;

    //是否匿名 0否 1是
    private Integer isAnonymous;
}
