package com.mot.exception.Model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class ApiError {
    private int statusCode;
    private List<String> messages;

    public ApiError(int statusCode, List<String> messages) {
        this.statusCode = statusCode;
        this.messages = messages;
    }

    public ApiError(int statusCode, String message) {
        this.statusCode = statusCode;
        this.messages = List.of(message);
    }

}