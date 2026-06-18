SET NAMES utf8mb4;
SET time_zone = '+08:00';

CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL DEFAULT 1,
    openid VARCHAR(128) NOT NULL,
    unionid VARCHAR(128) NULL,
    nickname VARCHAR(64) NOT NULL DEFAULT '',
    avatar VARCHAR(512) NOT NULL DEFAULT '',
    phone VARCHAR(32) NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1封禁',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_store_openid (store_id, openid),
    KEY idx_user_store_phone (store_id, phone),
    KEY idx_user_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SELECT GET_LOCK('dz_schema_user_store_migration', 30);

SET @user_store_id_column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME = 'store_id'
);
SET @user_store_id_ddl = IF(
    @user_store_id_column_exists = 0,
    'ALTER TABLE `user` ADD COLUMN store_id BIGINT NOT NULL DEFAULT 1 AFTER id',
    'SELECT 1'
);
PREPARE user_store_id_statement FROM @user_store_id_ddl;
EXECUTE user_store_id_statement;
DEALLOCATE PREPARE user_store_id_statement;

SET @user_openid_index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND INDEX_NAME = 'uk_user_openid'
);
SET @user_openid_index_ddl = IF(
    @user_openid_index_exists > 0,
    'ALTER TABLE `user` DROP INDEX uk_user_openid',
    'SELECT 1'
);
PREPARE user_openid_index_statement FROM @user_openid_index_ddl;
EXECUTE user_openid_index_statement;
DEALLOCATE PREPARE user_openid_index_statement;

SET @user_store_openid_index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND INDEX_NAME = 'uk_user_store_openid'
);
SET @user_store_openid_index_ddl = IF(
    @user_store_openid_index_exists = 0,
    'ALTER TABLE `user` ADD UNIQUE KEY uk_user_store_openid (store_id, openid)',
    'SELECT 1'
);
PREPARE user_store_openid_index_statement FROM @user_store_openid_index_ddl;
EXECUTE user_store_openid_index_statement;
DEALLOCATE PREPARE user_store_openid_index_statement;

SET @user_store_phone_index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND INDEX_NAME = 'idx_user_store_phone'
);
SET @user_store_phone_index_ddl = IF(
    @user_store_phone_index_exists = 0,
    'ALTER TABLE `user` ADD KEY idx_user_store_phone (store_id, phone)',
    'SELECT 1'
);
PREPARE user_store_phone_index_statement FROM @user_store_phone_index_ddl;
EXECUTE user_store_phone_index_statement;
DEALLOCATE PREPARE user_store_phone_index_statement;

SELECT RELEASE_LOCK('dz_schema_user_store_migration');

