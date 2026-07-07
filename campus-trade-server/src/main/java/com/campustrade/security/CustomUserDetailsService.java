package com.campustrade.security;

import com.campustrade.entity.User;
import com.campustrade.mapper.UserMapper;
import com.campustrade.mapper.PermissionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        if (user.getStatus() == 0) {
            throw new UsernameNotFoundException("账号已被禁用: " + username);
        }

        List<String> permissions = permissionMapper.selectPermissionCodesByUserId(user.getId());
        if (permissions == null) {
            permissions = new ArrayList<>();
        }

        return new CustomUserDetails(user, permissions);
    }
}