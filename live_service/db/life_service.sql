-- ============================================================================
--  生 活 服 务 网  数 据 库 设 计
-- ----------------------------------------------------------------------------
--  数据库名 : life_service
--  来源     : 基于「苍穹外卖 sky_take_out」改造
--  字符集   : utf8mb4 / utf8mb4_general_ci
--  说明     : 所有表统一使用 CREATE TABLE IF NOT EXISTS，可重复执行
--             所有表均预留 merchant_id 字段（默认 1），为将来升级多商户平台留口子
-- ============================================================================

CREATE DATABASE IF NOT EXISTS life_service
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE life_service;

SET NAMES utf8mb4;


-- ============================================================================
--  域一 · 用户与权限
-- ============================================================================

-- ----------------------------------------------------------------------------
--  employee  平台员工表（管理端登录账号）
--  说明：管理端后台的登录账号。生活服务网里员工分为三种角色：
--        超级管理员（管账号、看全部数据）、运营（管服务项目/套餐/评价）、
--        派单员（只管订单和派单）。师傅不在这张表里，师傅在 provider 表。
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `employee`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(32)  NOT NULL COMMENT '登录用户名',
    `name`        VARCHAR(32)  NOT NULL COMMENT '姓名',
    `password`    VARCHAR(64)  NOT NULL COMMENT '密码（MD5 加密存储）',
    `phone`       VARCHAR(11)  NOT NULL COMMENT '手机号',
    `sex`         VARCHAR(2)   NOT NULL COMMENT '性别 0女 1男',
    `id_number`   VARCHAR(18)  NOT NULL COMMENT '身份证号',
    `role`        TINYINT      NOT NULL DEFAULT 3 COMMENT '角色 1超级管理员 2运营 3派单员',
    `status`      INT          NOT NULL DEFAULT 1 COMMENT '账号状态 0锁定 1正常',
    `create_time` DATETIME              DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME              DEFAULT NULL COMMENT '更新时间',
    `create_user` BIGINT                DEFAULT NULL COMMENT '创建人id',
    `update_user` BIGINT                DEFAULT NULL COMMENT '修改人id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='平台员工表';


