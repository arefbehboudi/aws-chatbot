package com.aref.cloud_assistant_mcp.service.strategy;

import com.aref.cloud_assistant_mcp.dto.ResponseType;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.json.JSONObject;
import java.util.HashMap;


public class AwsToolCallStrategy implements ToolCallStrategy {

    @Override
    public boolean supports(String toolId) {
        return toolId != null && toolId.contains("aws_");
    }

    @Override
    public String handleToolCall(ToolExecutionRequest toolExecutionRequest, String response) {
        return new JSONObject()
                .put("type", ResponseType.TOOL_CALLING.getValue())
                .put("toolId", toolExecutionRequest.id())
                .put("toolName", toolExecutionRequest.name())
                .put("arguments", toolExecutionRequest.arguments())
                .put("toolResponse", response)
                .put("completed", response != null)
                .toString();
    }
}
