-- ============================================================================
--  生活服务网 · 演示数据
-- ----------------------------------------------------------------------------
--  用途：导入一批贴近真实业务的数据，用于查看管理端 / 小程序的实际效果。
--
--  ⚠️  这是「重置 + 填充」脚本：会先清空业务表再写入。
--      如果已经手工录入过数据，执行前请先备份。
--      分类（category）和服务区域（service_area）不受影响，保留原样。
--
--  服务日期基于 CURDATE() 相对计算，任何时候执行都表现为「未来三天」，
--  订单则覆盖了全部 7 种状态，方便查看派单中心的效果。
-- ============================================================================

USE life_service;
SET NAMES utf8mb4;

-- ---------- 清空业务表 ----------
DELETE FROM `review`;
DELETE FROM `service_order_item`;
DELETE FROM `service_order`;
DELETE FROM `slot`;
DELETE FROM `provider_skill`;
DELETE FROM `provider`;
DELETE FROM `service_package_item`;
DELETE FROM `service_package`;
DELETE FROM `service_spec`;
DELETE FROM `service_item`;
DELETE FROM `shopping_cart`;
DELETE FROM `address_book`;
DELETE FROM `user`;

-- ---------- 服务项目 23 个 ----------
INSERT INTO `service_item` (`id`,`name`,`category_id`,`price`,`original_price`,`image`,`description`,`unit`,`duration`,`service_mode`,`need_appoint`,`sales`,`score`,`status`,`merchant_id`,`create_time`,`update_time`) VALUES
(1001,'日常保洁 2小时',1,99,129,'https://picsum.photos/seed/lifesvc1001/600/400','日常家居清洁，含地面、桌面、卫生间基础清理','次',120,1,1,528,4.9,1,1,NOW(),NOW()),
(1002,'深度保洁 4小时',1,299,399,'https://picsum.photos/seed/lifesvc1002/600/400','厨房油污、卫生间死角、边角缝隙深度清洁','次',240,1,1,326,4.85,1,1,NOW(),NOW()),
(1003,'玻璃清洗',1,159,199,'https://picsum.photos/seed/lifesvc1003/600/400','室内玻璃双面清洗，含窗框和纱窗','次',90,1,1,214,4.8,1,1,NOW(),NOW()),
(1004,'新居开荒保洁',1,499,659,'https://picsum.photos/seed/lifesvc1004/600/400','装修后全屋除尘、除胶、除漆点，入住前彻底清洁','次',360,1,1,158,4.95,1,1,NOW(),NOW()),
(1005,'油烟机深度清洗',1,129,169,'https://picsum.photos/seed/lifesvc1005/600/400','拆洗油烟机内部涡轮，去重油污','台',90,1,1,412,4.88,1,1,NOW(),NOW()),
(2001,'空调加氟',2,199,239,'https://picsum.photos/seed/lifesvc2001/600/400','空调制冷效果差时补充制冷剂','台',60,1,1,286,4.82,1,1,NOW(),NOW()),
(2002,'空调不制冷维修',2,159,199,'https://picsum.photos/seed/lifesvc2002/600/400','检测并修复空调不制冷故障','台',60,1,1,195,4.75,1,1,NOW(),NOW()),
(2003,'洗衣机维修',2,129,159,'https://picsum.photos/seed/lifesvc2003/600/400','滚筒/波轮洗衣机故障检测与维修','台',60,1,1,142,4.7,1,1,NOW(),NOW()),
(2004,'冰箱维修',2,149,189,'https://picsum.photos/seed/lifesvc2004/600/400','冰箱不制冷、异响、漏水等故障维修','台',60,1,1,98,4.72,1,1,NOW(),NOW()),
(2005,'热水器维修',2,139,179,'https://picsum.photos/seed/lifesvc2005/600/400','燃气/电热水器故障检测与维修','台',60,1,1,126,4.78,1,1,NOW(),NOW()),
(3001,'小型搬家（3公里内）',3,299,359,'https://picsum.photos/seed/lifesvc3001/600/400','面包车搬运，含2名搬运工','单',180,1,1,87,4.65,1,1,NOW(),NOW()),
(3002,'家具拆装',3,199,259,'https://picsum.photos/seed/lifesvc3002/600/400','衣柜、床、书桌等家具拆卸与安装','件',120,1,1,63,4.7,1,1,NOW(),NOW()),
(3003,'长途搬家（按公里计）',3,899,1099,'https://picsum.photos/seed/lifesvc3003/600/400','跨城搬家，含打包与运输','单',480,1,1,34,4.6,1,1,NOW(),NOW()),
(4001,'上门理发',4,69,89,'https://picsum.photos/seed/lifesvc4001/600/400','理发师上门服务，含洗剪吹','次',45,2,1,156,4.86,1,1,NOW(),NOW()),
(4002,'上门美甲',4,129,169,'https://picsum.photos/seed/lifesvc4002/600/400','美甲师上门，含基础护理与涂色','次',90,2,1,74,4.9,1,1,NOW(),NOW()),
(5001,'上门开锁',5,99,129,'https://picsum.photos/seed/lifesvc5001/600/400','持证开锁，需核验房屋产权证件','次',30,1,0,238,4.68,1,1,NOW(),NOW()),
(5002,'换锁芯',5,259,329,'https://picsum.photos/seed/lifesvc5002/600/400','更换C级锁芯，含安装与调试','个',60,1,1,112,4.8,1,1,NOW(),NOW()),
(6001,'马桶疏通',6,129,159,'https://picsum.photos/seed/lifesvc6001/600/400','机械疏通，不损坏管道','次',60,1,0,176,4.72,1,1,NOW(),NOW()),
(6002,'下水道疏通',6,159,199,'https://picsum.photos/seed/lifesvc6002/600/400','厨房、阳台下水管道疏通','次',60,1,0,143,4.74,1,1,NOW(),NOW()),
(7001,'代取快递',7,19,25,'https://picsum.photos/seed/lifesvc7001/600/400','同城代取快递并送达','单',30,1,0,392,4.92,1,1,NOW(),NOW()),
(7002,'代买代办',7,39,49,'https://picsum.photos/seed/lifesvc7002/600/400','代买日用品、代排队等','单',60,1,0,168,4.85,1,1,NOW(),NOW()),
(8001,'上门喂猫',8,59,79,'https://picsum.photos/seed/lifesvc8001/600/400','喂养、换水、铲屎，含拍照反馈','次',30,1,1,208,4.94,1,1,NOW(),NOW()),
(8002,'宠物洗澡',8,99,129,'https://picsum.photos/seed/lifesvc8002/600/400','上门或到店宠物洗澡护理','次',60,2,1,97,4.8,1,1,NOW(),NOW());

