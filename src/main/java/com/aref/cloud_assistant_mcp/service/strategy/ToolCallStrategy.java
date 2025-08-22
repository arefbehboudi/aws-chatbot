package com.aref.cloud_assistant_mcp.service.strategy;

import dev.langchain4j.agent.tool.ToolExecutionRequest;



public interface ToolCallStrategy {

    boolean supports(String toolId);

    String handleToolCall(ToolExecutionRequest toolExecutionRequest, String response);
}
