package com.aref.cloud_assistant_mcp.service;

import com.aref.cloud_assistant_mcp.dto.MessageDto;
import com.aref.cloud_assistant_mcp.dto.PromptDto;
import com.aref.cloud_assistant_mcp.dto.ResponseType;
import com.aref.cloud_assistant_mcp.model.CustomChatMemory;
import com.aref.cloud_assistant_mcp.repository.CustomJdbcChatMemoryRepositoryImp;
import com.aref.cloud_assistant_mcp.service.strategy.AwsToolCallStrategy;
import com.aref.cloud_assistant_mcp.service.strategy.ToolCallContext;
import com.aref.cloud_assistant_mcp.service.tools.AWSEc2Tools;
import com.aref.cloud_assistant_mcp.service.tools.AWSS3Tools;
import com.google.gson.Gson;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.*;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private static final int HISTORY_PAGE_SIZE = 8;
    private final List<ToolSpecification> toolSpecs;


    private static final String SYSTEM_PROMPT = """
            You are an AWS Management Assistant chatbot designed to help users manage AWS resources securely and efficiently.
            
            Your primary responsibilities include:
            - Creating, starting, stopping, and terminating EC2 instances.
            - Managing S3 buckets (create, list, delete, upload, and download files).
            - Providing information about IAM users, roles, and permissions (but not altering sensitive IAM policies without explicit confirmation).
            - Monitoring and retrieving details about resources (e.g., instance status, cost estimation).
            
            ### Important Rules & Security Guidelines:
            1. Always ask for confirmation before making any changes that incur costs or could delete resources.
            2. Never expose AWS credentials or request them directly; assume they are configured securely in the environment.
            3. If a request seems unclear or potentially harmful (e.g., deleting critical resources), ask for clarification.
            4. Provide responses in a clear, step-by-step format when giving instructions or performing actions.
            5. If a requested action is outside your allowed capabilities, politely explain the limitation.
            
            ### Output Formatting Instructions (VERY IMPORTANT):
            - Write text in natural, grammatically correct English with proper punctuation and spacing.
            - Do not split service names or technical terms into separate words or tokens.
              Correct: EC2, S3, IAM
              Incorrect: EC 2, S 3, I A M
            - Use human-like sentence structure, just like a professional AWS consultant would write.
            - Do not output incomplete words or broken sentences.
            """;

    private final OpenAiStreamingChatModel streamingChatModel;
    private final OpenAiChatModel openAiChatModel;
    private final CustomJdbcChatMemoryRepositoryImp chatMemoryRepo;
    private final ToolCallContext toolCallContext;


    private final AWSS3Tools awss3Tools;
    private final AWSEc2Tools awsEc2Tools;

    private static final Gson GSON = new Gson();

    public ChatService(OpenAiStreamingChatModel streamingModel,
                       OpenAiChatModel syncModel,
                       AWSEc2Tools awsEc2Tools,
                       AWSS3Tools awss3Tools,
                       CustomJdbcChatMemoryRepositoryImp chatMemoryRepo) {

        this.streamingChatModel = Objects.requireNonNull(streamingModel);
        this.openAiChatModel = Objects.requireNonNull(syncModel);
        this.chatMemoryRepo = Objects.requireNonNull(chatMemoryRepo);
        this.toolCallContext = new ToolCallContext(List.of(new AwsToolCallStrategy()));

        this.awss3Tools = awss3Tools;
        this.awsEc2Tools = awsEc2Tools;

        this.toolSpecs = new ArrayList<>();
        this.toolSpecs.addAll(ToolSpecifications.toolSpecificationsFrom(awss3Tools));
        this.toolSpecs.addAll(ToolSpecifications.toolSpecificationsFrom(awsEc2Tools));
    }


    public Flux<String> prompt(PromptDto prompt) {
        final User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        final Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

        saveChatMemory(
                prompt.getConversationId(),
                prompt.getMessage(),
                prompt.getTitle(),
                user.getUsername(),
                ChatMessageType.USER,
                null,
                UUID.randomUUID().toString());

        LinkedList<ChatMessage> history = createConversationHistory(prompt.getConversationId(), prompt.getMessage());
        ChatRequest chatRequest = buildRequest(history, toolSpecs);

        startStreaming(prompt, chatRequest, history, user, sink, UUID.randomUUID().toString());

        return sink.asFlux();
    }


    void startStreaming(PromptDto prompt,
                        ChatRequest chatRequest,
                        LinkedList<ChatMessage> history,
                        User user,
                        Sinks.Many<String> sink,
                        String chatId) {

        streamingChatModel.chat(chatRequest, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                safeEmit(sink, buildMessageEvent(partialResponse));
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                AiMessage ai = completeResponse.aiMessage();
                String text = ai.text();

                history.addLast(ai);

                if (text != null)
                    saveChatMemory(
                            prompt.getConversationId(),
                            text,
                            prompt.getTitle(),
                            user.getUsername(),
                            ChatMessageType.AI,
                            ai.toolExecutionRequests(),
                            chatId
                    );

                if (ai.toolExecutionRequests() == null || ai.toolExecutionRequests().isEmpty()) {
                    sink.tryEmitComplete();
                }
            }

            @Override
            public void onError(Throwable error) {
                log.error("Streaming error", error);
                sink.tryEmitError(error);
            }

            @Override
            public void onPartialThinking(PartialThinking partialThinking) {
                log.debug("Partial thinking: {}", partialThinking.text());
            }

            @Override
            public void onPartialToolCall(PartialToolCall partialToolCall) {
                String handle = toolCallContext.handle(
                        ToolExecutionRequest.builder()
                                .name(partialToolCall.name())
                                .id(partialToolCall.id())
                                .arguments(partialToolCall.partialArguments())
                                .build(),
                        null
                );
                safeEmit(sink, handle);
            }

            @Override
            public void onCompleteToolCall(CompleteToolCall completeToolCall) {
                ToolExecutionRequest toolExecutionRequest = completeToolCall.toolExecutionRequest();
                callingTool(toolExecutionRequest, prompt, history, user, sink, chatId);
            }
        });
    }

    private ChatRequest buildRequest(LinkedList<ChatMessage> history, List<ToolSpecification> toolSpecs) {
        return ChatRequest.builder()
                .messages(history)
                .toolSpecifications(toolSpecs)
                .build();
    }

    private void callingTool(ToolExecutionRequest req,
                             PromptDto prompt,
                             LinkedList<ChatMessage> history,
                             User user,
                             Sinks.Many<String> sink,
                             String chatId) {
        ToolExecutor executor = null;

        if (req.name().startsWith("aws_ec2"))
            executor = new DefaultToolExecutor(awsEc2Tools, req);
        else
            executor = new DefaultToolExecutor(awss3Tools, req);

        log.info("Executing tool: {}", req.name());
        String result = executor.execute(req, UUID.randomUUID().toString());

        ToolExecutionResultMessage toolMsg = ToolExecutionResultMessage.from(req, result);
        safeEmit(sink, toolCallContext.handle(req, toolMsg.text()));

        saveChatMemoryToolCalling(
                prompt.getConversationId(),
                user.getUsername(),
                prompt.getTitle(),
                toolMsg.id(),
                toolMsg.toolName(),
                toolMsg.text(),
                UUID.randomUUID().toString());

        history.addLast(toolMsg);

        ChatRequest nextReq = buildRequest(history, toolSpecs);
        startStreaming(prompt, nextReq, history, user, sink, chatId);
    }

    private void saveChatMemoryToolCalling(String conversationId,
                                           String username,
                                           String title,
                                           String toolId,
                                           String toolName,
                                           String toolResponse,
                                           String chatId) {
        CustomChatMemory entity = new CustomChatMemory(
                conversationId,
                username,
                title,
                toolId,
                toolName,
                toolResponse,
                ChatMessageType.TOOL_EXECUTION_RESULT,
                LocalDateTime.now().plusSeconds(1),
                chatId
        );
        chatMemoryRepo.save(entity);
    }

    private void safeEmit(Sinks.Many<String> sink, String payload) {
        Sinks.EmitResult r = sink.tryEmitNext(payload);
        if (r.isFailure()) {
            log.warn("Emit failed: {}", r);
            sink.emitNext(payload, Sinks.EmitFailureHandler.FAIL_FAST);
        }
    }


    private String buildMessageEvent(String text) {
        return new JSONObject()
                .put("type", ResponseType.MESSAGE.getValue())
                .put("message", text)
                .toString();
    }

    private void saveChatMemory(String conversationId,
                                String content,
                                String title,
                                String username,
                                ChatMessageType type,
                                List<ToolExecutionRequest> toolExecutionRequests,
                                String chatId) {

        CustomChatMemory entity = new CustomChatMemory(
                conversationId,
                title,
                content,
                username,
                type,
                LocalDateTime.now()
        );

        if (toolExecutionRequests != null) entity.setToolExecutionRequests(GSON.toJson(toolExecutionRequests));

        entity.setChatId(chatId);



        if (chatId != null && chatMemoryRepo.existByChatId(chatId))
            chatMemoryRepo.update(entity);
        else
            chatMemoryRepo.save(entity);
    }

    public String generateTitle(String message) {
        log.debug("Generate title for message: {}", message);
        return openAiChatModel.chat(List.of(
                new SystemMessage(String.format("""
                        Generate a meaningful and appropriate title with a maximum length of 28 characters for the following text,
                        Don't use any tools:
                        %s
                        """, message))
        )).aiMessage().text();
    }

    private LinkedList<ChatMessage> createConversationHistory(String conversationId, String latestUserMessage) {
        LinkedList<ChatMessage> history = chatMemoryRepo
                .getMessagesByConversationId(conversationId, PageRequest.of(0, HISTORY_PAGE_SIZE, Sort.by("timestamp").ascending()))
                .stream()
                .map(this::getChatMessage)
                .collect(Collectors.toCollection(LinkedList::new));

        history.addLast(new UserMessage(latestUserMessage));
        history.addFirst(new SystemMessage(SYSTEM_PROMPT));
        return history;
    }

    private ChatMessage getChatMessage(MessageDto dto) {
        if (dto.getType() == ChatMessageType.USER)
            return new UserMessage(dto.getContent());
        else if (dto.getType() == ChatMessageType.TOOL_EXECUTION_RESULT) {
            return new ToolExecutionResultMessage(dto.getToolId(), dto.getToolName(), dto.getToolResponse());
        } else
            return new AiMessage(dto.getContent());
    }
}