-- ---------- 服务规格 25 条 ----------
INSERT INTO `service_spec` (`service_id`,`name`,`value`,`price_delta`,`sort`) VALUES
(1001,'房屋面积','60平方米以下',0,1),
(1001,'房屋面积','60-90平方米',30,2),
(1001,'房屋面积','90平方米以上',80,3),
(1002,'房屋面积','60平方米以下',0,1),
(1002,'房屋面积','60-90平方米',80,2),
(1002,'房屋面积','90-120平方米',180,3),
(1002,'房屋面积','120平方米以上',300,4),
(1003,'窗户数量','3扇以内',0,1),
(1003,'窗户数量','4-6扇',60,2),
(1003,'窗户数量','7扇以上',150,3),
(1004,'房屋面积','80平方米以下',0,1),
(1004,'房屋面积','80-120平方米',200,2),
(1004,'房屋面积','120平方米以上',450,3),
(2001,'空调匹数','1匹',0,1),
(2001,'空调匹数','1.5匹',0,2),
(2001,'空调匹数','2匹',50,3),
(2001,'空调匹数','柜机3匹',100,4),
(4001,'服务对象','成人',0,1),
(4001,'服务对象','儿童',-20,2),
(4001,'服务对象','老人',-20,3),
(3001,'楼层情况','电梯房',0,1),
(3001,'楼层情况','无电梯1-3层',80,2),
(3001,'楼层情况','无电梯4层以上',180,3),
(5002,'锁芯等级','B级锁芯',0,1),
(5002,'锁芯等级','C级锁芯',120,2);

-- ---------- 服务套餐 4 个 ----------
INSERT INTO `service_package` (`id`,`category_id`,`name`,`price`,`original_price`,`image`,`description`,`duration`,`service_mode`,`sales`,`status`,`merchant_id`,`create_time`,`update_time`) VALUES
(9001,9,'新居开荒保洁套餐',569,658,'https://picsum.photos/seed/lifesvcpkg9001/600/400','新居开荒保洁 + 玻璃清洗，入住前一次搞定',450,1,86,1,1,NOW(),NOW()),
(9002,9,'全屋深度保洁季卡',799,897,'https://picsum.photos/seed/lifesvcpkg9002/600/400','深度保洁 3 次，季度内有效',720,1,42,1,1,NOW(),NOW()),
(9003,10,'空调年度维保套餐',299,358,'https://picsum.photos/seed/lifesvcpkg9003/600/400','空调检修 + 加氟，一年内有效',120,1,163,1,1,NOW(),NOW()),
(9004,10,'家电无忧年卡',359,417,'https://picsum.photos/seed/lifesvcpkg9004/600/400','洗衣机 + 冰箱 + 热水器各一次上门维修',180,1,58,1,1,NOW(),NOW());

