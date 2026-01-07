package com.exprivia.nest.cruud.exception;

import java.time.Instant;

/**
 * Standard API error payload.
 */
public record ApiError(Instant timestamp, int status, String error, String message, String path) {
}
