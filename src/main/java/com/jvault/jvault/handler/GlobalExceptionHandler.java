package com.jvault.jvault.handler;

import com.jvault.jvault.utils.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFound.class)
    public ResponseEntity<ErrorResponse> handle(UserNotFound exp){
        ErrorResponse error = new ErrorResponse(
                exp.getMessage(),
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value()
        );
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handle(UserAlreadyExistsException exp){
        ErrorResponse error = new ErrorResponse(
                exp.getMessage(),
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value()
        );
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle(AccountNotFoundException exp){
        ErrorResponse error = new ErrorResponse(
                exp.getMessage(),
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value()
        );
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(InvalidCurrencyException.class)
    public ResponseEntity<ErrorResponse> handle(InvalidCurrencyException exp){
        ErrorResponse error = new ErrorResponse(
                exp.getMessage(),
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value()
        );
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(NotYourAccountException.class)
    public ResponseEntity<ErrorResponse> handle(NotYourAccountException exp){
        ErrorResponse error = new ErrorResponse(
                exp.getMessage(),
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value()
        );
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(error);
    }

    @ExceptionHandler(RefreshTokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handle(RefreshTokenExpiredException exp){
        ErrorResponse error = new ErrorResponse(
                exp.getMessage(),
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value()
        );
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(error);
    }

    @ExceptionHandler(OldPasswordIncorrect.class)
    public ResponseEntity<ErrorResponse> handle(OldPasswordIncorrect exp){
        ErrorResponse error = new ErrorResponse(
                exp.getMessage(),
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value()
        );
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(InvalidPasswordDeleteException.class)
    public ResponseEntity<ErrorResponse> handle(InvalidPasswordDeleteException exp){
        ErrorResponse error = new ErrorResponse(
                exp.getMessage(),
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value()
        );
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(error);
    }

    @ExceptionHandler(CantTransferMoneyToSameAccount.class)
    public ResponseEntity<ErrorResponse> handle(CantTransferMoneyToSameAccount exp){
        ErrorResponse error = new ErrorResponse(
                exp.getMessage(),
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value()
        );
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(NotEnoughMoneyException.class)
    public ResponseEntity<ErrorResponse> handle(NotEnoughMoneyException exp){
        ErrorResponse error = new ErrorResponse(
                exp.getMessage(),
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value()
        );
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(error);
    }

    @ExceptionHandler(NotSupportedYetException.class)
    public ResponseEntity<ErrorResponse> handle(NotSupportedYetException exp){
        ErrorResponse error = new ErrorResponse(
                exp.getMessage(),
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value()
        );
        return ResponseEntity
                .status(HttpStatus.NOT_IMPLEMENTED)
                .body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException exp){
        Map<String, String> errors = new HashMap<>();
        exp.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }
}
