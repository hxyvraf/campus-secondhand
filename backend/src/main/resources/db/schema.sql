-- ============================================================
-- 校园二手交易平台 数据库结构（首次启动自动执行，可重复执行）
-- 数据库 campus_trade 由连接串参数 createDatabaseIfNotExist=true 自动创建
-- ============================================================

CREATE TABLE IF NOT EXISTS `user` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username`   VARCHAR(50)  NOT NULL COMMENT '登录账号',
  `password`   VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码',
  `nickname`   VARCHAR(50)  NOT NULL COMMENT '昵称',
  `phone`      VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
  `email`      VARCHAR(100)          DEFAULT NULL COMMENT '邮箱',
  `avatar`     VARCHAR(255)          DEFAULT NULL COMMENT '头像地址',
  `school`     VARCHAR(100)          DEFAULT NULL COMMENT '学校',
  `role`       VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色：USER 普通用户 / ADMIN 管理员',
  `status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1 正常，0 已禁用',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `category` (
  `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name`       VARCHAR(20) NOT NULL COMMENT '分类名称',
  `sort`       INT         NOT NULL DEFAULT 99 COMMENT '排序值，越小越靠前',
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

CREATE TABLE IF NOT EXISTS `product` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `seller_id`       BIGINT        NOT NULL COMMENT '卖家用户ID',
  `category_id`     BIGINT        NOT NULL COMMENT '分类ID',
  `title`           VARCHAR(50)   NOT NULL COMMENT '商品标题',
  `description`     VARCHAR(1000)          DEFAULT NULL COMMENT '商品描述',
  `price`           DECIMAL(10,2) NOT NULL COMMENT '售价',
  `original_price`  DECIMAL(10,2)          DEFAULT NULL COMMENT '原价',
  `condition_level` VARCHAR(20)   NOT NULL COMMENT '成色：全新/九成新/八成新/七成新及以下',
  `trade_place`     VARCHAR(50)            DEFAULT NULL COMMENT '交易地点',
  `status`          VARCHAR(20)   NOT NULL DEFAULT 'ON_SALE' COMMENT '状态：ON_SALE/LOCKED/SOLD/OFF_SHELF',
  `view_count`      INT           NOT NULL DEFAULT 0 COMMENT '浏览量',
  `favorite_count`  INT           NOT NULL DEFAULT 0 COMMENT '收藏数',
  `deleted`         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_seller` (`seller_id`),
  KEY `idx_product_category` (`category_id`),
  KEY `idx_product_status` (`status`),
  KEY `idx_product_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

CREATE TABLE IF NOT EXISTS `product_image` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `product_id` BIGINT       NOT NULL COMMENT '商品ID',
  `url`        VARCHAR(255) NOT NULL COMMENT '图片地址',
  `sort`       INT          NOT NULL DEFAULT 0 COMMENT '排序值',
  PRIMARY KEY (`id`),
  KEY `idx_image_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品图片表';

CREATE TABLE IF NOT EXISTS `favorite` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id`    BIGINT   NOT NULL COMMENT '用户ID',
  `product_id` BIGINT   NOT NULL COMMENT '商品ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_favorite_user_product` (`user_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

CREATE TABLE IF NOT EXISTS `trade_order` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no`      VARCHAR(32)   NOT NULL COMMENT '订单号',
  `product_id`    BIGINT        NOT NULL COMMENT '商品ID',
  `buyer_id`      BIGINT        NOT NULL COMMENT '买家用户ID',
  `seller_id`     BIGINT        NOT NULL COMMENT '卖家用户ID',
  `amount`        DECIMAL(10,2) NOT NULL COMMENT '成交金额',
  `status`        VARCHAR(20)   NOT NULL DEFAULT 'PENDING_CONFIRM' COMMENT '订单状态',
  `remark`        VARCHAR(200)           DEFAULT NULL COMMENT '买家留言',
  `cancel_reason` VARCHAR(200)           DEFAULT NULL COMMENT '取消原因',
  `expire_at`     DATETIME               DEFAULT NULL COMMENT '卖家确认超时时间点',
  `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `confirmed_at`  DATETIME               DEFAULT NULL COMMENT '卖家确认时间',
  `finished_at`   DATETIME               DEFAULT NULL COMMENT '交易完成时间',
  `canceled_at`   DATETIME               DEFAULT NULL COMMENT '取消/关闭时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_order_buyer` (`buyer_id`),
  KEY `idx_order_seller` (`seller_id`),
  KEY `idx_order_status_expire` (`status`, `expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE IF NOT EXISTS `message` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `user_id`    BIGINT       NOT NULL COMMENT '接收人用户ID',
  `type`       VARCHAR(20)  NOT NULL COMMENT '消息类型：ORDER/FAVORITE/SYSTEM',
  `title`      VARCHAR(50)  NOT NULL COMMENT '消息标题',
  `content`    VARCHAR(255) NOT NULL COMMENT '消息内容',
  `related_id` BIGINT                DEFAULT NULL COMMENT '关联业务ID（订单ID或商品ID）',
  `is_read`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读：0 未读，1 已读',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_message_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息表';