INSERT INTO `service_package_item` (`service_package_id`,`service_id`,`name`,`price`,`copies`) VALUES
(9001,1004,'新居开荒保洁',499,1),
(9001,1003,'玻璃清洗',159,1),
(9002,1002,'深度保洁 4小时',299,3),
(9003,2002,'空调不制冷维修',159,1),
(9003,2001,'空调加氟',199,1),
(9004,2003,'洗衣机维修',129,1),
(9004,2004,'冰箱维修',149,1),
(9004,2005,'热水器维修',139,1);

-- ---------- 服务人员 8 位 ----------
INSERT INTO `provider` (`id`,`name`,`phone`,`avatar`,`sex`,`work_years`,`intro`,`service_mode`,`status`,`score`,`good_rate`,`order_count`,`merchant_id`,`create_time`,`update_time`) VALUES
(7001,'张伟','13910000001','https://picsum.photos/seed/provider7001/200/200','1',12,'10年家政经验，擅长深度保洁与开荒',1,1,4.92,98.6,326,1,NOW(),NOW()),
(7002,'李强','13910000002','https://picsum.photos/seed/provider7002/200/200','1',8,'保洁与管道疏通，持管道工证',1,1,4.85,96.4,241,1,NOW(),NOW()),
(7003,'王芳','13910000003','https://picsum.photos/seed/provider7003/200/200','0',6,'细心耐心，擅长保洁与宠物照料',1,1,4.95,99.1,187,1,NOW(),NOW()),
(7004,'刘建国','13910000004','https://picsum.photos/seed/provider7004/200/200','1',15,'高级制冷维修工，持电工证',1,1,4.88,97.2,293,1,NOW(),NOW()),
(7005,'陈明','13910000005','https://picsum.photos/seed/provider7005/200/200','1',9,'家电维修与开锁换锁',3,1,4.8,95.8,205,1,NOW(),NOW()),
(7006,'赵磊','13910000006','https://picsum.photos/seed/provider7006/200/200','1',7,'搬家搬运，熟悉家具拆装',1,1,4.75,94.3,118,1,NOW(),NOW()),
(7007,'孙丽','13910000007','https://picsum.photos/seed/provider7007/200/200','0',5,'美发美甲，上门服务',2,1,4.9,98,96,1,NOW(),NOW()),
(7008,'周涛','13910000008','https://picsum.photos/seed/provider7008/200/200','1',11,'开锁换锁与管道疏通，持证上岗',1,1,4.82,96.1,174,1,NOW(),NOW());

INSERT INTO `provider_skill` (`provider_id`,`category_id`,`level`) VALUES
(7001,1,3),
(7002,1,2),
(7002,6,2),
(7003,1,2),
(7003,8,2),
(7004,2,3),
(7005,2,2),
(7005,5,2),
(7006,3,2),
(7007,4,2),
(7008,5,3),
(7008,6,2);

-- ---------- 用户与地址 ----------
INSERT INTO `user` (`id`,`openid`,`name`,`phone`,`sex`,`status`,`create_time`,`last_login_time`) VALUES
(8001,'oDemoUser0000000000000001','王小雅','13800000001','0',1,NOW(),NOW()),
(8002,'oDemoUser0000000000000002','陈浩然','13800000002','1',1,NOW(),NOW()),
(8003,'oDemoUser0000000000000003','刘思远','13800000003','1',1,NOW(),NOW());

INSERT INTO `address_book` (`id`,`user_id`,`consignee`,`phone`,`sex`,`province_code`,`province_name`,`city_code`,`city_name`,`district_code`,`district_name`,`detail`,`label`,`is_default`) VALUES
(8501,8001,'王小雅','13800000001','0','110000','北京市','110100','北京市','110108','海淀区','上地十街10号院3号楼502','家',1),
(8502,8001,'王小雅','13800000001','0','110000','北京市','110100','北京市','110108','海淀区','中关村大街27号1801','公司',0),
(8503,8002,'陈浩然','13800000002','1','110000','北京市','110100','北京市','110105','朝阳区','望京SOHO T1 2203','公司',1),
(8504,8003,'刘思远','13800000003','1','110000','北京市','110100','北京市','110102','西城区','西直门南大街16号院2号楼801','家',1);

