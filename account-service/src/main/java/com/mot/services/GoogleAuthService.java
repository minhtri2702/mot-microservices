package com.mot.services;

import com.mot.entity.Role;
import com.mot.entity.SocialAccounts;
import com.mot.entity.User;
import com.mot.enums.ERole;
import com.mot.payload.request.GoogleLoginRequest;
import com.mot.repository.RoleRepository;
import com.mot.repository.SocialAccountsRepository;
import com.mot.repository.UserRepository;
import com.mot.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

@Service
public class GoogleAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private SocialAccountsRepository socialAccountsRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Transactional
    public Map<String, Object> authenticateWithGoogle(GoogleLoginRequest request) {
        // Decode the ID token to get user info
        // In production, you should verify the token with Google's API
        // For now, we'll parse the payload from the credential

        String idToken = request.getIdToken();

        // Decode JWT payload (without verification for now)
        String[] parts = idToken.split("\\.");
        if (parts.length < 2) {
            throw new RuntimeException("Invalid Google ID token");
        }

        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode Google ID token", e);
        }

        // Parse JSON payload manually (avoiding additional dependencies)
        String email = extractJsonValue(payload, "email");
        String name = extractJsonValue(payload, "name");
        String picture = extractJsonValue(payload, "picture");
        String sub = extractJsonValue(payload, "sub"); // Google user ID

        if (email == null) {
            throw new RuntimeException("Email not found in Google token");
        }

        // Check if user exists by email
        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            // Update avatar if not set
            if (user.getAvatarUrl() == null && picture != null) {
                user.setAvatarUrl(picture);
                userRepository.save(user);
            }
        } else {
            // Create new user
            user = new User();
            user.setEmail(email);
            user.setUserName(name != null ? name : email.split("@")[0]);
            user.setAvatarUrl(picture);
            user.setActive(true);
            user.setCreatedAt(java.time.LocalDateTime.now());
            user.setUpdatedAt(java.time.LocalDateTime.now());

            Set<Role> roles = new HashSet<>();
            roleRepository.findRoleByName(ERole.ROLE_USER).ifPresent(roles::add);
            user.setRoles(roles);

            user = userRepository.save(user);
            log.info("Created new user from Google login: {}", email);
        }

        // Link social account if not already linked
        Optional<SocialAccounts> existingSocial = socialAccountsRepository.findByProviderAndProviderId("google", sub);
        if (existingSocial.isEmpty()) {
            SocialAccounts socialAccount = new SocialAccounts();
            socialAccount.setProvider("google");
            socialAccount.setProviderId(sub);
            socialAccount.setLinkedAt(Date.valueOf(LocalDate.now()));
            socialAccount.setUser(user);
            socialAccountsRepository.save(socialAccount);
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(user);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("type", "Bearer");
        response.put("id", user.getId());
        response.put("username", user.getUserName());
        response.put("email", user.getEmail());
        response.put("roles", user.getRoles().stream().map(r -> r.getName().name()).toList());

        return response;
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);
        if (start == -1) return null;
        start += searchKey.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }
}
