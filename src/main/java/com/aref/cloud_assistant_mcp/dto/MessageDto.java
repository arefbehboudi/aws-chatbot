package com.aref.cloud_assistant_mcp.dto;

import dev.langchain4j.data.message.ChatMessageType;

public class MessageDto {
    private String conversationId;

    private String content;

    private ChatMessageType type;

    private String toolId;

    private String toolName;

    private String toolResponse;

    public MessageDto(String conversationId, String content, ChatMessageType type, String toolName, String toolId, String toolResponse) {
        this.conversationId = conversationId;
        this.content = content;
        this.type = type;
        this.toolId = toolId;
        this.toolName = toolName;
        this.toolResponse = toolResponse;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getContent() {
        return content;
    }

    public ChatMessageType getType() {
        return type;
    }

    public String getToolName() {
        return toolName;
    }

    public String getToolResponse() {
        return toolResponse;
    }

    public String getToolId() {
        return toolId;
    }
}
