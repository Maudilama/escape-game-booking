package com.demo.escape_game_booking.common;

import java.util.Map;

public class BusinessRuleException extends ApiException {

    public BusinessRuleException(ErrorCode code, String message) {
        super(code, message);
    }

    public BusinessRuleException(ErrorCode code, String message, Map<String, Object> params) {
        super(code, message, params);
    }
}