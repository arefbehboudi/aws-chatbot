package com.aref.cloud_assistant_mcp.views.vaadin;

import dev.langchain4j.data.message.ChatMessageType;

public class Message {

    private String text;
    private ChatMessageType messageType;
    private String type;
    private String toolName;
    private String toolResponse;

    public Message() {}

    public Message(ChatMessageType messageType, String type) {
        this.messageType = messageType;
        this.type = type;
    }

    public Message(String text, ChatMessageType messageType) {
        this.text = text;
        this.messageType = messageType;
    }

    public Message(String text, ChatMessageType messageType, String toolName, String toolResponse) {
        this.text = text;
        this.messageType = messageType;
        this.toolName = toolName;
        this.toolResponse = toolResponse;
    }

    // Getters and Setters
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public void appendText(String text) {
        if(this.text == null)
            this.text = text;
        else
            this.text = this.text.concat(text);
    }

    public ChatMessageType getMessageType() { return messageType; }
    public void setMessageType(ChatMessageType messageType) { this.messageType = messageType; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public String getToolResponse() { return toolResponse; }
    public void setToolResponse(String toolResponse) { this.toolResponse = toolResponse; }
}
