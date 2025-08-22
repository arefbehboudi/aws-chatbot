package com.aref.cloud_assistant_mcp.service.strategy;

import dev.langchain4j.agent.tool.ToolExecutionRequest;

import java.util.List;
import java.util.HashMap;


public class ToolCallContext {

    private final List<ToolCallStrategy> strategies;

    public ToolCallContext(List<ToolCallStrategy> strategies) {
        this.strategies = strategies;
    }


    public String handle(ToolExecutionRequest toolExecutionRequest, String response) {
        String toolId = toolExecutionRequest.id();
        return strategies.stream()
                .filter(strategy -> strategy.supports(toolId))
                .findFirst()
                .map(strategy -> strategy.handleToolCall(toolExecutionRequest, response))
                .orElse(null);
    }
}
