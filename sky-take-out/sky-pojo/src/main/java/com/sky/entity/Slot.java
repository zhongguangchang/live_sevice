package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 服务时段排期
 * <p>
 * 一行 = 某个师傅在某天某个时段、能接某个服务的名额。
 * 例如：张师傅 2026-09-20 09:00-11:00 深度保洁，共 1 个名额。
 * <p>
 * 库存双层结构（本项目核心亮点）：
 * <ul>
 *   <li>Redis：slot:stock:{slotId} 做预扣层，抢名额走 Lua 脚本原子扣减，
 *       高并发下完全不碰数据库，防止超卖</li>
 *   <li>MySQL：totalStock / bookedCount 做权威层，支付成功时用乐观锁
 *       （where booked_count &lt; total_stock）真正落账</li>
 *   <li>兜底：定时任务对账，修正两层不一致的数据</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Slot implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 时段状态：可预约 */
    public static final Integer STATUS_OPEN = 1;
    /** 时段状态：已关闭 */
    public static final Integer STATUS_CLOSED = 0;

    private Long id;

    //服务人员id
    private Long providerId;

    //服务项目id
    private Long serviceId;

    //服务日期
    private LocalDate serviceDate;

    //时段开始时间
    private LocalTime startTime;

    //时段结束时间
    private LocalTime endTime;

    //该时段总名额
    private Integer totalStock;

    //已预约数量（含已下单未支付的占用）
    private Integer bookedCount;

    //时段状态 1可预约 0已关闭
    private Integer status;

    //乐观锁版本号
    private Integer version;

    //商户id（预留多商户）
    private Long merchantId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
