package com.sky.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

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
    //必须显式声明 ISO 格式：Spring 对 LocalDate 的默认转换用的是
    //本地化短格式（2026/9/19），不认 yyyy-MM-dd，
    //不写这行查询参数会直接报 400
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate serviceDate;

    //服务人员id（可空，为空则查全平台可约师傅）
    private Long providerId;
}
