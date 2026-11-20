package com.demo.escape_game_booking.common;

import java.util.Map;

public abstract class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, Object> params;

    protected ApiException(ErrorCode code, String message, Map<String, Object> params) {
        super(message);
        this.code = code;
        this.params = Map.copyOf(params);
    }

    protected ApiException(ErrorCode code, String message) {
        this(code, message, Map.of());
    }

    public ErrorCode getCode() {
        return code;
    }

    public Map<String, Object> getParams() {
        return params;
    }
}