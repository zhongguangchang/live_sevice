package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 服务时段视图对象
 * <p>
 * 用户端下单页选时段时用。remainStock 是实时剩余名额——
 * 注意这里读的是 Redis 预扣层的值，不是数据库的 booked_count，
 * 因为数据库那边还没落账，只有 Redis 才知道此刻真实还剩多少。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotVO implements Serializable {

    private Long id;

    private Long providerId;

    private Long serviceId;

    private LocalDate serviceDate;

    private LocalTime startTime;

    private LocalTime endTime;

    //总名额
    private Integer totalStock;

    //已预约数
    private Integer bookedCount;

    //实时剩余名额（读 Redis）
    private Integer remainStock;

    //时段状态 1可预约 0已关闭
    private Integer status;

    //师傅姓名
    private String providerName;

    //师傅头像
    private String providerAvatar;

    //师傅评分
    private java.math.BigDecimal providerScore;
}
