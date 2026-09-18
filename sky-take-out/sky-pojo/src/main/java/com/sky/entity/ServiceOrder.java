package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务订单
 * <p>
 * 对应原项目的 Orders 订单表。本质区别：原来卖的是「送到家」，
 * 现在卖的是「上门做 / 到店做」，所以删掉了配送相关字段
 * （打包费、餐具数量、预计送达时间），换成了预约与服务人员相关字段。
 * <p>
 * 订单状态流转：
 * <pre>
 *   1 待付款 --支付成功--&gt; 2 待接单 --派单+师傅接单--&gt; 3 已接单
 *     |                      |                          |
 *     |超时/取消              |拒单                       |师傅上门
 *     v                      v                          v
 *   7 已取消 &lt;--------------+                    4 服务中
 *                                                        |
 *                                                 服务完成|
 *                                                        v
 *                                      6 已完成 &lt;--评价-- 5 待评价
 *                                                        |
 *                                           7天未评价自动完成 -&gt; 6 已完成
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 订单状态：待付款 */
    public static final Integer PENDING_PAYMENT = 1;
    /** 订单状态：待接单（已派单，等师傅接） */
    public static final Integer TO_BE_ACCEPTED = 2;
    /** 订单状态：已接单（待上门 / 待服务） */
    public static final Integer ACCEPTED = 3;
    /** 订单状态：服务中 */
    public static final Integer IN_SERVICE = 4;
    /** 订单状态：待评价 */
    public static final Integer TO_BE_REVIEWED = 5;
    /** 订单状态：已完成 */
    public static final Integer COMPLETED = 6;
    /** 订单状态：已取消 */
    public static final Integer CANCELLED = 7;

    /** 支付状态：未支付 */
    public static final Integer UN_PAID = 0;
    /** 支付状态：已支付 */
    public static final Integer PAID = 1;
    /** 支付状态：已退款 */
    public static final Integer REFUND = 2;

    /** 服务方式：上门服务 */
    public static final Integer MODE_HOME = 1;
    /** 服务方式：到店服务 */
    public static final Integer MODE_STORE = 2;

    /** 支付方式：微信 */
    public static final Integer PAY_WECHAT = 1;
    /** 支付方式：支付宝 */
    public static final Integer PAY_ALIPAY = 2;

    private Long id;

    //订单号（业务主键，唯一）
    private String number;

    //订单状态 1待付款 2待接单 3已接单 4服务中 5待评价 6已完成 7已取消
    private Integer status;

    //下单用户id
    private Long userId;

    //服务地址id（上门服务必填，到店服务为空）
    private Long addressBookId;

    //指派的服务人员id（派单后回填）
    private Long providerId;

    //预约的时段id
    private Long slotId;

    //服务方式 1上门服务 2到店服务
    private Integer serviceMode;

    //预约上门/到店的具体时间
    private LocalDateTime serviceTime;

    //预计服务时长（分钟）
    private Integer serviceDuration;

    //订单实收金额
    private BigDecimal amount;

    //支付方式 1微信 2支付宝
    private Integer payMethod;

    //支付状态 0未支付 1已支付 2已退款
    private Integer payStatus;

    //下单时间
    private LocalDateTime orderTime;

    //结账（支付）时间
    private LocalDateTime checkoutTime;

    //下单用户名
    private String userName;

    //联系人姓名
    private String consignee;

    //联系电话
    private String phone;

    //服务地址（完整拼接后的字符串快照）
    private String address;

    //订单备注
    private String remark;

    //到店核销码（到店服务用，6位数字）
    private String verifyCode;

    //核销时间
    private LocalDateTime verifyTime;

    //派单时间
    private LocalDateTime dispatchTime;

    //师傅接单时间
    private LocalDateTime acceptTime;

    //开始服务时间
    private LocalDateTime startTime;

    //服务完成时间
    private LocalDateTime finishTime;

    //订单取消原因
    private String cancelReason;

    //师傅拒单原因
    private String rejectionReason;

    //订单取消时间
    private LocalDateTime cancelTime;

    //商户id（预留多商户）
    private Long merchantId;
}