-- ----------------------------------------------------------------------------
--  user  用户表（小程序端用户）
--  说明：小程序用户通过微信登录（openid 唯一）。phone 首次下单时通过
--        微信手机号快捷验证补全，用于师傅上门联系。
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user`
(
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `openid`          VARCHAR(45) NOT NULL COMMENT '微信用户唯一标识',
    `unionid`         VARCHAR(45)          DEFAULT NULL COMMENT '微信开放平台唯一标识（多端打通用）',
    `name`            VARCHAR(32)          DEFAULT NULL COMMENT '姓名',
    `phone`           VARCHAR(11)          DEFAULT NULL COMMENT '手机号',
    `sex`             VARCHAR(2)           DEFAULT NULL COMMENT '性别 0女 1男',
    `id_number`       VARCHAR(18)          DEFAULT NULL COMMENT '身份证号',
    `avatar`          VARCHAR(500)         DEFAULT NULL COMMENT '头像url',
    `status`          TINYINT     NOT NULL DEFAULT 1 COMMENT '账号状态 0禁用 1正常',
    `create_time`     DATETIME             DEFAULT NULL COMMENT '注册时间',
    `last_login_time` DATETIME             DEFAULT NULL COMMENT '最近登录时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_openid` (`openid`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';


-- ============================================================================
--  域二 · 服务商品
-- ============================================================================

-- ----------------------------------------------------------------------------
--  category  服务分类表
--  说明：对应小程序首页顶部的分类导航，例如「家政保洁 / 家电维修 / 搬家
--        运输 / 美容美发 / 开锁换锁 / 管道疏通 / 跑腿代办 / 宠物服务」。
--        type 区分这个分类下面挂的是单项服务还是组合套餐。
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `category`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `type`        INT         NOT NULL DEFAULT 1 COMMENT '分类类型 1服务项目分类 2服务套餐分类',
    `name`        VARCHAR(32) NOT NULL COMMENT '分类名称',
    `icon`        VARCHAR(500)         DEFAULT NULL COMMENT '分类图标url（小程序首页金刚区使用）',
    `sort`        INT         NOT NULL DEFAULT 0 COMMENT '排序（升序，数字越小越靠前）',
    `status`      INT         NOT NULL DEFAULT 1 COMMENT '状态 0禁用 1启用',
    `merchant_id` BIGINT      NOT NULL DEFAULT 1 COMMENT '商户id（预留多商户，单商户固定为1）',
    `create_time` DATETIME             DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME             DEFAULT NULL COMMENT '更新时间',
    `create_user` BIGINT               DEFAULT NULL COMMENT '创建人id',
    `update_user` BIGINT               DEFAULT NULL COMMENT '修改人id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`),
    KEY `idx_type_status` (`type`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='服务分类表';


-- ----------------------------------------------------------------------------
--  service_item  服务项目表  ★核心表（替代原 dish 菜品表）
--  说明：平台售卖的最小服务单元，例如「深度保洁3小时」「空调加氟」
--        「上门开锁」「管道疏通」。这是用户在小程序里直接下单的东西。
--        duration 用于计算排期占用的时段长度；
--        sales / score 是冗余字段，列表页展示和排序用，避免每次 count 订单表。
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `service_item`
(
    `id`             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`           VARCHAR(64)    NOT NULL COMMENT '服务名称',
    `category_id`    BIGINT         NOT NULL COMMENT '服务分类id',
    `price`          DECIMAL(10, 2) NOT NULL COMMENT '服务价格',
    `original_price` DECIMAL(10, 2)          DEFAULT NULL COMMENT '原价（划线价，用于展示优惠）',
    `image`          VARCHAR(500)            DEFAULT NULL COMMENT '服务图片url',
    `description`    VARCHAR(500)            DEFAULT NULL COMMENT '服务描述',
    `detail`         TEXT                    DEFAULT NULL COMMENT '服务详情（富文本，说明服务内容/注意事项）',
    `unit`           VARCHAR(16)    NOT NULL DEFAULT '次' COMMENT '计价单位 次/小时/平方米/台',
    `duration`       INT            NOT NULL DEFAULT 60 COMMENT '服务时长（分钟），用于排期时段占用计算',
    `service_mode`   TINYINT        NOT NULL DEFAULT 1 COMMENT '服务方式 1上门服务 2到店服务 3两者都支持',
    `need_appoint`   TINYINT        NOT NULL DEFAULT 1 COMMENT '是否需要预约 0否 1是',
    `sales`          INT            NOT NULL DEFAULT 0 COMMENT '累计销量（冗余字段，列表展示用）',
    `score`          DECIMAL(3, 2)  NOT NULL DEFAULT 5.00 COMMENT '综合评分（冗余字段，由评价模块回写）',
    `status`         INT            NOT NULL DEFAULT 1 COMMENT '状态 0停售 1起售',
    `merchant_id`    BIGINT         NOT NULL DEFAULT 1 COMMENT '商户id（预留多商户）',
    `create_time`    DATETIME                DEFAULT NULL COMMENT '创建时间',
    `update_time`    DATETIME                DEFAULT NULL COMMENT '更新时间',
    `create_user`    BIGINT                  DEFAULT NULL COMMENT '创建人id',
    `update_user`    BIGINT                  DEFAULT NULL COMMENT '修改人id',
    PRIMARY KEY (`id`),
    KEY `idx_category_status` (`category_id`, `status`),
    KEY `idx_sales` (`sales`),
    KEY `idx_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='服务项目表';


-- ----------------------------------------------------------------------------
--  service_spec  服务规格表（替代原 dish_flavor 菜品口味表）
--  说明：一个服务项目下面的可选项。比如「深度保洁」按房屋面积分档：
--        60㎡以下 / 60-90㎡ / 90㎡以上，不同档位价格不同，所以有 price_delta。
--        「空调加氟」按匹数分档。「维修」不选规格但选紧急程度。
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `service_spec`
(
    `id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `service_id`  BIGINT         NOT NULL COMMENT '所属服务项目id',
    `name`        VARCHAR(32)    NOT NULL COMMENT '规格名（如：房屋面积）',
    `value`       VARCHAR(64)    NOT NULL COMMENT '规格值（如：60-90平方米）',
    `price_delta` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '加价金额（正数加价，0为不加价）',
    `sort`        INT            NOT NULL DEFAULT 0 COMMENT '同规格下的排序',
    PRIMARY KEY (`id`),
    KEY `idx_service_id` (`service_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='服务规格表';


-- ----------------------------------------------------------------------------
--  service_package  服务套餐表（替代原 setmeal 套餐表）
--  说明：把多个服务项目打包优惠售卖，例如「新居开荒保洁套餐 = 深度保洁 +
--        玻璃清洗 + 甲醛检测」「搬家套餐 = 搬运 + 打包 + 拆装家具」。
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `service_package`
(
    `id`             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `category_id`    BIGINT         NOT NULL COMMENT '服务套餐分类id',
    `name`           VARCHAR(64)    NOT NULL COMMENT '套餐名称',
    `price`          DECIMAL(10, 2) NOT NULL COMMENT '套餐价格',
    `original_price` DECIMAL(10, 2)          DEFAULT NULL COMMENT '套餐原价（各项单买之和）',
    `image`          VARCHAR(500)            DEFAULT NULL COMMENT '套餐图片url',
    `description`    VARCHAR(500)            DEFAULT NULL COMMENT '套餐描述',
    `duration`       INT            NOT NULL DEFAULT 60 COMMENT '套餐总时长（分钟）',
    `service_mode`   TINYINT        NOT NULL DEFAULT 1 COMMENT '服务方式 1上门 2到店 3都支持',
    `sales`          INT            NOT NULL DEFAULT 0 COMMENT '累计销量',
    `status`         INT            NOT NULL DEFAULT 1 COMMENT '状态 0停用 1启用',
    `merchant_id`    BIGINT         NOT NULL DEFAULT 1 COMMENT '商户id（预留多商户）',
    `create_time`    DATETIME                DEFAULT NULL COMMENT '创建时间',
    `update_time`    DATETIME                DEFAULT NULL COMMENT '更新时间',
    `create_user`    BIGINT                  DEFAULT NULL COMMENT '创建人id',
    `update_user`    BIGINT                  DEFAULT NULL COMMENT '修改人id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`),
    KEY `idx_category_status` (`category_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='服务套餐表';


-- ----------------------------------------------------------------------------
--  service_package_item  套餐明细表（替代原 setmeal_dish 表）
--  说明：套餐里包含哪些服务项目、各几份。下架某个服务项目时，需要反查
--        这张表判断它是否还被套餐引用，被引用则不允许删除（对应原项目
--        DeletionNotAllowedException 的校验逻辑）。
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `service_package_item`
(
    `id`                 BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `service_package_id` BIGINT         NOT NULL COMMENT '套餐id',
    `service_id`         BIGINT         NOT NULL COMMENT '服务项目id',
    `name`               VARCHAR(64)    NOT NULL COMMENT '服务名称（冗余，防止服务改名后历史套餐显示错乱）',
    `price`              DECIMAL(10, 2) NOT NULL COMMENT '服务单价（冗余快照）',
    `copies`             INT            NOT NULL DEFAULT 1 COMMENT '份数',
    PRIMARY KEY (`id`),
    KEY `idx_package_id` (`service_package_id`),
    KEY `idx_service_id` (`service_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='服务套餐明细表';


-- ============================================================================
--  域三 · 服务人员
-- ============================================================================

-- ----------------------------------------------------------------------------
--  provider  服务人员表（师傅）  ★新增核心
--  说明：上门服务的执行者。与 employee 的区别：employee 是坐办公室用后台的
--        运营人员，provider 是干活的师傅。师傅通常是外部合作、按单结算，
--        所以单独一张表，可以没有后台账号，用小程序师傅端接单。
--        status 决定派单时能不能选到他；score / good_rate 由评价模块回写，
--        是派单排序（优先派给评分高的师傅）的依据。
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `provider`
(
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `employee_id`  BIGINT                 DEFAULT NULL COMMENT '关联的后台账号id（可为空，师傅不一定有后台账号）',
    `openid`       VARCHAR(45)            DEFAULT NULL COMMENT '师傅端小程序的微信openid',
    `name`         VARCHAR(32)   NOT NULL COMMENT '师傅姓名',
    `phone`        VARCHAR(11)   NOT NULL COMMENT '联系电话',
    `avatar`       VARCHAR(500)           DEFAULT NULL COMMENT '头像url',
    `sex`          VARCHAR(2)             DEFAULT NULL COMMENT '性别 0女 1男',
    `id_number`    VARCHAR(18)            DEFAULT NULL COMMENT '身份证号',
    `cert_image`   VARCHAR(500)           DEFAULT NULL COMMENT '资质证书图片url（健康证/电工证等，合规用）',
    `work_years`   INT           NOT NULL DEFAULT 0 COMMENT '从业年限',
    `intro`        VARCHAR(500)           DEFAULT NULL COMMENT '个人简介',
    `service_mode` TINYINT       NOT NULL DEFAULT 1 COMMENT '可提供的服务方式 1上门 2到店 3都支持',
    `status`       TINYINT       NOT NULL DEFAULT 1 COMMENT '接单状态 1可接单 2忙碌 3休息中 4已离职',
    `score`        DECIMAL(3, 2) NOT NULL DEFAULT 5.00 COMMENT '综合评分（评价模块回写）',
    `good_rate`    DECIMAL(5, 2) NOT NULL DEFAULT 100.00 COMMENT '好评率百分比（评价模块回写）',
    `order_count`  INT           NOT NULL DEFAULT 0 COMMENT '累计完成订单数',
    `merchant_id`  BIGINT        NOT NULL DEFAULT 1 COMMENT '商户id（预留多商户）',
    `create_time`  DATETIME               DEFAULT NULL COMMENT '创建时间',
    `update_time`  DATETIME               DEFAULT NULL COMMENT '更新时间',
    `create_user`  BIGINT                 DEFAULT NULL COMMENT '创建人id',
    `update_user`  BIGINT                 DEFAULT NULL COMMENT '修改人id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_status` (`status`),
    KEY `idx_score` (`score`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='服务人员（师傅）表';


-- ----------------------------------------------------------------------------
--  provider_skill  服务人员技能表  ★新增
--  说明：师傅会干什么，多对多关系。派单时的第一层过滤条件——
--        只把订单派给「掌握了该服务所属分类」的师傅。
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `provider_skill`
(
    `id`          BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `provider_id` BIGINT NOT NULL COMMENT '服务人员id',
    `category_id` BIGINT NOT NULL COMMENT '擅长的服务分类id',
    `level`       TINYINT NOT NULL DEFAULT 1 COMMENT '技能等级 1初级 2中级 3高级（可影响派单优先级）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_provider_category` (`provider_id`, `category_id`),
    KEY `idx_category_id` (`category_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='服务人员技能表';


-- ============================================================================
--  域四 · 排期与时段   （Redis 并发亮点的依托）
-- ============================================================================

-- ----------------------------------------------------------------------------
--  slot  服务时段排期表  ★新增核心（Redis 预扣库存的数据库依据）
--  说明：一行 = 「某个师傅在某天某个时段，能接某个服务的名额」。
--        例如：张师傅 2026-09-20 09:00-11:00 深度保洁，共 1 个名额。
--
--  库存双层结构（本项目的核心亮点）：
--    · Redis  : slot:stock:{slotId} 做预扣层，抢名额走 Lua 脚本原子扣减，
--               高并发下不碰数据库，防止超卖。
--    · MySQL  : total_stock / booked_count 做权威层，支付成功时用乐观锁
--               （where booked_count < total_stock）真正落账。
--    · 兜底   : OrderTask 定时任务每 5 分钟对账，修正两层不一致的数据。
--
--  唯一索引 uk_provider_slot 保证同一师傅同一天同一开始时间不会生成重复时段。
--  version 字段是 MyBatis-Plus 风格的乐观锁版本号，用原生 SQL 时可直接加在
--  update 的 where 条件里（where id = ? and version = ?）。
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `slot`
(
    `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `provider_id`  BIGINT   NOT NULL COMMENT '服务人员id',
    `service_id`   BIGINT   NOT NULL COMMENT '服务项目id',
    `service_date` DATE     NOT NULL COMMENT '服务日期',
    `start_time`   TIME     NOT NULL COMMENT '时段开始时间',
    `end_time`     TIME     NOT NULL COMMENT '时段结束时间',
    `total_stock`  INT      NOT NULL DEFAULT 1 COMMENT '该时段总名额',
    `booked_count` INT      NOT NULL DEFAULT 0 COMMENT '已预约数量（含已下单未支付的占用）',
    `status`       TINYINT  NOT NULL DEFAULT 1 COMMENT '时段状态 1可预约 0已关闭',
    `version`      INT      NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `merchant_id`  BIGINT   NOT NULL DEFAULT 1 COMMENT '商户id（预留多商户）',
    `create_time`  DATETIME          DEFAULT NULL COMMENT '创建时间',
    `update_time`  DATETIME          DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_provider_slot` (`provider_id`, `service_date`, `start_time`),
    KEY `idx_service_date` (`service_id`, `service_date`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='服务时段排期表';


-- ============================================================================
--  域五 · 交易
-- ============================================================================

-- ----------------------------------------------------------------------------
--  shopping_cart  服务清单表（替代原购物车）
--  说明：严格来说生活服务不叫「购物车」，叫「待预约清单」更贴切。
--        用户可以先把几个服务加进清单，最后一起选时段提交成一个订单。
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `shopping_cart`
(
    `id`                 BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`               VARCHAR(64)    NOT NULL COMMENT '服务名称（冗余快照）',
    `user_id`            BIGINT         NOT NULL COMMENT '用户id',
    `service_id`         BIGINT                  DEFAULT NULL COMMENT '服务项目id（与套餐id二选一）',
    `service_package_id` BIGINT                  DEFAULT NULL COMMENT '服务套餐id（与项目id二选一）',
    `spec`               VARCHAR(128)            DEFAULT NULL COMMENT '选中的规格，JSON 串，如 {"房屋面积":"60-90平方米"}',
    `number`             INT            NOT NULL DEFAULT 1 COMMENT '数量',
    `amount`             DECIMAL(10, 2) NOT NULL COMMENT '金额（含规格加价后的实际单价）',
    `image`              VARCHAR(500)            DEFAULT NULL COMMENT '图片',
    `create_time`        DATETIME                DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='服务清单表';


-- ----------------------------------------------------------------------------
--  service_order  服务订单表  ★核心表（替代原 orders 表）
--  说明：注意这张表和原 orders 表的本质区别——原来卖的是「送到家」，
--        现在卖的是「上门做/到店做」，所以：
--          · 删掉了配送相关字段（打包费、餐具数量、预计送达时间）
--          · 新增了预约相关字段（slot_id / service_time / service_duration）
--          · 新增了服务人员字段（provider_id，派单后回填）
--          · 新增了到店核销字段（verify_code / verify_time）
--
--  订单状态流转（status）：
--    1 待付款 ──支付成功──→ 2 待接单 ──派单+师傅接单──→ 3 已接单
--      │                       │                            │
--      │超时/取消               │拒单                        │师傅上门
--      ↓                       ↓                            ↓
--    7 已取消 ←───────────────┘                      4 服务中
--                                                           │
--                                                    服务完成│
--                                                           ↓
--                                         6 已完成 ←──评价── 5 待评价
--                                                           │
--                                              7天未评价自动完成→ 6 已完成
--
--  支付状态（pay_status）：0 未支付  1 已支付  2 已退款
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `service_order`
(
    `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `number`          VARCHAR(50)    NOT NULL COMMENT '订单号（业务主键，唯一）',
    `status`          TINYINT        NOT NULL DEFAULT 1 COMMENT '订单状态 1待付款 2待接单 3已接单 4服务中 5待评价 6已完成 7已取消',
    `user_id`         BIGINT         NOT NULL COMMENT '下单用户id',
    `address_book_id` BIGINT                  DEFAULT NULL COMMENT '服务地址id（上门服务必填，到店服务为空）',
    `provider_id`     BIGINT                  DEFAULT NULL COMMENT '指派的服务人员id（派单后回填）',
    `slot_id`         BIGINT                  DEFAULT NULL COMMENT '预约的时段id',

    -- 预约信息
    `service_mode`    TINYINT        NOT NULL DEFAULT 1 COMMENT '服务方式 1上门服务 2到店服务',
    `service_time`    DATETIME                DEFAULT NULL COMMENT '预约上门/到店的具体时间（由 slot 的日期+开始时间拼出）',
    `service_duration` INT           NOT NULL DEFAULT 60 COMMENT '预计服务时长（分钟）',

    -- 金额与支付
    `amount`          DECIMAL(10, 2) NOT NULL COMMENT '订单实收金额',
    `pay_method`      TINYINT                 DEFAULT NULL COMMENT '支付方式 1微信 2支付宝 3余额',
    `pay_status`      TINYINT        NOT NULL DEFAULT 0 COMMENT '支付状态 0未支付 1已支付 2已退款',
    `order_time`      DATETIME                DEFAULT NULL COMMENT '下单时间',
    `checkout_time`   DATETIME                DEFAULT NULL COMMENT '结账（支付）时间',

    -- 联系人快照（下单时从 address_book 冗余过来，防止用户改地址影响历史订单）
    `user_name`       VARCHAR(32)             DEFAULT NULL COMMENT '下单用户名',
    `consignee`       VARCHAR(32)             DEFAULT NULL COMMENT '联系人姓名',
    `phone`           VARCHAR(11)             DEFAULT NULL COMMENT '联系电话',
    `address`         VARCHAR(255)            DEFAULT NULL COMMENT '服务地址（完整拼接后的字符串）',
    `remark`          VARCHAR(255)            DEFAULT NULL COMMENT '订单备注',

    -- 到店核销
    `verify_code`     VARCHAR(16)             DEFAULT NULL COMMENT '到店核销码（到店服务用，6位数字）',
    `verify_time`     DATETIME                DEFAULT NULL COMMENT '核销时间',

    -- 服务过程时间戳（用于计算响应速度、服务时长统计）
    `dispatch_count`  TINYINT        NOT NULL DEFAULT 0 COMMENT '派单次数（含自动转派）。超过 3 次就停止自动转派、转人工处理，避免订单每 5 分钟无限换人',
    `dispatch_time`   DATETIME                DEFAULT NULL COMMENT '派单时间',
    `accept_time`     DATETIME                DEFAULT NULL COMMENT '师傅接单时间',
    `start_time`      DATETIME                DEFAULT NULL COMMENT '开始服务时间',
    `finish_time`     DATETIME                DEFAULT NULL COMMENT '服务完成时间',

    -- 取消与拒单
    `cancel_reason`     VARCHAR(255)          DEFAULT NULL COMMENT '订单取消原因',
    `rejection_reason`  VARCHAR(255)          DEFAULT NULL COMMENT '师傅拒单原因',
    `cancel_time`       DATETIME              DEFAULT NULL COMMENT '订单取消时间',

    `merchant_id`     BIGINT         NOT NULL DEFAULT 1 COMMENT '商户id（预留多商户）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_number` (`number`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_status_order_time` (`status`, `order_time`),
    KEY `idx_provider_id` (`provider_id`),
    KEY `idx_slot_id` (`slot_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='服务订单表';


-- ----------------------------------------------------------------------------
--  service_order_item  订单明细表（替代原 order_detail 表）
--  说明：一单可以买多个服务。name / price / image 全部是下单瞬间的快照，
--        这样运营改价、换图、改名字都不会影响历史订单的展示。
--        spec 存用户选的规格，如 {"房屋面积":"60-90平方米"}。
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `service_order_item`
(
    `id`                 BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`           BIGINT         NOT NULL COMMENT '订单id',
    `service_id`         BIGINT                  DEFAULT NULL COMMENT '服务项目id',
    `service_package_id` BIGINT                  DEFAULT NULL COMMENT '服务套餐id',
    `name`               VARCHAR(64)    NOT NULL COMMENT '服务名称快照',
    `spec`               VARCHAR(128)            DEFAULT NULL COMMENT '选中的规格快照',
    `price`              DECIMAL(10, 2) NOT NULL COMMENT '单价快照（含规格加价）',
    `number`             INT            NOT NULL DEFAULT 1 COMMENT '数量',
    `amount`             DECIMAL(10, 2) NOT NULL COMMENT '小计金额 = price * number',
    `image`              VARCHAR(500)            DEFAULT NULL COMMENT '图片快照',
    `duration`           INT            NOT NULL DEFAULT 60 COMMENT '单项服务时长（分钟）',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_service_id` (`service_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单明细表';


-- ----------------------------------------------------------------------------
--  address_book  服务地址表
--  说明：沿用原项目结构。新增 location 字段存经纬度，两个用途：
--        1. 下单时校验地址是否落在 service_area 服务范围内
--        2. 派单时按距离就近分配师傅
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `address_book`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT       NOT NULL COMMENT '用户id',
    `consignee`     VARCHAR(50)  NOT NULL COMMENT '联系人',
    `phone`         VARCHAR(11)  NOT NULL COMMENT '联系电话',
    `sex`           VARCHAR(2)            DEFAULT NULL COMMENT '性别 0女 1男',
    `province_code` VARCHAR(12)           DEFAULT NULL COMMENT '省级区划编号',
    `province_name` VARCHAR(32)           DEFAULT NULL COMMENT '省级名称',
    `city_code`     VARCHAR(12)           DEFAULT NULL COMMENT '市级区划编号',
    `city_name`     VARCHAR(32)           DEFAULT NULL COMMENT '市级名称',
    `district_code` VARCHAR(12)           DEFAULT NULL COMMENT '区级区划编号',
    `district_name` VARCHAR(32)           DEFAULT NULL COMMENT '区级名称',
    `detail`        VARCHAR(200)          DEFAULT NULL COMMENT '详细地址',
    `label`         VARCHAR(100)          DEFAULT NULL COMMENT '标签 家/公司/学校',
    `location`      VARCHAR(64)           DEFAULT NULL COMMENT '经纬度，格式：经度,纬度（如 116.404,39.915）',
    `is_default`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认 0否 1是',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='服务地址表';


-- ============================================================================
--  域六 · 评价
-- ============================================================================

-- ----------------------------------------------------------------------------
--  review  评价表  ★新增核心
--  说明：生活服务的命脉。拆成三个维度打分而不是只给一个总分，因为
--        「师傅态度好但来得慢」和「来得快但活干得糙」需要区分开，这样
--        平台才知道该优化哪一环。
--        uk_order_id 保证一单只能评一次。
--        评价提交后要异步回写三处：provider.score / provider.good_rate、
--        service_item.score、以及订单状态推进到已完成。
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `review`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`      BIGINT       NOT NULL COMMENT '订单id',
    `user_id`       BIGINT       NOT NULL COMMENT '评价用户id',
    `provider_id`   BIGINT                DEFAULT NULL COMMENT '被评价的服务人员id',
    `service_id`    BIGINT                DEFAULT NULL COMMENT '被评价的服务项目id',
    `score`         TINYINT      NOT NULL COMMENT '综合评分 1-5',
    `service_score` TINYINT               DEFAULT NULL COMMENT '服务态度评分 1-5',
    `speed_score`   TINYINT               DEFAULT NULL COMMENT '响应速度评分 1-5',
    `quality_score` TINYINT               DEFAULT NULL COMMENT '服务质量评分 1-5',
    `content`       VARCHAR(500)          DEFAULT NULL COMMENT '评价文字内容',
    `images`        VARCHAR(2000)         DEFAULT NULL COMMENT '评价图片url，多个用逗号分隔',
    `is_anonymous`  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否匿名 0否 1是',
    `reply`         VARCHAR(500)          DEFAULT NULL COMMENT '平台/商家回复内容',
    `reply_time`    DATETIME              DEFAULT NULL COMMENT '回复时间',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 0隐藏 1显示',
    `create_time`   DATETIME              DEFAULT NULL COMMENT '评价时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_id` (`order_id`),
    KEY `idx_provider_id` (`provider_id`),
    KEY `idx_service_id` (`service_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='服务评价表';


-- ============================================================================
--  域七 · 服务区域
-- ============================================================================

-- ----------------------------------------------------------------------------
--  service_area  服务区域表  ★新增
--  说明：平台不是哪儿都上门。这张表定义了业务覆盖的行政区划。
--        用户下单时用地址的 district_code 来匹配，匹配不到就提示
--        「该区域暂未开通服务」。这是生活服务网比外卖多出来的一道业务校验。
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `service_area`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(32) NOT NULL COMMENT '区域名称（如：海淀区）',
    `code`        VARCHAR(12) NOT NULL COMMENT '区级区划编号',
    `city_code`   VARCHAR(12)          DEFAULT NULL COMMENT '所属市级区划编号',
    `city_name`   VARCHAR(32)          DEFAULT NULL COMMENT '所属市级名称',
    `sort`        INT         NOT NULL DEFAULT 0 COMMENT '排序',
    `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态 0未开通 1已开通',
    `create_time` DATETIME             DEFAULT NULL COMMENT '创建时间',
    `update_time` DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='服务区域表';


-- ============================================================================
--  初始化数据
-- ============================================================================

-- 默认管理员账号：admin / 123456（密码为 MD5("123456") = e10adc3949ba59abbe56e057f20f883e）
INSERT INTO `employee` (`username`, `name`, `password`, `phone`, `sex`, `id_number`, `role`, `status`,
                        `create_time`, `update_time`)
VALUES ('admin', '超级管理员', 'e10adc3949ba59abbe56e057f20f883e', '13800000000', '1',
        '110000000000000000', 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE `update_time` = NOW();

-- 服务分类（type=1 服务项目分类）
INSERT INTO `category` (`type`, `name`, `sort`, `status`, `create_time`, `update_time`)
VALUES (1, '家政保洁', 1, 1, NOW(), NOW()),
       (1, '家电维修', 2, 1, NOW(), NOW()),
       (1, '搬家运输', 3, 1, NOW(), NOW()),
       (1, '美容美发', 4, 1, NOW(), NOW()),
       (1, '开锁换锁', 5, 1, NOW(), NOW()),
       (1, '管道疏通', 6, 1, NOW(), NOW()),
       (1, '跑腿代办', 7, 1, NOW(), NOW()),
       (1, '宠物服务', 8, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE `update_time` = NOW();

-- 服务套餐分类（type=2）
INSERT INTO `category` (`type`, `name`, `sort`, `status`, `create_time`, `update_time`)
VALUES (2, '新居入住套餐', 1, 1, NOW(), NOW()),
       (2, '年度维保套餐', 2, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE `update_time` = NOW();

-- 服务区域示例（北京市）
INSERT INTO `service_area` (`name`, `code`, `city_code`, `city_name`, `sort`, `status`, `create_time`, `update_time`)
VALUES ('海淀区', '110108', '110100', '北京市', 1, 1, NOW(), NOW()),
       ('朝阳区', '110105', '110100', '北京市', 2, 1, NOW(), NOW()),
       ('东城区', '110101', '110100', '北京市', 3, 1, NOW(), NOW()),
       ('西城区', '110102', '110100', '北京市', 4, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE `update_time` = NOW();
