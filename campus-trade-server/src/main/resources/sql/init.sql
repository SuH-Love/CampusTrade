-- CampusTrade 数据库初始化脚本
-- 数据库：campus_trade
-- 字符集：utf8mb4
-- 时区由docker-compose MySQL命令 --default-time-zone='+08:00' 统一管理

CREATE DATABASE IF NOT EXISTS campus_trade DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE campus_trade;

-- ============================================================
-- 用户与权限
-- ============================================================

CREATE TABLE t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码(BCrypt)',
    nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    real_name VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    student_id VARCHAR(50) DEFAULT NULL COMMENT '学号',
    real_verified TINYINT DEFAULT 0 COMMENT '实名认证状态 0-未认证 1-已认证',
    status TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone),
    UNIQUE KEY uk_email (email),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE t_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名',
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
    description VARCHAR(200) DEFAULT NULL COMMENT '描述',
    status TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE t_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    permission_name VARCHAR(50) NOT NULL COMMENT '权限名',
    permission_code VARCHAR(50) NOT NULL COMMENT '权限编码',
    resource_type TINYINT DEFAULT 1 COMMENT '资源类型 1-菜单 2-按钮',
    parent_id BIGINT DEFAULT 0 COMMENT '父权限ID',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    UNIQUE KEY uk_permission_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

CREATE TABLE t_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    KEY idx_user_id (user_id),
    KEY idx_role_id (role_id),
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

