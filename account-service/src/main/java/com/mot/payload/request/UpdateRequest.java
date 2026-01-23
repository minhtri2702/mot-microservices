package com.mot.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;

@Data
public class UpdateRequest {
    @NotBlank(message = "Username is requited")
    private String userName ;
    @NotBlank(message = "Password is requited")
    @ToString.Exclude
    private String passWord;

}
