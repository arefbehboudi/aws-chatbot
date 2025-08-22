package com.aref.cloud_assistant_mcp.model;

import dev.langchain4j.data.message.ChatMessageType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity(name = "chatmemory")
@Table(name = "chatmemory")
public class CustomChatMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false, length = 36)
    private String conversationId;

    @Column(name = "chat_id", length = 191)
    private String chatId;

    @Column(name = "username", nullable = false, columnDefinition = "VARCHAR(191)")
    private String username;

    @Column(name = "title", nullable = false, columnDefinition = "VARCHAR(191)")
    private String title;

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "tool_execution_requests", columnDefinition = "JSON")
    private String toolExecutionRequests;

    @Column(name = "tool_id", columnDefinition = "VARCHAR(191)")
    private String toolId;

    @Column(name = "tool_name", columnDefinition = "VARCHAR(191)")
    private String toolName;

    @Column(name = "tool_response", columnDefinition = "JSON")
    private String toolResponse;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ChatMessageType type;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    public CustomChatMemory(String chatId) {
        this.chatId = chatId;
    }

    public CustomChatMemory(String conversationId,
                            String username,
                            String title,
                            String toolId,
                            String toolName,
                            String toolResponse,
                            ChatMessageType type,
                            LocalDateTime timestamp,
                            String chatId) {
        this.conversationId = conversationId;
        this.username = username;
        this.title = title;
        this.toolId = toolId;
        this.toolName = toolName;
        this.toolResponse = toolResponse;
        this.type = type;
        this.timestamp = timestamp;
        this.chatId = chatId;
    }

    public CustomChatMemory(String conversationId,
                            String title,
                            String text,
                            String username,
                            ChatMessageType type,
                            LocalDateTime timestamp) {
        this.conversationId = conversationId;
        this.title = title;
        this.text = text;
        this.type = type;
        this.timestamp = timestamp;
        this.username = username;
    }

    public void setToolExecutionRequests(String toolExecutionRequests) {
        this.toolExecutionRequests = toolExecutionRequests;
    }

    public void setToolId(String toolId) {
        this.toolId = toolId;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public void setToolResponse(String toolResponse) {
        this.toolResponse = toolResponse;
    }

    public String getChatId() {
        return chatId;
    }

    public String getText() {
        return text;
    }

    public ChatMessageType getType() {
        return type;
    }

    public Long getId() {
        return id;
    }

}
