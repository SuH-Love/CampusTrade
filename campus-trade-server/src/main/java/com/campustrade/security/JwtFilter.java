package com.campustrade.security;

import com.campustrade.constant.RedisConstant;
import com.campustrade.util.JwtUtil;
import com.campustrade.util.TraceIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        TraceIdUtil.generate();
        String token = getTokenFromRequest(request);

        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            String blacklistKey = RedisConstant.BLACKLIST_PREFIX + token;
            Boolean isBlacklisted = redisTemplate.hasKey(blacklistKey);
            if (isBlacklisted != null && isBlacklisted) {
                filterChain.doFilter(request, response);
                return;
            }

            Long userId = jwtUtil.getUserIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);

            String banKey = "ban:user:" + userId;
            Boolean isBanned = redisTemplate.hasKey(banKey);
            if (isBanned != null && isBanned) {
                filterChain.doFilter(request, response);
                return;
            }

            List<SimpleGrantedAuthority> grantedAuthorities = List.of();
            try {
                @SuppressWarnings("unchecked")
                List<String> authorities = (List<String>) redisTemplate.opsForValue().get(RedisConstant.PERMISSIONS_PREFIX + userId);
                if (authorities != null) {
                    grantedAuthorities = authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
                }
            } catch (Exception e) {
                log.warn("Failed to load permissions from Redis for userId={}: {}", userId, e.getMessage());
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, grantedAuthorities);
            authentication.setDetails(username);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
        TraceIdUtil.remove();
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}