-- ---------- 服务时段排期 72 条（未来 3 天，每位师傅每天 3 个时段）----------
INSERT INTO `slot` (`id`,`provider_id`,`service_id`,`service_date`,`start_time`,`end_time`,`total_stock`,`booked_count`,`status`,`version`,`merchant_id`,`create_time`,`update_time`) VALUES
(6001,7001,1001,DATE_ADD(CURDATE(), INTERVAL 1 DAY),'09:00:00','11:00:00',1,0,1,0,1,NOW(),NOW()),
(6002,7001,1001,DATE_ADD(CURDATE(), INTERVAL 1 DAY),'14:00:00','16:00:00',1,0,1,0,1,NOW(),NOW()),
(6003,7001,1001,DATE_ADD(CURDATE(), INTERVAL 1 DAY),'16:00:00','18:00:00',1,0,1,0,1,NOW(),NOW()),
(6004,7001,1001,DATE_ADD(CURDATE(), INTERVAL 2 DAY),'09:00:00','11:00:00',1,0,1,0,1,NOW(),NOW()),
(6005,7001,1001,DATE_ADD(CURDATE(), INTERVAL 2 DAY),'14:00:00','16:00:00',1,0,1,0,1,NOW(),NOW()),
(6006,7001,1001,DATE_ADD(CURDATE(), INTERVAL 2 DAY),'16:00:00','18:00:00',1,0,1,0,1,NOW(),NOW()),
(6007,7001,1001,DATE_ADD(CURDATE(), INTERVAL 3 DAY),'09:00:00','11:00:00',1,0,1,0,1,NOW(),NOW()),
(6008,7001,1001,DATE_ADD(CURDATE(), INTERVAL 3 DAY),'14:00:00','16:00:00',1,0,1,0,1,NOW(),NOW()),
(6009,7001,1001,DATE_ADD(CURDATE(), INTERVAL 3 DAY),'16:00:00','18:00:00',1,0,1,0,1,NOW(),NOW()),
(6010,7002,1001,DATE_ADD(CURDATE(), INTERVAL 1 DAY),'09:00:00','11:00:00',1,0,1,0,1,NOW(),NOW()),
(6011,7002,1001,DATE_ADD(CURDATE(), INTERVAL 1 DAY),'14:00:00','16:00:00',1,0,1,0,1,NOW(),NOW()),
(6012,7002,1001,DATE_ADD(CURDATE(), INTERVAL 1 DAY),'16:00:00','18:00:00',1,0,1,0,1,NOW(),NOW()),
(6013,7002,1001,DATE_ADD(CURDATE(), INTERVAL 2 DAY),'09:00:00','11:00:00',1,0,1,0,1,NOW(),NOW()),
(6014,7002,1001,DATE_ADD(CURDATE(), INTERVAL 2 DAY),'14:00:00','16:00:00',1,0,1,0,1,NOW(),NOW()),
(6015,7002,1001,DATE_ADD(CURDATE(), INTERVAL 2 DAY),'16:00:00','18:00:00',1,0,1,0,1,NOW(),NOW()),
(6016,7002,1001,DATE_ADD(CURDATE(), INTERVAL 3 DAY),'09:00:00','11:00:00',1,0,1,0,1,NOW(),NOW()),
(6017,7002,1001,DATE_ADD(CURDATE(), INTERVAL 3 DAY),'14:00:00','16:00:00',1,0,1,0,1,NOW(),NOW()),
(6018,7002,1001,DATE_ADD(CURDATE(), INTERVAL 3 DAY),'16:00:00','18:00:00',1,0,1,0,1,NOW(),NOW()),
(6019,7003,1001,DATE_ADD(CURDATE(), INTERVAL 1 DAY),'09:00:00','11:00:00',1,0,1,0,1,NOW(),NOW()),
(6020,7003,1001,DATE_ADD(CURDATE(), INTERVAL 1 DAY),'14:00:00','16:00:00',1,0,1,0,1,NOW(),NOW()),
(6021,7003,1001,DATE_ADD(CURDATE(), INTERVAL 1 DAY),'16:00:00','18:00:00',1,0,1,0,1,NOW(),NOW()),
(6022,7003,1001,DATE_ADD(CURDATE(), INTERVAL 2 DAY),'09:00:00','11:00:00',1,0,1,0,1,NOW(),NOW()),
(6023,7003,1001,DATE_ADD(CURDATE(), INTERVAL 2 DAY),'14:00:00','16:00:00',1,0,1,0,1,NOW(),NOW()),
(6024,7003,1001,DATE_ADD(CURDATE(), INTERVAL 2 DAY),'16:00:00','18:00:00',1,0,1,0,1,NOW(),NOW()),
(6025,7003,1001,DATE_ADD(CURDATE(), INTERVAL 3 DAY),'09:00:00','11:00:00',1,0,1,0,1,NOW(),NOW()),
(6026,7003,1001,DATE_ADD(CURDATE(), INTERVAL 3 DAY),'14:00:00','16:00:00',1,0,1,0,1,NOW(),NOW()),
(6027,7003,1001,DATE_ADD(CURDATE(), INTERVAL 3 DAY),'16:00:00','18:00:00',1,0,1,0,1,NOW(),NOW()),
(6028,7004,2001,DATE_ADD(CURDATE(), INTERVAL 1 DAY),'09:00:00','10:00:00',1,0,1,0,1,NOW(),NOW()),
(6029,7004,2001,DATE_ADD(CURDATE(), INTERVAL 1 DAY),'14:00:00','15:00:00',1,0,1,0,1,NOW(),NOW()),
(6030,7004,2001,DATE_ADD(CURDATE(), INTERVAL 1 DAY),'16:00:00','17:00:00',1,0,1,0,1,NOW(),NOW()),
(6031,7004,2001,DATE_ADD(CURDATE(), INTERVAL 2 DAY),'09:00:00','10:00:00',1,0,1,0,1,NOW(),NOW()),
(6032,7004,2001,DATE_ADD(CURDATE(), INTERVAL 2 DAY),'14:00:00','15:00:00',1,0,1,0,1,NOW(),NOW()),
(6033,7004,2001,DATE_ADD(CURDATE(), INTERVAL 2 DAY),'16:00:00','17:00:00',1,0,1,0,1,NOW(),NOW()),
(6034,7004,2001,DATE_ADD(CURDATE(), INTERVAL 3 DAY),'09:00:00','10:00:00',1,0,1,0,1,NOW(),NOW()),
(6035,7004,2001,DATE_ADD(CURDATE(), INTERVAL 3 DAY),'14:00:00','15:00:00',1,0,1,0,1,NOW(),NOW()),
(6036,7004,2001,DATE_ADD(CURDATE(), INTERVAL 3 DAY),'16:00:00','17:00:00',1,0,1,0,1,NOW(),NOW()),
(6037,7005,2001,DATE_ADD(CURDATE(), INTERVAL 1 DAY),'09:00:00','10:00:00',1,0,1,0,1,NOW(),NOW()),
(6038,7005,2001,DATE_ADD(CURDATE(), INTERVAL 1 DAY),'14:00:00','15:00:00',1,0,1,0,1,NOW(),NOW()),
(6039,7005,2001,DATE_ADD(CURDATE(), INTERVAL 1 DAY),'16:00:00','17:00:00',1,0,1,0,1,NOW(),NOW()),
(6040,7005,2001,DATE_ADD(CURDATE(), INTERVAL 2 DAY),'09:00:00','10:00:00',1,0,1,0,1,NOW(),NOW()),
(6041,7005,2001,DATE_ADD(CURDATE(), INTERVAL 2 DAY),'14:00:00','15:00:00',1,0,1,0,1,NOW(),NOW()),
(6042,7005,2001,DATE_ADD(CURDATE(), INTERVAL 2 DAY),'16:00:00','17:00:00',1,0,1,0,1,NOW(),NOW()),
(6043,7005,2001,DATE_ADD(CURDATE(), INTERVAL 3 DAY),'09:00:00','10:00:00',1,0,1,0,1,NOW(),NOW()),
(6044,7005,2001,DATE_ADD(CURDATE(), INTERVAL 3 DAY),'14:00:00','15:00:00',1,0,1,0,1,NOW(),NOW()),
(6045,7005,2001,DATE_ADD(CURDATE(), INTERVAL 3 DAY),'16:00:00','17:00:00',1,0,1,0,1,NOW(),NOW()),
(6046,7006,3001,DATE_ADD(CURDATE(), INTERVAL 1 DAY),'09:00:00','12:00:00',1,0,1,0,1,NOW(),NOW()),
(6047,7006,3001,DATE_ADD(CURDATE(), INTERVAL 1 DAY),'14:00:00','17:00:00',1,0,1,0,1,NOW(),NOW()),
(6048,7006,3001,DATE_ADD(CURDATE(), INTERVAL 1 DAY),'16:00:00','19:00:00',1,0,1,0,1,NOW(),NOW()),
(6049,7006,3001,DATE_ADD(CURDATE(), INTERVAL 2 DAY),'09:00:00','12:00:00',1,0,1,0,1,NOW(),NOW()),
(6050,7006,3001,DATE_ADD(CURDATE(), INTERVAL 2 DAY),'14:00:00','17:00:00',1,0,1,0,1,NOW(),NOW()),
(6051,7006,3001,DATE_ADD(CURDATE(), INTERVAL 2 DAY),'16:00:00','19:00:00',1,0,1,0,1,NOW(),NOW()),
(6052,7006,3001,DATE_ADD(CURDATE(), INTERVAL 3 DAY),'09:00:00','12:00:00',1,0,1,0,1,NOW(),NOW()),
(6053,7006,3001,DATE_ADD(CURDATE(), INTERVAL 3 DAY),'14:00:00','17:00:00',1,0,1,0,1,NOW(),NOW()),
(6054,7006,3001,DATE_ADD(CURDATE(), INTERVAL 3 DAY),'16:00:00','19:00:00',1,0,1,0,1,NOW(),NOW()),
(6055,7007,4001,DATE_ADD(CURDATE(), INTERVAL 1 DAY),'09:00:00','09:45:00',1,0,1,0,1,NOW(),NOW()),
(6056,7007,4001,DATE_ADD(CURDATE(), INTERVAL 1 DAY),'14:00:00','14:45:00',1,0,1,0,1,NOW(),NOW()),
(6057,7007,4001,DATE_ADD(CURDATE(), INTERVAL 1 DAY),'16:00:00','16:45:00',1,0,1,0,1,NOW(),NOW()),
(6058,7007,4001,DATE_ADD(CURDATE(), INTERVAL 2 DAY),'09:00:00','09:45:00',1,0,1,0,1,NOW(),NOW()),
(6059,7007,4001,DATE_ADD(CURDATE(), INTERVAL 2 DAY),'14:00:00','14:45:00',1,0,1,0,1,NOW(),NOW()),
(6060,7007,4001,DATE_ADD(CURDATE(), INTERVAL 2 DAY),'16:00:00','16:45:00',1,0,1,0,1,NOW(),NOW()),
(6061,7007,4001,DATE_ADD(CURDATE(), INTERVAL 3 DAY),'09:00:00','09:45:00',1,0,1,0,1,NOW(),NOW()),
(6062,7007,4001,DATE_ADD(CURDATE(), INTERVAL 3 DAY),'14:00:00','14:45:00',1,0,1,0,1,NOW(),NOW()),
(6063,7007,4001,DATE_ADD(CURDATE(), INTERVAL 3 DAY),'16:00:00','16:45:00',1,0,1,0,1,NOW(),NOW()),
(6064,7008,5001,DATE_ADD(CURDATE(), INTERVAL 1 DAY),'09:00:00','09:30:00',1,0,1,0,1,NOW(),NOW()),
(6065,7008,5001,DATE_ADD(CURDATE(), INTERVAL 1 DAY),'14:00:00','14:30:00',1,0,1,0,1,NOW(),NOW()),
(6066,7008,5001,DATE_ADD(CURDATE(), INTERVAL 1 DAY),'16:00:00','16:30:00',1,0,1,0,1,NOW(),NOW()),
(6067,7008,5001,DATE_ADD(CURDATE(), INTERVAL 2 DAY),'09:00:00','09:30:00',1,0,1,0,1,NOW(),NOW()),
(6068,7008,5001,DATE_ADD(CURDATE(), INTERVAL 2 DAY),'14:00:00','14:30:00',1,0,1,0,1,NOW(),NOW()),
(6069,7008,5001,DATE_ADD(CURDATE(), INTERVAL 2 DAY),'16:00:00','16:30:00',1,0,1,0,1,NOW(),NOW()),
(6070,7008,5001,DATE_ADD(CURDATE(), INTERVAL 3 DAY),'09:00:00','09:30:00',1,0,1,0,1,NOW(),NOW()),
(6071,7008,5001,DATE_ADD(CURDATE(), INTERVAL 3 DAY),'14:00:00','14:30:00',1,0,1,0,1,NOW(),NOW()),
(6072,7008,5001,DATE_ADD(CURDATE(), INTERVAL 3 DAY),'16:00:00','16:30:00',1,0,1,0,1,NOW(),NOW());

