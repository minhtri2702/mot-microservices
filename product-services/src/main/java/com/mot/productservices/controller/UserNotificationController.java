package com.mot.productservices.controller;

import com.mot.productservices.dto.PagedResponseDTO;
import com.mot.productservices.dto.UserNotificationDTO;
import com.mot.productservices.service.UserNotificationService;
import com.mot.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class UserNotificationController {
    private final UserNotificationService service;

    @GetMapping
    public ResponseEntity<BaseResponse<PagedResponseDTO<UserNotificationDTO>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication auth) {
        return ResponseEntity.ok(BaseResponse.<PagedResponseDTO<UserNotificationDTO>>ok(
                service.list(auth.getName(), page, size)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<BaseResponse<Map<String, Long>>> unread(Authentication auth) {
        return ResponseEntity.ok(BaseResponse.<Map<String, Long>>ok(
                Map.of("count", service.unreadCount(auth.getName()))));
    }

    @PostMapping("/read-all")
    public ResponseEntity<BaseResponse<Void>> readAll(Authentication auth) {
        service.markAllRead(auth.getName());
        return ResponseEntity.ok(BaseResponse.<Void>ok(null));
    }
}
