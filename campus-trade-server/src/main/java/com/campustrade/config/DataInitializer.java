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

import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    @Value("${admin.password:admin123}")
    private String adminPassword;

    @Autowired private UserMapper userMapper;
    @Autowired private UserRoleMapper userRoleMapper;
    @Autowired private RoleMapper roleMapper;
    @Autowired private PermissionMapper permissionMapper;
    @Autowired private RolePermissionMapper rolePermissionMapper;
    @Autowired private GoodsCategoryMapper categoryMapper;
    @Autowired private BannerMapper bannerMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private RedisTemplate<String, Object> redisTemplate;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        safeRun("initRoles", this::initRoles);
        safeRun("initPermissions", this::initPermissions);
        safeRun("initRolePermissions", this::initRolePermissions);
        safeRun("initAdminUser", this::initAdminUser);
        safeRun("initNormalUser", this::initNormalUser);
        safeRun("initCategories", this::initCategories);
        safeRun("createTables", this::createTables);
        safeRun("initBanners", this::initBanners);
        safeRun("initSystemConfigDefaults", this::initSystemConfigDefaults);
        safeRun("clearPermissionCache", this::clearPermissionCache);
        log.info("DataInitializer completed");
    }

    private void safeRun(String name, Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            log.warn("DataInitializer task '{}' failed: {}", name, e.getMessage());
        }
    }

    private void initRoles() {
        if (roleMapper.selectById(1L) == null) insertRole(1L, "超级管理员", "ROLE_SUPER_ADMIN", "系统超级管理员");
        if (roleMapper.selectById(2L) == null) insertRole(2L, "管理员", "ROLE_ADMIN", "普通管理员");
        if (roleMapper.selectById(3L) == null) insertRole(3L, "普通用户", "ROLE_USER", "注册用户");
    }

    private void insertRole(Long id, String name, String code, String desc) {
        Role role = new Role();
        role.setId(id); role.setRoleName(name); role.setRoleCode(code);
        role.setDescription(desc); role.setStatus(1);
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
        p.setId(id); p.setPermissionName(name); p.setPermissionCode(code);
        p.setResourceType(type); p.setParentId(parentId); p.setSortOrder(sort); p.setStatus(1);
        permissionMapper.insert(p);
    }

    private void initRolePermissions() {
        if (rolePermissionMapper.selectByRoleIdAndPermissionId(1L, 1L) == null) {
            Long[][] superAdminPerms = {
                {1L, 1L}, {1L, 2L}, {1L, 3L}, {1L, 4L}, {1L, 5L},
                {1L, 6L}, {1L, 7L}, {1L, 8L}, {1L, 9L}, {1L, 10L}, {1L, 11L}
            };
            for (Long[] rp : superAdminPerms) insertRolePermission(rp[0], rp[1]);
            Long[][] adminPerms = {
                {2L, 1L}, {2L, 4L}, {2L, 6L}, {2L, 7L},
                {2L, 8L}, {2L, 9L}, {2L, 10L}, {2L, 11L}
            };
            for (Long[] rp : adminPerms) insertRolePermission(rp[0], rp[1]);
        }
    }

    private void insertRolePermission(Long roleId, Long permId) {
        RolePermission rp = new RolePermission();
        rp.setRoleId(roleId); rp.setPermissionId(permId);
        rolePermissionMapper.insert(rp);
    }

    private void initAdminUser() {
        if (userMapper.selectByUsername("admin") == null) {
            User admin = new User();
            admin.setUsername("admin"); admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setNickname("超级管理员"); admin.setAvatar("/default-avatar.svg");
            admin.setStatus(1); admin.setRealVerified(0);
            userMapper.insert(admin);
            UserRole userRole = new UserRole();
            userRole.setUserId(admin.getId()); userRole.setRoleId(1L);
            userRoleMapper.insert(userRole);
            log.info("Admin user created: admin/admin123");
        }
    }

    private void initNormalUser() {
        if (userMapper.selectByUsername("user") == null) {
            User user = new User();
            user.setUsername("user"); user.setPassword(passwordEncoder.encode("user123"));
            user.setNickname("普通用户"); user.setAvatar("/default-avatar.svg");
            user.setStatus(1); user.setRealVerified(0);
            userMapper.insert(user);
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId()); userRole.setRoleId(3L);
            userRoleMapper.insert(userRole);
            log.info("Normal user created: user/user123");
        }
    }

    private void initCategories() {
        String[][] renames = {
            {"数码电子", "电脑整机"}, {"手机周边", "手机平板"},
            {"运动户外", "运动健身"},
            {"自行车电动车", "代步工具"},
            {"游戏设备", "游戏外设"}, {"书籍教材", "教材书籍"},
            {"学习资料", "考试资料"}, {"乐器音响", "乐器"},
            {"生活日用", "生活用品"}, {"箱包", "箱包配饰"},
            {"票券卡券", "虚拟票券"}
        };
        String[] toDelete = {"食品零食", "宿舍家电", "宿舍用品", "饰品配件", "虚拟商品"};
        String[][] cats = {
            {"电脑整机", "1"}, {"电脑配件", "2"}, {"手机平板", "3"},
            {"数码配件", "4"}, {"影音设备", "5"}, {"游戏外设", "6"},
            {"教材书籍", "7"}, {"考试资料", "8"}, {"文具用品", "9"},
            {"生活用品", "10"}, {"服装鞋帽", "11"}, {"美妆护肤", "12"},
            {"箱包配饰", "13"}, {"运动健身", "14"}, {"户外露营", "15"},
            {"代步工具", "16"}, {"乐器", "17"}, {"游戏娱乐", "18"},
            {"手工艺品", "19"}, {"收藏爱好", "20"}, {"植物宠物", "21"},
            {"虚拟票券", "22"}, {"技能服务", "23"}, {"跑腿代办", "24"},
            {"家教辅导", "25"}, {"搬家服务", "26"}, {"租房转租", "27"},
            {"课外读物", "28"}, {"打印复印", "29"}, {"其他", "30"}
        };
        List<GoodsCategory> existing = categoryMapper.selectAll();
        java.util.Map<String, GoodsCategory> existingMap = new java.util.HashMap<>();
        for (GoodsCategory c : existing) existingMap.put(c.getCategoryName(), c);
        for (String[] rename : renames) {
            GoodsCategory old = existingMap.get(rename[0]);
            if (old != null) {
                jdbcTemplate.update("UPDATE t_goods_category SET category_name=? WHERE id=?", rename[1], old.getId());
                old.setCategoryName(rename[1]);
                existingMap.remove(rename[0]);
                existingMap.put(rename[1], old);
            }
        }
        for (String delName : toDelete) {
            GoodsCategory del = existingMap.get(delName);
            if (del != null) {
                jdbcTemplate.update("UPDATE t_goods_category SET deleted=1 WHERE id=?", del.getId());
                existingMap.remove(delName);
            }
        }
        int added = 0;
        for (String[] cat : cats) {
            int sortOrder = Integer.parseInt(cat[1]);
            GoodsCategory existingCat = existingMap.get(cat[0]);
            if (existingCat == null) {
                GoodsCategory c = new GoodsCategory();
                c.setCategoryName(cat[0]); c.setParentId(0L);
                c.setSortOrder(sortOrder); c.setStatus(1);
                categoryMapper.insert(c);
                added++;
            } else if (existingCat.getSortOrder() == null || existingCat.getSortOrder() != sortOrder) {
                jdbcTemplate.update("UPDATE t_goods_category SET sort_order=? WHERE id=?", sortOrder, existingCat.getId());
                existingCat.setSortOrder(sortOrder);
            }
        }
        if (added > 0) log.info("Goods categories initialized: added {} new categories (total {})", added, cats.length);
    }

    private void createTables() {
        executeSql("CREATE TABLE IF NOT EXISTS t_user (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "username VARCHAR(50) NOT NULL," +
            "password VARCHAR(100) NOT NULL," +
            "nickname VARCHAR(50) DEFAULT NULL," +
            "phone VARCHAR(20) DEFAULT NULL," +
            "email VARCHAR(100) DEFAULT NULL," +
            "avatar VARCHAR(255) DEFAULT NULL," +
            "real_name VARCHAR(50) DEFAULT NULL," +
            "student_id VARCHAR(50) DEFAULT NULL," +
            "real_verified TINYINT DEFAULT 0," +
            "status TINYINT DEFAULT 1," +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            "deleted TINYINT DEFAULT 0," +
            "version INT DEFAULT 0," +
            "UNIQUE KEY uk_username (username)," +
            "KEY idx_status (status)," +
            "KEY idx_create_time (create_time)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeSql("CREATE TABLE IF NOT EXISTS t_role (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "role_name VARCHAR(50) NOT NULL," +
            "role_code VARCHAR(50) NOT NULL," +
            "description VARCHAR(200) DEFAULT NULL," +
            "status TINYINT DEFAULT 1," +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            "deleted TINYINT DEFAULT 0," +
            "version INT DEFAULT 0," +
            "UNIQUE KEY uk_role_code (role_code)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeSql("CREATE TABLE IF NOT EXISTS t_permission (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "permission_name VARCHAR(50) NOT NULL," +
            "permission_code VARCHAR(50) NOT NULL," +
            "resource_type TINYINT DEFAULT 1," +
            "parent_id BIGINT DEFAULT 0," +
            "sort_order INT DEFAULT 0," +
            "status TINYINT DEFAULT 1," +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            "deleted TINYINT DEFAULT 0," +
            "version INT DEFAULT 0," +
            "UNIQUE KEY uk_permission_code (permission_code)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeSql("CREATE TABLE IF NOT EXISTS t_user_role (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "user_id BIGINT NOT NULL," +
            "role_id BIGINT NOT NULL," +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            "deleted TINYINT DEFAULT 0," +
            "version INT DEFAULT 0," +
            "KEY idx_user_id (user_id)," +
            "KEY idx_role_id (role_id)," +
            "UNIQUE KEY uk_user_role (user_id, role_id)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeSql("CREATE TABLE IF NOT EXISTS t_role_permission (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "role_id BIGINT NOT NULL," +
            "permission_id BIGINT NOT NULL," +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            "deleted TINYINT DEFAULT 0," +
            "version INT DEFAULT 0," +
            "KEY idx_role_id (role_id)," +
            "KEY idx_permission_id (permission_id)," +
            "UNIQUE KEY uk_role_permission (role_id, permission_id)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeSql("CREATE TABLE IF NOT EXISTS t_goods_category (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "category_name VARCHAR(50) NOT NULL," +
            "parent_id BIGINT DEFAULT 0," +
            "sort_order INT DEFAULT 0," +
            "icon VARCHAR(255) DEFAULT NULL," +
            "status TINYINT DEFAULT 1," +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            "deleted TINYINT DEFAULT 0," +
            "version INT DEFAULT 0," +
            "KEY idx_parent_id (parent_id)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeSql("CREATE TABLE IF NOT EXISTS t_goods (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "user_id BIGINT NOT NULL," +
            "category_id BIGINT NOT NULL," +
            "title VARCHAR(200) NOT NULL," +
            "description TEXT," +
            "price DECIMAL(10,2) NOT NULL," +
            "original_price DECIMAL(10,2) DEFAULT NULL," +
            "`condition` VARCHAR(20) DEFAULT NULL," +
            "cover_image VARCHAR(255) DEFAULT NULL," +
            "images TEXT," +
            "status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'," +
            "reject_reason VARCHAR(500) DEFAULT NULL," +
            "view_count INT DEFAULT 0," +
            "favorite_count INT DEFAULT 0," +
            "stock INT DEFAULT 1," +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            "deleted TINYINT DEFAULT 0," +
            "version INT DEFAULT 0," +
            "KEY idx_user_id (user_id)," +
            "KEY idx_category_id (category_id)," +
            "KEY idx_status (status)," +
            "KEY idx_create_time (create_time)," +
            "KEY idx_user_status (user_id, status)," +
            "KEY idx_goods_status (id, status)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeSql("CREATE TABLE IF NOT EXISTS t_goods_favorite (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "user_id BIGINT NOT NULL," +
            "goods_id BIGINT NOT NULL," +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            "deleted TINYINT DEFAULT 0," +
            "version INT DEFAULT 0," +
            "KEY idx_user_id (user_id)," +
            "KEY idx_goods_id (goods_id)," +
            "UNIQUE KEY uk_user_goods (user_id, goods_id)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeSql("CREATE TABLE IF NOT EXISTS t_order (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "order_no VARCHAR(64) NOT NULL," +
            "buyer_id BIGINT NOT NULL," +
            "seller_id BIGINT NOT NULL," +
            "total_amount DECIMAL(10,2) NOT NULL," +
            "status VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAY'," +
            "remark VARCHAR(500) DEFAULT NULL," +
            "delivery_method TINYINT DEFAULT 1," +
            "address VARCHAR(500) DEFAULT NULL," +
            "tracking_no VARCHAR(64) DEFAULT NULL," +
            "trade_no VARCHAR(64) DEFAULT NULL," +
            "pre_refund_status VARCHAR(20) DEFAULT NULL," +
            "seller_payment_config_id BIGINT DEFAULT NULL," +
            "pay_time DATETIME DEFAULT NULL," +
            "ship_time DATETIME DEFAULT NULL," +
            "finish_time DATETIME DEFAULT NULL," +
            "cancel_time DATETIME DEFAULT NULL," +
            "cancel_reason VARCHAR(500) DEFAULT NULL," +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            "deleted TINYINT DEFAULT 0," +
            "version INT DEFAULT 0," +
            "UNIQUE KEY uk_order_no (order_no)," +
            "KEY idx_buyer_id (buyer_id)," +
            "KEY idx_seller_id (seller_id)," +
            "KEY idx_status (status)," +
            "KEY idx_create_time (create_time)," +
            "KEY idx_buyer_status (buyer_id, status)," +
            "KEY idx_seller_status (seller_id, status)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeSql("CREATE TABLE IF NOT EXISTS t_order_item (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "order_id BIGINT NOT NULL," +
            "goods_id BIGINT NOT NULL," +
            "goods_title VARCHAR(200) NOT NULL," +
            "goods_image VARCHAR(255) DEFAULT NULL," +
            "price DECIMAL(10,2) NOT NULL," +
            "quantity INT DEFAULT 1," +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            "deleted TINYINT DEFAULT 0," +
            "version INT DEFAULT 0," +
            "KEY idx_order_id (order_id)," +
            "KEY idx_goods_id (goods_id)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeSql("CREATE TABLE IF NOT EXISTS t_chat_message (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "sender_id BIGINT NOT NULL," +
            "receiver_id BIGINT NOT NULL," +
            "content TEXT NOT NULL," +
            "message_type TINYINT DEFAULT 1," +
            "is_read TINYINT DEFAULT 0," +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            "deleted TINYINT DEFAULT 0," +
            "version INT DEFAULT 0," +
            "KEY idx_sender_id (sender_id)," +
            "KEY idx_receiver_id (receiver_id)," +
            "KEY idx_create_time (create_time)," +
            "KEY idx_sender_receiver (sender_id, receiver_id)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeSql("CREATE TABLE IF NOT EXISTS t_report (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "reporter_id BIGINT NOT NULL," +
            "target_type TINYINT NOT NULL," +
            "target_id BIGINT NOT NULL," +
            "reason VARCHAR(500) NOT NULL," +
            "description TEXT," +
            "images TEXT," +
            "status VARCHAR(20) NOT NULL DEFAULT 'PENDING'," +
            "handler_id BIGINT DEFAULT NULL," +
            "handle_result VARCHAR(500) DEFAULT NULL," +
            "handle_time DATETIME DEFAULT NULL," +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            "deleted TINYINT DEFAULT 0," +
            "version INT DEFAULT 0," +
            "KEY idx_reporter_id (reporter_id)," +
            "KEY idx_target (target_type, target_id)," +
            "KEY idx_status (status)," +
            "KEY idx_create_time (create_time)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeSql("CREATE TABLE IF NOT EXISTS t_notification (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "user_id BIGINT NOT NULL," +
            "title VARCHAR(200) NOT NULL," +
            "content TEXT," +
            "notification_type VARCHAR(20) NOT NULL DEFAULT 'SYSTEM'," +
            "related_id BIGINT DEFAULT NULL," +
            "is_read TINYINT DEFAULT 0," +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            "deleted TINYINT DEFAULT 0," +
            "version INT DEFAULT 0," +
            "KEY idx_user_id (user_id)," +
            "KEY idx_status (is_read)," +
            "KEY idx_create_time (create_time)," +
            "KEY idx_user_read (user_id, is_read)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeSql("CREATE TABLE IF NOT EXISTS t_operation_log (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "user_id BIGINT DEFAULT NULL," +
            "username VARCHAR(50) DEFAULT NULL," +
            "module VARCHAR(50) NOT NULL," +
            "operation VARCHAR(200) NOT NULL," +
            "method VARCHAR(200) DEFAULT NULL," +
            "request_url VARCHAR(255) DEFAULT NULL," +
            "request_params TEXT," +
            "response_result TEXT," +
            "ip VARCHAR(50) DEFAULT NULL," +
            "duration BIGINT DEFAULT NULL," +
            "status TINYINT DEFAULT 1," +
            "error_msg TEXT," +
            "trace_id VARCHAR(64) DEFAULT NULL," +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            "deleted TINYINT DEFAULT 0," +
            "version INT DEFAULT 0," +
            "KEY idx_user_id (user_id)," +
            "KEY idx_module (module)," +
            "KEY idx_create_time (create_time)," +
            "KEY idx_trace_id (trace_id)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeSql("CREATE TABLE IF NOT EXISTS t_security_log (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "user_id BIGINT DEFAULT NULL," +
            "username VARCHAR(50) DEFAULT NULL," +
            "event_type VARCHAR(50) NOT NULL," +
            "ip VARCHAR(50) DEFAULT NULL," +
            "detail TEXT," +
            "trace_id VARCHAR(64) DEFAULT NULL," +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            "deleted TINYINT DEFAULT 0," +
            "version INT DEFAULT 0," +
            "KEY idx_user_id (user_id)," +
            "KEY idx_event_type (event_type)," +
            "KEY idx_create_time (create_time)," +
            "KEY idx_trace_id (trace_id)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeSql("CREATE TABLE IF NOT EXISTS t_banner (" +
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
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeSql("CREATE TABLE IF NOT EXISTS t_announcement (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "title VARCHAR(200) NOT NULL," +
            "content TEXT," +
            "status TINYINT DEFAULT 1," +
            "sort_order INT DEFAULT 0," +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            "deleted TINYINT DEFAULT 0," +
            "version INT DEFAULT 0" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeSql("CREATE TABLE IF NOT EXISTS t_user_blacklist (" +
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
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeSql("CREATE TABLE IF NOT EXISTS t_seller_rating (" +
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
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeSql("CREATE TABLE IF NOT EXISTS t_user_follow (" +
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
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeSql("CREATE TABLE IF NOT EXISTS t_notification_preference (" +
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
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeSql("CREATE TABLE IF NOT EXISTS t_cart (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "user_id BIGINT NOT NULL," +
            "goods_id BIGINT NOT NULL," +
            "quantity INT DEFAULT 1," +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            "deleted TINYINT DEFAULT 0," +
            "version INT DEFAULT 0," +
            "KEY idx_user_id (user_id)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeSql("CREATE TABLE IF NOT EXISTS t_delivery_address (" +
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
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeSql("CREATE TABLE IF NOT EXISTS t_payment_config (" +
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
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeSql("CREATE TABLE IF NOT EXISTS t_fund_log (" +
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
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        executeSql("CREATE TABLE IF NOT EXISTS t_system_config (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "config_key VARCHAR(100) NOT NULL," +
            "config_value TEXT DEFAULT NULL," +
            "description VARCHAR(200) DEFAULT NULL," +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
            "UNIQUE KEY uk_config_key (config_key)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        addColumnIfNotExists("t_goods", "condition", "VARCHAR(20) DEFAULT NULL COMMENT '成色' AFTER original_price");
        addColumnIfNotExists("t_goods", "stock", "INT DEFAULT 1 COMMENT '库存' AFTER favorite_count");
        addColumnIfNotExists("t_order_item", "quantity", "INT DEFAULT 1 COMMENT '数量' AFTER price");
        addColumnIfNotExists("t_order", "delivery_method", "TINYINT DEFAULT 1 COMMENT '配送方式' AFTER remark");
        addColumnIfNotExists("t_order", "address", "VARCHAR(500) DEFAULT NULL COMMENT '收货地址' AFTER delivery_method");
        addColumnIfNotExists("t_order", "tracking_no", "VARCHAR(64) DEFAULT NULL COMMENT '物流单号'");
        addColumnIfNotExists("t_order", "trade_no", "VARCHAR(64) DEFAULT NULL COMMENT '支付宝交易号'");
        addColumnIfNotExists("t_order", "pre_refund_status", "VARCHAR(20) DEFAULT NULL COMMENT '退款前状态'");
        addColumnIfNotExists("t_order", "seller_payment_config_id", "BIGINT DEFAULT NULL COMMENT '卖家收款配置ID'");

        try { jdbcTemplate.execute("UPDATE t_goods SET stock = 1 WHERE stock IS NULL"); } catch (Exception ignored) {}
        try { jdbcTemplate.execute("UPDATE t_order_item SET quantity = 1 WHERE quantity IS NULL"); } catch (Exception ignored) {}

        log.info("All tables and columns verified");
    }

    private void executeSql(String sql) {
        try { jdbcTemplate.execute(sql); } catch (Exception e) { log.debug("SQL skipped: {}", e.getMessage()); }
    }

    private void addColumnIfNotExists(String table, String column, String definition) {
        try { jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition); log.info("Added column {}.{}", table, column); }
        catch (Exception e) { log.debug("Column {}.{} already exists", table, column); }
    }

    private void initBanners() {
        Long count = bannerMapper.selectCount();
        if (count == null || count == 0) {
            insertBanner("CampusTrade 校园贸易平台", "安全 · 便捷 · 值得信赖的校园闲置好物流转平台", null, "/goods", "linear-gradient(135deg, #6366f1 0%, #8b5cf6 50%, #a78bfa 100%)", "浏览商品", null, 1, 1);
            insertBanner("闲置好物 低价淘", "学长学姐的优质好物，超值价格等你来", null, "/goods", "linear-gradient(135deg, #ec4899, #8b5cf6)", "立即淘宝", null, 2, 1);
            insertBanner("发布闲置 轻松变现", "一键发布，快速找到买家，让闲置不再闲置", null, "/goods/publish", "linear-gradient(135deg, #dc2626 0%, #f97316 50%, #fbbf24 100%)", "发布商品", null, 3, 1);
            log.info("Default banners initialized");
        }
        try { jdbcTemplate.update("UPDATE t_banner SET title = 'CampusTrade 校园贸易平台' WHERE title LIKE '%校园二手交易%'"); } catch (Exception ignored) {}
    }

    private void insertBanner(String title, String subtitle, String imageUrl, String linkUrl, String bgColor, String buttonText, String buttonColor, int sortOrder, int status) {
        Banner banner = new Banner();
        banner.setTitle(title); banner.setSubtitle(subtitle); banner.setImageUrl(imageUrl);
        banner.setLinkUrl(linkUrl); banner.setBgColor(bgColor); banner.setButtonText(buttonText);
        banner.setButtonColor(buttonColor); banner.setSortOrder(sortOrder); banner.setStatus(status);
        bannerMapper.insert(banner);
    }

    private void initSystemConfigDefaults() {
        String[][] defaults = {
            {"alipay.app_id", "", "支付宝应用ID"},
            {"alipay.private_key", "", "支付宝应用私钥"},
            {"alipay.alipay_public_key", "", "支付宝公钥"},
            {"alipay.gateway", "https://openapi-sandbox.dl.alipaydev.com/gateway.do", "支付宝网关"},
            {"alipay.notify_url", "", "支付宝异步通知URL"},
            {"alipay.return_url", "", "支付宝同步跳转URL"},
            {"mail.host", "smtp.qq.com", "SMTP服务器地址"},
            {"mail.port", "465", "SMTP服务器端口"},
            {"mail.username", "", "发件人邮箱地址"},
            {"mail.password", "", "发件人邮箱授权码"},
            {"mail.from", "CampusTrade校园贸易", "发件人显示名称"},
            {"mail.ssl", "true", "是否启用SSL"}
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

    private void clearPermissionCache() {
        try {
            Set<String> keys = redisTemplate.keys("permissions:*");
            if (keys != null && !keys.isEmpty()) redisTemplate.delete(keys);
        } catch (Exception ignored) {}
    }
}
