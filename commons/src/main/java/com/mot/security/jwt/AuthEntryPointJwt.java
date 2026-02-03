//package com.mot.security.jwt;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.mot.exception.Model.ApiError;
//import com.mot.exception.UnauthorizedException;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.http.MediaType;
//import org.springframework.security.core.AuthenticationException;
//import org.springframework.security.web.AuthenticationEntryPoint;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//
//@Component
//public class AuthEntryPointJwt implements AuthenticationEntryPoint {
//private  final  ObjectMapper objectMapper = new ObjectMapper();
//    @Override
//    public void commence(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
//        UnauthorizedException unauthorized = new UnauthorizedException("Unauthorized");
//        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
//        ApiError body = new ApiError(HttpServletResponse.SC_UNAUTHORIZED, unauthorized.getMessages());
//        objectMapper.writeValue(response.getOutputStream(), body);
//    }
//}
