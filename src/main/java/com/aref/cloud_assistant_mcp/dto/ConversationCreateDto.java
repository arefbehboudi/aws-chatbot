package com.aref.cloud_assistant_mcp.dto;


public class ConversationCreateDto {

    private String conversationId;

    private String message;

    private String title;

    public ConversationCreateDto(String conversationId, String message, String title) {
        this.conversationId = conversationId;
        this.message = message;
        this.title = title;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getMessage() {
        return message;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isNew() {
        return conversationId == null;
    }
}
