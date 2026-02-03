package com.mot.services;


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
import io.jsonwebtoken.io.Decoders;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    @Autowired
    private final UserRepository userRepository;
    @Autowired
    private final RoleRepository roleRepository;
    @Autowired
    private final SocialAccountsRepository socialAccountsRepository;
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


}

