package com.fintech.walletservice.dto.response;

import java.time.Instant;
import java.util.Map;

public record ValidationErrorResponse(String code, String message, Map<String, String> errors, Instant timestamp) {
}
