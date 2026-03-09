package yandex.workshop.accountsservice.controller;

import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import yandex.workshop.api.model.OperationResponse;

@RestControllerAdvice
@Slf4j
public class ControllerExceptionHandler {

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public OperationResponse handleRuntimeException(RuntimeException ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        return new OperationResponse()
            .success(false)
            .message("Internal server error" + ex.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public OperationResponse handleIllegalArgument(IllegalArgumentException ex) {

        log.warn("Business validation error: {}", ex.getMessage());

        return new OperationResponse()
            .success(false)
            .message(ex.getMessage());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoSuchElementException.class)
    public OperationResponse handleNotFound(NoSuchElementException ex) {

        log.warn("Entity not found: {}", ex.getMessage());

        return new OperationResponse()
            .success(false)
            .message(ex.getMessage());
    }
}
