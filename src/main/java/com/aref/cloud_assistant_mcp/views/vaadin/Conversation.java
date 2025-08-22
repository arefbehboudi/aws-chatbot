package com.aref.cloud_assistant_mcp.views.vaadin;

import java.util.ArrayList;
import java.util.List;

public class Conversation {
    private String id;
    private String title;
    private List<Message> messages = new ArrayList<>();

    public Conversation() {}

    public Conversation(String id) {
        this.id = id;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }

    public void addMessage(Message message) {
        if(this.messages == null)
            this.messages = new ArrayList<>();
        messages.add(message);
    }

    public Message getLastMessage() {
        return messages.getLast();
    }
}