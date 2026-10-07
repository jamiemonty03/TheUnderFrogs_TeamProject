package com.neueda.orderservice.exceptions;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.neueda.orderservice.dtos.responses.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.neueda.orderservice.dtos.responses.StatusConflictResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccountNotActiveException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotActive(AccountNotActiveException ex) {
        return reject(HttpStatus.FORBIDDEN, "ACC-403", ex.getMessage());
    }

    @ExceptionHandler(AccountAuthorizationException.class)
    public ResponseEntity<ErrorResponse> handleAccountAuthorization(AccountAuthorizationException ex) {
        return reject(HttpStatus.FORBIDDEN, "AUTH-403", ex.getMessage());
    }

    @ExceptionHandler(InstrumentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleInstrumentNotFound(InstrumentNotFoundException ex) {
        return reject(HttpStatus.NOT_FOUND, "INS-404", ex.getMessage());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(InsufficientFundsException ex) {
        return reject(HttpStatus.BAD_REQUEST, "ORD-400", ex.getMessage());
    }

    @ExceptionHandler(InsufficientHoldingsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientHoldings(InsufficientHoldingsException ex) {
        return reject(HttpStatus.CONFLICT, "ORD-409", ex.getMessage());
    }

    @ExceptionHandler(DuplicateOrderException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateOrder(DuplicateOrderException ex) {
        return reject(HttpStatus.CONFLICT, "ORD-409", ex.getMessage());
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
        return reject(HttpStatus.NOT_FOUND, "ORD-404", ex.getMessage());
    }

    @ExceptionHandler(OrderNotCancellableException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotCancellable(OrderNotCancellableException ex) {
        return reject(HttpStatus.CONFLICT, "ORD-409", ex.getMessage());
    }

    @ExceptionHandler(OrderStatusConflictException.class)
    public ResponseEntity<StatusConflictResponse> handleStatusConflict(OrderStatusConflictException ex) {
        log.warn("Request rejected {} {} {} {}", kv("status", 409), kv("errorCode", "ORD-409"),
                kv("currentStatus", ex.getCurrentStatus()), kv("reason", ex.getMessage()));
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new StatusConflictResponse("ORD-409", ex.getMessage(), ex.getCurrentStatus()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Validation failed");
        return reject(HttpStatus.UNPROCESSABLE_ENTITY, "VAL-422", message);
    }

    @ExceptionHandler(InvalidOrderException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrder(InvalidOrderException ex) {
        return reject(HttpStatus.UNPROCESSABLE_ENTITY, "VAL-422", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return reject(HttpStatus.UNPROCESSABLE_ENTITY, "VAL-422", ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        String message = "Malformed request body";
        if (ex.getCause() instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
            String field = mismatch.getPath().get(mismatch.getPath().size() - 1).getFieldName();
            message = "Invalid value for field '" + field + "'";
        }
        return reject(HttpStatus.UNPROCESSABLE_ENTITY, "VAL-422", message);
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(TypeMismatchException ex) {
        return reject(HttpStatus.UNPROCESSABLE_ENTITY, "VAL-422", "Invalid value for '" + ex.getPropertyName() + "'");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        if (ex instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatusCode status = springError.getStatusCode();
            return reject(status, "REQ-" + status.value(), springError.getBody().getDetail());
        }
        log.error("Unexpected error {} {}", kv("status", 500), kv("errorCode", "SRV-500"), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("SRV-500", "Unexpected error"));
    }

    private ResponseEntity<ErrorResponse> reject(HttpStatusCode status, String errorCode, String message) {
        log.warn("Request rejected {} {} {}", kv("status", status.value()), kv("errorCode", errorCode),
                kv("reason", message));
        return ResponseEntity.status(status).body(new ErrorResponse(errorCode, message));
    }
}
