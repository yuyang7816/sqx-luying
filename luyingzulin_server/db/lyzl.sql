/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80409
 Source Host           : localhost:3306
 Source Schema         : lyzl

 Target Server Type    : MySQL
 Target Server Version : 80409
 File Encoding         : 65001

 Date: 03/11/2026 15:56:16
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for banner
-- ----------------------------
DROP TABLE IF EXISTS `banner`;
CREATE TABLE `banner`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `classify` int(0) NULL DEFAULT NULL COMMENT '1首页轮播 2金刚区 3推广左图 4推广区块',
  `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `sort` int(0) NULL DEFAULT 0,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `state` tinyint(0) NULL DEFAULT 1,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_banner_classify`(`classify`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Banner' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of banner
-- ----------------------------
INSERT INTO `banner` VALUES (1, 'integration-banner', 'https://tu.mianyazuju.com/2026/08/28/36a77b69-1d03-4e3f-97a4-3320bd027e48.png', 1, 'link:/pages/index/index', 1, NULL, 1, '2026-08-03 22:32:42', '2026-08-28 13:05:08');
INSERT INTO `banner` VALUES (2, '啊实打实', 'https://tu.mianyazuju.com/2026/08/31/ffe1274e-8719-4287-b9fc-b59113967a02.png', 1, 'category:1', 1, NULL, 1, '2026-08-31 15:19:18', '2026-08-31 15:19:18');

-- ----------------------------
-- Table structure for coupon_template
-- ----------------------------
DROP TABLE IF EXISTS `coupon_template`;
CREATE TABLE `coupon_template`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `template_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模板编号（业务键）',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '优惠券名称',
  `coupon_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型：amount_off满减/discount折扣/delivery_free免配送/deposit_free免押',
  `amount` decimal(10, 2) NOT NULL COMMENT '面额（元）',
  `min_amount` decimal(10, 2) NOT NULL COMMENT '最低消费门槛（元）',
  `discount_rate` decimal(5, 4) NULL DEFAULT NULL COMMENT '折扣率（0-1，discount 类型）',
  `valid_start` datetime(0) NULL DEFAULT NULL COMMENT '有效期开始',
  `valid_end` datetime(0) NULL DEFAULT NULL COMMENT '有效期结束',
  `scope_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'all' COMMENT '适用范围：all/category/goods',
  `scope_value` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '范围值（分类ID/商品ID，逗号分隔）',
  `status` tinyint(0) NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `deleted` int(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 -1删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_coupon_template_no`(`template_no`, `deleted`) USING BTREE,
  INDEX `idx_coupon_template_status`(`status`, `deleted`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '优惠券模板' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of coupon_template
-- ----------------------------
INSERT INTO `coupon_template` VALUES (1, 'CTM001', '新人立减券', 'amount_off', 20.00, 199.00, NULL, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 'all', NULL, 1, 'M06 演示满减券', '2026-08-04 02:20:45', '2026-08-04 02:20:45', 0);
INSERT INTO `coupon_template` VALUES (2, 'CTM002', '免配送券', 'delivery_free', 18.00, 0.00, NULL, '2026-01-01 00:00:00', '2026-10-15 23:59:59', 'all', NULL, 1, 'M06 演示配送券', '2026-08-04 02:20:45', '2026-08-04 02:20:45', 0);

-- ----------------------------
-- Table structure for im_conversation
-- ----------------------------
DROP TABLE IF EXISTS `im_conversation`;
CREATE TABLE `im_conversation`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user_id` bigint(0) NOT NULL COMMENT 'C端用户ID',
  `merchant_id` bigint(0) NOT NULL COMMENT '商户ID',
  `last_msg_time` datetime(0) NULL DEFAULT NULL COMMENT '最后消息时间',
  `last_msg_preview` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最后消息摘要',
  `user_unread` int(0) NOT NULL DEFAULT 0 COMMENT '用户未读数',
  `merchant_unread` int(0) NOT NULL DEFAULT 0 COMMENT '商户未读数',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_im_conv_user_merchant`(`user_id`, `merchant_id`) USING BTREE,
  INDEX `idx_im_conv_user`(`user_id`) USING BTREE,
  INDEX `idx_im_conv_merchant`(`merchant_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'IM 会话' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of im_conversation
-- ----------------------------
INSERT INTO `im_conversation` VALUES (1, 4, 2, '2026-09-02 16:27:17', '你好大苏打撒旦', 0, 0, '2026-09-02 09:48:28', '2026-09-02 17:07:29');
INSERT INTO `im_conversation` VALUES (2, 4, 1, '2026-09-02 11:30:56', 'nihao', 0, 1, '2026-09-02 11:29:32', '2026-09-02 11:30:55');
INSERT INTO `im_conversation` VALUES (3, 7, 2, '2026-09-03 14:20:35', '[使用教程视频] 【双人自动重启帐篷带遮阳】使用教程视频，请查收。', 1, 0, '2026-09-03 14:20:35', '2026-09-03 14:20:35');

-- ----------------------------
-- Table structure for im_message
-- ----------------------------
DROP TABLE IF EXISTS `im_message`;
CREATE TABLE `im_message`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `conversation_id` bigint(0) NOT NULL COMMENT '会话ID',
  `sender_role` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '发送方：user/merchant/system',
  `sender_user_id` bigint(0) NULL DEFAULT NULL COMMENT '发送人用户ID',
  `msg_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'text' COMMENT '消息类型：text/video/image',
  `content` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文本内容',
  `media_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '媒体URL',
  `order_id` bigint(0) NULL DEFAULT NULL COMMENT '关联订单ID',
  `goods_id` bigint(0) NULL DEFAULT NULL COMMENT '关联商品ID',
  `biz_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '业务类型：tutorial_video 等',
  `is_read` tinyint(0) NOT NULL DEFAULT 0 COMMENT '接收方是否已读',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_im_msg_conv`(`conversation_id`, `create_time`) USING BTREE,
  INDEX `idx_im_msg_order_biz`(`order_id`, `biz_type`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'IM 消息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of im_message
-- ----------------------------
INSERT INTO `im_message` VALUES (1, 1, 'merchant', 4, 'video', '【双人自动重启帐篷带遮阳】使用教程视频，请查收。', 'https://tu.mianyazuju.com/2026/09/01/716580ce-d007-49c0-97bc-8a437f244ccc.mp4', 45, 15, 'tutorial_video', 1, '2026-09-02 09:48:28');
INSERT INTO `im_message` VALUES (2, 2, 'user', 4, 'text', 'nihao', NULL, NULL, NULL, NULL, 0, '2026-09-02 11:30:56');
INSERT INTO `im_message` VALUES (3, 1, 'user', 4, 'text', '你好', NULL, NULL, NULL, NULL, 1, '2026-09-02 16:25:20');
INSERT INTO `im_message` VALUES (4, 1, 'user', 4, 'text', '你好', NULL, NULL, NULL, NULL, 1, '2026-09-02 16:25:24');
INSERT INTO `im_message` VALUES (5, 1, 'merchant', 4, 'text', '你哈对对对对', NULL, NULL, NULL, NULL, 1, '2026-09-02 16:26:20');
INSERT INTO `im_message` VALUES (6, 1, 'user', 4, 'text', '你好大苏打撒旦', NULL, NULL, NULL, NULL, 1, '2026-09-02 16:27:17');
INSERT INTO `im_message` VALUES (7, 3, 'merchant', 4, 'video', '【双人自动重启帐篷带遮阳】使用教程视频，请查收。', 'https://tu.mianyazuju.com/2026/09/01/716580ce-d007-49c0-97bc-8a437f244ccc.mp4', 54, 15, 'tutorial_video', 0, '2026-09-03 14:20:35');

-- ----------------------------
-- Table structure for merchant
-- ----------------------------
DROP TABLE IF EXISTS `merchant`;
CREATE TABLE `merchant`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '商户ID',
  `merchant_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '商户业务编号',
  `user_id` bigint(0) NOT NULL COMMENT '负责人用户ID（APP登录）',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '商户名称',
  `contact_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '联系人姓名',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '联系人手机号',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'active' COMMENT '状态：active启用 frozen冻结',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `deleted` int(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 -1删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_merchant_no`(`merchant_no`) USING BTREE,
  INDEX `idx_merchant_user`(`user_id`, `deleted`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商户主表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of merchant
-- ----------------------------
INSERT INTO `merchant` VALUES (1, 'MCH-SEED-001', 2, '山野营地户外租赁', '李晨', '13900001001', 'active', '2026-08-04 12:28:12', '2026-08-04 12:28:12', 0);

-- ----------------------------
-- Table structure for merchant_apply
-- ----------------------------
DROP TABLE IF EXISTS `merchant_apply`;
CREATE TABLE `merchant_apply`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `apply_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '申请业务编号',
  `user_id` bigint(0) NOT NULL COMMENT '申请人用户ID',
  `merchant_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '商家名称',
  `contact_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '联系人姓名',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '联系电话',
  `store_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '门店地址',
  `store_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '门店名称',
  `business_hours` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '营业时间',
  `service_area` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '配送范围描述',
  `province` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '省',
  `city` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '市',
  `district` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '区/县',
  `latitude` decimal(10, 6) NULL DEFAULT NULL COMMENT '纬度',
  `longitude` decimal(10, 6) NULL DEFAULT NULL COMMENT '经度',
  `base_delivery_fee` decimal(10, 2) NULL DEFAULT NULL COMMENT '基础配送费（元）',
  `free_delivery_threshold` decimal(10, 2) NULL DEFAULT NULL COMMENT '满额免配送门槛（元，按租金计）',
  `business_scope` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '经营范围，逗号分隔',
  `license_urls` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '资质材料URL，逗号分隔',
  `extra_urls` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '补充资料URL，逗号分隔',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'pending' COMMENT '状态：pending/reviewing/approved/rejected',
  `reject_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '驳回原因',
  `audit_admin_id` bigint(0) NULL DEFAULT NULL COMMENT '审核管理员ID',
  `audit_time` datetime(0) NULL DEFAULT NULL COMMENT '审核时间',
  `merchant_id` bigint(0) NULL DEFAULT NULL COMMENT '审核通过后关联商户ID',
  `store_id` bigint(0) NULL DEFAULT NULL COMMENT '审核通过后关联门店ID',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `deleted` int(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 -1删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_merchant_apply_no`(`apply_no`) USING BTREE,
  INDEX `idx_merchant_apply_user_status`(`user_id`, `status`, `deleted`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商户入驻申请' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of merchant_apply
-- ----------------------------

-- ----------------------------
-- Table structure for merchant_deposit
-- ----------------------------
DROP TABLE IF EXISTS `merchant_deposit`;
CREATE TABLE `merchant_deposit`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `merchant_id` bigint(0) NOT NULL COMMENT '商户ID',
  `total_amount` decimal(12, 2) NOT NULL COMMENT '保证金总额',
  `frozen_amount` decimal(12, 2) NOT NULL COMMENT '冻结金额',
  `status` tinyint(0) NOT NULL DEFAULT 1 COMMENT '1正常 2冻结',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_merchant_deposit_merchant`(`merchant_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商户保证金账户' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of merchant_deposit
-- ----------------------------
INSERT INTO `merchant_deposit` VALUES (1, 2, 50.00, 0.00, 1, '2026-09-01 10:42:02', '2026-09-03 10:24:45');
INSERT INTO `merchant_deposit` VALUES (2, 1, 50.00, 0.00, 1, '2026-09-03 09:28:54', '2026-09-03 09:32:01');

-- ----------------------------
-- Table structure for merchant_deposit_log
-- ----------------------------
DROP TABLE IF EXISTS `merchant_deposit_log`;
CREATE TABLE `merchant_deposit_log`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `log_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '流水号',
  `merchant_id` bigint(0) NOT NULL COMMENT '商户ID',
  `log_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'pay/refund/adjust',
  `amount` decimal(12, 2) NOT NULL COMMENT '金额',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联支付单号',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'completed' COMMENT 'pending/completed',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_merchant_deposit_log_merchant`(`merchant_id`, `create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商户保证金流水' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of merchant_deposit_log
-- ----------------------------
INSERT INTO `merchant_deposit_log` VALUES (6, 'MDL202609030956247652', 2, 'pay', 50.00, 'MDP202609030956245371', 'cancelled', '支付已关闭', '2026-09-03 09:56:25', '2026-09-03 09:56:24');
INSERT INTO `merchant_deposit_log` VALUES (7, 'MDL202609031024451859', 2, 'pay', 50.00, 'MDP202609031024459707', 'completed', '商户保证金缴纳成功', '2026-09-03 10:24:45', '2026-09-03 10:24:45');

-- ----------------------------
-- Table structure for merchant_order
-- ----------------------------
DROP TABLE IF EXISTS `merchant_order`;
CREATE TABLE `merchant_order`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '商户订单ID',
  `merchant_order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '商户订单号，格式 LINK-{rentOrderId}',
  `merchant_id` bigint(0) NOT NULL COMMENT '商户ID',
  `store_id` bigint(0) NULL DEFAULT NULL COMMENT '门店ID',
  `rent_order_id` bigint(0) NOT NULL COMMENT '关联用户租赁订单ID',
  `rent_order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '关联用户租赁订单号',
  `user_id` bigint(0) NOT NULL COMMENT '下单用户ID',
  `goods_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商品摘要名称',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'pending_prepare' COMMENT '商户侧状态：pending_prepare/prepared/renting/returning/inspecting/completed',
  `contact_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人姓名',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人手机号',
  `pickup_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '取货方式：store/pickup_point/delivery',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '配送/自提地址摘要',
  `pay_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '实付金额',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `prepare_time` datetime(0) NULL DEFAULT NULL COMMENT '备货完成时间',
  `pickup_time` datetime(0) NULL DEFAULT NULL COMMENT '核销取货时间',
  `complete_time` datetime(0) NULL DEFAULT NULL COMMENT '验收完成时间',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `deleted` int(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 -1删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_merchant_order_no`(`merchant_order_no`) USING BTREE,
  UNIQUE INDEX `uk_merchant_order_rent`(`rent_order_id`, `deleted`) USING BTREE,
  INDEX `idx_merchant_order_merchant_status`(`merchant_id`, `status`, `create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商户订单（用户订单同步）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of merchant_order
-- ----------------------------

-- ----------------------------
-- Table structure for merchant_staff
-- ----------------------------
DROP TABLE IF EXISTS `merchant_staff`;
CREATE TABLE `merchant_staff`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '员工ID',
  `merchant_id` bigint(0) NOT NULL COMMENT '所属商户ID',
  `store_id` bigint(0) NULL DEFAULT NULL COMMENT '所属门店ID',
  `user_id` bigint(0) NULL DEFAULT NULL COMMENT '关联APP用户ID（可选）',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '员工姓名',
  `role` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位角色',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '手机号',
  `status` tinyint(0) NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `deleted` int(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 -1删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_merchant_staff_merchant`(`merchant_id`, `status`, `deleted`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商户门店员工' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of merchant_staff
-- ----------------------------

-- ----------------------------
-- Table structure for merchant_store
-- ----------------------------
DROP TABLE IF EXISTS `merchant_store`;
CREATE TABLE `merchant_store`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '门店ID',
  `store_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '门店业务编号',
  `merchant_id` bigint(0) NOT NULL COMMENT '所属商户ID',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '门店名称',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '门店地址',
  `province` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '省',
  `city` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '市',
  `district` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '区/县',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '门店联系电话',
  `business_hours` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '营业时间',
  `service_area` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '服务区域',
  `longitude` decimal(10, 6) NULL DEFAULT NULL COMMENT '经度',
  `latitude` decimal(10, 6) NULL DEFAULT NULL COMMENT '纬度',
  `delivery_enabled` tinyint(0) NOT NULL DEFAULT 1 COMMENT '是否支持配送：1是 0否',
  `self_pickup_enabled` tinyint(0) NOT NULL DEFAULT 1 COMMENT '是否支持自提：1是 0否',
  `base_delivery_fee` decimal(10, 2) NULL DEFAULT NULL COMMENT '基础配送费',
  `free_delivery_threshold` decimal(10, 2) NULL DEFAULT NULL COMMENT '免配送门槛',
  `status` tinyint(0) NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `deleted` int(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 -1删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_merchant_store_no`(`store_no`) USING BTREE,
  INDEX `idx_merchant_store_merchant`(`merchant_id`, `status`, `deleted`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商户门店' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of merchant_store
-- ----------------------------

-- ----------------------------
-- Table structure for merchant_wallet
-- ----------------------------
DROP TABLE IF EXISTS `merchant_wallet`;
CREATE TABLE `merchant_wallet`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `merchant_id` bigint(0) NOT NULL COMMENT '商户ID',
  `amount` decimal(12, 2) NOT NULL COMMENT '可用余额',
  `frozen_amount` decimal(12, 2) NOT NULL COMMENT '冻结金额',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_merchant_wallet_merchant`(`merchant_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商户经营钱包' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of merchant_wallet
-- ----------------------------
INSERT INTO `merchant_wallet` VALUES (1, 2, 0.00, 0.00, '2026-09-01 10:42:02', '2026-09-03 09:54:14');

-- ----------------------------
-- Table structure for merchant_wallet_transaction
-- ----------------------------
DROP TABLE IF EXISTS `merchant_wallet_transaction`;
CREATE TABLE `merchant_wallet_transaction`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `merchant_id` bigint(0) NOT NULL COMMENT '商户ID',
  `transaction_type` tinyint(0) NOT NULL COMMENT '1充值 2消费 3退款 5调整 10提现 11提现驳回',
  `amount` decimal(12, 2) NOT NULL COMMENT '交易金额',
  `direction` tinyint(0) NOT NULL COMMENT '1收入 2支出',
  `biz_type` tinyint(0) NULL DEFAULT NULL COMMENT '业务类型',
  `biz_id` bigint(0) NULL DEFAULT NULL COMMENT '业务ID',
  `status` tinyint(0) NOT NULL DEFAULT 2 COMMENT '1待处理 2已完成 3已取消 4失败',
  `title` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标题',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述/关联单号',
  `extra_data` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '扩展信息',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_merchant_wallet_tx_merchant`(`merchant_id`, `create_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商户经营钱包流水' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of merchant_wallet_transaction
-- ----------------------------

-- ----------------------------
-- Table structure for messages
-- ----------------------------
DROP TABLE IF EXISTS `messages`;
CREATE TABLE `messages`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NULL DEFAULT NULL,
  `data_id` bigint(0) NULL DEFAULT NULL COMMENT '关联数据ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `image_urls` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图片URL，逗号分隔',
  `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '跳转链接',
  `type` int(0) NOT NULL DEFAULT 2 COMMENT '消息类型: 1=系统公告, 2=用户消息',
  `is_read` tinyint(0) NULL DEFAULT 0,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_messages_user_id`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '消息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of messages
-- ----------------------------
INSERT INTO `messages` VALUES (2, 4, NULL, '收到新的客服消息', '18892020825：你好', NULL, '/packageMerchant/im-chat?userId=4&userName=18892020825', 2, 0, '2026-09-02 16:25:20', '2026-09-02 16:25:19');
INSERT INTO `messages` VALUES (3, 4, NULL, '收到新的客服消息', '18892020825：你好', NULL, '/packageMerchant/im-chat?userId=4&userName=18892020825', 2, 1, '2026-09-02 16:25:24', '2026-09-02 16:26:11');

-- ----------------------------
-- Table structure for platform_cs_conversation
-- ----------------------------
DROP TABLE IF EXISTS `platform_cs_conversation`;
CREATE TABLE `platform_cs_conversation`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user_id` bigint(0) NOT NULL COMMENT 'C端用户ID',
  `last_msg_time` datetime(0) NULL DEFAULT NULL COMMENT '最后消息时间',
  `last_msg_preview` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最后消息摘要',
  `user_unread` int(0) NOT NULL DEFAULT 0 COMMENT '用户未读数',
  `admin_unread` int(0) NOT NULL DEFAULT 0 COMMENT '平台客服未读数',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_platform_cs_user`(`user_id`) USING BTREE,
  INDEX `idx_platform_cs_admin_unread`(`admin_unread`, `last_msg_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '平台官方客服会话' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of platform_cs_conversation
-- ----------------------------
INSERT INTO `platform_cs_conversation` VALUES (1, 4, '2026-09-02 16:34:19', '是的请问您有什么问题', 0, 0, '2026-09-02 16:29:31', '2026-09-02 16:34:35');

-- ----------------------------
-- Table structure for platform_cs_message
-- ----------------------------
DROP TABLE IF EXISTS `platform_cs_message`;
CREATE TABLE `platform_cs_message`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `conversation_id` bigint(0) NOT NULL COMMENT '会话ID',
  `sender_role` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '发送方：user/admin',
  `sender_user_id` bigint(0) NULL DEFAULT NULL COMMENT 'C端用户ID',
  `sender_admin_id` bigint(0) NULL DEFAULT NULL COMMENT '管理端操作人ID',
  `msg_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'text' COMMENT '消息类型',
  `content` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文本内容',
  `is_read` tinyint(0) NOT NULL DEFAULT 0 COMMENT '接收方是否已读',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_platform_cs_msg_conv`(`conversation_id`, `create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '平台官方客服消息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of platform_cs_message
-- ----------------------------
INSERT INTO `platform_cs_message` VALUES (1, 1, 'user', 4, NULL, 'text', '你好，是客服吗', 1, '2026-09-02 16:29:37');
INSERT INTO `platform_cs_message` VALUES (2, 1, 'admin', NULL, NULL, 'text', '是的请问您有什么问题', 1, '2026-09-02 16:34:19');

-- ----------------------------
-- Table structure for referral_commission
-- ----------------------------
DROP TABLE IF EXISTS `referral_commission`;
CREATE TABLE `referral_commission`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL,
  `order_id` bigint(0) NULL DEFAULT NULL,
  `rate` decimal(10, 4) NULL DEFAULT NULL COMMENT '????',
  `amount` decimal(12, 2) NOT NULL,
  `type` tinyint(0) NULL DEFAULT NULL,
  `direction` tinyint(0) NULL DEFAULT 1 COMMENT '????: 1=??, 2=??',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '????',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '????',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_referral_commission_user_id`(`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '佣金明细' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of referral_commission
-- ----------------------------

-- ----------------------------
-- Table structure for referral_user_commission
-- ----------------------------
DROP TABLE IF EXISTS `referral_user_commission`;
CREATE TABLE `referral_user_commission`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL,
  `commission` decimal(12, 2) NULL DEFAULT NULL,
  `invite_count` int(0) NULL DEFAULT 0,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_referral_commission_user_id`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户佣金汇总' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of referral_user_commission
-- ----------------------------
INSERT INTO `referral_user_commission` VALUES (1, 2, 0.00, 0, '2026-08-31 09:32:15', '2026-08-31 09:32:15');
INSERT INTO `referral_user_commission` VALUES (2, 3, 0.00, 0, '2026-08-31 10:30:05', '2026-08-31 10:30:05');
INSERT INTO `referral_user_commission` VALUES (3, 4, 0.00, 0, '2026-08-31 10:39:42', '2026-08-31 10:39:42');
INSERT INTO `referral_user_commission` VALUES (4, 5, 0.00, 0, '2026-09-01 15:06:59', '2026-09-01 15:06:59');
INSERT INTO `referral_user_commission` VALUES (5, 6, 0.00, 0, '2026-09-02 20:10:23', '2026-09-02 20:10:23');
INSERT INTO `referral_user_commission` VALUES (6, 7, 0.00, 0, '2026-09-03 11:39:35', '2026-09-03 11:39:35');

-- ----------------------------
-- Table structure for rent_cart
-- ----------------------------
DROP TABLE IF EXISTS `rent_cart`;
CREATE TABLE `rent_cart`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '购物车ID',
  `user_id` bigint(0) NOT NULL COMMENT '用户ID',
  `goods_id` bigint(0) NOT NULL COMMENT '商品ID',
  `spec_id` bigint(0) NOT NULL DEFAULT 0 COMMENT '规格ID',
  `package_id` bigint(0) NOT NULL DEFAULT 0 COMMENT '配件套餐ID，0表示无',
  `rent_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'day' COMMENT '租赁类型：day/hour/overnight',
  `start_time` datetime(0) NULL DEFAULT NULL COMMENT '租期开始',
  `end_time` datetime(0) NULL DEFAULT NULL COMMENT '租期结束',
  `qty` int(0) NOT NULL DEFAULT 1 COMMENT '数量',
  `selected` tinyint(0) NOT NULL DEFAULT 1 COMMENT '是否勾选：1是 0否',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `deleted` int(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 -1删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rent_cart_user`(`user_id`, `deleted`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 42 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租赁购物车' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rent_cart
-- ----------------------------
INSERT INTO `rent_cart` VALUES (1, 1, 1, 0, 0, 'day', '2026-09-15 10:00:00', '2026-09-17 18:00:00', 4, 1, '2026-08-04 00:34:17', '2026-08-04 00:34:54', -1);
INSERT INTO `rent_cart` VALUES (2, 1, 1, 0, 0, 'day', '2026-09-15 10:00:00', '2026-09-17 18:00:00', 1, 1, '2026-08-04 00:34:55', '2026-08-04 00:34:55', -1);
INSERT INTO `rent_cart` VALUES (3, 1, 1, 0, 0, 'day', '2026-09-15 10:00:00', '2026-09-17 18:00:00', 1, 1, '2026-08-04 00:34:55', '2026-08-04 00:34:55', -1);
INSERT INTO `rent_cart` VALUES (4, 1, 1, 0, 0, 'day', '2026-09-15 10:00:00', '2026-09-17 18:00:00', 2, 1, '2026-08-04 00:35:30', '2026-08-04 00:35:58', -1);
INSERT INTO `rent_cart` VALUES (5, 1, 1, 0, 0, 'day', '2026-09-15 10:00:00', '2026-09-17 18:00:00', 1, 1, '2026-08-04 00:36:14', '2026-08-04 00:36:13', -1);
INSERT INTO `rent_cart` VALUES (6, 1, 1, 0, 0, 'day', '2026-09-15 10:00:00', '2026-09-17 18:00:00', 1, 1, '2026-08-04 01:07:06', '2026-08-04 01:07:06', -1);
INSERT INTO `rent_cart` VALUES (7, 1, 1, 0, 0, 'day', '2026-09-15 10:00:00', '2026-09-17 18:00:00', 1, 1, '2026-08-04 01:07:06', '2026-08-04 01:07:06', -1);
INSERT INTO `rent_cart` VALUES (8, 1, 1, 0, 0, 'day', '2026-09-15 10:00:00', '2026-09-17 18:00:00', 1, 1, '2026-08-04 01:07:07', '2026-08-04 01:07:07', -1);
INSERT INTO `rent_cart` VALUES (9, 1, 1, 0, 0, 'day', '2026-09-15 10:00:00', '2026-09-17 18:00:00', 1, 1, '2026-08-04 01:35:18', '2026-08-04 01:35:18', -1);
INSERT INTO `rent_cart` VALUES (10, 1, 1, 0, 0, 'day', '2026-09-15 10:00:00', '2026-09-17 18:00:00', 1, 1, '2026-08-04 01:35:56', '2026-08-04 01:35:56', -1);
INSERT INTO `rent_cart` VALUES (11, 1, 1, 0, 0, 'day', '2026-09-15 10:00:00', '2026-09-17 18:00:00', 1, 1, '2026-08-04 01:35:57', '2026-08-04 01:35:56', -1);
INSERT INTO `rent_cart` VALUES (12, 1, 1, 0, 0, 'day', '2026-09-15 10:00:00', '2026-09-17 18:00:00', 1, 1, '2026-08-04 01:37:55', '2026-08-04 01:37:55', -1);
INSERT INTO `rent_cart` VALUES (13, 1, 1, 0, 0, 'day', '2026-09-15 10:00:00', '2026-09-17 18:00:00', 1, 1, '2026-08-04 01:37:56', '2026-08-04 01:37:55', -1);
INSERT INTO `rent_cart` VALUES (14, 1, 1, 0, 0, 'day', '2026-09-15 10:00:00', '2026-09-17 18:00:00', 1, 1, '2026-08-04 01:38:19', '2026-08-04 01:38:19', -1);
INSERT INTO `rent_cart` VALUES (15, 1, 1, 0, 0, 'day', '2026-09-15 10:00:00', '2026-09-17 18:00:00', 1, 1, '2026-08-04 01:38:20', '2026-08-04 01:38:19', -1);
INSERT INTO `rent_cart` VALUES (16, 1, 1, 0, 0, 'day', '2026-10-01 10:00:00', '2026-10-08 18:00:00', 1, 1, '2026-08-04 02:22:33', '2026-08-04 02:22:32', -1);
INSERT INTO `rent_cart` VALUES (17, 1, 1, 0, 0, 'day', '2026-10-01 10:00:00', '2026-10-08 18:00:00', 1, 1, '2026-08-04 02:23:30', '2026-08-04 02:23:30', -1);
INSERT INTO `rent_cart` VALUES (18, 1, 1, 0, 0, 'day', '2026-10-01 10:00:00', '2026-10-08 18:00:00', 1, 1, '2026-08-04 02:27:24', '2026-08-04 02:27:24', -1);
INSERT INTO `rent_cart` VALUES (19, 1, 1, 0, 0, 'day', '2026-11-01 10:00:00', '2026-11-03 18:00:00', 1, 1, '2026-08-04 02:27:24', '2026-08-04 02:27:24', -1);
INSERT INTO `rent_cart` VALUES (20, 1, 1, 0, 0, 'day', '2026-09-15 10:00:00', '2026-09-17 18:00:00', 1, 1, '2026-08-04 02:38:01', '2026-08-04 02:38:00', -1);
INSERT INTO `rent_cart` VALUES (21, 1, 1, 0, 0, 'day', '2026-09-15 10:00:00', '2026-09-17 18:00:00', 1, 1, '2026-08-04 02:38:50', '2026-08-04 02:38:50', -1);
INSERT INTO `rent_cart` VALUES (22, 1, 1, 0, 0, 'day', '2026-09-15 10:00:00', '2026-09-17 18:00:00', 1, 1, '2026-08-04 02:39:17', '2026-08-04 02:39:17', -1);
INSERT INTO `rent_cart` VALUES (23, 1, 1, 0, 0, 'day', '2026-09-15 10:00:00', '2026-09-17 18:00:00', 1, 1, '2026-08-04 02:39:51', '2026-08-04 02:39:50', -1);
INSERT INTO `rent_cart` VALUES (24, 1, 1, 0, 0, 'day', '2026-09-15 10:00:00', '2026-09-17 18:00:00', 1, 1, '2026-08-04 02:40:13', '2026-08-04 02:40:13', -1);
INSERT INTO `rent_cart` VALUES (25, 1, 1, 0, 0, 'day', '2026-09-15 10:00:00', '2026-09-17 18:00:00', 1, 1, '2026-08-04 02:40:42', '2026-08-04 02:40:41', -1);
INSERT INTO `rent_cart` VALUES (26, 1, 1, 0, 0, 'day', '2026-12-05 10:00:00', '2026-12-07 18:00:00', 1, 1, '2026-08-04 12:43:13', '2026-08-04 12:43:13', -1);
INSERT INTO `rent_cart` VALUES (27, 1, 1, 0, 0, 'day', '2026-12-01 10:00:00', '2026-12-03 18:00:00', 1, 1, '2026-08-04 12:43:20', '2026-08-04 12:43:20', -1);
INSERT INTO `rent_cart` VALUES (28, 1, 1, 0, 0, 'day', '2026-12-01 10:00:00', '2026-12-03 18:00:00', 1, 1, '2026-08-04 13:17:16', '2026-08-04 13:17:17', -1);
INSERT INTO `rent_cart` VALUES (29, 1, 1, 0, 0, 'day', '2026-12-05 10:00:00', '2026-12-07 18:00:00', 1, 1, '2026-08-04 13:17:27', '2026-08-04 13:17:27', -1);
INSERT INTO `rent_cart` VALUES (30, 1, 1, 0, 0, 'day', '2026-12-01 10:00:00', '2026-12-03 18:00:00', 1, 1, '2026-08-04 13:22:24', '2026-08-04 13:22:24', -1);
INSERT INTO `rent_cart` VALUES (31, 4, 2, 0, 0, 'day', '2026-09-02 11:00:00', '2026-09-03 11:00:00', 1, 1, '2026-08-31 13:26:26', '2026-08-31 13:26:25', 0);
INSERT INTO `rent_cart` VALUES (32, 4, 1, 0, 0, 'day', '2026-09-02 11:00:00', '2026-09-03 11:00:00', 3, 1, '2026-08-31 17:48:48', '2026-08-31 17:48:47', 0);
INSERT INTO `rent_cart` VALUES (33, 4, 7, 0, 0, 'day', '2026-09-02 11:00:00', '2026-09-03 11:00:00', 1, 1, '2026-08-31 17:57:03', '2026-08-31 17:57:02', 0);
INSERT INTO `rent_cart` VALUES (34, 4, 15, 44, 0, 'day', '2026-09-02 11:00:00', '2026-09-03 11:00:00', 1, 1, '2026-09-01 17:20:36', '2026-09-01 17:20:36', 0);
INSERT INTO `rent_cart` VALUES (35, 7, 15, 44, 0, 'day', '2026-09-03 17:00:00', '2026-09-04 17:00:00', 1, 1, '2026-09-03 16:30:35', '2026-09-03 17:11:11', -1);
INSERT INTO `rent_cart` VALUES (36, 7, 2, 0, 0, 'day', '2026-09-03 17:00:00', '2026-09-04 17:00:00', 1, 1, '2026-09-03 16:30:38', '2026-09-03 17:11:11', -1);
INSERT INTO `rent_cart` VALUES (37, 1, 15, 44, 0, 'day', '2026-09-15 10:00:00', '2026-09-17 18:00:00', 1, 1, '2026-09-03 16:59:19', '2026-09-03 16:59:18', 0);
INSERT INTO `rent_cart` VALUES (38, 1, 15, 44, 21, 'day', '2026-09-15 10:00:00', '2026-09-17 18:00:00', 1, 1, '2026-09-03 17:09:12', '2026-09-03 17:09:12', 0);
INSERT INTO `rent_cart` VALUES (39, 7, 15, 44, 0, 'day', '2026-09-03 18:00:00', '2026-09-04 18:00:00', 1, 1, '2026-09-03 17:11:16', '2026-09-03 17:20:55', -1);
INSERT INTO `rent_cart` VALUES (40, 7, 1, 0, 0, 'day', '2026-09-03 18:00:00', '2026-09-04 18:00:00', 1, 1, '2026-09-03 17:11:21', '2026-09-03 17:20:55', -1);
INSERT INTO `rent_cart` VALUES (41, 7, 15, 44, 20, 'day', '2026-09-03 18:00:00', '2026-09-04 18:00:00', 1, 1, '2026-09-03 17:21:00', '2026-09-03 17:21:00', 0);

-- ----------------------------
-- Table structure for rent_category
-- ----------------------------
DROP TABLE IF EXISTS `rent_category`;
CREATE TABLE `rent_category`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `parent_id` bigint(0) NOT NULL DEFAULT 0 COMMENT '父分类ID，根为0',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图标地址',
  `sort` int(0) NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
  `status` tinyint(0) NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `deleted` int(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 -1删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rent_category_parent`(`parent_id`, `status`, `sort`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租赁商品分类' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rent_category
-- ----------------------------
INSERT INTO `rent_category` VALUES (2, 0, '桌椅', NULL, 2, 1, '2026-08-03 22:32:42', '2026-08-03 22:32:42', 0);
INSERT INTO `rent_category` VALUES (3, 0, '炊具', NULL, 3, 1, '2026-08-03 22:32:42', '2026-08-03 22:32:42', 0);
INSERT INTO `rent_category` VALUES (4, 0, '照明', NULL, 4, 1, '2026-08-03 22:32:42', '2026-08-03 22:32:42', 0);
INSERT INTO `rent_category` VALUES (5, 0, '收纳', NULL, 5, 1, '2026-08-03 22:32:42', '2026-08-03 22:32:42', 0);
INSERT INTO `rent_category` VALUES (6, 0, '户外配件', NULL, 6, 1, '2026-08-03 22:32:42', '2026-08-03 22:32:42', 0);
INSERT INTO `rent_category` VALUES (11, 1, '帐篷睡袋', NULL, 1, 1, '2026-08-03 22:32:42', '2026-08-03 22:32:42', 0);
INSERT INTO `rent_category` VALUES (12, 6, '天幕遮阳', NULL, 1, 1, '2026-08-03 22:32:42', '2026-08-03 22:32:42', 0);
INSERT INTO `rent_category` VALUES (13, 2, '桌椅家具', NULL, 1, 1, '2026-08-03 22:32:42', '2026-08-03 22:32:42', 0);
INSERT INTO `rent_category` VALUES (14, 3, '炉具炊具', NULL, 1, 1, '2026-08-03 22:32:42', '2026-08-03 22:32:42', 0);
INSERT INTO `rent_category` VALUES (15, 4, '露营灯具', NULL, 1, 1, '2026-08-03 22:32:42', '2026-08-03 22:32:42', 0);
INSERT INTO `rent_category` VALUES (16, 5, '营地车包', NULL, 1, 1, '2026-08-03 22:32:42', '2026-08-03 22:32:42', 0);
INSERT INTO `rent_category` VALUES (17, 1, '充气帐篷', NULL, 0, 1, '2026-09-01 14:35:22', '2026-09-01 14:35:21', 0);

-- ----------------------------
-- Table structure for rent_goods
-- ----------------------------
DROP TABLE IF EXISTS `rent_goods`;
CREATE TABLE `rent_goods`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `category_id` bigint(0) NOT NULL COMMENT '所属分类ID',
  `goods_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商品业务编号',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '商品名称',
  `subtitle` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '副标题/卖点摘要',
  `detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '商品详情描述',
  `cover_image` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '封面图',
  `tutorial_video_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '使用教程视频URL',
  `rent_price_day` decimal(10, 2) NOT NULL COMMENT '日租价（元）',
  `rent_price_hour` decimal(10, 2) NULL DEFAULT NULL COMMENT '时租价（元）',
  `rent_price_overnight` decimal(10, 2) NULL DEFAULT NULL COMMENT '过夜租价（元）',
  `overnight_start_time` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '18:00' COMMENT '过夜租开始时间 HH:mm',
  `overnight_end_time` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '10:00' COMMENT '过夜租结束时间 HH:mm',
  `deposit_amount` decimal(10, 2) NOT NULL COMMENT '押金（元）',
  `sales_count` int(0) NOT NULL DEFAULT 0 COMMENT '销量',
  `tags` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标签，逗号分隔',
  `status` tinyint(0) NOT NULL DEFAULT 1 COMMENT '状态：1上架 0下架',
  `merchant_id` bigint(0) NULL DEFAULT NULL COMMENT '商户ID',
  `sort` int(0) NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `deleted` int(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 -1删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_rent_goods_no`(`goods_no`) USING BTREE,
  INDEX `idx_rent_goods_category`(`category_id`, `status`, `sort`) USING BTREE,
  INDEX `idx_rent_goods_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租赁商品' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rent_goods
-- ----------------------------
INSERT INTO `rent_goods` VALUES (1, 11, 'RG001', '双峰自动速开帐篷 3-4人', '液压速开｜防暴雨3000mm', '液压自动速开支架，3秒开合；210D牛津布外帐，PU3000mm防水涂层；双门双窗通风设计，带防蚊纱网；含地钉、防风绳、收纳袋全套配件。', 'https://tu.mianyazuju.com/2026/08/31/784f962e-e1e0-4770-9da6-65def24f3896.jpg', NULL, 29.90, 4.90, 19.90, '18:00', '10:00', 200.00, 326, '免搭建,防暴雨', 1, 1, 1, '2026-08-03 22:32:42', '2026-08-31 10:02:56', 0);
INSERT INTO `rent_goods` VALUES (2, 12, 'RG002', '黑胶天幕 5.2×4.5m', 'UPF50+｜可容纳8-10人', '210T黑胶涂层涤纶面料，全遮光防晒；加固织带挂点，抗风性强；含2.4m加粗撑杆×2、地钉、防风绳、收纳袋。', 'https://tu.mianyazuju.com/2026/08/31/271b6a6d-bdf8-4a41-b8f0-ca1e84831471.jpg', NULL, 19.90, 3.50, 15.90, '18:00', '10:00', 150.00, 512, '防晒,大空间', 1, 1, 2, '2026-08-03 22:32:42', '2026-08-31 10:02:56', 0);
INSERT INTO `rent_goods` VALUES (3, 13, 'RG003', '铝合金蛋卷桌 120cm', '折叠收纳｜承重50kg', '6063铝合金桌腿+蛋卷式桌面，一秒折叠展开；三角稳固结构不晃动；附赠便携收纳包，车载携带无压力。', 'https://tu.mianyazuju.com/2026/08/31/2746cba5-7de2-4053-963e-db0c139557a1.jpg', NULL, 9.90, 2.50, NULL, '18:00', '10:00', 80.00, 189, '轻便,折叠', 1, 1, 3, '2026-08-03 22:32:42', '2026-08-31 10:02:55', 0);
INSERT INTO `rent_goods` VALUES (4, 13, 'RG004', '克米特折叠椅 双人', '实木扶手｜久坐不累', '经典克米特椅型，加宽坐面包裹感强；榉木扶手温润防烫；X型折叠结构，收纳体积小；含椅套收纳袋。', 'https://tu.mianyazuju.com/2026/08/31/bd5d8727-7e1f-4b87-8a6f-95bc674ff5e4.jpg', NULL, 8.80, NULL, NULL, '18:00', '10:00', 60.00, 245, '舒适,高颜值', 1, 1, 4, '2026-08-03 22:32:42', '2026-08-31 10:02:54', 0);
INSERT INTO `rent_goods` VALUES (5, 14, 'RG005', '卡式炉+烤盘套装', '2900W猛火｜防风设计', '2900W猛火蓝焰，煎烤快人一步；双防风圈设计，户外微风不慌；配不粘烤盘+汤锅，套装到手即烤；过压保护自动断气，安全可靠。', 'https://tu.mianyazuju.com/2026/08/31/8f87789c-9e25-4388-9db2-8b4ead4537aa.jpg', NULL, 12.90, NULL, NULL, '18:00', '10:00', 100.00, 158, '猛火,套装', 1, 1, 5, '2026-08-03 22:32:42', '2026-08-31 10:02:54', 0);
INSERT INTO `rent_goods` VALUES (6, 15, 'RG006', '复古营地马灯 充电款', '无极调光｜续航80h', '复古马灯造型，氛围感拉满；暖黄/暖白双色温切换；旋钮无极调光；顶部挂钩+底部1/4螺口，帐篷天幕随心挂。', 'https://tu.mianyazuju.com/2026/08/31/dad1a2f5-fc17-4374-bc2a-c4aee69b2fd1.jpg', NULL, 5.90, 2.00, NULL, '18:00', '10:00', 50.00, 421, '氛围感,长续航', 1, NULL, 6, '2026-08-03 22:32:42', '2026-08-31 10:02:53', 0);
INSERT INTO `rent_goods` VALUES (7, 11, 'RG007', '羽绒睡袋 -5℃温标', '90%白鸭绒｜可拼接', '800蓬90%白鸭绒，轻盈保暖；信封式设计可完全展开当被子；左右款可拼接成双人睡袋；20D防绒面料，附压缩收纳袋。', 'https://tu.mianyazuju.com/2026/08/31/4914e98e-3812-4270-8d59-07130f65ab96.jpg', NULL, 15.90, NULL, 12.90, '18:00', '10:00', 120.00, 97, '保暖,轻量', 1, NULL, 7, '2026-08-03 22:32:42', '2026-08-31 10:02:53', 0);
INSERT INTO `rent_goods` VALUES (8, 16, 'RG008', '越野轮营地推车', '150L大容量｜坦克轮', '150L超大容量，一车装完全家装备；10寸加宽坦克轮，草地沙滩泥地通吃；可调节拉杆+双刹车；对折秒收纳，后备箱好安放。', 'https://tu.mianyazuju.com/2026/08/31/3b4b54f5-0645-473c-910e-a1549df14326.jpg', NULL, 16.90, NULL, NULL, '18:00', '10:00', 150.00, 203, '大容量,省力', 1, NULL, 8, '2026-08-03 22:32:42', '2026-08-31 10:02:52', 0);
INSERT INTO `rent_goods` VALUES (9, 11, 'RG-SMOKE-M02-1785772405902', 'SMOKE-M02-1785772405902', NULL, NULL, '/upload/m02/smoke-cover.jpg', NULL, 19.90, NULL, NULL, '18:00', '10:00', 100.00, 0, NULL, 0, NULL, 0, '2026-08-03 23:53:26', '2026-08-03 23:53:26', -1);
INSERT INTO `rent_goods` VALUES (10, 11, 'RG-E2E-M02-1785772577713', 'E2E-M02-测试商品-1785772577713', NULL, NULL, NULL, NULL, 19.90, NULL, NULL, '18:00', '10:00', 100.00, 0, NULL, 1, NULL, 0, '2026-08-03 23:56:18', '2026-08-03 23:56:17', -1);
INSERT INTO `rent_goods` VALUES (11, 11, 'RG-E2E-M02-1785774299982', 'E2E-M02-测试商品-1785774299982', NULL, NULL, NULL, NULL, 19.90, NULL, NULL, '18:00', '10:00', 100.00, 0, NULL, 1, NULL, 0, '2026-08-04 00:25:00', '2026-08-04 00:25:00', -1);
INSERT INTO `rent_goods` VALUES (12, 11, 'RG-E2E-M02-1785774856074', 'E2E-M02-测试商品-1785774856074', NULL, NULL, NULL, NULL, 19.90, NULL, NULL, '18:00', '10:00', 100.00, 0, NULL, 1, NULL, 0, '2026-08-04 00:34:16', '2026-08-04 00:34:16', -1);
INSERT INTO `rent_goods` VALUES (13, 11, 'RG-E2E-M02-1785774893553', 'E2E-M02-测试商品-1785774893553', NULL, NULL, NULL, NULL, 19.90, NULL, NULL, '18:00', '10:00', 100.00, 0, NULL, 1, NULL, 0, '2026-08-04 00:34:54', '2026-08-04 00:34:53', -1);
INSERT INTO `rent_goods` VALUES (14, 11, 'RG-E2E-M02-1785776825170', 'E2E-M02-测试商品-1785776825170', NULL, NULL, NULL, NULL, 19.90, NULL, NULL, '18:00', '10:00', 100.00, 0, NULL, 1, NULL, 0, '2026-08-04 01:07:05', '2026-08-04 01:07:05', -1);
INSERT INTO `rent_goods` VALUES (15, 17, 'RG1788244175371', '双人自动重启帐篷带遮阳', '双人自动重启帐篷带遮阳、地丁、一件充气操作简单', '双人自动重启帐篷带遮阳、地丁、一件充气操作简单', 'https://tu.mianyazuju.com/2026/09/01/63f447db-8b5b-4b09-b789-215bcf3fa742.png', 'https://tu.mianyazuju.com/2026/09/01/716580ce-d007-49c0-97bc-8a437f244ccc.mp4', 120.00, 12.00, 150.00, '17:00', '11:00', 200.00, 0, NULL, 1, 2, 0, '2026-09-01 14:29:35', '2026-09-01 16:47:59', 0);

-- ----------------------------
-- Table structure for rent_goods_image
-- ----------------------------
DROP TABLE IF EXISTS `rent_goods_image`;
CREATE TABLE `rent_goods_image`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `goods_id` bigint(0) NOT NULL COMMENT '商品ID',
  `image_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图片地址',
  `sort` int(0) NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `deleted` int(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 -1删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rent_goods_image_goods`(`goods_id`, `sort`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 44 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租赁商品图片' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rent_goods_image
-- ----------------------------
INSERT INTO `rent_goods_image` VALUES (1, 1, 'https://tu.mianyazuju.com/2026/08/31/c0e96578-d848-496b-9a66-93f488b3d54f.jpg', 1, '2026-08-03 22:32:42', '2026-08-31 10:03:05', 0);
INSERT INTO `rent_goods_image` VALUES (2, 1, 'https://tu.mianyazuju.com/2026/08/31/2fc0c241-a7b6-4cde-bfe3-1dca4ce71162.jpg', 2, '2026-08-03 22:32:42', '2026-08-31 10:03:05', 0);
INSERT INTO `rent_goods_image` VALUES (3, 1, 'https://tu.mianyazuju.com/2026/08/31/52bdaf8d-6639-4328-8cc1-53cd5c963735.jpg', 3, '2026-08-03 22:32:42', '2026-08-31 10:03:06', 0);
INSERT INTO `rent_goods_image` VALUES (4, 2, 'https://tu.mianyazuju.com/2026/08/31/bc44e105-3d72-4d46-bbce-9e550f012ba1.jpg', 1, '2026-08-03 22:32:42', '2026-08-31 10:03:04', 0);
INSERT INTO `rent_goods_image` VALUES (5, 2, 'https://tu.mianyazuju.com/2026/08/31/47b40fdc-e33a-4047-a19a-6ff543b03bc3.jpg', 2, '2026-08-03 22:32:42', '2026-08-31 10:03:04', 0);
INSERT INTO `rent_goods_image` VALUES (6, 3, 'https://tu.mianyazuju.com/2026/08/31/1fceb956-feb4-4c9b-be99-f4d36ca5d6e7.jpg', 1, '2026-08-03 22:32:42', '2026-08-31 10:03:02', 0);
INSERT INTO `rent_goods_image` VALUES (7, 3, 'https://tu.mianyazuju.com/2026/08/31/a89048c7-ecdb-41af-b27b-31564ca72072.jpg', 2, '2026-08-03 22:32:42', '2026-08-31 10:03:03', 0);
INSERT INTO `rent_goods_image` VALUES (8, 4, 'https://tu.mianyazuju.com/2026/08/31/70c609b2-1a98-4f22-b73b-46b2fbe51109.jpg', 1, '2026-08-03 22:32:42', '2026-08-31 10:03:01', 0);
INSERT INTO `rent_goods_image` VALUES (9, 4, 'https://tu.mianyazuju.com/2026/08/31/d5728a83-a9fa-41bb-9c50-b935e6803e1f.jpg', 2, '2026-08-03 22:32:42', '2026-08-31 10:03:02', 0);
INSERT INTO `rent_goods_image` VALUES (10, 5, 'https://tu.mianyazuju.com/2026/08/31/0e08e08a-7324-4adb-8d31-39ef8a38c0cc.jpg', 1, '2026-08-03 22:32:42', '2026-08-31 10:03:00', 0);
INSERT INTO `rent_goods_image` VALUES (11, 5, 'https://tu.mianyazuju.com/2026/08/31/573d2346-87f3-4a80-a7a6-7b535a434f8a.jpg', 2, '2026-08-03 22:32:42', '2026-08-31 10:03:01', 0);
INSERT INTO `rent_goods_image` VALUES (12, 6, 'https://tu.mianyazuju.com/2026/08/31/afb079d8-2318-4c66-98d4-cfdee2f7a7d2.jpg', 1, '2026-08-03 22:32:42', '2026-08-31 10:02:59', 0);
INSERT INTO `rent_goods_image` VALUES (13, 6, 'https://tu.mianyazuju.com/2026/08/31/393ead98-2614-466f-803b-8b13551012e4.jpg', 2, '2026-08-03 22:32:42', '2026-08-31 10:03:00', 0);
INSERT INTO `rent_goods_image` VALUES (14, 7, 'https://tu.mianyazuju.com/2026/08/31/ef05f454-25bd-43b7-91fb-0368c480ab85.jpg', 1, '2026-08-03 22:32:42', '2026-08-31 10:02:58', 0);
INSERT INTO `rent_goods_image` VALUES (15, 7, 'https://tu.mianyazuju.com/2026/08/31/0aff0755-7770-48ba-81de-514da18d9b76.jpg', 2, '2026-08-03 22:32:42', '2026-08-31 10:02:58', 0);
INSERT INTO `rent_goods_image` VALUES (16, 8, 'https://tu.mianyazuju.com/2026/08/31/dbc6d111-3d03-4eb4-bf3e-69851a023af1.jpg', 1, '2026-08-03 22:32:42', '2026-08-31 10:02:57', 0);
INSERT INTO `rent_goods_image` VALUES (17, 8, 'https://tu.mianyazuju.com/2026/08/31/2e6f9e76-e03c-4395-8450-149453a3d0f0.jpg', 2, '2026-08-03 22:32:42', '2026-08-31 10:02:57', 0);
INSERT INTO `rent_goods_image` VALUES (18, 9, '/upload/m02/smoke-1.jpg', 1, '2026-08-03 23:53:26', '2026-08-03 23:53:26', -1);
INSERT INTO `rent_goods_image` VALUES (19, 9, '/upload/m02/smoke-2.jpg', 2, '2026-08-03 23:53:26', '2026-08-03 23:53:26', -1);
INSERT INTO `rent_goods_image` VALUES (20, 15, 'https://tu.mianyazuju.com/2026/09/01/63f447db-8b5b-4b09-b789-215bcf3fa742.png', 1, '2026-09-01 14:29:35', '2026-09-01 16:06:03', -1);
INSERT INTO `rent_goods_image` VALUES (21, 15, 'https://tu.mianyazuju.com/2026/09/01/396e2f24-66b9-42e5-b863-afcece8df85b.png', 2, '2026-09-01 14:29:35', '2026-09-01 16:06:03', -1);
INSERT INTO `rent_goods_image` VALUES (22, 15, 'https://tu.mianyazuju.com/2026/09/01/5b5d63ae-58da-4a3e-8b90-ed2768454733.png', 3, '2026-09-01 14:29:35', '2026-09-01 16:06:03', -1);
INSERT INTO `rent_goods_image` VALUES (23, 15, 'https://tu.mianyazuju.com/2026/09/01/94844a3b-ba7b-4add-8fd2-381738db5327.png', 4, '2026-09-01 14:29:35', '2026-09-01 16:06:03', -1);
INSERT INTO `rent_goods_image` VALUES (28, 15, 'https://tu.mianyazuju.com/2026/09/01/63f447db-8b5b-4b09-b789-215bcf3fa742.png', 1, '2026-09-01 16:06:03', '2026-09-01 16:06:16', -1);
INSERT INTO `rent_goods_image` VALUES (29, 15, 'https://tu.mianyazuju.com/2026/09/01/396e2f24-66b9-42e5-b863-afcece8df85b.png', 2, '2026-09-01 16:06:03', '2026-09-01 16:06:16', -1);
INSERT INTO `rent_goods_image` VALUES (30, 15, 'https://tu.mianyazuju.com/2026/09/01/5b5d63ae-58da-4a3e-8b90-ed2768454733.png', 3, '2026-09-01 16:06:03', '2026-09-01 16:06:16', -1);
INSERT INTO `rent_goods_image` VALUES (31, 15, 'https://tu.mianyazuju.com/2026/09/01/94844a3b-ba7b-4add-8fd2-381738db5327.png', 4, '2026-09-01 16:06:03', '2026-09-01 16:06:16', -1);
INSERT INTO `rent_goods_image` VALUES (32, 15, 'https://tu.mianyazuju.com/2026/09/01/63f447db-8b5b-4b09-b789-215bcf3fa742.png', 1, '2026-09-01 16:06:17', '2026-09-01 16:10:03', -1);
INSERT INTO `rent_goods_image` VALUES (33, 15, 'https://tu.mianyazuju.com/2026/09/01/396e2f24-66b9-42e5-b863-afcece8df85b.png', 2, '2026-09-01 16:06:17', '2026-09-01 16:10:03', -1);
INSERT INTO `rent_goods_image` VALUES (34, 15, 'https://tu.mianyazuju.com/2026/09/01/5b5d63ae-58da-4a3e-8b90-ed2768454733.png', 3, '2026-09-01 16:06:17', '2026-09-01 16:10:03', -1);
INSERT INTO `rent_goods_image` VALUES (35, 15, 'https://tu.mianyazuju.com/2026/09/01/94844a3b-ba7b-4add-8fd2-381738db5327.png', 4, '2026-09-01 16:06:17', '2026-09-01 16:10:03', -1);
INSERT INTO `rent_goods_image` VALUES (36, 15, 'https://tu.mianyazuju.com/2026/09/01/63f447db-8b5b-4b09-b789-215bcf3fa742.png', 1, '2026-09-01 16:10:03', '2026-09-01 16:47:49', -1);
INSERT INTO `rent_goods_image` VALUES (37, 15, 'https://tu.mianyazuju.com/2026/09/01/396e2f24-66b9-42e5-b863-afcece8df85b.png', 2, '2026-09-01 16:10:03', '2026-09-01 16:47:49', -1);
INSERT INTO `rent_goods_image` VALUES (38, 15, 'https://tu.mianyazuju.com/2026/09/01/5b5d63ae-58da-4a3e-8b90-ed2768454733.png', 3, '2026-09-01 16:10:03', '2026-09-01 16:47:49', -1);
INSERT INTO `rent_goods_image` VALUES (39, 15, 'https://tu.mianyazuju.com/2026/09/01/94844a3b-ba7b-4add-8fd2-381738db5327.png', 4, '2026-09-01 16:10:03', '2026-09-01 16:47:49', -1);
INSERT INTO `rent_goods_image` VALUES (40, 15, 'https://tu.mianyazuju.com/2026/09/01/63f447db-8b5b-4b09-b789-215bcf3fa742.png', 1, '2026-09-01 16:47:49', '2026-09-01 16:47:49', 0);
INSERT INTO `rent_goods_image` VALUES (41, 15, 'https://tu.mianyazuju.com/2026/09/01/396e2f24-66b9-42e5-b863-afcece8df85b.png', 2, '2026-09-01 16:47:49', '2026-09-01 16:47:49', 0);
INSERT INTO `rent_goods_image` VALUES (42, 15, 'https://tu.mianyazuju.com/2026/09/01/5b5d63ae-58da-4a3e-8b90-ed2768454733.png', 3, '2026-09-01 16:47:49', '2026-09-01 16:47:49', 0);
INSERT INTO `rent_goods_image` VALUES (43, 15, 'https://tu.mianyazuju.com/2026/09/01/94844a3b-ba7b-4add-8fd2-381738db5327.png', 4, '2026-09-01 16:47:49', '2026-09-01 16:47:49', 0);

-- ----------------------------
-- Table structure for rent_goods_package
-- ----------------------------
DROP TABLE IF EXISTS `rent_goods_package`;
CREATE TABLE `rent_goods_package`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '套餐ID',
  `goods_id` bigint(0) NOT NULL COMMENT '商品ID',
  `package_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '套餐名称',
  `package_desc` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '套餐说明',
  `price` decimal(10, 2) NOT NULL COMMENT '套餐加价（元）',
  `sort` int(0) NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `deleted` int(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 -1删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rent_goods_package_goods`(`goods_id`, `sort`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租赁商品配件套餐' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rent_goods_package
-- ----------------------------
INSERT INTO `rent_goods_package` VALUES (1, 1, '地钉防风绳加强包', '含地钉×12、防风绳×4、备用帐杆', 9.90, 1, '2026-08-03 23:52:13', '2026-08-03 23:52:13', 0);
INSERT INTO `rent_goods_package` VALUES (2, 1, '防潮垫双人款', '2cm厚自动充气防潮垫', 15.00, 2, '2026-08-03 23:52:13', '2026-08-03 23:52:13', 0);
INSERT INTO `rent_goods_package` VALUES (3, 5, '气罐4只装', '丁烷气罐220g×4（需到店自提）', 28.00, 1, '2026-08-03 23:52:13', '2026-08-03 23:52:13', 0);
INSERT INTO `rent_goods_package` VALUES (4, 5, '烧烤工具包', '夹子、刷子、调料盒', 12.00, 2, '2026-08-03 23:52:13', '2026-08-03 23:52:13', 0);
INSERT INTO `rent_goods_package` VALUES (5, 9, '联调测试套餐', 'M02 冒烟', 8.80, 1, '2026-08-03 23:53:26', '2026-08-03 23:53:26', -1);
INSERT INTO `rent_goods_package` VALUES (6, 15, '露营桌椅', '一套桌椅包含一张桌子四把椅子', 15.00, 1, '2026-09-01 14:29:35', '2026-09-01 16:06:03', -1);
INSERT INTO `rent_goods_package` VALUES (7, 15, '围炉煮茶', '套餐内包含茶具、以及煮茶设备', 15.00, 2, '2026-09-01 14:29:35', '2026-09-01 16:06:03', -1);
INSERT INTO `rent_goods_package` VALUES (11, 15, '露营桌椅', '一套桌椅包含一张桌子四把椅子', 15.00, 1, '2026-09-01 16:06:03', '2026-09-01 16:06:16', -1);
INSERT INTO `rent_goods_package` VALUES (12, 15, '围炉煮茶', '套餐内包含茶具、以及煮茶设备', 15.00, 2, '2026-09-01 16:06:03', '2026-09-01 16:06:16', -1);
INSERT INTO `rent_goods_package` VALUES (13, 15, '充气床', '充气床可以午休用', 10.00, 3, '2026-09-01 16:06:03', '2026-09-01 16:06:16', -1);
INSERT INTO `rent_goods_package` VALUES (14, 15, '露营桌椅', '一套桌椅包含一张桌子四把椅子', 15.00, 1, '2026-09-01 16:06:17', '2026-09-01 16:10:03', -1);
INSERT INTO `rent_goods_package` VALUES (15, 15, '围炉煮茶', '套餐内包含茶具、以及煮茶设备', 15.00, 2, '2026-09-01 16:06:17', '2026-09-01 16:10:03', -1);
INSERT INTO `rent_goods_package` VALUES (16, 15, '充气床', '充气床可以午休用', 10.00, 3, '2026-09-01 16:06:17', '2026-09-01 16:10:03', -1);
INSERT INTO `rent_goods_package` VALUES (17, 15, '露营桌椅', '一套桌椅包含一张桌子四把椅子', 15.00, 1, '2026-09-01 16:10:03', '2026-09-01 16:47:49', -1);
INSERT INTO `rent_goods_package` VALUES (18, 15, '围炉煮茶', '套餐内包含茶具、以及煮茶设备', 15.00, 2, '2026-09-01 16:10:03', '2026-09-01 16:47:49', -1);
INSERT INTO `rent_goods_package` VALUES (19, 15, '充气床', '充气床可以午休用', 10.00, 3, '2026-09-01 16:10:03', '2026-09-01 16:47:49', -1);
INSERT INTO `rent_goods_package` VALUES (20, 15, '露营桌椅', '一套桌椅包含一张桌子四把椅子', 15.00, 1, '2026-09-01 16:47:49', '2026-09-01 16:47:49', 0);
INSERT INTO `rent_goods_package` VALUES (21, 15, '围炉煮茶', '套餐内包含茶具、以及煮茶设备', 15.00, 2, '2026-09-01 16:47:49', '2026-09-01 16:47:49', 0);
INSERT INTO `rent_goods_package` VALUES (22, 15, '充气床', '充气床可以午休用', 10.00, 3, '2026-09-01 16:47:49', '2026-09-01 16:47:49', 0);
INSERT INTO `rent_goods_package` VALUES (23, 15, '搜索', '少受是', 10.00, 4, '2026-09-01 16:47:49', '2026-09-01 16:47:49', 0);

-- ----------------------------
-- Table structure for rent_goods_spec
-- ----------------------------
DROP TABLE IF EXISTS `rent_goods_spec`;
CREATE TABLE `rent_goods_spec`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '规格ID',
  `goods_id` bigint(0) NOT NULL COMMENT '商品ID',
  `spec_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '规格名称',
  `spec_value` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '规格值',
  `spec_type` tinyint(0) NOT NULL DEFAULT 1 COMMENT '1租赁规格 2商品参数',
  `sort` int(0) NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `deleted` int(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 -1删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rent_goods_spec_goods`(`goods_id`, `sort`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 46 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租赁商品规格' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rent_goods_spec
-- ----------------------------
INSERT INTO `rent_goods_spec` VALUES (1, 1, '容纳人数', '3-4人', 2, 1, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (2, 1, '外帐防水', '3000mm', 2, 2, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (3, 1, '展开尺寸', '210×210×135cm', 2, 3, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (4, 1, '收纳重量', '约4.8kg', 2, 4, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (5, 2, '尺寸', '5.2×4.5m', 2, 1, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (6, 2, '防晒指数', 'UPF50+', 2, 2, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (7, 2, '容纳人数', '8-10人', 2, 3, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (8, 2, '撑杆高度', '2.4m', 2, 4, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (9, 3, '桌面尺寸', '120×60cm', 2, 1, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (10, 3, '桌面高度', '43cm', 2, 2, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (11, 3, '承重', '50kg', 2, 3, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (12, 3, '自重', '约4.2kg', 2, 4, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (13, 4, '坐面材质', '600D牛津布', 2, 1, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (14, 4, '扶手', '天然榉木', 2, 2, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (15, 4, '承重', '120kg', 2, 3, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (16, 4, '自重', '约3.2kg', 2, 4, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (17, 5, '火力', '2900W', 2, 1, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (18, 5, '气源', '丁烷气罐(不含)', 2, 2, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (19, 5, '烤盘尺寸', '33×28cm', 2, 3, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (20, 5, '点火方式', '压电点火', 2, 4, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (21, 6, '亮度', '10-350流明无极调光', 2, 1, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (22, 6, '续航', '最长80小时', 2, 2, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (23, 6, '电池', '5000mAh锂电池', 2, 3, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (24, 6, '充电', 'Type-C快充', 2, 4, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (25, 7, '温标', '舒适-5℃/极限-12℃', 2, 1, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (26, 7, '填充', '90%白鸭绒 800蓬', 2, 2, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (27, 7, '重量', '约1.1kg', 2, 3, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (28, 7, '尺寸', '210×80cm', 2, 4, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (29, 8, '容量', '150L', 2, 1, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (30, 8, '承重', '100kg', 2, 2, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (31, 8, '车轮', '10寸越野坦克轮', 2, 3, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (32, 8, '收纳', '对折收纳，站立存放', 2, 4, '2026-08-03 22:32:42', '2026-09-01 17:15:36', 0);
INSERT INTO `rent_goods_spec` VALUES (33, 9, '容纳人数', '2-3人', 2, 1, '2026-08-03 23:53:26', '2026-09-01 17:15:36', -1);
INSERT INTO `rent_goods_spec` VALUES (34, 15, '2人款', '120', 1, 1, '2026-09-01 14:29:35', '2026-09-01 16:06:03', -1);
INSERT INTO `rent_goods_spec` VALUES (35, 15, '3人款', '130', 1, 2, '2026-09-01 14:29:35', '2026-09-01 16:06:03', -1);
INSERT INTO `rent_goods_spec` VALUES (38, 15, '2人款', '120', 1, 1, '2026-09-01 16:06:03', '2026-09-01 16:06:16', -1);
INSERT INTO `rent_goods_spec` VALUES (39, 15, '3人款', '130', 1, 2, '2026-09-01 16:06:03', '2026-09-01 16:06:16', -1);
INSERT INTO `rent_goods_spec` VALUES (40, 15, '2人款', '120', 1, 1, '2026-09-01 16:06:17', '2026-09-01 16:10:03', -1);
INSERT INTO `rent_goods_spec` VALUES (41, 15, '3人款', '130', 1, 2, '2026-09-01 16:06:17', '2026-09-01 16:10:03', -1);
INSERT INTO `rent_goods_spec` VALUES (42, 15, '2人款', '120', 1, 1, '2026-09-01 16:10:03', '2026-09-01 16:47:49', -1);
INSERT INTO `rent_goods_spec` VALUES (43, 15, '3人款', '130', 1, 2, '2026-09-01 16:10:03', '2026-09-01 16:47:49', -1);
INSERT INTO `rent_goods_spec` VALUES (44, 15, '2人款', '120', 1, 1, '2026-09-01 16:47:49', '2026-09-01 16:47:49', 0);
INSERT INTO `rent_goods_spec` VALUES (45, 15, '3人款', '130', 1, 2, '2026-09-01 16:47:49', '2026-09-01 16:47:49', 0);

-- ----------------------------
-- Table structure for rent_inspection
-- ----------------------------
DROP TABLE IF EXISTS `rent_inspection`;
CREATE TABLE `rent_inspection`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '验收记录ID',
  `order_id` bigint(0) NOT NULL COMMENT '订单ID',
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单号',
  `merchant_id` bigint(0) NULL DEFAULT NULL COMMENT '商户ID',
  `inspector_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'merchant' COMMENT '验收人类型：merchant/admin',
  `inspector_id` bigint(0) NULL DEFAULT NULL COMMENT '验收人ID',
  `device_condition` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'good' COMMENT '设备状况：good/minor_damage/major_damage',
  `deduct_amount` decimal(10, 2) NOT NULL COMMENT '扣款金额',
  `refund_amount` decimal(10, 2) NOT NULL COMMENT '退还押金金额',
  `image_urls` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '验收图片（逗号分隔存储路径）',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '验收备注',
  `audit_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'pending' COMMENT '复核状态：pending/approved/rejected',
  `audit_remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '复核备注',
  `audit_time` datetime(0) NULL DEFAULT NULL COMMENT '复核时间',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `deleted` int(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 -1删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rent_inspection_order`(`order_id`) USING BTREE,
  INDEX `idx_rent_inspection_audit`(`audit_status`, `create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租赁验收记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rent_inspection
-- ----------------------------
INSERT INTO `rent_inspection` VALUES (1, 28, '202608040238501434', NULL, 'merchant', 1, 'good', 0.00, 200.00, NULL, NULL, 'approved', NULL, '2026-08-04 02:38:51', '2026-08-04 02:38:51', '2026-08-04 02:38:50', 0);
INSERT INTO `rent_inspection` VALUES (2, 29, '202608040239175096', NULL, 'merchant', 1, 'good', 0.00, 200.00, NULL, NULL, 'approved', NULL, '2026-08-04 02:39:18', '2026-08-04 02:39:18', '2026-08-04 02:39:17', 0);

-- ----------------------------
-- Table structure for rent_order
-- ----------------------------
DROP TABLE IF EXISTS `rent_order`;
CREATE TABLE `rent_order`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单号',
  `user_id` bigint(0) NOT NULL COMMENT '用户ID',
  `merchant_id` bigint(0) NULL DEFAULT NULL COMMENT '商户ID',
  `store_id` bigint(0) NULL DEFAULT NULL COMMENT '门店ID',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'pending_payment' COMMENT '订单状态',
  `rent_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'day' COMMENT '租赁类型：day/hour/overnight',
  `start_time` datetime(0) NULL DEFAULT NULL COMMENT '租期开始',
  `end_time` datetime(0) NULL DEFAULT NULL COMMENT '租期结束',
  `days` int(0) NULL DEFAULT NULL COMMENT '租赁天数',
  `pickup_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'store' COMMENT '取货方式：store/pickup_point/delivery',
  `pickup_store_id` bigint(0) NULL DEFAULT NULL COMMENT '自提门店ID',
  `address_id` bigint(0) NULL DEFAULT NULL COMMENT '配送地址ID',
  `contact_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `rent_amount` decimal(10, 2) NOT NULL COMMENT '租金合计',
  `deposit_amount` decimal(10, 2) NOT NULL COMMENT '押金合计',
  `delivery_amount` decimal(10, 2) NOT NULL COMMENT '配送费',
  `coupon_amount` decimal(10, 2) NOT NULL COMMENT '优惠金额',
  `coupon_id` bigint(0) NULL DEFAULT NULL COMMENT '使用的用户优惠券ID',
  `pay_amount` decimal(10, 2) NOT NULL COMMENT '应付总额',
  `pickup_code` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '取货码',
  `pay_time` datetime(0) NULL DEFAULT NULL COMMENT '支付时间',
  `pickup_time` datetime(0) NULL DEFAULT NULL COMMENT '取货时间',
  `return_time` datetime(0) NULL DEFAULT NULL COMMENT '归还时间',
  `complete_time` datetime(0) NULL DEFAULT NULL COMMENT '完成时间',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `deleted` int(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 -1删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_rent_order_no`(`order_no`) USING BTREE,
  INDEX `idx_rent_order_user`(`user_id`, `status`, `create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 55 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租赁订单' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rent_order
-- ----------------------------
INSERT INTO `rent_order` VALUES (1, 'RO_M01_001', 1, NULL, NULL, 'pending_payment', 'day', NULL, NULL, NULL, 'store', NULL, NULL, NULL, NULL, NULL, 0.00, 0.00, 0.00, 0.00, NULL, 29.90, NULL, NULL, NULL, NULL, NULL, '2026-08-03 23:19:29', '2026-08-03 23:19:29', 0);
INSERT INTO `rent_order` VALUES (2, 'RO_M01_002', 1, NULL, NULL, 'waiting_pickup', 'day', NULL, NULL, NULL, 'store', NULL, NULL, NULL, NULL, NULL, 0.00, 0.00, 0.00, 0.00, NULL, 19.90, NULL, NULL, NULL, NULL, NULL, '2026-08-03 23:19:29', '2026-08-03 23:19:29', 0);

-- ----------------------------
-- Table structure for rent_order_item
-- ----------------------------
DROP TABLE IF EXISTS `rent_order_item`;
CREATE TABLE `rent_order_item`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `order_id` bigint(0) NOT NULL COMMENT '订单ID',
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单号',
  `goods_id` bigint(0) NOT NULL COMMENT '商品ID',
  `goods_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '商品名称快照',
  `spec_info` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '规格信息快照',
  `package_id` bigint(0) NULL DEFAULT NULL COMMENT '配件套餐ID快照',
  `package_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '配件套餐名称快照',
  `package_desc` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '配件套餐描述快照',
  `cover_image` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '封面图快照',
  `rent_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'day' COMMENT '租赁类型',
  `start_time` datetime(0) NULL DEFAULT NULL COMMENT '租期开始',
  `end_time` datetime(0) NULL DEFAULT NULL COMMENT '租期结束',
  `days` int(0) NULL DEFAULT NULL COMMENT '租赁天数',
  `qty` int(0) NOT NULL DEFAULT 1 COMMENT '数量',
  `rent_price` decimal(10, 2) NOT NULL COMMENT '租金单价',
  `deposit_amount` decimal(10, 2) NOT NULL COMMENT '押金',
  `item_amount` decimal(10, 2) NOT NULL COMMENT '明细金额',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `deleted` int(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 -1删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rent_order_item_order`(`order_id`) USING BTREE,
  INDEX `idx_rent_order_item_goods`(`goods_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 49 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租赁订单明细' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rent_order_item
-- ----------------------------
INSERT INTO `rent_order_item` VALUES (2, 8, '202608040034544707', 1, '双峰自动速开帐篷 3-4人', NULL, NULL, NULL, NULL, 'https://picsum.photos/seed/camp-tent/500/360', 'day', '2026-09-15 10:00:00', '2026-09-17 18:00:00', 2, 4, 29.90, 200.00, 1039.20, '2026-08-04 00:34:54', '2026-08-04 00:34:54', 0);
INSERT INTO `rent_order_item` VALUES (3, 9, '202608040034553636', 1, '双峰自动速开帐篷 3-4人', NULL, NULL, NULL, NULL, 'https://picsum.photos/seed/camp-tent/500/360', 'day', '2026-09-15 10:00:00', '2026-09-17 18:00:00', 2, 1, 29.90, 200.00, 259.80, '2026-08-04 00:34:55', '2026-08-04 00:34:55', 0);

-- ----------------------------
-- Table structure for rent_order_log
-- ----------------------------
DROP TABLE IF EXISTS `rent_order_log`;
CREATE TABLE `rent_order_log`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `order_id` bigint(0) NOT NULL COMMENT '订单ID',
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单号',
  `from_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '变更前状态',
  `to_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '变更后状态',
  `operator_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人类型：user/admin/merchant/system',
  `operator_id` bigint(0) NULL DEFAULT NULL COMMENT '操作人ID',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rent_order_log_order`(`order_id`, `create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 115 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租赁订单状态日志' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rent_order_log
-- ----------------------------
INSERT INTO `rent_order_log` VALUES (1, 8, '202608040034544707', NULL, 'pending_payment', 'user', 1, '创建订单', '2026-08-04 00:34:54');
INSERT INTO `rent_order_log` VALUES (2, 8, '202608040034544707', 'pending_payment', 'waiting_pickup', 'system', NULL, 'E2E 模拟支付成功', '2026-08-04 00:34:54');

-- ----------------------------
-- Table structure for rent_order_payment
-- ----------------------------
DROP TABLE IF EXISTS `rent_order_payment`;
CREATE TABLE `rent_order_payment`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '支付记录ID',
  `order_id` bigint(0) NOT NULL COMMENT '订单ID',
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单号',
  `pay_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支付流水号',
  `pay_biz_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '支付业务类型',
  `amount` decimal(10, 2) NOT NULL COMMENT '支付金额',
  `pay_channel` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支付渠道',
  `pay_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'pending' COMMENT '支付状态：pending/success/fail',
  `pay_time` datetime(0) NULL DEFAULT NULL COMMENT '支付时间',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `deleted` int(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 -1删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rent_order_payment_order`(`order_id`) USING BTREE,
  INDEX `idx_rent_order_payment_no`(`pay_no`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 28 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租赁订单支付记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rent_order_payment
-- ----------------------------
INSERT INTO `rent_order_payment` VALUES (1, 8, '202608040034544707', 'E2E-MOCK-1785774894532', 'rent_order', 1039.20, 'e2e', 'success', '2026-08-04 00:34:55', '2026-08-04 00:34:54', '2026-08-04 00:34:54', 0);
INSERT INTO `rent_order_payment` VALUES (2, 19, '202608040107067206', 'E2E-MOCK-1785776826129', 'rent_order', 259.80, 'e2e', 'success', '2026-08-04 01:07:06', '2026-08-04 01:07:06', '2026-08-04 01:07:06', 0);

-- ----------------------------
-- Table structure for rent_pickup_code
-- ----------------------------
DROP TABLE IF EXISTS `rent_pickup_code`;
CREATE TABLE `rent_pickup_code`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '取货码ID',
  `order_id` bigint(0) NOT NULL COMMENT '订单ID',
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单号',
  `pickup_code` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '取货码',
  `expire_time` datetime(0) NULL DEFAULT NULL COMMENT '过期时间',
  `used` tinyint(0) NOT NULL DEFAULT 0 COMMENT '是否已使用：1是 0否',
  `use_time` datetime(0) NULL DEFAULT NULL COMMENT '使用时间',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `deleted` int(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 -1删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_rent_pickup_code`(`pickup_code`) USING BTREE,
  INDEX `idx_rent_pickup_order`(`order_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租赁取货码' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rent_pickup_code
-- ----------------------------
INSERT INTO `rent_pickup_code` VALUES (1, 8, '202608040034544707', '184697', '2026-09-17 18:00:00', 0, NULL, '2026-08-04 00:34:54', '2026-08-04 00:34:54', 0);
INSERT INTO `rent_pickup_code` VALUES (2, 19, '202608040107067206', '938977', '2026-09-17 18:00:00', 0, NULL, '2026-08-04 01:07:06', '2026-08-04 01:07:06', 0);

-- ----------------------------
-- Table structure for rent_renew_record
-- ----------------------------
DROP TABLE IF EXISTS `rent_renew_record`;
CREATE TABLE `rent_renew_record`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '续租记录ID',
  `renew_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '续租单号',
  `order_id` bigint(0) NOT NULL COMMENT '原订单ID',
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '原订单号',
  `user_id` bigint(0) NOT NULL COMMENT '用户ID',
  `extra_days` int(0) NULL DEFAULT NULL COMMENT '续租天数',
  `old_end_time` datetime(0) NULL DEFAULT NULL COMMENT '续租前结束时间',
  `new_end_time` datetime(0) NULL DEFAULT NULL COMMENT '续租后结束时间',
  `renew_amount` decimal(10, 2) NOT NULL COMMENT '续租租金',
  `pay_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'pending' COMMENT '支付状态：pending/success/fail',
  `pay_time` datetime(0) NULL DEFAULT NULL COMMENT '支付时间',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `deleted` int(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 -1删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_rent_renew_no`(`renew_no`) USING BTREE,
  INDEX `idx_rent_renew_order`(`order_id`, `create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租赁续租记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rent_renew_record
-- ----------------------------
INSERT INTO `rent_renew_record` VALUES (1, 'RN202608040238501324', 28, '202608040238501434', 1, 1, '2026-09-17 18:00:00', '2026-09-18 18:00:00', 29.90, 'success', '2026-08-04 02:38:50', '2026-08-04 02:38:51', '2026-08-04 02:38:50', 0);
INSERT INTO `rent_renew_record` VALUES (2, 'RN202608040239171711', 29, '202608040239175096', 1, 1, '2026-09-17 18:00:00', '2026-09-18 18:00:00', 29.90, 'success', '2026-08-04 02:39:17', '2026-08-04 02:39:18', '2026-08-04 02:39:17', 0);
INSERT INTO `rent_renew_record` VALUES (3, 'RN202608040239514129', 30, '202608040239502330', 1, 1, '2026-09-17 18:00:00', '2026-09-18 18:00:00', 29.90, 'success', '2026-08-04 02:39:51', '2026-08-04 02:39:51', '2026-08-04 02:39:51', 0);
INSERT INTO `rent_renew_record` VALUES (4, 'RN202608040240132800', 31, '202608040240133492', 1, 1, '2026-09-17 18:00:00', '2026-09-18 18:00:00', 29.90, 'success', '2026-08-04 02:40:14', '2026-08-04 02:40:14', '2026-08-04 02:40:13', 0);
INSERT INTO `rent_renew_record` VALUES (5, 'RN202608040240428822', 32, '202608040240416012', 1, 1, '2026-09-17 18:00:00', '2026-09-18 18:00:00', 29.90, 'success', '2026-08-04 02:40:42', '2026-08-04 02:40:42', '2026-08-04 02:40:42', 0);
INSERT INTO `rent_renew_record` VALUES (7, 'RN202609031421157031', 54, '202609031404058909', 7, 1, '2026-09-04 15:00:00', '2026-09-04 16:00:00', 12.00, 'success', '2026-09-03 14:21:15', '2026-09-03 14:21:15', '2026-09-03 14:21:15', 0);

-- ----------------------------
-- Table structure for rent_return_apply
-- ----------------------------
DROP TABLE IF EXISTS `rent_return_apply`;
CREATE TABLE `rent_return_apply`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '归还申请ID',
  `order_id` bigint(0) NOT NULL COMMENT '订单ID',
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单号',
  `user_id` bigint(0) NOT NULL COMMENT '用户ID',
  `return_method` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'store' COMMENT '归还方式：store/pickup_point/delivery',
  `return_date` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '计划归还日期描述',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '归还备注',
  `image_urls` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '归还图片URL，逗号分隔',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'pending' COMMENT '状态：pending/completed',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `deleted` int(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 -1删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rent_return_order`(`order_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租赁归还申请' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rent_return_apply
-- ----------------------------
INSERT INTO `rent_return_apply` VALUES (1, 28, '202608040238501434', 1, 'store', '2026-09-18', NULL, NULL, 'completed', '2026-08-04 02:38:51', '2026-08-04 02:38:50', 0);
INSERT INTO `rent_return_apply` VALUES (2, 29, '202608040239175096', 1, 'store', '2026-09-18', NULL, NULL, 'completed', '2026-08-04 02:39:18', '2026-08-04 02:39:17', 0);

-- ----------------------------
-- Table structure for rent_stock
-- ----------------------------
DROP TABLE IF EXISTS `rent_stock`;
CREATE TABLE `rent_stock`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '库存ID',
  `goods_id` bigint(0) NOT NULL COMMENT '商品ID',
  `spec_id` bigint(0) NOT NULL DEFAULT 0 COMMENT '规格ID，0表示默认规格',
  `total_qty` int(0) NOT NULL DEFAULT 0 COMMENT '总库存',
  `available_qty` int(0) NOT NULL DEFAULT 0 COMMENT '可用库存',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `deleted` int(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 -1删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_rent_stock_goods_spec`(`goods_id`, `spec_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租赁商品库存' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rent_stock
-- ----------------------------
INSERT INTO `rent_stock` VALUES (1, 1, 0, 12, 12, '2026-08-03 22:32:42', '2026-08-03 22:32:42', 0);
INSERT INTO `rent_stock` VALUES (2, 2, 0, 20, 20, '2026-08-03 22:32:42', '2026-08-03 22:32:42', 0);

-- ----------------------------
-- Table structure for rent_stock_occupy
-- ----------------------------
DROP TABLE IF EXISTS `rent_stock_occupy`;
CREATE TABLE `rent_stock_occupy`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '占用ID',
  `goods_id` bigint(0) NOT NULL COMMENT '商品ID',
  `spec_id` bigint(0) NOT NULL DEFAULT 0 COMMENT '规格ID',
  `order_id` bigint(0) NULL DEFAULT NULL COMMENT '订单ID',
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '订单号',
  `start_time` datetime(0) NOT NULL COMMENT '占用开始时间',
  `end_time` datetime(0) NOT NULL COMMENT '占用结束时间',
  `qty` int(0) NOT NULL DEFAULT 1 COMMENT '占用数量',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PRELOCK' COMMENT '状态：PRELOCK/OCCUPIED/RELEASED',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `deleted` int(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 -1删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_rent_stock_occupy_goods_time`(`goods_id`, `spec_id`, `start_time`, `end_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 45 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '租赁时段库存占用' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rent_stock_occupy
-- ----------------------------
INSERT INTO `rent_stock_occupy` VALUES (1, 1, 0, NULL, 'ORD-M02-DEMO-001', '2026-08-10 10:00:00', '2026-08-12 18:00:00', 2, 'OCCUPIED', '2026-08-03 23:52:13', '2026-08-03 23:52:13', 0);
INSERT INTO `rent_stock_occupy` VALUES (2, 1, 0, 8, '202608040034544707', '2026-09-15 10:00:00', '2026-09-17 18:00:00', 4, 'OCCUPIED', '2026-08-04 00:34:54', '2026-08-04 00:34:54', 0);

-- ----------------------------
-- Table structure for search_hot_keyword
-- ----------------------------
DROP TABLE IF EXISTS `search_hot_keyword`;
CREATE TABLE `search_hot_keyword`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `keyword` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '搜索关键词',
  `sort` int(0) NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint(0) NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `deleted` int(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 -1删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_search_hot_keyword_sort`(`status`, `sort`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '热门搜索词' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of search_hot_keyword
-- ----------------------------
INSERT INTO `search_hot_keyword` VALUES (1, '帐篷', 1, 1, '2026-08-03 22:32:42', '2026-08-03 22:32:42', 0);
INSERT INTO `search_hot_keyword` VALUES (2, '天幕', 2, 1, '2026-08-03 22:32:42', '2026-08-03 22:32:42', 0);
INSERT INTO `search_hot_keyword` VALUES (3, '蛋卷桌', 3, 1, '2026-08-03 22:32:42', '2026-08-03 22:32:42', 0);
INSERT INTO `search_hot_keyword` VALUES (4, '睡袋', 4, 1, '2026-08-03 22:32:42', '2026-08-03 22:32:42', 0);
INSERT INTO `search_hot_keyword` VALUES (5, '营地车', 5, 1, '2026-08-03 22:32:42', '2026-08-03 22:32:42', 0);

-- ----------------------------
-- Table structure for sms_template
-- ----------------------------
DROP TABLE IF EXISTS `sms_template`;
CREATE TABLE `sms_template`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `biz_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `template_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sms_template_biz_code`(`biz_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '短信模板' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sms_template
-- ----------------------------
INSERT INTO `sms_template` VALUES (1, 'register', NULL, '【%s】您的验证码是%s，10分钟内有效', '2026-08-03 22:32:28', '2026-08-03 22:32:28');
INSERT INTO `sms_template` VALUES (2, 'login', NULL, '【%s】您的验证码是%s，10分钟内有效', '2026-08-03 22:32:28', '2026-08-03 22:32:28');
INSERT INTO `sms_template` VALUES (3, 'forget', NULL, '【%s】您的验证码是%s，10分钟内有效', '2026-08-03 22:32:28', '2026-08-03 22:32:28');

-- ----------------------------
-- Table structure for sys_captcha
-- ----------------------------
DROP TABLE IF EXISTS `sys_captcha`;
CREATE TABLE `sys_captcha`  (
  `uuid` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `code` varchar(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `expire_time` datetime(0) NULL DEFAULT NULL,
  PRIMARY KEY (`uuid`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '验证码' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_captcha
-- ----------------------------
INSERT INTO `sys_captcha` VALUES ('25eb07e1-9d3f-4e97-8dec-76db2238b1ac', 'm8aen', '2026-09-02 20:29:29');
INSERT INTO `sys_captcha` VALUES ('5a90409a-25c1-4fef-8af7-74b8275838d0', '6aabc', '2026-08-31 15:40:30');
INSERT INTO `sys_captcha` VALUES ('6193a0cc-fd9b-4908-a557-b439b8e835f8', '7wy7c', '2026-09-02 20:28:29');
INSERT INTO `sys_captcha` VALUES ('6396fda3-b8b3-43b7-9326-9498c051735f', 'wxd8f', '2026-08-27 10:04:33');

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `param_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `param_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `status` tinyint(0) NULL DEFAULT 1,
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `param_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `param_type_desc` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sys_config_param_key`(`param_key`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 75 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统配置' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_config
-- ----------------------------
INSERT INTO `sys_config` VALUES (14, 'sys_user_ma_appid', '', 1, '微信小程序AppId', 'sys', '微信');
INSERT INTO `sys_config` VALUES (15, 'sys_user_ma_secret', '', 1, '微信小程序Secret', 'sys', '微信');
INSERT INTO `sys_config` VALUES (16, 'sys_user_mp_appid', '', 1, '微信公众号AppId', 'sys', '微信');
INSERT INTO `sys_config` VALUES (17, 'sys_user_mp_secret', '', 1, '微信公众号Secret', 'pay', '微信支付');
INSERT INTO `sys_config` VALUES (18, 'file_storage_type', '', 1, '文件存储类型：local=本地，ali=阿里云 OSS', 'sys', '文件');
INSERT INTO `sys_config` VALUES (19, 'file_local_upload_path', '', 1, '本地上传目录（file_storage_type=local 时使用）', 'sys', '文件');
INSERT INTO `sys_config` VALUES (20, 'file_endpoint', '', 1, '阿里云 OSS Endpoint', 'sys', '文件');
INSERT INTO `sys_config` VALUES (21, 'file_bucket', '', 1, '阿里云 OSS Bucket', 'sys', '文件');
INSERT INTO `sys_config` VALUES (22, 'file_ali_oss_accessKeyId', '', 1, '阿里云 OSS AccessKeyId', 'sys', '文件');
INSERT INTO `sys_config` VALUES (23, 'file_ali_oss_secretAccessKey', '', 1, '阿里云 OSS SecretAccessKey', 'sys', '文件');
INSERT INTO `sys_config` VALUES (24, 'sys_server_name', '', 1, NULL, 'sys', '系统');
INSERT INTO `sys_config` VALUES (25, 'sms_provider', '', 1, '短信平台：1=腾讯云，2=阿里云，3=短信宝', 'sys', '短信');
INSERT INTO `sys_config` VALUES (26, 'sys_h5_domain', '', 1, '用户端 H5 域名，支付完成后浏览器回跳地址', 'pay', '支付回调');
INSERT INTO `sys_config` VALUES (27, 'file_cdn_domain', '', 1, 'OSS 静态 CDN 域名（file_storage_type=ali 时使用）', 'sys', '文件');
INSERT INTO `sys_config` VALUES (28, 'file_public_base_url', '', 1, 'API 公网前缀（含 /sqx_fast），支付宝/微信异步通知：{前缀}/app/base/pay/notify/ali|wx', 'pay', '支付回调');
INSERT INTO `sys_config` VALUES (29, 'pay_wechat_mch_id', '', 1, '微信支付商户号（本地联调占位）', 'pay', '微信支付');
INSERT INTO `sys_config` VALUES (30, 'pay_wechat_mch_key', '', 1, '微信支付商户密钥（本地联调占位）', 'pay', '微信支付');
INSERT INTO `sys_config` VALUES (31, 'sys_wx_key_path', '', 1, '微信支付证书路径（本地联调占位）', 'pay', '微信支付');
INSERT INTO `sys_config` VALUES (32, 'sys_wechat_app_appid', '', 1, '微信 App AppId（支付模块）', 'pay', '微信支付');
INSERT INTO `sys_config` VALUES (33, 'sys_wechat_h5_appid', '', 1, '微信 H5/公众号 AppId（支付模块）', 'pay', '微信支付');
INSERT INTO `sys_config` VALUES (34, 'sys_wechat_jsapi_appid', '', 1, '微信 JSAPI/小程序 AppId（支付模块）', 'pay', '微信支付');
INSERT INTO `sys_config` VALUES (35, 'sys_withdraw_max', '', 1, 'M06 最大提现金额（元）', 'withdraw', '提现配置');
INSERT INTO `sys_config` VALUES (36, 'sys_withdraw_min', '', 1, 'M06 最小提现金额（元）', 'withdraw', '提现配置');
INSERT INTO `sys_config` VALUES (37, 'sys_withdraw_rate', '', 1, 'M06 提现手续费率', 'withdraw', '提现配置');
INSERT INTO `sys_config` VALUES (39, 'biz_referral_commission_rate', '', 1, '邀请分销佣金比例（订单租金基数，如 0.05=5%）', 'referral', '分销配置');
INSERT INTO `sys_config` VALUES (40, 'rental.delivery.base_fee', '', 1, NULL, 'rental', '租赁业务');
INSERT INTO `sys_config` VALUES (41, 'app_rental_agreement_html', '', 1, '租赁协议', 'app', '用户端协议');
INSERT INTO `sys_config` VALUES (42, 'app_deposit_agreement_html', '', 1, '押金规则', 'app', '用户端协议');
INSERT INTO `sys_config` VALUES (43, 'app_damage_agreement_html', '', 1, '损坏赔付说明', 'app', '用户端协议');
INSERT INTO `sys_config` VALUES (44, 'app_user_agreement_html', '', 1, '用户服务协议', 'app', '用户端协议');
INSERT INTO `sys_config` VALUES (45, 'app_privacy_policy_html', '', 1, '隐私政策', 'app', '用户端协议');
INSERT INTO `sys_config` VALUES (46, 'app_service_channels_json', '', 1, '用户端客服中心：联系渠道（四选一）', 'app', '用户端配置');
INSERT INTO `sys_config` VALUES (47, 'app_help_faq_json', '', 1, NULL, 'app', '用户端配置');
INSERT INTO `sys_config` VALUES (48, 'map_platform', '', 1, '地图平台：tencent/amap/tianditu/google', 'map', '地图配置');
INSERT INTO `sys_config` VALUES (49, 'map_tencent_key', '', 1, '腾讯地图 WebService Key', 'map', '地图配置');
INSERT INTO `sys_config` VALUES (50, 'sms_smsbao_username', '', 1, '短信宝用户名', 'sms', '短信配置');
INSERT INTO `sys_config` VALUES (51, 'sms_smsbao_password', '', 1, '短信宝密码', 'sms', '短信配置');
INSERT INTO `sys_config` VALUES (52, 'sys_oa_oauth_enabled', '', 1, '公众号授权登录开关 1=开 0=关', 'sys', '微信');
INSERT INTO `sys_config` VALUES (53, 'app_invite_poster_json', '', 1, '用户端邀请有礼宣传海报文案与配色', 'app', '用户端配置');
INSERT INTO `sys_config` VALUES (54, 'merchant_security_deposit_amount', '', 1, '商户入驻应缴保证金金额（元），0 表示无需缴纳', 'merchant', '商户配置');
INSERT INTO `sys_config` VALUES (55, 'pay_ali_appid', '', 1, '支付宝 AppId（开放平台应用）', 'pay', '支付宝');
INSERT INTO `sys_config` VALUES (56, 'pay_ali_private_key', '', 1, '支付宝应用私钥（RSA2，一行字符串，不含头尾）', 'pay', '支付宝');
INSERT INTO `sys_config` VALUES (57, 'pay_ali_public_key', '', 1, '支付宝公钥（公钥模式 pay_ali_mode=1 时必填）', 'pay', '支付宝');
INSERT INTO `sys_config` VALUES (58, 'pay_ali_mode', '', 1, '支付宝支付模式：1=公钥模式，2=证书模式', 'pay', '支付宝');
INSERT INTO `sys_config` VALUES (59, 'pay_ali_sandbox', '', 1, '支付宝沙箱：1=沙箱环境，2=正式环境', 'pay', '支付宝');
INSERT INTO `sys_config` VALUES (60, 'pay_ali_app_cert_path', '', 1, '应用公钥证书路径（证书模式 pay_ali_mode=2）', 'pay', '支付宝');
INSERT INTO `sys_config` VALUES (61, 'pay_ali_alipay_public_cert_path', '', 1, '支付宝公钥证书路径（证书模式）', 'pay', '支付宝');
INSERT INTO `sys_config` VALUES (62, 'pay_ali_root_cert_path', '', 1, '支付宝根证书路径（证书模式）', 'pay', '支付宝');
INSERT INTO `sys_config` VALUES (63, 'pay_wx_mch_id', '', 1, '微信支付商户号（与 pay_wechat_mch_id 保持一致）', 'pay', '微信支付');
INSERT INTO `sys_config` VALUES (64, 'pay_wx_mch_key', '', 1, '微信支付商户密钥（与 pay_wechat_mch_key 保持一致）', 'pay', '微信支付');
INSERT INTO `sys_config` VALUES (65, 'pay_wx_key_path', '', 1, '微信商户 p12 证书路径（与 sys_wx_key_path 保持一致）', 'pay', '微信支付');
INSERT INTO `sys_config` VALUES (66, 'pay_wx_app_id', '', 1, '微信 App 支付 AppId', 'pay', '微信支付');
INSERT INTO `sys_config` VALUES (67, 'pay_wx_h5_app_id', '', 1, '微信 H5/公众号支付 AppId', 'pay', '微信支付');
INSERT INTO `sys_config` VALUES (68, 'pay_wx_jsapi_app_id', '', 1, '微信小程序/JSAPI 支付 AppId', 'pay', '微信支付');
INSERT INTO `sys_config` VALUES (69, 'sys_wechat_jsapi_secret', '', 1, '微信小程序 Secret（登录与 JSAPI 授权）', 'pay', '微信支付');
INSERT INTO `sys_config` VALUES (70, 'sys_domain', '', 1, 'API/业务域名', 'sys', '域名');
INSERT INTO `sys_config` VALUES (71, 'pay_wechat_app_appid', '', 1, '微信 App AppId（支付模块）', 'pay', '微信支付');
INSERT INTO `sys_config` VALUES (72, 'pay_wechat_h5_appid', '', 1, '微信 H5/公众号 AppId（支付模块）', 'pay', '微信支付');
INSERT INTO `sys_config` VALUES (73, 'pay_wechat_jsapi_appid', '', 1, '微信小程序 AppId（支付模块）', 'pay', '微信支付');
INSERT INTO `sys_config` VALUES (74, 'user_min_deposit_balance', '', 1, '用户押金账户需保留的最低可用余额（元），0 表示不限制', 'rental', '租赁配置');

-- ----------------------------
-- Table structure for sys_dict
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict`;
CREATE TABLE `sys_dict`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `value` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `order_num` int(0) NULL DEFAULT 0,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `del_flag` tinyint(0) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '数据字典' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dict
-- ----------------------------

-- ----------------------------
-- Table structure for sys_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_log`;
CREATE TABLE `sys_log`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `operation` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `method` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `time` bigint(0) NULL DEFAULT NULL,
  `ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_date` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 38 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '操作日志' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_log
-- ----------------------------
INSERT INTO `sys_log` VALUES (1, 'admin', '刷新缓存', 'com.sqx.system.controller.SysConfigController.refresh()', '[]', 39, '127.0.0.1', '2026-08-31 09:48:11');

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `menu_id` bigint(0) NOT NULL AUTO_INCREMENT,
  `parent_id` bigint(0) NULL DEFAULT 0,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `url` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `perms` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `type` int(0) NULL DEFAULT 0 COMMENT '0目录 1菜单 2按钮',
  `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `order_num` int(0) NULL DEFAULT 0,
  PRIMARY KEY (`menu_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 452 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '菜单' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (100, 0, '工作台', NULL, NULL, 0, 'DataBoard', 1);
INSERT INTO `sys_menu` VALUES (101, 100, '数据统计', '/dashboard', NULL, 1, 'DataBoard', 1);
INSERT INTO `sys_menu` VALUES (200, 0, '订单履约', NULL, NULL, 0, 'DataBoard', 2);
INSERT INTO `sys_menu` VALUES (201, 200, '租赁订单', '/rental/orders', 'rental:order:list', 1, 'DataBoard', 1);
INSERT INTO `sys_menu` VALUES (202, 200, '验收记录', '/rental/inspections', 'rental:inspection:list', 1, 'DataBoard', 2);
INSERT INTO `sys_menu` VALUES (203, 200, '库存管理', '/rental/stock', 'rental:stock:list', 1, 'Goods', 3);
INSERT INTO `sys_menu` VALUES (300, 0, '商品与定价', NULL, NULL, 0, 'Goods', 3);
INSERT INTO `sys_menu` VALUES (301, 300, '租赁商品', '/rental/goods', 'rental:goods:list', 1, 'Goods', 1);
INSERT INTO `sys_menu` VALUES (302, 300, '租赁分类', '/rental/categories', 'rental:category:list', 1, 'Goods', 2);
INSERT INTO `sys_menu` VALUES (303, 300, '定价配置', '/rental/pricing', 'rental:pricing:list', 1, 'Setting', 3);
INSERT INTO `sys_menu` VALUES (400, 0, '系统管理', NULL, NULL, 0, 'Setting', 9);
INSERT INTO `sys_menu` VALUES (401, 400, '管理员', '/system/admins', 'sys:user:list', 1, 'Setting', 1);
INSERT INTO `sys_menu` VALUES (402, 400, '角色管理', '/system/roles', 'sys:role:list', 1, 'Setting', 2);
INSERT INTO `sys_menu` VALUES (403, 400, '菜单管理', '/system/menus', 'sys:menu:list', 1, 'Setting', 3);
INSERT INTO `sys_menu` VALUES (404, 400, '字典管理', '/system/dicts', 'sys:dict:list', 1, 'Setting', 4);
INSERT INTO `sys_menu` VALUES (405, 400, '系统配置', '/system/configs', 'sys:config:list', 1, 'Setting', 5);
INSERT INTO `sys_menu` VALUES (406, 400, '操作日志', '/system/logs', 'sys:log:list', 1, 'Setting', 6);
INSERT INTO `sys_menu` VALUES (411, 401, '查看', NULL, 'sys:user:list', 2, NULL, 0);
INSERT INTO `sys_menu` VALUES (412, 401, '新增', NULL, 'sys:user:save', 2, NULL, 1);
INSERT INTO `sys_menu` VALUES (413, 401, '修改', NULL, 'sys:user:update', 2, NULL, 2);
INSERT INTO `sys_menu` VALUES (414, 401, '删除', NULL, 'sys:user:delete', 2, NULL, 3);
INSERT INTO `sys_menu` VALUES (421, 402, '查看', NULL, 'sys:role:list', 2, NULL, 0);
INSERT INTO `sys_menu` VALUES (431, 403, '查看', NULL, 'sys:menu:list', 2, NULL, 0);
INSERT INTO `sys_menu` VALUES (441, 404, '查看', NULL, 'sys:dict:list', 2, NULL, 0);
INSERT INTO `sys_menu` VALUES (451, 406, '查看', NULL, 'sys:log:list', 2, NULL, 0);

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `role_id` bigint(0) NOT NULL AUTO_INCREMENT,
  `role_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `remark` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_user_id` bigint(0) NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`role_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, '超级管理员', '露营租赁平台超级管理员', 1, '2026-08-27 10:09:24');

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `role_id` bigint(0) NOT NULL,
  `menu_id` bigint(0) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sys_role_menu_role_id`(`role_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 40 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色菜单' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (1, 1, 100);
INSERT INTO `sys_role_menu` VALUES (2, 1, 101);
INSERT INTO `sys_role_menu` VALUES (3, 1, 200);
INSERT INTO `sys_role_menu` VALUES (4, 1, 201);
INSERT INTO `sys_role_menu` VALUES (5, 1, 202);
INSERT INTO `sys_role_menu` VALUES (6, 1, 203);
INSERT INTO `sys_role_menu` VALUES (7, 1, 300);
INSERT INTO `sys_role_menu` VALUES (8, 1, 301);
INSERT INTO `sys_role_menu` VALUES (9, 1, 302);
INSERT INTO `sys_role_menu` VALUES (10, 1, 303);
INSERT INTO `sys_role_menu` VALUES (11, 1, 400);
INSERT INTO `sys_role_menu` VALUES (12, 1, 401);
INSERT INTO `sys_role_menu` VALUES (13, 1, 402);
INSERT INTO `sys_role_menu` VALUES (14, 1, 403);
INSERT INTO `sys_role_menu` VALUES (15, 1, 404);
INSERT INTO `sys_role_menu` VALUES (16, 1, 405);
INSERT INTO `sys_role_menu` VALUES (17, 1, 406);
INSERT INTO `sys_role_menu` VALUES (32, 1, 411);
INSERT INTO `sys_role_menu` VALUES (33, 1, 412);
INSERT INTO `sys_role_menu` VALUES (34, 1, 413);
INSERT INTO `sys_role_menu` VALUES (35, 1, 414);
INSERT INTO `sys_role_menu` VALUES (36, 1, 421);
INSERT INTO `sys_role_menu` VALUES (37, 1, 431);
INSERT INTO `sys_role_menu` VALUES (38, 1, 441);
INSERT INTO `sys_role_menu` VALUES (39, 1, 451);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `user_id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
  `salt` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '盐',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `mobile` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `status` tinyint(0) NULL DEFAULT 1 COMMENT '0禁用 1正常',
  `create_user_id` bigint(0) NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `inviter_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE INDEX `uk_sys_user_username`(`username`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统用户' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '8ab9c1970383d0340800009a428450a2cf6db0fc2deef6f8a59c1ec7a7d4848d', 'phase0adminsalt01', NULL, NULL, 1, NULL, '2026-08-03 22:32:41', NULL);

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL,
  `role_id` bigint(0) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sys_user_role_user_id`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户角色' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (1, 1, 1);

-- ----------------------------
-- Table structure for sys_user_token
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_token`;
CREATE TABLE `sys_user_token`  (
  `user_id` bigint(0) NOT NULL,
  `token` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `expire_time` datetime(0) NULL DEFAULT NULL,
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户Token' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_token
-- ----------------------------

-- ----------------------------
-- Table structure for tb_user
-- ----------------------------
DROP TABLE IF EXISTS `tb_user`;
CREATE TABLE `tb_user`  (
  `user_id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `sex` tinyint(0) NULL DEFAULT NULL,
  `age` int(0) NULL DEFAULT NULL,
  `ma_open_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `mp_open_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `apple_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `sys_phone` tinyint(0) NULL DEFAULT NULL,
  `status` tinyint(0) NULL DEFAULT 1,
  `platform` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `cid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `user_type` tinyint(0) NULL DEFAULT 1,
  `is_vip` tinyint(0) NULL DEFAULT 0,
  `invitation_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `superior_invitation_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE INDEX `uk_tb_user_phone`(`phone`) USING BTREE,
  INDEX `idx_tb_user_invitation_code`(`invitation_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'C端用户' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tb_user
-- ----------------------------

-- ----------------------------
-- Table structure for user_address
-- ----------------------------
DROP TABLE IF EXISTS `user_address`;
CREATE TABLE `user_address`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '地址ID',
  `user_id` bigint(0) NOT NULL COMMENT '用户ID',
  `contact_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '收货人姓名',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '收货人手机号',
  `province` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '省',
  `city` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '市',
  `district` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '区/县',
  `detail_address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '详细地址',
  `is_default` tinyint(0) NOT NULL DEFAULT 0 COMMENT '是否默认：1是 0否',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `deleted` int(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 -1删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_address_user`(`user_id`, `is_default`, `deleted`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户配送地址' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_address
-- ----------------------------

-- ----------------------------
-- Table structure for user_coupon
-- ----------------------------
DROP TABLE IF EXISTS `user_coupon`;
CREATE TABLE `user_coupon`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '用户券ID',
  `coupon_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '券编号（业务键）',
  `user_id` bigint(0) NOT NULL COMMENT '用户ID',
  `template_id` bigint(0) NOT NULL COMMENT '模板ID',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'unused' COMMENT '状态：unused/used/expired',
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '核销订单号',
  `used_time` datetime(0) NULL DEFAULT NULL COMMENT '使用时间',
  `expire_time` datetime(0) NOT NULL COMMENT '过期时间',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '领取时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `deleted` int(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 -1删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_coupon_no`(`coupon_no`, `deleted`) USING BTREE,
  INDEX `idx_user_coupon_user_status`(`user_id`, `status`, `deleted`) USING BTREE,
  INDEX `idx_user_coupon_template`(`template_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户优惠券' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_coupon
-- ----------------------------

-- ----------------------------
-- Table structure for user_deposit
-- ----------------------------
DROP TABLE IF EXISTS `user_deposit`;
CREATE TABLE `user_deposit`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '押金账户ID',
  `user_id` bigint(0) NOT NULL COMMENT '用户ID',
  `total_amount` decimal(10, 2) NOT NULL COMMENT '累计缴纳押金（元）',
  `frozen_amount` decimal(10, 2) NOT NULL COMMENT '冻结中押金（元）',
  `status` tinyint(0) NOT NULL DEFAULT 1 COMMENT '状态：1正常 0冻结',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_deposit_user`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户押金账户' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_deposit
-- ----------------------------

-- ----------------------------
-- Table structure for user_deposit_log
-- ----------------------------
DROP TABLE IF EXISTS `user_deposit_log`;
CREATE TABLE `user_deposit_log`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `log_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '流水号（业务键）',
  `user_id` bigint(0) NOT NULL COMMENT '用户ID',
  `log_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型：pay缴纳/freeze冻结/unfreeze解冻/refund退还/deduct扣款',
  `amount` decimal(10, 2) NOT NULL COMMENT '金额（元，正数）',
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联订单号',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'completed' COMMENT '状态：pending/completed/cancelled',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_deposit_log_no`(`log_no`) USING BTREE,
  INDEX `idx_user_deposit_log_user`(`user_id`, `log_type`, `create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 48 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '押金流水' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_deposit_log
-- ----------------------------
INSERT INTO `user_deposit_log` VALUES (1, 'DLOG-M06-001', 1, 'pay', 200.00, NULL, 'completed', 'M06 演示押金缴纳', '2026-08-04 02:20:45', '2026-08-04 02:20:45');

-- ----------------------------
-- Table structure for user_favorite
-- ----------------------------
DROP TABLE IF EXISTS `user_favorite`;
CREATE TABLE `user_favorite`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` bigint(0) NOT NULL COMMENT '用户ID',
  `goods_id` bigint(0) NOT NULL COMMENT '商品ID',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '收藏时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `deleted` int(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 -1删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_favorite_user_goods`(`user_id`, `goods_id`, `deleted`) USING BTREE,
  INDEX `idx_user_favorite_goods`(`goods_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户商品收藏' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_favorite
-- ----------------------------
INSERT INTO `user_favorite` VALUES (1, 1, 1, '2026-08-04 01:14:43', '2026-08-04 01:45:38', -1);
INSERT INTO `user_favorite` VALUES (2, 1, 2, '2026-08-04 01:14:43', '2026-08-04 01:14:43', 0);
INSERT INTO `user_favorite` VALUES (3, 1, 4, '2026-08-04 01:14:43', '2026-08-04 01:14:43', 0);
INSERT INTO `user_favorite` VALUES (5, 1, 3, '2026-08-04 01:35:55', '2026-08-04 01:38:18', -1);
INSERT INTO `user_favorite` VALUES (6, 4, 1, '2026-08-31 13:15:45', '2026-08-31 13:15:45', 0);

-- ----------------------------
-- Table structure for user_feedback
-- ----------------------------
DROP TABLE IF EXISTS `user_feedback`;
CREATE TABLE `user_feedback`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
  `feedback_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '反馈编号（业务键）',
  `user_id` bigint(0) NOT NULL COMMENT '用户ID',
  `feedback_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型：order/order_service/product/suggestion/other',
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '反馈内容',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `image_urls` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图片URL（逗号分隔，上传组件）',
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'pending' COMMENT '状态：pending/processed',
  `reply_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '处理备注',
  `process_time` datetime(0) NULL DEFAULT NULL COMMENT '处理时间',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '提交时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `deleted` int(0) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 -1删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_feedback_no`(`feedback_no`, `deleted`) USING BTREE,
  INDEX `idx_user_feedback_user`(`user_id`, `status`, `deleted`) USING BTREE,
  INDEX `idx_user_feedback_type`(`feedback_type`, `status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户意见反馈' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_feedback
-- ----------------------------
INSERT INTO `user_feedback` VALUES (1, 'FB-M06-001', 1, 'suggestion', '希望增加更多露营套餐组合', '13812348888', NULL, 'pending', NULL, NULL, '2026-08-04 02:20:45', '2026-08-04 02:20:45', 0);
INSERT INTO `user_feedback` VALUES (2, 'FB202608040220484113', 1, 'suggestion', 'M06 smoke feedback', '13812348888', NULL, 'pending', NULL, NULL, '2026-08-04 02:20:48', '2026-08-04 02:20:48', 0);
INSERT INTO `user_feedback` VALUES (3, 'FB202608040227244212', 1, 'suggestion', 'M06-smoke-feedback-1785781644309', '13812348888', NULL, 'processed', 'M06-smoke-已处理', '2026-08-04 02:27:24', '2026-08-04 02:27:24', '2026-08-04 02:27:24', 0);

-- ----------------------------
-- Table structure for user_wallet
-- ----------------------------
DROP TABLE IF EXISTS `user_wallet`;
CREATE TABLE `user_wallet`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL,
  `amount` decimal(12, 2) NULL DEFAULT NULL,
  `frozen_amount` decimal(10, 2) NOT NULL COMMENT '冻结金额（元）',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_wallet_user_id`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户钱包' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_wallet
-- ----------------------------

-- ----------------------------
-- Table structure for user_wallet_transaction
-- ----------------------------
DROP TABLE IF EXISTS `user_wallet_transaction`;
CREATE TABLE `user_wallet_transaction`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL,
  `amount` decimal(12, 2) NOT NULL,
  `direction` tinyint(0) NOT NULL COMMENT '1收入 2支出',
  `transaction_type` tinyint(0) NULL DEFAULT NULL,
  `biz_type` tinyint(0) NULL DEFAULT NULL,
  `biz_id` bigint(0) NULL DEFAULT NULL,
  `title` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '交易标题',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '交易描述',
  `status` tinyint(0) NOT NULL DEFAULT 2 COMMENT '状态 1待处理 2完成',
  `extra_data` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鎵╁睍淇℃伅锛堝鏀粯鏂瑰紡銆佹笭閬撶瓑锛�',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_wallet_tx_user_id`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 52 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '钱包流水' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_wallet_transaction
-- ----------------------------
INSERT INTO `user_wallet_transaction` VALUES (1, 1, 10.00, 2, 2, 10, 1, '余额提现', '提现申请', 2, NULL, NULL, '2026-08-04 02:27:24', '2026-08-04 02:27:24');

-- ----------------------------
-- Table structure for verify_code
-- ----------------------------
DROP TABLE IF EXISTS `verify_code`;
CREATE TABLE `verify_code`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` tinyint(0) NULL DEFAULT 1 COMMENT '1未使用 2已使用',
  `biz_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `expire_time` datetime(0) NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_verify_code_phone`(`phone`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2095355974081241091 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '验证码' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of verify_code
-- ----------------------------

-- ----------------------------
-- Table structure for vip_order
-- ----------------------------
DROP TABLE IF EXISTS `vip_order`;
CREATE TABLE `vip_order`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '会员订单ID',
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会员订单号 VIP 前缀',
  `user_id` bigint(0) NOT NULL COMMENT '用户ID',
  `plan_id` bigint(0) NOT NULL COMMENT '套餐ID',
  `plan_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '套餐名称快照',
  `month` int(0) NULL DEFAULT NULL COMMENT '月数快照',
  `amount` decimal(10, 2) NOT NULL COMMENT '应付金额',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'pending_payment' COMMENT 'pending_payment/paid/cancelled',
  `pay_channel` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支付渠道',
  `pay_trade_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '第三方交易号',
  `pay_time` datetime(0) NULL DEFAULT NULL COMMENT '支付时间',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_vip_order_no`(`order_no`) USING BTREE,
  INDEX `idx_vip_order_user`(`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '会员购买订单' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of vip_order
-- ----------------------------

-- ----------------------------
-- Table structure for vip_plan
-- ----------------------------
DROP TABLE IF EXISTS `vip_plan`;
CREATE TABLE `vip_plan`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `month_count` int(0) NULL DEFAULT 1,
  `price` decimal(10, 2) NOT NULL,
  `origin_price` decimal(10, 2) NULL DEFAULT NULL,
  `sort` int(0) NULL DEFAULT 0,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '会员套餐' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of vip_plan
-- ----------------------------

-- ----------------------------
-- Table structure for vip_rights
-- ----------------------------
DROP TABLE IF EXISTS `vip_rights`;
CREATE TABLE `vip_rights`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `icon` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `sort` int(0) NULL DEFAULT 0,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '会员特权' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of vip_rights
-- ----------------------------

-- ----------------------------
-- Table structure for withdraw_record
-- ----------------------------
DROP TABLE IF EXISTS `withdraw_record`;
CREATE TABLE `withdraw_record`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL,
  `source_type` tinyint(0) NOT NULL DEFAULT 1 COMMENT '来源：1佣金提现 2钱包提现',
  `amount` decimal(12, 2) NOT NULL,
  `actual_amount` decimal(10, 2) NULL DEFAULT NULL COMMENT '实际到账金额',
  `service_fee` decimal(10, 2) NULL DEFAULT NULL COMMENT '手续费',
  `account_type` tinyint(0) NULL DEFAULT NULL COMMENT '提现账户 1微信 2支付宝',
  `status` tinyint(0) NULL DEFAULT 0,
  `reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '拒绝理由',
  `withdraw_time` datetime(0) NULL DEFAULT NULL COMMENT '转账时间',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_withdraw_record_user_id`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '提现记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of withdraw_record
-- ----------------------------
INSERT INTO `withdraw_record` VALUES (1, 1, 2, 10.00, 9.94, 0.06, 2, 2, NULL, '2026-08-04 02:27:24', NULL, '2026-08-04 02:27:24', '2026-08-04 02:27:24');
INSERT INTO `withdraw_record` VALUES (2, 4, 2, 50.00, 49.70, 0.30, 1, 3, '啊实打实的a', NULL, NULL, '2026-08-31 13:56:45', '2026-08-31 13:56:45');

-- ----------------------------
-- Table structure for withdrawal_account
-- ----------------------------
DROP TABLE IF EXISTS `withdrawal_account`;
CREATE TABLE `withdrawal_account`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL,
  `account_type` tinyint(0) NULL DEFAULT NULL,
  `account_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0),
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_withdrawal_account_user_id`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '提现账号' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of withdrawal_account
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