-- ---------- 服务订单 8 条（覆盖全部 7 种状态）----------
INSERT INTO `service_order` (`id`,`number`,`status`,`user_id`,`address_book_id`,`provider_id`,`slot_id`,`service_mode`,`service_time`,`service_duration`,`amount`,`pay_method`,`pay_status`,`order_time`,`checkout_time`,`user_name`,`consignee`,`phone`,`address`,`remark`,`verify_code`,`dispatch_time`,`accept_time`,`start_time`,`finish_time`,`cancel_reason`,`cancel_time`,`merchant_id`) VALUES
(4001,'LS20260918A014001',1,8001,8501,null,null,1,TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '09:00:00'),120,99,1,0,DATE_SUB(NOW(), INTERVAL 20 MINUTE),NULL,'王小雅','王小雅','13800000001','北京市海淀区上地十街10号院3号楼502','还没付款的订单，用于演示超时自动取消','104001',NULL,NULL,NULL,NULL,NULL,NULL,1),
(4002,'LS20260918A024002',2,8002,8503,null,null,1,TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 3 DAY), '14:00:00'),240,299,1,1,DATE_SUB(NOW(), INTERVAL 29 MINUTE),DATE_ADD(DATE_SUB(NOW(), INTERVAL 29 MINUTE), INTERVAL 3 MINUTE),'陈浩然','陈浩然','13800000002','北京市朝阳区望京SOHO T1 2203','已付款，等待派单','104002',NULL,NULL,NULL,NULL,NULL,NULL,1),
(4003,'LS20260918A034003',3,8001,8502,7004,6028,1,TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:00:00'),60,199,1,1,DATE_SUB(NOW(), INTERVAL 38 MINUTE),DATE_ADD(DATE_SUB(NOW(), INTERVAL 38 MINUTE), INTERVAL 3 MINUTE),'王小雅','王小雅','13800000001','北京市海淀区中关村大街27号1801','已派单给刘建国，等待上门','104003',DATE_ADD(DATE_SUB(NOW(), INTERVAL 38 MINUTE), INTERVAL 12 MINUTE),DATE_ADD(DATE_SUB(NOW(), INTERVAL 38 MINUTE), INTERVAL 18 MINUTE),NULL,NULL,NULL,NULL,1),
(4004,'LS20260918A044004',4,8003,8504,7008,null,1,TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 0 DAY), '14:00:00'),90,129,1,1,DATE_SUB(TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 0 DAY), '14:00:00'), INTERVAL 1 DAY),DATE_ADD(DATE_SUB(TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 0 DAY), '14:00:00'), INTERVAL 1 DAY), INTERVAL 3 MINUTE),'刘思远','刘思远','13800000003','北京市西城区西直门南大街16号院2号楼801','师傅已上门，正在服务中','104004',DATE_ADD(DATE_SUB(TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 0 DAY), '14:00:00'), INTERVAL 1 DAY), INTERVAL 12 MINUTE),DATE_ADD(DATE_SUB(TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 0 DAY), '14:00:00'), INTERVAL 1 DAY), INTERVAL 18 MINUTE),TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 0 DAY), '14:00:00'),NULL,NULL,NULL,1),
(4005,'LS20260918A054005',5,8002,8503,7003,null,1,TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:00:00'),30,59,1,1,DATE_SUB(TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:00:00'), INTERVAL 1 DAY),DATE_ADD(DATE_SUB(TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:00:00'), INTERVAL 1 DAY), INTERVAL 3 MINUTE),'陈浩然','陈浩然','13800000002','北京市朝阳区望京SOHO T1 2203','服务完成，等待用户评价','104005',DATE_ADD(DATE_SUB(TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:00:00'), INTERVAL 1 DAY), INTERVAL 12 MINUTE),DATE_ADD(DATE_SUB(TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:00:00'), INTERVAL 1 DAY), INTERVAL 18 MINUTE),TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:00:00'),DATE_ADD(TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:00:00'), INTERVAL 30 MINUTE),NULL,NULL,1),
(4006,'LS20260918A064006',6,8001,8501,7001,null,1,TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '14:00:00'),90,159,1,1,DATE_SUB(TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '14:00:00'), INTERVAL 1 DAY),DATE_ADD(DATE_SUB(TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '14:00:00'), INTERVAL 1 DAY), INTERVAL 3 MINUTE),'王小雅','王小雅','13800000001','北京市海淀区上地十街10号院3号楼502','已完成，用户打了五星','104006',DATE_ADD(DATE_SUB(TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '14:00:00'), INTERVAL 1 DAY), INTERVAL 12 MINUTE),DATE_ADD(DATE_SUB(TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '14:00:00'), INTERVAL 1 DAY), INTERVAL 18 MINUTE),TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '14:00:00'),DATE_ADD(TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '14:00:00'), INTERVAL 90 MINUTE),NULL,NULL,1),
(4007,'LS20260918A074007',6,8003,8504,7008,null,1,TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '16:00:00'),60,129,1,1,DATE_SUB(TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '16:00:00'), INTERVAL 1 DAY),DATE_ADD(DATE_SUB(TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '16:00:00'), INTERVAL 1 DAY), INTERVAL 3 MINUTE),'刘思远','刘思远','13800000003','北京市西城区西直门南大街16号院2号楼801','已完成，用户评价了一次','104007',DATE_ADD(DATE_SUB(TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '16:00:00'), INTERVAL 1 DAY), INTERVAL 12 MINUTE),DATE_ADD(DATE_SUB(TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '16:00:00'), INTERVAL 1 DAY), INTERVAL 18 MINUTE),TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '16:00:00'),DATE_ADD(TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '16:00:00'), INTERVAL 60 MINUTE),NULL,NULL,1),
(4008,'LS20260918A084008',7,8002,8503,null,null,1,TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '09:00:00'),180,299,1,0,DATE_SUB(TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '09:00:00'), INTERVAL 1 DAY),NULL,'陈浩然','陈浩然','13800000002','北京市朝阳区望京SOHO T1 2203','用户主动取消','104008',DATE_ADD(DATE_SUB(TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '09:00:00'), INTERVAL 1 DAY), INTERVAL 12 MINUTE),DATE_ADD(DATE_SUB(TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '09:00:00'), INTERVAL 1 DAY), INTERVAL 18 MINUTE),TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '09:00:00'),DATE_ADD(TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '09:00:00'), INTERVAL 180 MINUTE),'用户取消：临时有事，改期再约',DATE_ADD(DATE_SUB(TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -2 DAY), '09:00:00'), INTERVAL 1 DAY), INTERVAL 25 MINUTE),1);

