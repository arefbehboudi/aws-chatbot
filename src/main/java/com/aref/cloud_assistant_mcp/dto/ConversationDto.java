package com.aref.cloud_assistant_mcp.dto;

public class ConversationDto {
    private String conversationId;
    private String title;

    public ConversationDto(String conversationId, String title) {
        this.conversationId = conversationId;
        this.title = title;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getTitle() {
        return title;
    }
}
