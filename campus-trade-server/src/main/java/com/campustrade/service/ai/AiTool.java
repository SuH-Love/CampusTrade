package com.campustrade.service.ai;

import java.util.Map;

/**
 * AI工具接口 — 统一工具元信息描述。
 * 每个工具需提供名称、描述、是否写操作、是否需要确认、分组等元信息。
 * 写操作清单(WRITE_TOOLS)和DB同步(writeOps)均从实现此接口的注册表自动派生，
 * 消除手工维护两份重复清单的维护隐患。
 */
public interface AiTool {

    String getName();

    String getDescription();

    default boolean isWrite() {
        return false;
    }

    default boolean needConfirm() {
        return false;
    }

    default String getDisplayName() {
        return getName();
    }

    default String getGroup() {
        return getName().startsWith("admin_") ? "admin" : "base";
    }

    Map<String, Object> getParameters();
}