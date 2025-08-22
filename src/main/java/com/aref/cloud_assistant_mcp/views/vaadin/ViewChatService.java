package com.aref.cloud_assistant_mcp.views.vaadin;

import com.aref.cloud_assistant_mcp.dto.ConversationCreateDto;
import com.aref.cloud_assistant_mcp.dto.ConversationDto;
import com.aref.cloud_assistant_mcp.service.ConversationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ViewChatService {

    private final Map<String, Conversation> conversationMap = new ConcurrentHashMap<>();

    private final ConversationService conversationService;

    public ViewChatService(ConversationService conversationService) {
        this.conversationService = conversationService;
    }


    public Conversation createNewConversation() {
        String id = "temp-" + System.currentTimeMillis();
        Conversation conversation = new Conversation(id);
        conversationMap.put(id, conversation);
        return conversation;
    }

    public List<Conversation> getAllConversations() {
        if (conversationMap.isEmpty()) {
            User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Map<String, Conversation> conversationMap = conversationService
                    .getConversations(user.getUsername(), PageRequest.of(0, 100))
                    .stream()
                    .collect(Collectors.toMap(
                            ConversationDto::getConversationId,
                            conversation -> {
                                Conversation c = new Conversation();
                                c.setId(conversation.getConversationId());
                                c.setTitle(conversation.getTitle());
                                return c;
                            }
                    ));
            this.conversationMap.putAll(conversationMap);

        }
        return new ArrayList<>(conversationMap.values());
    }

    public Conversation getConversation(String id) {
        return conversationMap.get(id);
    }

    public void addMessage(String conversationId, Message message) {
        Conversation conv = conversationMap.get(conversationId);
        if (conv != null) {
            conv.addMessage(message);
        }
    }

    public void renameConversation(String oldId, String newId, String title) {
        Conversation conv = conversationMap.remove(oldId);
        if (conv != null) {
            conv.setId(newId);
            conv.setTitle(title);
            conversationMap.put(newId, conv);
        }
    }

    public Flux<String> create(ConversationCreateDto conversationCreateDto) {
        return conversationService.create(conversationCreateDto);
    }

    public void clear() {
        conversationMap.clear();
    }

    public List<Message> getConversationMessages(String id) {
        Conversation conversation = conversationMap.get(id);
        if (conversation == null)
            return new ArrayList<>();
        List<Message> messages = conversation.getMessages();
        if (messages == null || messages.isEmpty()) {
            conversation.setMessages(conversationService.getMessages(id, PageRequest.of(0, 100))
                    .stream()
                    .map(msg -> new Message(msg.getContent(), msg.getType(), msg.getToolName(), msg.getToolResponse()))
                    .collect(Collectors.toList()));
        }
        return conversation.getMessages();
    }
}

