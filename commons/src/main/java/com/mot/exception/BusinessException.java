package com.mot.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Setter
@Getter
public abstract class BusinessException extends RuntimeException {
    protected abstract HttpStatus getHttpStatus();
    private final String messages;

    protected  BusinessException(String  message) {
        super(message);
        this.messages =  message ;
    }
    protected BusinessException() {
        this.messages = null;
    }
}


