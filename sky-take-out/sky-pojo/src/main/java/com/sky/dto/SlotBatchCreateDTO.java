package com.sky.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 批量生成排期入参
 * <p>
 * 运营在管理端给师傅排班：选一个日期区间、一组时段、勾选能做的服务，
 * 系统按「日期 × 时段 × 服务」笛卡尔积批量生成 slot 记录。
 * 这是 schedule 数据的唯一入口。
 */
@Data
public class SlotBatchCreateDTO implements Serializable {

    //服务人员id列表（可以一次给多个师傅排班）
    private List<Long> providerIds;

    //服务项目id列表
    private List<Long> serviceIds;

    //排期开始日期
    private LocalDate startDate;

    //排期结束日期（含）
    private LocalDate endDate;

    //每天要生成的时段开始时间列表，如 09:00、14:00
    private List<LocalTime> startTimes;

    //每个时段的时长（分钟），用于自动推算 end_time
    private Integer slotDuration;

    //每个时段的名额，默认 1
    private Integer totalStock;
}
