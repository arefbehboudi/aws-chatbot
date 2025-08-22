package com.aref.cloud_assistant_mcp.service;

import com.aref.cloud_assistant_mcp.dto.*;
import com.aref.cloud_assistant_mcp.repository.CustomJdbcChatMemoryRepositoryImp;
import org.json.JSONObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import java.util.UUID;

@Service
public class ConversationService {

    private final CustomJdbcChatMemoryRepositoryImp chatMemoryRepository;
    private final ChatService chatService;

    public ConversationService(CustomJdbcChatMemoryRepositoryImp chatMemoryRepository, ChatService chatService) {
        this.chatMemoryRepository = chatMemoryRepository;
        this.chatService = chatService;
    }

    public Flux<String> create(ConversationCreateDto conversationCreateDto) {
        if(conversationCreateDto.isNew()) {
            String conversationId = UUID.randomUUID().toString();
            conversationCreateDto.setConversationId(conversationId);
        }

        if(conversationCreateDto.getTitle() == null) {
            String generateTitle = chatService.generateTitle(conversationCreateDto.getMessage());
            conversationCreateDto.setTitle(generateTitle);
        }

        PromptDto prompt = new PromptDto(conversationCreateDto.getMessage(), conversationCreateDto.getConversationId(), conversationCreateDto.getTitle());

        return Flux.concat(
                Flux.just(new JSONObject()
                        .put("title", conversationCreateDto.getTitle())
                        .put("conversationId", conversationCreateDto.getConversationId())
                        .put("type", ResponseType.CONVERSATION_DETAIL_METADATA.getValue())
                        .toString()),
                chatService.prompt(prompt)

        );
    }

    public Page<MessageDto> getMessages(String conversationId, Pageable page) {
        return chatMemoryRepository.getMessagesByConversationId(conversationId, page);
    }

    public Page<ConversationDto> getConversations(String username, Pageable page) {
        return chatMemoryRepository.getConversationsByUsername(username, page);
    }
}
