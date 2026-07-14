package com.campustrade.config;

import com.campustrade.entity.*;
import com.campustrade.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    @Value("${admin.password:admin123}")
    private String adminPassword;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private PermissionMapper permissionMapper;
    @Autowired
    private RolePermissionMapper rolePermissionMapper;
    @Autowired
    private GoodsCategoryMapper categoryMapper;
    @Autowired
    private BannerMapper bannerMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        safeRun("initRoles", this::initRoles);
        safeRun("initPermissions", this::initPermissions);
        safeRun("initRolePermissions", this::initRolePermissions);
        safeRun("initAdminUser", this::initAdminUser);
        safeRun("initNormalUser", this::initNormalUser);
        safeRun("initCategories", this::initCategories);
        safeRun("createBannerTableIfNeeded", this::createBannerTableIfNeeded);
        safeRun("initBanners", this::initBanners);
        safeRun("alterGoodsTableAddCondition", this::alterGoodsTableAddCondition);
        safeRun("alterOrderItemTableAddQuantity", this::alterOrderItemTableAddQuantity);
        safeRun("alterOrderTableAddDelivery", this::alterOrderTableAddDelivery);
        safeRun("createSellerRatingTableIfNeeded", this::createSellerRatingTableIfNeeded);
        safeRun("createUserFollowTableIfNeeded", this::createUserFollowTableIfNeeded);
        safeRun("createNotificationPreferenceTableIfNeeded", this::createNotificationPreferenceTableIfNeeded);
        safeRun("createCartTableIfNeeded", this::createCartTableIfNeeded);
        safeRun("createDeliveryAddressTableIfNeeded", this::createDeliveryAddressTableIfNeeded);
        safeRun("createPaymentConfigTableIfNeeded", this::createPaymentConfigTableIfNeeded);
        safeRun("createFundLogTableIfNeeded", this::createFundLogTableIfNeeded);
        safeRun("alterOrderTableAddPaymentFields", this::alterOrderTableAddPaymentFields);
        safeRun("createSystemConfigTableIfNeeded", this::createSystemConfigTableIfNeeded);
        safeRun("initSystemConfigDefaults", this::initSystemConfigDefaults);
        safeRun("clearPermissionCache", this::clearPermissionCache);
    }

    private void safeRun(String name, Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            log.warn("DataInitializer task '{}' failed: {}", name, e.getMessage());
        }
    }
    private void initRoles() {
        if (roleMapper.selectById(1L) == null) {
            insertRole(1L, "超级管理员", "ROLE_SUPER_ADMIN", "系统超级管理员");
        }
        if (roleMapper.selectById(2L) == null) {
            insertRole(2L, "管理员", "ROLE_ADMIN", "普通管理员");
        }
        if (roleMapper.selectById(3L) == null) {
            insertRole(3L, "普通用户", "ROLE_USER", "注册用户");
        }
    }

    private void insertRole(Long id, String name, String code, String desc) {
        Role role = new Role();
        role.setId(id);
        role.setRoleName(name);
        role.setRoleCode(code);
        role.setDescription(desc);
        role.setStatus(1);
        roleMapper.insert(role);
    }

    private void initPermissions() {
        if (permissionMapper.selectById(1L) == null) {
            insertPerm(1L, "商品管理", "goods:manage", 1, 0L, 1);
            insertPerm(2L, "商品创建", "goods:create", 2, 1L, 1);
            insertPerm(3L, "商品修改", "goods:update", 2, 1L, 2);
            insertPerm(4L, "商品删除", "goods:delete", 2, 1L, 3);
            insertPerm(5L, "商品审核", "goods:audit", 2, 1L, 4);
            insertPerm(6L, "用户管理", "user:manage", 1, 0L, 2);
            insertPerm(7L, "用户封禁", "user:ban", 2, 6L, 1);
            insertPerm(8L, "举报管理", "report:manage", 1, 0L, 3);
            insertPerm(9L, "举报审核", "report:review", 2, 8L, 1);
            insertPerm(10L, "日志管理", "log:manage", 1, 0L, 4);
            insertPerm(11L, "日志查看", "log:view", 2, 10L, 1);
        }
    }

    private void insertPerm(Long id, String name, String code, int type, Long parentId, int sort) {
        Permission p = new Permission();
        p.setId(id);
        p.setPermissionName(name);
        p.setPermissionCode(code);
        p.setResourceType(type);
        p.setParentId(parentId);
        p.setSortOrder(sort);
        p.setStatus(1);
        permissionMapper.insert(p);
    }

    private void initRolePermissions() {
        if (rolePermissionMapper.selectByRoleIdAndPermissionId(1L, 1L) == null) {
            Long[][] superAdminPerms = {
                    {1L, 1L}, {1L, 2L}, {1L, 3L}, {1L, 4L}, {1L, 5L},
                    {1L, 6L}, {1L, 7L}, {1L, 8L}, {1L, 9L}, {1L, 10L}, {1L, 11L}
            };
            for (Long[] rp : superAdminPerms) {
                insertRolePermission(rp[0], rp[1]);
            }
            Long[][] adminPerms = {
                    {2L, 1L}, {2L, 4L}, {2L, 6L}, {2L, 7L},
                    {2L, 8L}, {2L, 9L}, {2L, 10L}, {2L, 11L}
            };
            for (Long[] rp : adminPerms) {
                insertRolePermission(rp[0], rp[1]);
            }
        }
    }

    private void insertRolePermission(Long roleId, Long permId) {
        RolePermission rp = new RolePermission();
        rp.setRoleId(roleId);
        rp.setPermissionId(permId);
        rolePermissionMapper.insert(rp);
    }

    private void initAdminUser() {
        User admin = userMapper.selectByUsername("admin");
        if (admin == null) {
            admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setNickname("超级管理员");
            admin.setAvatar("/default-avatar.svg");
            admin.setStatus(1);
            admin.setRealVerified(0);
            userMapper.insert(admin);

            UserRole userRole = new UserRole();
            userRole.setUserId(admin.getId());
            userRole.setRoleId(1L);
            userRoleMapper.insert(userRole);

            log.info("Admin user created: admin/admin123");
        }
    }

    private void initNormalUser() {
        User user = userMapper.selectByUsername("user");
        if (user == null) {
            user = new User();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setNickname("普通用户");
            user.setAvatar("/default-avatar.svg");
            user.setStatus(1);
            user.setRealVerified(0);
            userMapper.insert(user);

            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(3L);
            userRoleMapper.insert(userRole);

            log.info("Normal user created: user/user123");
        }
    }

    private void initCategories() {
        Long count = categoryMapper.selectCountAll();
        if (count == null || count == 0) {
            String[][] cats = {{"数码电子", "1"}, {"书籍教材", "2"}, {"生活用品", "3"}, {"服装鞋帽", "4"}, {"运动户外", "5"}, {"其他", "6"}};
            for (String[] cat : cats) {
                GoodsCategory c = new GoodsCategory();
                c.setCategoryName(cat[0]);
                c.setParentId(0L);
                c.setSortOrder(Integer.parseInt(cat[1]));
                c.setStatus(1);
                categoryMapper.insert(c);
            }
            log.info("Goods categories initialized");
        }
    }

    private void createBannerTableIfNeeded() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS t_banner (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "title VARCHAR(200) NOT NULL," +
                "subtitle VARCHAR(500) DEFAULT NULL," +
                "image_url VARCHAR(255) DEFAULT NULL," +
                "link_url VARCHAR(255) DEFAULT NULL," +
                "bg_color VARCHAR(500) DEFAULT NULL," +
                "button_text VARCHAR(50) DEFAULT NULL," +
                "button_color VARCHAR(50) DEFAULT NULL," +
                "sort_order INT DEFAULT 0," +
                "status TINYINT DEFAULT 1," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "version INT DEFAULT 0," +
                "KEY idx_sort_order (sort_order)," +
                "KEY idx_status (status)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            alterBannerTableAddColumn("button_text", "VARCHAR(50) DEFAULT NULL");
            alterBannerTableAddColumn("button_color", "VARCHAR(50) DEFAULT NULL");
            log.info("t_banner table ready");
        } catch (Exception e) {
            log.warn("Create t_banner table failed: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS t_announcement (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "title VARCHAR(200) NOT NULL," +
                "content TEXT," +
                "status TINYINT DEFAULT 1," +
                "sort_order INT DEFAULT 0," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "version INT DEFAULT 0" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            log.info("t_announcement table ready");
        } catch (Exception e) {
            log.warn("Create t_announcement table failed: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS t_user_blacklist (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "user_id BIGINT NOT NULL," +
                "blocked_id BIGINT NOT NULL," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "version INT DEFAULT 0," +
                "KEY idx_user_id (user_id)," +
                "KEY idx_blocked_id (blocked_id)," +
                "UNIQUE KEY uk_user_blocked (user_id, blocked_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            log.info("t_user_blacklist table ready");
        } catch (Exception e) {
            log.warn("Create t_user_blacklist table failed: {}", e.getMessage());
        }
        try {
            jdbcTemplate.execute("ALTER TABLE t_user_blacklist ADD UNIQUE KEY uk_user_blocked (user_id, blocked_id)");
        } catch (Exception ignored) {}
    }

    private void alterBannerTableAddColumn(String column, String definition) {
        try {
            jdbcTemplate.execute("ALTER TABLE t_banner ADD COLUMN " + column + " " + definition);
        } catch (Exception ignored) {}
    }

    private void initBanners() {
        Long count = bannerMapper.selectCount();
        if (count == null || count == 0) {
            insertBanner("校园二手交易平台", "安全 · 便捷 · 值得信赖的校园闲置好物流转平台", null, "/goods", "linear-gradient(135deg, #6366f1 0%, #8b5cf6 50%, #a78bfa 100%)", "浏览商品", null, 1, 1);
            insertBanner("闲置好物 低价淘", "学长学姐的优质好物，超值价格等你来", null, "/goods", "linear-gradient(135deg, #059669 0%, #10b981 50%, #34d399 100%)", "立即淘宝", null, 2, 1);
            insertBanner("发布闲置 轻松变现", "一键发布，快速找到买家，让闲置不再闲置", null, "/goods/publish", "linear-gradient(135deg, #dc2626 0%, #f97316 50%, #fbbf24 100%)", "发布商品", null, 3, 1);
            log.info("Default banners initialized");
        }
    }

    private void insertBanner(String title, String subtitle, String imageUrl, String linkUrl, String bgColor, String buttonText, String buttonColor, int sortOrder, int status) {
        Banner banner = new Banner();
        banner.setTitle(title);
        banner.setSubtitle(subtitle);
        banner.setImageUrl(imageUrl);
        banner.setLinkUrl(linkUrl);
        banner.setBgColor(bgColor);
        banner.setButtonText(buttonText);
        banner.setButtonColor(buttonColor);
        banner.setSortOrder(sortOrder);
        banner.setStatus(status);
        bannerMapper.insert(banner);
    }

    private void alterGoodsTableAddCondition() {
        try { jdbcTemplate.execute("ALTER TABLE t_goods ADD COLUMN `condition` VARCHAR(20) DEFAULT NULL COMMENT '成色' AFTER original_price"); } catch (Exception ignored) {}
        try { jdbcTemplate.execute("ALTER TABLE t_goods ADD COLUMN stock INT DEFAULT 1 COMMENT '库存' AFTER favorite_count"); } catch (Exception ignored) {}
        try { jdbcTemplate.execute("UPDATE t_goods SET stock = 1 WHERE stock IS NULL"); } catch (Exception ignored) {}
    }

    private void alterOrderItemTableAddQuantity() {
        try { jdbcTemplate.execute("ALTER TABLE t_order_item ADD COLUMN quantity INT DEFAULT 1 COMMENT '数量' AFTER price"); } catch (Exception ignored) {}
        try { jdbcTemplate.execute("UPDATE t_order_item SET quantity = 1 WHERE quantity IS NULL"); } catch (Exception ignored) {}
    }

    private void alterOrderTableAddDelivery() {
        try { jdbcTemplate.execute("ALTER TABLE t_order ADD COLUMN delivery_method TINYINT DEFAULT 1 COMMENT '配送方式1自取2配送' AFTER remark"); } catch (Exception ignored) {}
        try { jdbcTemplate.execute("ALTER TABLE t_order ADD COLUMN address VARCHAR(500) DEFAULT NULL COMMENT '收货地址' AFTER delivery_method"); } catch (Exception ignored) {}
    }

    private void createSellerRatingTableIfNeeded() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS t_seller_rating (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "order_id BIGINT NOT NULL," +
                "buyer_id BIGINT NOT NULL," +
                "seller_id BIGINT NOT NULL," +
                "rating TINYINT NOT NULL," +
                "comment VARCHAR(500) DEFAULT NULL," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "version INT DEFAULT 0," +
                "KEY idx_order_id (order_id)," +
                "KEY idx_seller_id (seller_id)," +
                "UNIQUE KEY uk_order_rating (order_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
        } catch (Exception e) { log.warn("Create t_seller_rating failed: {}", e.getMessage()); }
    }

    private void createUserFollowTableIfNeeded() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS t_user_follow (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "follower_id BIGINT NOT NULL," +
                "following_id BIGINT NOT NULL," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "version INT DEFAULT 0," +
                "KEY idx_follower_id (follower_id)," +
                "KEY idx_following_id (following_id)," +
                "UNIQUE KEY uk_follower_following (follower_id, following_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
        } catch (Exception e) { log.warn("Create t_user_follow failed: {}", e.getMessage()); }
        try {
            jdbcTemplate.execute("ALTER TABLE t_user_follow ADD UNIQUE KEY uk_follower_following (follower_id, following_id)");
        } catch (Exception ignored) {}
    }

    private void createNotificationPreferenceTableIfNeeded() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS t_notification_preference (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "user_id BIGINT NOT NULL," +
                "notification_type VARCHAR(20) NOT NULL," +
                "enabled TINYINT DEFAULT 1," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "version INT DEFAULT 0," +
                "KEY idx_user_id (user_id)," +
                "UNIQUE KEY uk_user_type (user_id, notification_type)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
        } catch (Exception e) { log.warn("Create t_notification_preference failed: {}", e.getMessage()); }
    }

    private void createCartTableIfNeeded() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS t_cart (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "user_id BIGINT NOT NULL," +
                "goods_id BIGINT NOT NULL," +
                "quantity INT DEFAULT 1," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "version INT DEFAULT 0," +
                "KEY idx_user_id (user_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
        } catch (Exception e) { log.warn("Create t_cart failed: {}", e.getMessage()); }
    }

    private void createDeliveryAddressTableIfNeeded() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS t_delivery_address (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "user_id BIGINT NOT NULL," +
                "receiver_name VARCHAR(50) NOT NULL," +
                "receiver_phone VARCHAR(20) NOT NULL," +
                "province VARCHAR(50) DEFAULT NULL," +
                "city VARCHAR(50) DEFAULT NULL," +
                "district VARCHAR(50) DEFAULT NULL," +
                "detail_address VARCHAR(200) NOT NULL," +
                "is_default TINYINT DEFAULT 0," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "version INT DEFAULT 0," +
                "KEY idx_user_id (user_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
        } catch (Exception e) { log.warn("Create t_delivery_address failed: {}", e.getMessage()); }
    }

    private void clearPermissionCache() {
        try {
            Set<String> keys = redisTemplate.keys("permissions:*");
            if (keys != null && !keys.isEmpty()) redisTemplate.delete(keys);
        } catch (Exception ignored) {}
    }

    private void createPaymentConfigTableIfNeeded() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS t_payment_config (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "user_id BIGINT NOT NULL," +
                "payment_type VARCHAR(20) NOT NULL DEFAULT 'ALIPAY'," +
                "alipay_account VARCHAR(100) DEFAULT NULL," +
                "real_name VARCHAR(50) DEFAULT NULL," +
                "is_default TINYINT DEFAULT 0," +
                "status VARCHAR(20) DEFAULT 'ACTIVE'," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "deleted TINYINT DEFAULT 0," +
                "version INT DEFAULT 0," +
                "KEY idx_user_id (user_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
        } catch (Exception e) { log.warn("Create t_payment_config failed: {}", e.getMessage()); }
    }

    private void createFundLogTableIfNeeded() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS t_fund_log (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "order_id BIGINT NOT NULL," +
                "user_id BIGINT NOT NULL," +
                "amount DECIMAL(10,2) NOT NULL," +
                "type VARCHAR(20) NOT NULL," +
                "status VARCHAR(20) DEFAULT 'SUCCESS'," +
                "trade_no VARCHAR(64) DEFAULT NULL," +
                "remark VARCHAR(500) DEFAULT NULL," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "KEY idx_order_id (order_id)," +
                "KEY idx_user_id (user_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
        } catch (Exception e) { log.warn("Create t_fund_log failed: {}", e.getMessage()); }
    }

    private void alterOrderTableAddPaymentFields() {
        try { jdbcTemplate.execute("ALTER TABLE t_order ADD COLUMN trade_no VARCHAR(64) DEFAULT NULL COMMENT '支付宝交易号' AFTER tracking_no"); log.info("Added column trade_no to t_order"); } catch (Exception e) { log.info("Column trade_no already exists or add failed: {}", e.getMessage()); }
        try { jdbcTemplate.execute("ALTER TABLE t_order ADD COLUMN pre_refund_status VARCHAR(20) DEFAULT NULL COMMENT '退款前状态' AFTER trade_no"); log.info("Added column pre_refund_status to t_order"); } catch (Exception e) { log.info("Column pre_refund_status already exists or add failed: {}", e.getMessage()); }
        try { jdbcTemplate.execute("ALTER TABLE t_order ADD COLUMN seller_payment_config_id BIGINT DEFAULT NULL COMMENT '卖家收款配置ID' AFTER pre_refund_status"); log.info("Added column seller_payment_config_id to t_order"); } catch (Exception e) { log.info("Column seller_payment_config_id already exists or add failed: {}", e.getMessage()); }
    }

    private void createSystemConfigTableIfNeeded() {
        try {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS t_system_config (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "config_key VARCHAR(100) NOT NULL," +
                "config_value TEXT DEFAULT NULL," +
                "description VARCHAR(200) DEFAULT NULL," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "UNIQUE KEY uk_config_key (config_key)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
        } catch (Exception e) { log.warn("Create t_system_config failed: {}", e.getMessage()); }
    }

    private void initSystemConfigDefaults() {
        String[][] defaults = {
            {"alipay.app_id", "", "支付宝应用ID"},
            {"alipay.private_key", "", "支付宝应用私钥"},
            {"alipay.alipay_public_key", "", "支付宝公钥"},
            {"alipay.gateway", "https://openapi-sandbox.dl.alipaydev.com/gateway.do", "支付宝网关"},
            {"alipay.notify_url", "", "支付宝异步通知URL"},
            {"alipay.return_url", "", "支付宝同步跳转URL"}
        };
        for (String[] item : defaults) {
            try {
                Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM t_system_config WHERE config_key = ?", Integer.class, item[0]);
                if (count != null && count == 0) {
                    jdbcTemplate.update(
                        "INSERT INTO t_system_config (config_key, config_value, description) VALUES (?, ?, ?)",
                        item[0], item[1], item[2]);
                }
            } catch (Exception ignored) {}
        }
    }
}
