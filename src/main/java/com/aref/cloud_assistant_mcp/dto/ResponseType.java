package com.aref.cloud_assistant_mcp.dto;

/**
 * ResponseType
 * ------------------------------
 * Enum representing different types of chat responses.
 */
public enum ResponseType {
    TOOL_CALLING("toolCalling"),
    MESSAGE("message"),
    CONVERSATION_DETAIL_METADATA("conversation_detail_metadata"),
    ERROR("error"); // Can be extended in the future

    private final String value;

    ResponseType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
