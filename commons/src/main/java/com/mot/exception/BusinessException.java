package com.mot.exception;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Setter
public abstract class BusinessException extends RuntimeException {
    protected abstract HttpStatus getHttpStatus();
    private final List<String> messages;
    protected  BusinessException(String... message) {
        super(String.join(",",message));
        this.messages = List.of(message) ;
    }
    protected BusinessException() {
        this.messages = null;
    }
}