-- ---------- 订单明细 ----------
INSERT INTO `service_order_item` (`id`,`order_id`,`service_id`,`name`,`spec`,`price`,`number`,`amount`,`image`,`duration`) VALUES
(5001,4001,1001,'日常保洁 2小时',NULL,99,1,99,'https://picsum.photos/seed/lifesvc1001/600/400',120),
(5002,4002,1002,'深度保洁 4小时',NULL,299,1,299,'https://picsum.photos/seed/lifesvc1002/600/400',240),
(5003,4003,2001,'空调加氟',NULL,199,1,199,'https://picsum.photos/seed/lifesvc2001/600/400',60),
(5004,4004,1005,'油烟机深度清洗',NULL,129,1,129,'https://picsum.photos/seed/lifesvc1005/600/400',90),
(5005,4005,8001,'上门喂猫',NULL,59,1,59,'https://picsum.photos/seed/lifesvc8001/600/400',30),
(5006,4006,1003,'玻璃清洗',NULL,159,1,159,'https://picsum.photos/seed/lifesvc1003/600/400',90),
(5007,4007,6001,'马桶疏通',NULL,129,1,129,'https://picsum.photos/seed/lifesvc6001/600/400',60),
(5008,4008,3001,'小型搬家（3公里内）',NULL,299,1,299,'https://picsum.photos/seed/lifesvc3001/600/400',180);

