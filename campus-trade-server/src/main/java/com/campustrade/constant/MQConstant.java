package com.campustrade.constant;

public class MQConstant {

    public static final String ORDER_EXCHANGE = "campus.order.direct";
    public static final String CHAT_EXCHANGE = "campus.chat.direct";
    public static final String AUDIT_EXCHANGE = "campus.audit.direct";
    public static final String NOTIFY_EXCHANGE = "campus.notify.direct";
    public static final String LOG_EXCHANGE = "campus.log.direct";
    public static final String GOODS_AUDIT_EXCHANGE = "campus.goods.audit.direct";

    public static final String ORDER_CREATE_QUEUE = "order.create.queue";
    public static final String CHAT_SAVE_QUEUE = "chat.save.queue";
    public static final String AUDIT_REPORT_QUEUE = "audit.report.queue";
    public static final String NOTIFY_SEND_QUEUE = "notify.send.queue";
    public static final String LOG_RECORD_QUEUE = "log.record.queue";
    public static final String GOODS_AUDIT_QUEUE = "goods.audit.queue";

    public static final String ORDER_CREATE_KEY = "order.create";
    public static final String CHAT_SAVE_KEY = "chat.save";
    public static final String AUDIT_REPORT_KEY = "audit.report";
    public static final String NOTIFY_SEND_KEY = "notify.send";
    public static final String LOG_RECORD_KEY = "log.record";
    public static final String GOODS_AUDIT_KEY = "goods.audit";

    public static final String DEAD_LETTER_EXCHANGE = "campus.dlx.direct";
    public static final String DEAD_LETTER_QUEUE = "campus.dlx.queue";
    public static final String DEAD_LETTER_KEY = "campus.dlx";
}