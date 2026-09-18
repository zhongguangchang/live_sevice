package com.sky.constant;

/**
 * 信息提示常量类
 */
public class MessageConstant {

    public static final String PASSWORD_ERROR = "密码错误";
    public static final String ACCOUNT_NOT_FOUND = "账号不存在";
    public static final String ACCOUNT_LOCKED = "账号被锁定";
    public static final String ALREADY_EXISTS = "已存在";
    public static final String UNKNOWN_ERROR = "未知错误";
    public static final String USER_NOT_LOGIN = "用户未登录";
    public static final String CATEGORY_BE_RELATED_BY_SETMEAL = "当前分类关联了套餐,不能删除";
    public static final String CATEGORY_BE_RELATED_BY_DISH = "当前分类关联了菜品,不能删除";
    public static final String SHOPPING_CART_IS_NULL = "购物车数据为空，不能下单";
    public static final String ADDRESS_BOOK_IS_NULL = "用户地址为空，不能下单";
    public static final String LOGIN_FAILED = "登录失败";
    public static final String UPLOAD_FAILED = "文件上传失败";
    public static final String SETMEAL_ENABLE_FAILED = "套餐内包含未启售菜品，无法启售";
    public static final String PASSWORD_EDIT_FAILED = "密码修改失败";
    public static final String DISH_ON_SALE = "起售中的菜品不能删除";
    public static final String SETMEAL_ON_SALE = "起售中的套餐不能删除";
    public static final String DISH_BE_RELATED_BY_SETMEAL = "当前菜品关联了套餐,不能删除";
    public static final String ORDER_STATUS_ERROR = "订单状态错误";
    public static final String ORDER_NOT_FOUND = "订单不存在";

    // ===== 生活服务网新增 =====
    public static final String SLOT_SOLD_OUT = "该时段已约满，请选择其他时段";
    public static final String SLOT_NOT_FOUND = "预约时段不存在或已关闭";
    public static final String SLOT_ALREADY_BOOKED = "你已预约该时段，请勿重复下单";
    public static final String SERVICE_AREA_NOT_OPEN = "该区域暂未开通服务";
    public static final String PROVIDER_NOT_AVAILABLE = "该服务人员当前不可接单";
    public static final String SERVICE_ITEM_ON_SALE = "起售中的服务项目不能删除";
    public static final String SERVICE_ITEM_BE_RELATED_BY_PACKAGE = "当前服务项目关联了套餐,不能删除";
    public static final String CATEGORY_BE_RELATED_BY_SERVICE_PACKAGE = "当前分类关联了服务套餐,不能删除";
    public static final String CATEGORY_BE_RELATED_BY_SERVICE_ITEM = "当前分类关联了服务项目,不能删除";
    public static final String REVIEW_ALREADY_EXISTS = "该订单已评价过了";
    public static final String ORDER_NOT_COMPLETED = "订单尚未完成服务，不能评价";
    public static final String RATE_LIMIT_EXCEEDED = "操作过于频繁，请稍后再试";
    public static final String SERVICE_PACKAGE_ENABLE_FAILED = "套餐内包含已停售的服务项目，无法启用";
    public static final String NEW_PASSWORD_SAME_AS_OLD = "新密码不能与旧密码相同";

}
