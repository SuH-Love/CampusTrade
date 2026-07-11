package com.campustrade.constant;

public class RedisConstant {

    public static final String TOKEN_PREFIX = "token:user:";
    public static final String REFRESH_PREFIX = "refresh:user:";
    public static final String CAPTCHA_PREFIX = "captcha:";
    public static final String GOODS_DETAIL_PREFIX = "goods:detail:";
    public static final String GOODS_LIST_PREFIX = "goods:list:";
    public static final String GOODS_HOT_KEY = "goods:hot";
    public static final String GOODS_RECOMMEND_KEY = "goods:recommend";
    public static final String CHAT_RECENT_PREFIX = "chat:recent:";
    public static final String NOTIFY_USER_PREFIX = "notify:user:";
    public static final String REPEAT_PREFIX = "repeat:";
    public static final String RATE_LIMIT_PREFIX = "rate_limit:";
    public static final String LOGIN_RATE_LIMIT_PREFIX = "login_rate_limit:";
    public static final String BLACKLIST_PREFIX = "blacklist:token:";
    public static final String PERMISSIONS_PREFIX = "permissions:user:";
    public static final String LOCK_GOODS_PREFIX = "lock:goods:";
    public static final String ORDER_TIMEOUT_PREFIX = "order:timeout:";

    public static final long TOKEN_TTL = 7200;
    public static final long REFRESH_TTL = 604800;
    public static final long CAPTCHA_TTL = 300;
    public static final long GOODS_DETAIL_TTL = 1800;
    public static final long GOODS_HOT_TTL = 120;
    public static final long GOODS_RECOMMEND_TTL = 120;
    public static final long REPEAT_TTL = 5;
    public static final long RATE_LIMIT_TTL = 60;
    public static final long CHAT_RECENT_TTL = 3600;
    public static final long NOTIFY_UNREAD_TTL = 300;
    public static final long NULL_CACHE_TTL = 60;
    public static final long LOCK_TTL = 10;
    public static final long ORDER_TIMEOUT_TTL = 300;
}
