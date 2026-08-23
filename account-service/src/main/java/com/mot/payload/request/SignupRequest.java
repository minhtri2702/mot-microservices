package com.mot.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignupRequest {
    @NotBlank(message = "Username is not blank")
    @Size(min = 3, max = 20 , message = "Username must be between 3 and 20 character")
    private String userName;
    @NotBlank(message = "PassWord is not blank")
    @Size(min = 3, max = 20 , message = "PassWord must be between 3 and 20 character")
    @ToString.Exclude
    private String passWord;
    @NotBlank(message = "Email is required")
    @Size(max = 50, message = "Email must not exceed 50 characters")
    @Email(message = "Email must be valid")
    private String email;

}