CREATE TABLE IF NOT EXISTS user_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    balance_fen BIGINT NOT NULL DEFAULT 0,
    points BIGINT NOT NULL DEFAULT 0,
    frozen_points BIGINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_account_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS account_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    asset_type VARCHAR(16) NOT NULL COMMENT 'BALANCE/POINTS/FROZEN_POINTS',
    change_type VARCHAR(32) NOT NULL,
    change_value BIGINT NOT NULL,
    before_value BIGINT NOT NULL,
    after_value BIGINT NOT NULL,
    biz_no VARCHAR(64) NOT NULL,
    operator VARCHAR(64) NOT NULL,
    remark VARCHAR(255) NOT NULL DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_account_log_biz (biz_no, change_type, asset_type, remark(64)),
    KEY idx_account_log_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS store (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    address VARCHAR(255) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_category_store (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    main_image VARCHAR(512) NOT NULL DEFAULT '',
    images JSON NULL,
    description TEXT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0下架 1上架',
    recommended TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_product_store_category (store_id, category_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product_sku (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    spec_name VARCHAR(128) NOT NULL,
    price_fen BIGINT NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    sales INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_product_sku_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS cart (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_cart_user_sku (user_id, sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `order` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    total_fen BIGINT NOT NULL,
    pay_type VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    pay_time DATETIME NULL,
    cancel_time DATETIME NULL,
    remark VARCHAR(255) NOT NULL DEFAULT '',
    prepay_requested TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_order_user_status (user_id, status),
    KEY idx_order_status_time (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    spec_name VARCHAR(128) NOT NULL,
    price_fen BIGINT NOT NULL,
    quantity INT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_order_item_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payment_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(32) NOT NULL,
    transaction_id VARCHAR(64) NULL,
    amount_fen BIGINT NOT NULL,
    openid VARCHAR(128) NOT NULL,
    trade_state VARCHAR(32) NOT NULL,
    notify_raw TEXT NULL,
    verify_result VARCHAR(32) NOT NULL,
    query_result TEXT NULL,
    notify_count INT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_payment_order_no (order_no),
    UNIQUE KEY uk_payment_transaction (transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS recharge_tier (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    pay_fen BIGINT NOT NULL,
    bonus_fen BIGINT NOT NULL DEFAULT 0,
    label VARCHAR(64) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    sort INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS recharge_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recharge_no VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL,
    tier_id BIGINT NOT NULL,
    pay_fen BIGINT NOT NULL,
    bonus_fen BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL,
    credited TINYINT NOT NULL DEFAULT 0,
    prepay_requested TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_recharge_no (recharge_no),
    KEY idx_recharge_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS points_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type VARCHAR(16) NOT NULL,
    user_id BIGINT NOT NULL,
    points BIGINT NOT NULL,
    remark VARCHAR(255) NOT NULL,
    voucher_images JSON NULL,
    status VARCHAR(16) NOT NULL,
    auditor_id BIGINT NULL,
    audit_remark VARCHAR(255) NULL,
    audit_time DATETIME NULL,
    before_points BIGINT NULL,
    after_points BIGINT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_points_request_user (user_id),
    KEY idx_points_request_status (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(128) NOT NULL,
    path VARCHAR(255) NULL,
    sort INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_permission_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_role_permission (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(128) NOT NULL,
    real_name VARCHAR(64) NOT NULL,
    role_id BIGINT NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    last_login_time DATETIME NULL,
    login_fail_count INT NOT NULL DEFAULT 0,
    locked_until DATETIME NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_admin_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    admin_id BIGINT NOT NULL,
    module VARCHAR(64) NOT NULL,
    action VARCHAR(128) NOT NULL,
    params_digest VARCHAR(1000) NOT NULL,
    ip VARCHAR(64) NOT NULL,
    cost_ms BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_operation_admin_time (admin_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notify_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    template_type VARCHAR(32) NOT NULL,
    data JSON NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error VARCHAR(500) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_notify_pending (status, next_retry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS idempotent_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    idempotent_key VARCHAR(191) NOT NULL,
    status VARCHAR(16) NOT NULL,
    expire_time DATETIME NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_idempotent_key (idempotent_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS home_content (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    announcement VARCHAR(1000) NOT NULL DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_home_store (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(128) NOT NULL,
    config_value TEXT NOT NULL,
    description VARCHAR(255) NOT NULL DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_system_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS activity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    image_url VARCHAR(512) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_activity_store_status (store_id, status, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS coupon_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_coupon_category_store (store_id, status, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS coupon_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    category_id BIGINT NULL,
    name VARCHAR(128) NOT NULL,
    image_url VARCHAR(512) NOT NULL DEFAULT '',
    description VARCHAR(500) NOT NULL DEFAULT '',
    sale_product_id BIGINT NULL COMMENT '关联商品，支付成功后自动发券',
    purchase_valid_days INT NOT NULL DEFAULT 365,
    gift_valid_days INT NOT NULL DEFAULT 30,
    status TINYINT NOT NULL DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_coupon_sale_product (sale_product_id),
    KEY idx_coupon_template_store (store_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @coupon_template_category_column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'coupon_template'
      AND COLUMN_NAME = 'category_id'
);
SET @coupon_template_category_ddl = IF(
    @coupon_template_category_column_exists = 0,
    'ALTER TABLE coupon_template ADD COLUMN category_id BIGINT NULL AFTER store_id',
    'SELECT 1'
);
PREPARE coupon_template_category_statement FROM @coupon_template_category_ddl;
EXECUTE coupon_template_category_statement;
DEALLOCATE PREPARE coupon_template_category_statement;

CREATE TABLE IF NOT EXISTS user_coupon (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    coupon_no VARCHAR(40) NOT NULL,
    template_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    coupon_name VARCHAR(128) NOT NULL,
    image_url VARCHAR(512) NOT NULL DEFAULT '',
    source_type VARCHAR(16) NOT NULL COMMENT 'PURCHASE/GIFT',
    source_no VARCHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL COMMENT 'UNUSED/REDEEM_PENDING/USED/EXPIRED',
    expire_time DATETIME NOT NULL,
    used_time DATETIME NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_coupon_no (coupon_no),
    UNIQUE KEY uk_user_coupon_source (source_type, source_no),
    KEY idx_user_coupon_user_status (user_id, status, expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS store_operation_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    business_end_time VARCHAR(8) NOT NULL DEFAULT '02:00',
    home_slogan VARCHAR(128) NOT NULL DEFAULT '今晚，慢一点。',
    hero_image VARCHAR(512) NOT NULL DEFAULT '',
    gameplay_description VARCHAR(2000) NOT NULL DEFAULT '',
    menu_title VARCHAR(64) NOT NULL DEFAULT '今晚酒单',
    points_halving_enabled TINYINT NOT NULL DEFAULT 0,
    points_halving_day INT NOT NULL DEFAULT 7 COMMENT '积分减半间隔自然日',
    points_halving_time VARCHAR(8) NOT NULL DEFAULT '00:00',
    points_halving_last_period VARCHAR(16) NOT NULL DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_store_operation_config (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @store_points_halving_day_needs_modify = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'store_operation_config'
      AND COLUMN_NAME = 'points_halving_day'
      AND DATA_TYPE <> 'int'
);
SET @store_points_halving_day_ddl = IF(
    @store_points_halving_day_needs_modify > 0,
    'ALTER TABLE store_operation_config MODIFY COLUMN points_halving_day INT NOT NULL DEFAULT 7 COMMENT ''积分减半间隔自然日''',
    'SELECT 1'
);
PREPARE store_points_halving_day_statement FROM @store_points_halving_day_ddl;
EXECUTE store_points_halving_day_statement;
DEALLOCATE PREPARE store_points_halving_day_statement;

CREATE TABLE IF NOT EXISTS user_store_points (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    points BIGINT NOT NULL DEFAULT 0,
    frozen_points BIGINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_store_points (user_id, store_id),
    KEY idx_store_points_rank (store_id, points, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS store_points_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    type VARCHAR(16) NOT NULL DEFAULT 'WITHDRAW',
    points BIGINT NOT NULL,
    remark VARCHAR(255) NOT NULL DEFAULT '',
    status VARCHAR(16) NOT NULL,
    auditor_id BIGINT NULL,
    audit_remark VARCHAR(255) NULL,
    audit_time DATETIME NULL,
    before_points BIGINT NULL,
    after_points BIGINT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_store_points_request (store_id, status, create_time),
    KEY idx_user_store_points_request (user_id, store_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS store_points_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    change_type VARCHAR(32) NOT NULL,
    change_value BIGINT NOT NULL,
    before_value BIGINT NOT NULL,
    after_value BIGINT NOT NULL,
    biz_no VARCHAR(64) NOT NULL,
    operator VARCHAR(64) NOT NULL,
    remark VARCHAR(255) NOT NULL DEFAULT '',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_store_points_log_biz (biz_no, change_type, user_id, store_id),
    KEY idx_store_points_log_user (store_id, user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS coupon_redeem_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_coupon_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL COMMENT 'PENDING/APPROVED/REJECTED',
    remark VARCHAR(255) NOT NULL DEFAULT '',
    auditor_id BIGINT NULL,
    audit_remark VARCHAR(255) NULL,
    audit_time DATETIME NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_coupon_redeem_status (status, create_time),
    KEY idx_coupon_redeem_user (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wechat_access_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token_type VARCHAR(32) NOT NULL,
    access_token VARCHAR(1024) NOT NULL,
    expire_time DATETIME NOT NULL,
    version INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_token_type (token_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS reconcile_diff (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reconcile_date DATE NOT NULL,
    order_no VARCHAR(32) NULL,
    transaction_id VARCHAR(64) NULL,
    local_amount_fen BIGINT NULL,
    wechat_amount_fen BIGINT NULL,
    diff_type VARCHAR(32) NOT NULL,
    detail VARCHAR(1000) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_reconcile_date (reconcile_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO role(id, name, code, status, deleted)
VALUES (1, '超级管理员', 'SUPER_ADMIN', 1, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO permission(id, parent_id, name, code, path, sort, deleted) VALUES
(1, 0, '用户管理', 'user:manage', '/users', 10, 0),
(2, 0, '商品管理', 'product:manage', '/products', 20, 0),
(3, 0, '订单管理', 'order:manage', '/orders', 30, 0),
(4, 0, '支付管理', 'pay:manage', '/payments', 40, 0),
(5, 0, '充值管理', 'recharge:manage', '/recharges', 50, 0),
(6, 0, '积分审核', 'points:audit', '/points', 60, 0),
(7, 0, '账户调整', 'account:adjust', '/accounts', 70, 0),
(8, 0, '操作日志', 'operation:read', '/operations', 80, 0),
(9, 0, '对账管理', 'reconcile:manage', '/reconcile', 90, 0),
(10, 0, '内容配置', 'content:manage', '/content', 100, 0),
(11, 0, '卡券管理', 'coupon:manage', '/coupons', 110, 0),
(12, 0, '经营统计', 'statistics:read', '/statistics', 120, 0),
(13, 0, '系统参数', 'config:manage', '/settings', 130, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), path = VALUES(path);

INSERT IGNORE INTO role_permission(role_id, permission_id, deleted)
SELECT 1, id, 0 FROM permission WHERE deleted = 0;

INSERT INTO `user`(id, store_id, openid, nickname, avatar, phone, status, deleted)
VALUES (1, 1, 'mock-openid-demo', '演示用户', '', '13800138000', 0, 0)
ON DUPLICATE KEY UPDATE store_id = VALUES(store_id), nickname = VALUES(nickname);

INSERT INTO user_account(user_id, balance_fen, points, frozen_points, version, deleted)
VALUES (1, 10000, 1000, 0, 0, 0)
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

INSERT INTO store(id, name, address, phone, status, deleted)
VALUES (1, 'DZ 演示酒馆', '示例路 1 号', '01088886666', 1, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO category(id, store_id, name, sort, deleted) VALUES
(1, 1, '精酿啤酒', 10, 0),
(2, 1, '小食', 20, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO product(id, store_id, category_id, name, main_image, images, description,
                    status, recommended, deleted) VALUES
(1, 1, 1, '招牌小麦啤酒', '/mock/wheat.jpg', JSON_ARRAY('/mock/wheat.jpg'), '清爽小麦香气', 1, 1, 0),
(2, 1, 2, '香辣鸡翅', '/mock/wings.jpg', JSON_ARRAY('/mock/wings.jpg'), '现炸鸡翅', 1, 1, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO product_sku(id, product_id, spec_name, price_fen, stock, sales, deleted) VALUES
(1, 1, '500ml', 2800, 100, 0, 0),
(2, 2, '6只', 3600, 100, 0, 0)
ON DUPLICATE KEY UPDATE price_fen = VALUES(price_fen);

INSERT INTO recharge_tier(id, pay_fen, bonus_fen, label, status, sort, deleted) VALUES
(1, 10000, 0, '充100元', 1, 10, 0),
(2, 30000, 3000, '充300元赠30元', 1, 20, 0),
(3, 50000, 8000, '充500元赠80元', 1, 30, 0)
ON DUPLICATE KEY UPDATE label = VALUES(label);

INSERT INTO home_content(store_id, announcement, deleted)
VALUES (1, '欢迎来到 DZ 演示酒馆，当前运行在 mock 模式。', 0)
ON DUPLICATE KEY UPDATE announcement = VALUES(announcement);

INSERT INTO system_config(config_key, config_value, description, deleted) VALUES
('gameplay.description', '积分由酒馆管理员发放，用于排行榜荣誉展示。用户可提交取积分申请，审核通过后生效。', '小程序玩法说明', 0),
('points.halving.enabled', 'false', '是否启用积分定时减半', 0),
('points.halving.intervalDays', '7', '积分减半间隔自然日', 0),
('points.halving.time', '00:00', '积分减半触发时间', 0),
('points.halving.lastPeriod', '', '最后执行的自然日周期', 0),
('coupon.gift.validDays', '30', '后台赠送卡券有效天数', 0)
ON DUPLICATE KEY UPDATE description = VALUES(description);
