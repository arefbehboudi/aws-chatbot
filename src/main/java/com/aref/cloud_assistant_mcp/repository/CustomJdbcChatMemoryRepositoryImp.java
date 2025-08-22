package com.aref.cloud_assistant_mcp.repository;

import com.aref.cloud_assistant_mcp.dto.ConversationDto;
import com.aref.cloud_assistant_mcp.dto.MessageDto;
import com.aref.cloud_assistant_mcp.model.CustomChatMemory;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class CustomJdbcChatMemoryRepositoryImp {

    private final CustomJdbcChatMemoryRepository repository;


    public CustomJdbcChatMemoryRepositoryImp(CustomJdbcChatMemoryRepository repository) {
        this.repository = repository;
    }

    public void save(CustomChatMemory chatMemory) {
        repository.save(chatMemory);
    }

    public void deleteByConversationId(String conversationId) {
        repository.deleteByConversationId(conversationId);
    }

    public Page<MessageDto> getMessagesByConversationId(String conversationId, Pageable page) {
        return repository.getMessagesByConversationId(conversationId, page);
    }

    public Page<ConversationDto> getConversationsByUsername(String username, Pageable page) {
        return repository.getConversationsByUsername(username, page);
    }

    public void update(CustomChatMemory entity) {
        repository.update(entity);
    }

    public boolean existByChatId(String chatId) {
        Example<CustomChatMemory> example = Example.of(new CustomChatMemory(chatId));
        return repository.exists(example);
    }
}