CREATE TABLE t_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    KEY idx_role_id (role_id),
    KEY idx_permission_id (permission_id),
    UNIQUE KEY uk_role_permission (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ============================================================
-- 商品
-- ============================================================

CREATE TABLE t_goods_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    category_name VARCHAR(50) NOT NULL COMMENT '分类名',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID',
    sort_order INT DEFAULT 0 COMMENT '排序',
    icon VARCHAR(255) DEFAULT NULL COMMENT '图标',
    status TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

CREATE TABLE t_goods (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '卖家用户ID',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    title VARCHAR(200) NOT NULL COMMENT '商品标题',
    description TEXT COMMENT '商品描述',
    price DECIMAL(10,2) NOT NULL COMMENT '售价',
    original_price DECIMAL(10,2) DEFAULT NULL COMMENT '原价',
    cover_image VARCHAR(255) DEFAULT NULL COMMENT '封面图',
    images TEXT COMMENT '图片列表(JSON)',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT/PENDING/APPROVED/REJECTED/ONLINE/OFFLINE/SOLD',
    reject_reason VARCHAR(500) DEFAULT NULL COMMENT '驳回原因',
    view_count INT DEFAULT 0 COMMENT '浏览量',
    favorite_count INT DEFAULT 0 COMMENT '收藏量',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    KEY idx_user_id (user_id),
    KEY idx_category_id (category_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time),
    KEY idx_user_status (user_id, status),
    KEY idx_goods_status (id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

CREATE TABLE t_goods_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    goods_id BIGINT NOT NULL COMMENT '商品ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    KEY idx_user_id (user_id),
    KEY idx_goods_id (goods_id),
    UNIQUE KEY uk_user_goods (user_id, goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品收藏表';

-- ============================================================
-- 订单
-- ============================================================

CREATE TABLE t_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    buyer_id BIGINT NOT NULL COMMENT '买家ID',
    seller_id BIGINT NOT NULL COMMENT '卖家ID',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAY' COMMENT '状态 PENDING_PAY/PAID/SHIPPING/FINISHED/CANCELLED/REFUND',
    pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
    ship_time DATETIME DEFAULT NULL COMMENT '发货时间',
    finish_time DATETIME DEFAULT NULL COMMENT '完成时间',
    cancel_time DATETIME DEFAULT NULL COMMENT '取消时间',
    cancel_reason VARCHAR(500) DEFAULT NULL COMMENT '取消原因',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_buyer_id (buyer_id),
    KEY idx_seller_id (seller_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time),
    KEY idx_buyer_status (buyer_id, status),
    KEY idx_seller_status (seller_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE t_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    goods_id BIGINT NOT NULL COMMENT '商品ID',
    goods_title VARCHAR(200) NOT NULL COMMENT '商品标题(快照)',
    goods_image VARCHAR(255) DEFAULT NULL COMMENT '商品图片(快照)',
    price DECIMAL(10,2) NOT NULL COMMENT '成交价(快照)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    KEY idx_order_id (order_id),
    KEY idx_goods_id (goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- ============================================================
-- 聊天
-- ============================================================

CREATE TABLE t_chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    receiver_id BIGINT NOT NULL COMMENT '接收者ID',
    content TEXT NOT NULL COMMENT '消息内容',
    message_type TINYINT DEFAULT 1 COMMENT '消息类型 1-文本 2-图片',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读 0-未读 1-已读',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    KEY idx_sender_id (sender_id),
    KEY idx_receiver_id (receiver_id),
    KEY idx_create_time (create_time),
    KEY idx_sender_receiver (sender_id, receiver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';

-- ============================================================
-- 举报
-- ============================================================

CREATE TABLE t_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    reporter_id BIGINT NOT NULL COMMENT '举报人ID',
    target_type TINYINT NOT NULL COMMENT '举报对象类型 1-商品 2-用户 3-聊天',
    target_id BIGINT NOT NULL COMMENT '举报对象ID',
    reason VARCHAR(500) NOT NULL COMMENT '举报原因',
    description TEXT COMMENT '详细描述',
    images TEXT COMMENT '证据图片(JSON)',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/PROCESSING/FINISHED',
    handler_id BIGINT DEFAULT NULL COMMENT '处理人ID',
    handle_result VARCHAR(500) DEFAULT NULL COMMENT '处理结果',
    handle_time DATETIME DEFAULT NULL COMMENT '处理时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    KEY idx_reporter_id (reporter_id),
    KEY idx_target (target_type, target_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='举报表';

-- ============================================================
-- 通知
-- ============================================================

CREATE TABLE t_notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '接收用户ID',
    title VARCHAR(200) NOT NULL COMMENT '通知标题',
    content TEXT COMMENT '通知内容',
    notification_type VARCHAR(20) NOT NULL DEFAULT 'SYSTEM' COMMENT '类型 SYSTEM/ORDER/GOODS/REPORT/CHAT',
    related_id BIGINT DEFAULT NULL COMMENT '关联业务ID',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读 0-未读 1-已读',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    KEY idx_user_id (user_id),
    KEY idx_status (is_read),
    KEY idx_create_time (create_time),
    KEY idx_user_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- ============================================================
-- 横幅
-- ============================================================

CREATE TABLE t_banner (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    subtitle VARCHAR(500) DEFAULT NULL COMMENT '副标题',
    image_url VARCHAR(255) DEFAULT NULL COMMENT '背景图片URL',
    link_url VARCHAR(255) DEFAULT NULL COMMENT '点击跳转链接',
    bg_color VARCHAR(500) DEFAULT NULL COMMENT '背景色/渐变CSS',
    button_text VARCHAR(50) DEFAULT NULL COMMENT '按钮文字',
    sort_order INT DEFAULT 0 COMMENT '排序(升序)',
    status TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    KEY idx_sort_order (sort_order),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='横幅表';

-- ============================================================
-- 日志
-- ============================================================

CREATE TABLE t_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT DEFAULT NULL COMMENT '操作用户ID',
    username VARCHAR(50) DEFAULT NULL COMMENT '操作用户名',
    module VARCHAR(50) NOT NULL COMMENT '模块',
    operation VARCHAR(200) NOT NULL COMMENT '操作描述',
    method VARCHAR(200) DEFAULT NULL COMMENT '请求方法',
    request_url VARCHAR(255) DEFAULT NULL COMMENT '请求URL',
    request_params TEXT COMMENT '请求参数',
    response_result TEXT COMMENT '响应结果',
    ip VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    duration BIGINT DEFAULT NULL COMMENT '耗时(ms)',
    status TINYINT DEFAULT 1 COMMENT '状态 0-失败 1-成功',
    error_msg TEXT COMMENT '错误信息',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT '追踪ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    KEY idx_user_id (user_id),
    KEY idx_module (module),
    KEY idx_create_time (create_time),
    KEY idx_trace_id (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

CREATE TABLE t_security_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT DEFAULT NULL COMMENT '用户ID',
    username VARCHAR(50) DEFAULT NULL COMMENT '用户名',
    event_type VARCHAR(50) NOT NULL COMMENT '事件类型 LOGIN_FAIL/ACCESS_DENIED/TOKEN_EXPIRED/RATE_LIMIT/MALICIOUS_INPUT',
    ip VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    detail TEXT COMMENT '详细信息',
    trace_id VARCHAR(64) DEFAULT NULL COMMENT '追踪ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    KEY idx_user_id (user_id),
    KEY idx_event_type (event_type),
    KEY idx_create_time (create_time),
    KEY idx_trace_id (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全日志表';

-- ============================================================
-- 初始化数据
-- ============================================================

INSERT INTO t_role (role_name, role_code, description) VALUES
('超级管理员', 'ROLE_SUPER_ADMIN', '系统超级管理员'),
('管理员', 'ROLE_ADMIN', '普通管理员'),
('普通用户', 'ROLE_USER', '注册用户');

INSERT INTO t_permission (permission_name, permission_code, resource_type, parent_id, sort_order) VALUES
('商品管理', 'goods:manage', 1, 0, 1),
('商品创建', 'goods:create', 2, 1, 1),
('商品修改', 'goods:update', 2, 1, 2),
('商品删除', 'goods:delete', 2, 1, 3),
('商品审核', 'goods:audit', 2, 1, 4),
('用户管理', 'user:manage', 1, 0, 2),
('用户封禁', 'user:ban', 2, 5, 1),
('举报管理', 'report:manage', 1, 0, 3),
('举报审核', 'report:review', 2, 7, 1),
('日志管理', 'log:manage', 1, 0, 4),
('日志查看', 'log:view', 2, 9, 1);

INSERT INTO t_role_permission (role_id, permission_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10), (1, 11),
(2, 1), (2, 4), (2, 6), (2, 7), (2, 8), (2, 9), (2, 10), (2, 11);

INSERT INTO t_goods_category (category_name, parent_id, sort_order) VALUES
('数码电子', 0, 1),
('书籍教材', 0, 2),
('生活用品', 0, 3),
('服装鞋帽', 0, 4),
('运动户外', 0, 5),
('其他', 0, 6);
