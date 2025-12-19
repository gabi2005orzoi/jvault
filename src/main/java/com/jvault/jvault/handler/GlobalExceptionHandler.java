package com.jvault.jvault.handler;

import com.jvault.jvault.utils.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFound.class)
    public ResponseEntity<String> handle(UserNotFound exp){
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(exp.getMessage());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<String> handle(UserAlreadyExistsException exp){
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(exp.getMessage());
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<String> handle(AccountNotFoundException exp){
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(exp.getMessage());
    }

    @ExceptionHandler(InvalidCurrencyException.class)
    public ResponseEntity<String> handle(InvalidCurrencyException exp){
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(exp.getMessage());
    }

    @ExceptionHandler(NotYourAccountException.class)
    public ResponseEntity<String> handle(NotYourAccountException exp){
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(exp.getMessage());
    }

    @ExceptionHandler(RefreshTokenExpiredException.class)
    public ResponseEntity<String> handle(RefreshTokenExpiredException exp){
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(exp.getMessage());
    }

    @ExceptionHandler(OldPasswordIncorrect.class)
    public ResponseEntity<String> handle(OldPasswordIncorrect exp){
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(exp.getMessage());
    }

    @ExceptionHandler(InvalidPasswordDeleteException.class)
    public ResponseEntity<String> handle(InvalidPasswordDeleteException exp){
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(exp.getMessage());
    }

    @ExceptionHandler(CantTransferMoneyToSameAccount.class)
    public ResponseEntity<String> handle(CantTransferMoneyToSameAccount exp){
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(exp.getMessage());
    }

    @ExceptionHandler(NotEnoughMoneyException.class)
    public ResponseEntity<String> handle(NotEnoughMoneyException exp){
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(exp.getMessage());
    }

    @ExceptionHandler(NotSupportedYetException.class)
    public ResponseEntity<String> handle(NotSupportedYetException exp){
        return ResponseEntity
                .status(HttpStatus.NOT_IMPLEMENTED)
                .body(exp.getMessage());
    }
}
