package com.campustrade.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Slf4j

public class XssFilter implements Filter {

    private static final Pattern[] XSS_PATTERNS = {
            Pattern.compile("<script.*?>.*?</script>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE),
            Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<iframe.*?>.*?</iframe>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<img.*?on.*?=.*?>", Pattern.CASE_INSENSITIVE)
    };

    private static final Pattern[] SQL_INJECTION_PATTERNS = {
            Pattern.compile("(?i)(\\b(select|insert|update|delete|drop|truncate|exec|execute|union|create|alter|grant|revoke)\\b)",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)(\\b(or|and)\\s+\\d+\\s*=\\s*\\d+\\b)"),
            Pattern.compile("(?i)(\\b(or|and)\\s+['\"]\\w+['\"]\\s*=\\s*['\"]\\w+['\"]\\b)"),
            Pattern.compile("(?i)(--|;|/\\*|\\*/|xp_|sp_)")
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        chain.doFilter(new XssHttpServletRequestWrapper((HttpServletRequest) request), response);
    }

    private static String stripXssAndSql(String value) {
        if (value == null) return null;
        String result = value;
        for (Pattern pattern : XSS_PATTERNS) {
            result = pattern.matcher(result).replaceAll("");
        }
        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            if (pattern.matcher(result).find()) {
                log.warn("检测到SQL注入攻击: {}", result);
                result = pattern.matcher(result).replaceAll("");
            }
        }
        return result;
    }

    static class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

        public XssHttpServletRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            return stripXssAndSql(super.getParameter(name));
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) return null;
            String[] cleaned = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                cleaned[i] = stripXssAndSql(values[i]);
            }
            return cleaned;
        }

        @Override
        public String getHeader(String name) {
            return stripXssAndSql(super.getHeader(name));
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream()));
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            String body = StreamUtils.copyToString(super.getInputStream(), StandardCharsets.UTF_8);
            String cleaned = stripXssAndSql(body);
            byte[] bytes = cleaned.getBytes(StandardCharsets.UTF_8);
            return new ServletInputStream() {
                private final ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

                @Override
                public boolean isFinished() { return inputStream.available() == 0; }

                @Override
                public boolean isReady() { return true; }

                @Override
                public void setReadListener(ReadListener readListener) {}

                @Override
                public int read() { return inputStream.read(); }
            };
        }
    }
}