package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 服务人员（师傅）
 * <p>
 * 与 {@link Employee} 的区别：Employee 是坐办公室用管理后台的运营人员，
 * Provider 是真正上门干活的师傅。师傅通常是外部合作、按单结算，
 * 可以没有后台账号，通过师傅端小程序接单。
 * <p>
 * score / goodRate / orderCount 由评价模块与订单模块异步回写，
 * 是派单排序（优先派给评分高、负担轻的师傅）的依据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Provider implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 接单状态：可接单 */
    public static final Integer STATUS_AVAILABLE = 1;
    /** 接单状态：忙碌 */
    public static final Integer STATUS_BUSY = 2;
    /** 接单状态：休息中 */
    public static final Integer STATUS_RESTING = 3;
    /** 接单状态：已离职 */
    public static final Integer STATUS_QUIT = 4;

    private Long id;

    //关联的后台账号id（可为空）
    private Long employeeId;

    //师傅端小程序微信openid
    private String openid;

    //师傅姓名
    private String name;

    //联系电话
    private String phone;

    //头像url
    private String avatar;

    //性别 0女 1男
    private String sex;

    //身份证号
    private String idNumber;

    //资质证书图片url（健康证/电工证等，合规用）
    private String certImage;

    //从业年限
    private Integer workYears;

    //个人简介
    private String intro;

    //可提供的服务方式 1上门 2到店 3都支持
    private Integer serviceMode;

    //接单状态 1可接单 2忙碌 3休息中 4已离职
    private Integer status;

    //综合评分（评价模块回写）
    private BigDecimal score;

    //好评率百分比（评价模块回写）
    private BigDecimal goodRate;

    //累计完成订单数
    private Integer orderCount;

    //商户id（预留多商户）
    private Long merchantId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;
}
