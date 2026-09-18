package com.sky.vo;

import com.sky.entity.ServiceOrder;
import com.sky.entity.ServiceOrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务订单视图对象
 * <p>
 * 继承 ServiceOrder，额外带上订单明细和师傅信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    //订单主体信息
    private ServiceOrder order;

    //订单明细
    private List<ServiceOrderItem> items = new ArrayList<>();

    //服务项目名称拼接串，如「深度保洁 x1；玻璃清洗 x2」，管理端列表展示用
    private String serviceNames;

    //服务人员姓名
    private String providerName;

    //服务人员电话
    private String providerPhone;

    //服务人员头像
    private String providerAvatar;
}
