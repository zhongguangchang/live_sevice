package com.sky.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 可预约时段查询入参（用户端下单页用）
 */
@Data
public class SlotQueryDTO implements Serializable {

    //服务项目id
    private Long serviceId;

    //服务日期
    private LocalDate serviceDate;

    //服务人员id（可空，为空则查全平台可约师傅）
    private Long providerId;
}
