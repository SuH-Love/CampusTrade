package com.campustrade.service.ai;

import java.util.Map;

/**
 * 聊天输出适配器 — 统一Agent循环的输出接口。
 * 流式模式(SseOutputAdapter)将事件推送到SSE emitter，
 * 同步模式(SyncOutputAdapter)将事件收集到内存返回给调用方。
 */
public interface ChatOutputAdapter {

    void onThinking(String step, String detail);

    void onToolCall(String toolCallId, String toolName, String args);

    void onToolStart(String toolCallId, String toolName);

    void onToolResult(String toolCallId, String toolName, String result);

    void onToken(String token);

    void onComplete(String fullResponse);

    void onError(String error);

    boolean isStreaming();

    default void onToolConfirm(String toolCallId, String toolName, String args) {}
}