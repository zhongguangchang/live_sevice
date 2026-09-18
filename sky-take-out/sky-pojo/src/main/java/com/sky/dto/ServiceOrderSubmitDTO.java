package com.sky.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 用户提交订单入参
 * <p>
 * 对应原项目的 OrdersSubmitDTO，把「预计送达时间」换成了「预约时段」。
 * 注意这里只传 slotId，不传具体时间——具体服务时间由服务端根据 slot
 * 反查生成，防止前端伪造时间绕过时段校验。
 */
@Data
public class ServiceOrderSubmitDTO implements Serializable {

    //服务地址id（上门服务必填，到店服务为空）
    private Long addressBookId;

    //预约的时段id
    private Long slotId;

    //服务方式 1上门服务 2到店服务
    private Integer serviceMode;

    //付款方式 1微信 2支付宝
    private Integer payMethod;

    //订单备注
    private String remark;

    //总金额（服务端会重新核算，不信任前端传的值）
    private BigDecimal amount;
}
