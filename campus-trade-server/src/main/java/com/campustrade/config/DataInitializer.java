package com.campustrade.config;

import com.campustrade.entity.User;
import com.campustrade.entity.UserRole;
import com.campustrade.mapper.UserMapper;
import com.campustrade.mapper.UserRoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
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
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initAdminUser();
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
}