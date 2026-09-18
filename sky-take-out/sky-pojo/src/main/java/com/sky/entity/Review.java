package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 服务评价
 * <p>
 * 生活服务的命脉。拆成三个维度打分而不是只给一个总分，因为
 * 「师傅态度好但来得慢」和「来得快但活干得糙」需要区分开，
 * 这样平台才知道该优化哪一环。
 * <p>
 * 评价提交后要异步回写三处：provider.score 与 provider.goodRate、
 * serviceItem.score、以及订单状态推进。
 * orderId 上有唯一索引，保证一单只能评一次。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态：隐藏 */
    public static final Integer STATUS_HIDDEN = 0;
    /** 状态：显示 */
    public static final Integer STATUS_VISIBLE = 1;

    private Long id;

    //订单id（唯一，一单一评）
    private Long orderId;

    //评价用户id
    private Long userId;

    //被评价的服务人员id
    private Long providerId;

    //被评价的服务项目id
    private Long serviceId;

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

    //评价图片url，多个用逗号分隔
    private String images;

    //是否匿名 0否 1是
    private Integer isAnonymous;

    //平台/商家回复内容
    private String reply;

    //回复时间
    private LocalDateTime replyTime;

    //状态 0隐藏 1显示
    private Integer status;

    //评价时间
    private LocalDateTime createTime;
}
