package com.mot.controller;


import com.mot.entity.User;
import com.mot.payload.request.SignupRequest;
import com.mot.services.AuthService;
import com.mot.response.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    @Autowired
    AuthService authService ;

    @PostMapping("/signup")
    public ResponseEntity<BaseResponse<User>> registerUser(@Valid @RequestBody SignupRequest signupRequest){
        return ResponseEntity.ok(BaseResponse.ok(authService.registerUser(signupRequest)));
    }
}

