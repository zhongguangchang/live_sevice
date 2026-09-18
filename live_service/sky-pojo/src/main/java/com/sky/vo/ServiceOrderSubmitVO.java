package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提交订单返回结果
 * <p>
 * 只返回前端继续支付所必需的信息，不返回整个订单对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrderSubmitVO implements Serializable {

    //订单id
    private Long id;

    //订单号
    private String orderNumber;

    //订单金额
    private BigDecimal orderAmount;

    //下单时间
    private LocalDateTime orderTime;

    //预约的服务时间
    private LocalDateTime serviceTime;
}
