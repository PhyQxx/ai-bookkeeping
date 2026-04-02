-- AI 智能记账系统 - 数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS ai_bookkeeping DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_bookkeeping;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名/手机号/邮箱',
    `password` VARCHAR(255) NOT NULL COMMENT 'Bcrypt加密密码',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '软删除标记'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 账单表
CREATE TABLE IF NOT EXISTS `bill` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '关联用户',
    `amount` DECIMAL(12,2) NOT NULL COMMENT '金额',
    `type` TINYINT NOT NULL DEFAULT 2 COMMENT '1=收入, 2=支出',
    `category_id` BIGINT DEFAULT NULL COMMENT '关联分类',
    `bill_date` DATE NOT NULL COMMENT '账单日期',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `input_type` TINYINT NOT NULL DEFAULT 2 COMMENT '1=AI输入, 2=手动输入',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '软删除标记',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_bill_date` (`bill_date`),
    INDEX `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单表';

-- 分类表
CREATE TABLE IF NOT EXISTS `category` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT DEFAULT NULL COMMENT 'NULL=系统预设, 非NULL=用户自定义',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `type` TINYINT NOT NULL COMMENT '1=收入分类, 2=支出分类',
    `icon` VARCHAR(50) DEFAULT NULL COMMENT '图标标识',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类表';

-- 插入系统预设分类 - 支出
INSERT INTO `category` (`user_id`, `name`, `type`, `icon`, `sort_order`) VALUES
(NULL, '餐饮', 2, 'food', 1),
(NULL, '交通', 2, 'transport', 2),
(NULL, '购物', 2, 'shopping', 3),
(NULL, '娱乐', 2, 'entertainment', 4),
(NULL, '住房', 2, 'house', 5),
(NULL, '医疗', 2, 'medical', 6),
(NULL, '教育', 2, 'education', 7),
(NULL, '通讯', 2, 'telecom', 8),
(NULL, '日用', 2, 'daily', 9),
(NULL, '服饰', 2, 'clothing', 10),
(NULL, '美容', 2, 'beauty', 11),
(NULL, '运动', 2, 'sport', 12),
(NULL, '宠物', 2, 'pet', 13),
(NULL, '礼物', 2, 'gift', 14),
(NULL, '其他', 2, 'other', 15);

-- 插入系统预设分类 - 收入
INSERT INTO `category` (`user_id`, `name`, `type`, `icon`, `sort_order`) VALUES
(NULL, '工资', 1, 'salary', 1),
(NULL, '兼职', 1, 'parttime', 2),
(NULL, '理财', 1, 'finance', 3),
(NULL, '红包', 1, 'redpacket', 4),
(NULL, '报销', 1, 'reimburse', 5),
(NULL, '退款', 1, 'refund', 6),
(NULL, '其他', 1, 'other', 7);
