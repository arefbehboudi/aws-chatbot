package com.aref.cloud_assistant_mcp.repository;

import com.aref.cloud_assistant_mcp.dto.ConversationDto;
import com.aref.cloud_assistant_mcp.dto.MessageDto;
import com.aref.cloud_assistant_mcp.model.CustomChatMemory;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CustomJdbcChatMemoryRepository extends JpaRepository<CustomChatMemory, Long> {

    @Query(value = """
            SELECT conversationId, text, type, toolId, toolName, toolResponse FROM chatmemory WHERE conversationId = :conversationId
            ORDER BY timestamp
    """)
    Page<MessageDto> getMessagesByConversationId(String conversationId, Pageable page);

    @Query(value = """
            SELECT conversationId, title FROM chatmemory WHERE username = :username
            GROUP BY conversationId, title
    """)
    Page<ConversationDto> getConversationsByUsername(String username, Pageable page);

    @Modifying
    @Query(value = """
            DELETE FROM chatmemory WHERE conversationId = :conversationId
    """)
    @Transactional
    void deleteByConversationId(String conversationId);

    @Query(value = """
            UPDATE chatmemory SET
                text = CONCAT(text, :#{#entity.text})
            WHERE chatId = :#{#entity.chatId}
            """)
    @Transactional
    @Modifying
    void update(CustomChatMemory entity);

}
