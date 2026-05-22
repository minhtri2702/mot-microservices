package com.mot.services;


import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.mot.entity.Role;
import com.mot.entity.SocialAccounts;
import com.mot.entity.User;
import com.mot.enums.ERole;
import com.mot.exception.MasterDataIsNotFound;
import com.mot.exception.UnprocessableEntityException;
import com.mot.payload.request.SignupRequest;
import com.mot.repository.RoleRepository;
import com.mot.repository.SocialAccountsRepository;
import com.mot.repository.UserRepository;
import com.mot.payload.response.JwtResponse;
import com.mot.security.JwtUtil;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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
        user.setPassword(signupRequest.getPassWord());
        user.setActive(true);


        SocialAccounts socialAccounts = new SocialAccounts();
        socialAccounts.setUser(user);
        socialAccounts.setProvider("manual registration");
        Set<String> sRole = signupRequest.getRole();
        Set<Role> roles = new HashSet<>();
        Role role = roleRepository.findRoleByName(ERole.ROLE_USER).orElseThrow(() -> new MasterDataIsNotFound("ERROR: Role data is not found."));
        if (CollectionUtils.isEmpty(sRole)) {
            roles.add(role);
            user.setRoles(roles);
        }else{
            sRole.forEach(r-> {
                String rl = r == null ? "" : r.trim().toUpperCase();
                switch (r) {
                    case "ADMIN" , "ROLE_ADMIN":
                        Role roleAdmin = roleRepository.findRoleByName(ERole.ROLE_ADMIN).orElseThrow(()->new MasterDataIsNotFound("Error: Role is not found."));
                        roles.add(roleAdmin);
                    default:
                        Role roleUser = roleRepository.findRoleByName(ERole.ROLE_USER).orElseThrow(()->new MasterDataIsNotFound("Error: Role is not found."));
                        roles.add(roleUser);
                }

            });
        }
        user.setRoles(roles);
        user = userRepository.save(user);
        socialAccounts = socialAccountsRepository.save(socialAccounts);
        return user;
    }
    public JwtResponse login(String username, String password) {
        User user = userRepository.findWithRolesByUserName(username)
                .orElseThrow(() -> new UnprocessableEntityException("Invalid username or password"));

        if (!password.equals(user.getPassword())) {
            throw new UnprocessableEntityException("Invalid username or password");
        }

        String token = jwtUtil.generateToken(user);
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());

        return new JwtResponse(token, "Bearer", user.getId(), user.getUserName(), user.getEmail(), roles);
    }

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
                user.setPassword(googleId); // Use google ID as password
                user.setActive(true);
                user.setAvatarUrl(avatarUrl);

                Set<Role> roles = new HashSet<>();
                Role role = roleRepository.findRoleByName(ERole.ROLE_USER)
                        .orElseThrow(() -> new MasterDataIsNotFound("ERROR: Role data is not found."));
                roles.add(role);
                user.setRoles(roles);
                user = userRepository.save(user);

                SocialAccounts socialAccounts = new SocialAccounts();
                socialAccounts.setUser(user);
                socialAccounts.setProvider("google");
                socialAccountsRepository.save(socialAccounts);
            }

            String token = jwtUtil.generateToken(user);
            List<String> roles = user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toList());

            return new JwtResponse(token, "Bearer", user.getId(), user.getUserName(), user.getEmail(), roles);
        } catch (Exception e) {
            throw new UnprocessableEntityException("Google login failed: " + e.getMessage());
        }
    }

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }
}
