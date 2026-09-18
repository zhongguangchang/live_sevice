package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 服务清单（购物车）操作入参
 * <p>
 * 对应原项目的 ShoppingCartDTO，把菜品/套餐改成服务项目/服务套餐。
 * spec 传 JSON 字符串，如 {"房屋面积":"60-90平方米"}。
 */
@Data
public class ServiceCartDTO implements Serializable {

    //服务项目id（与套餐id二选一）
    private Long serviceId;

    //服务套餐id（与项目id二选一）
    private Long servicePackageId;

    //选中的规格，JSON串
    private String spec;
}
