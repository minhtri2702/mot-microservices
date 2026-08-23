package com.mot.services;


import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.mot.entity.Role;
import com.mot.entity.SocialAccounts;
import com.mot.entity.User;
import com.mot.enums.ERole;
import com.mot.exception.UnprocessableEntityException;
import com.mot.payload.request.SignupRequest;
import com.mot.repository.RoleRepository;
import com.mot.repository.SocialAccountsRepository;
import com.mot.repository.UserRepository;
import com.mot.payload.response.JwtResponse;
import com.mot.security.JwtUtil;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.Key;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    @Autowired
    private final UserRepository userRepository;
    @Autowired
    private final RoleRepository roleRepository;
    @Autowired
    private final SocialAccountsRepository socialAccountsRepository;
    private final PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;

    @Value("${mot.app.jwtSecret}")
    private String jwtSecret;

    public User registerUser(SignupRequest signupRequest) {
        if (!userRepository.findUsersByUserNameOrEmail(signupRequest.getUserName(), signupRequest.getEmail()).isEmpty()) {
            throw new UnprocessableEntityException("Username or email is already:" + signupRequest.getUserName());
        }
        User user = new User();
        user.setEmail(signupRequest.getEmail());
        user.setUserName(signupRequest.getUserName());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassWord()));
        user.setActive(true);


        SocialAccounts socialAccounts = new SocialAccounts();
        socialAccounts.setUser(user);
        socialAccounts.setProvider("manual registration");
        Set<Role> roles = new HashSet<>();
        Role role = getOrCreateRole(ERole.ROLE_USER);
        // Public signup must never accept roles supplied by the client.
        roles.add(role);
        user.setRoles(roles);
        user = userRepository.save(user);
        socialAccounts = socialAccountsRepository.save(socialAccounts);
        return user;
    }
    public JwtResponse login(String username, String password) {
        User user = userRepository.findWithRolesByUserName(username)
                .orElseThrow(() -> new UnprocessableEntityException("Invalid username or password"));

        if (socialAccountsRepository.existsByUser_IdAndProvider(user.getId(), "google")) {
            throw new UnprocessableEntityException("Use Google sign-in for this account");
        }

        String storedPassword = user.getPassword();
        boolean passwordMatches = storedPassword != null &&
                (storedPassword.startsWith("$2")
                        ? passwordEncoder.matches(password, storedPassword)
                        : password.equals(storedPassword));
        if (!passwordMatches) {
            throw new UnprocessableEntityException("Invalid username or password");
        }

        // Transparently migrate accounts created before BCrypt was enabled.
        if (!storedPassword.startsWith("$2")) {
            user.setPassword(passwordEncoder.encode(password));
            userRepository.save(user);
        }

        String token = jwtUtil.generateToken(user);
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());

        return new JwtResponse(token, "Bearer", user.getId().toString(), user.getUserName(), user.getEmail(), roles);
    }

    @Transactional
    public JwtResponse googleLogin(String idToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList("695451075871-jtr8p39l8dm7inm3bgmiqb2p912fg9o3.apps.googleusercontent.com"))
                    .build();

            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                throw new UnprocessableEntityException("Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String googleId = payload.getSubject();
            String avatarUrl = (String) payload.get("picture");

            // Check if user exists by email
            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                // Create new user
                user = new User();
                user.setEmail(email);
                user.setUserName(name != null ? name : email.split("@")[0]);
                user.setPassword(null);
                user.setActive(true);
                user.setAvatarUrl(avatarUrl);

                user = userRepository.save(user);

                SocialAccounts socialAccounts = new SocialAccounts();
                socialAccounts.setUser(user);
                socialAccounts.setProvider("google");
                socialAccounts.setProviderId(googleId);
                socialAccountsRepository.save(socialAccounts);
            }

            // Master role data may be missing after a database restore. Repair it
            // here and ensure both new and existing Google users keep a base role.
            Role userRole = getOrCreateRole(ERole.ROLE_USER);
            if (user.getRoles() == null) {
                user.setRoles(new HashSet<>());
            }
            if (user.getRoles().stream().noneMatch(role -> role.getName() == ERole.ROLE_USER)) {
                user.getRoles().add(userRole);
                user = userRepository.save(user);
            }

            String token = jwtUtil.generateToken(user);
            List<String> roles = user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toList());

            return new JwtResponse(token, "Bearer", user.getId().toString(), user.getUserName(), user.getEmail(), roles);
        } catch (Exception e) {
            throw new UnprocessableEntityException("Google login failed: " + e.getMessage());
        }
    }

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    private Role getOrCreateRole(ERole roleName) {
        return roleRepository.findRoleByName(roleName).orElseGet(() -> {
            Role role = new Role();
            role.setName(roleName);
            return roleRepository.save(role);
        });
    }
}
