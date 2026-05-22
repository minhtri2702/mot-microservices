package com.mot.controller;


import com.mot.entity.User;
import com.mot.payload.request.LoginRequest;
import com.mot.payload.request.SignupRequest;
import com.mot.payload.response.JwtResponse;
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

import java.util.Map;

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

    @PostMapping("/google")
    public ResponseEntity<BaseResponse<JwtResponse>> googleLogin(@RequestBody Map<String, String> body) {
        String idToken = body.get("idToken");
        JwtResponse jwtResponse = authService.googleLogin(idToken);
        return ResponseEntity.ok(BaseResponse.ok(jwtResponse));
    }

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<JwtResponse>> login(@RequestBody LoginRequest loginRequest) {
        JwtResponse jwtResponse = authService.login(loginRequest.getUsername(), loginRequest.getPassword());
        return ResponseEntity.ok(BaseResponse.ok(jwtResponse));
    }
}

