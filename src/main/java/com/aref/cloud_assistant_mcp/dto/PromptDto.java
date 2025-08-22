package com.aref.cloud_assistant_mcp.dto;

public class PromptDto {

    private String message;

    private String conversationId;

    private String title;

    public PromptDto(String message, String conversationId, String title) {
        this.message = message;
        this.conversationId = conversationId;
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getTitle() {
        return title;
    }
}
