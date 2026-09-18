package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务评价视图对象
 * <p>
 * 比实体多带用户名、头像和图片数组（实体里图片是逗号分隔的字符串，
 * 前端更希望直接拿到数组）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewVO implements Serializable {

    private Long id;

    private Long orderId;

    private Long userId;

    //评价人昵称（匿名为「匿名用户」）
    private String userName;

    //评价人头像
    private String userAvatar;

    private Long providerId;

    //师傅姓名
    private String providerName;

    private Long serviceId;

    //服务项目名称
    private String serviceName;

    private Integer score;

    private Integer serviceScore;

    private Integer speedScore;

    private Integer qualityScore;

    private String content;

    //评价图片数组
    private List<String> imageList = new ArrayList<>();

    private Integer isAnonymous;

    private String reply;

    private LocalDateTime replyTime;

    private Integer status;

    private LocalDateTime createTime;
}
