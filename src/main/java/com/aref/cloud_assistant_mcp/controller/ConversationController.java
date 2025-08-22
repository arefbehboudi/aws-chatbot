package com.aref.cloud_assistant_mcp.controller;

import com.aref.cloud_assistant_mcp.dto.ConversationCreateDto;
import com.aref.cloud_assistant_mcp.dto.ConversationDto;
import com.aref.cloud_assistant_mcp.dto.MessageDto;
import com.aref.cloud_assistant_mcp.service.ConversationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;


@RestController
@RequestMapping("v1/conversation")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> create(@RequestBody ConversationCreateDto conversationCreateDto) {
        return conversationService.create(conversationCreateDto);
    }

    @GetMapping
    public Page<ConversationDto> getConversations(@AuthenticationPrincipal User user, Pageable page) {
        return conversationService.getConversations(user.getUsername(), page);
    }

    @GetMapping("/{conversationId}/messages")
    public Page<MessageDto> getMessages(@PathVariable String conversationId, Pageable page) {
        return conversationService.getMessages(conversationId, page);
    }


}
