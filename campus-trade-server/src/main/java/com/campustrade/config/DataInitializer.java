package com.campustrade.config;

import com.campustrade.entity.*;
import com.campustrade.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

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
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        setTimeZone();
        initRoles();
        initPermissions();
        initRolePermissions();
        initAdminUser();
        initCategories();
    }

    private void setTimeZone() {
        try {
            jdbcTemplate.execute("SET GLOBAL time_zone = '+08:00'");
            log.info("MySQL time_zone set to +08:00");
        } catch (Exception e) {
            log.warn("Failed to set MySQL time_zone: {}", e.getMessage());
        }
        fixTimezoneData();
    }

    private void fixTimezoneData() {
        try {
            String[] tables = {"t_user", "t_goods", "t_order", "t_order_item", "t_report",
                    "t_chat_message", "t_operation_log", "t_security_log", "t_goods_favorite",
                    "t_notification", "t_user_role", "t_role_permission"};
            for (String table : tables) {
                try {
                    jdbcTemplate.execute("UPDATE " + table + " SET create_time = DATE_ADD(create_time, INTERVAL 8 HOUR) WHERE create_time IS NOT NULL AND create_time < '2026-07-09 00:00:00'");
                    jdbcTemplate.execute("UPDATE " + table + " SET update_time = DATE_ADD(update_time, INTERVAL 8 HOUR) WHERE update_time IS NOT NULL AND update_time < '2026-07-09 00:00:00'");
                } catch (Exception ignored) {}
            }
            String[] extraCols = {
                    "UPDATE t_order SET pay_time = DATE_ADD(pay_time, INTERVAL 8 HOUR) WHERE pay_time IS NOT NULL AND pay_time < '2026-07-09 00:00:00'",
                    "UPDATE t_order SET ship_time = DATE_ADD(ship_time, INTERVAL 8 HOUR) WHERE ship_time IS NOT NULL AND ship_time < '2026-07-09 00:00:00'",
                    "UPDATE t_order SET finish_time = DATE_ADD(finish_time, INTERVAL 8 HOUR) WHERE finish_time IS NOT NULL AND finish_time < '2026-07-09 00:00:00'",
                    "UPDATE t_order SET cancel_time = DATE_ADD(cancel_time, INTERVAL 8 HOUR) WHERE cancel_time IS NOT NULL AND cancel_time < '2026-07-09 00:00:00'",
                    "UPDATE t_report SET handle_time = DATE_ADD(handle_time, INTERVAL 8 HOUR) WHERE handle_time IS NOT NULL AND handle_time < '2026-07-09 00:00:00'"
            };
            for (String sql : extraCols) {
                try { jdbcTemplate.execute(sql); } catch (Exception ignored) {}
            }
            log.info("Timezone data fix applied");
        } catch (Exception e) {
            log.warn("Timezone data fix failed: {}", e.getMessage());
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
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setNickname("超级管理员");
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
}