-- 已付款订单占用的时段，booked_count 置为 1，保持与订单一致
UPDATE `slot` SET booked_count = 1 WHERE id IN (6028);

-- ---------- 服务评价 2 条 ----------
INSERT INTO `review` (`order_id`,`user_id`,`provider_id`,`service_id`,`score`,`service_score`,`speed_score`,`quality_score`,`content`,`images`,`is_anonymous`,`status`,`create_time`) VALUES
(4006,8001,7001,1003,5,5,5,5,'玻璃擦得很干净，窗框和纱窗也一起收拾了，师傅很细致，下次还找他。',NULL,0,1,DATE_SUB(NOW(), INTERVAL 2 DAY)),
(4007,8003,7008,6001,5,4,5,5,'晚上十点打的电话，半小时就到了，疏通得很彻底，价格也透明。',NULL,0,1,DATE_SUB(NOW(), INTERVAL 4 DAY));

-- ---------- 导入结果 ----------
SELECT
  (SELECT COUNT(*) FROM service_item)    AS 服务项目,
  (SELECT COUNT(*) FROM service_spec)    AS 服务规格,
  (SELECT COUNT(*) FROM service_package) AS 服务套餐,
  (SELECT COUNT(*) FROM provider)        AS 服务人员,
  (SELECT COUNT(*) FROM slot)            AS 排期时段,
  (SELECT COUNT(*) FROM service_order)   AS 订单,
  (SELECT COUNT(*) FROM review)          AS 评价;
