package com.mot.services;


import com.mot.entity.Role;
import com.mot.entity.User;
import com.mot.enums.ERole;
import com.mot.exception.MasterDataIsNotFound;
import com.mot.exception.UnprocessableEntityException;
import com.mot.payload.request.SignupRequest;
import com.mot.repository.RoleRepository;
import com.mot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    @Autowired
    private final UserRepository userRepository;
    @Autowired
    private final RoleRepository roleRepository;

    public User registerUser(SignupRequest signupRequest) {
        if (!userRepository.findUsersByUserNameOrEmail(signupRequest.getUserName(), signupRequest.getEmail()).isEmpty()) {
            throw new UnprocessableEntityException("Username or email is already:" + signupRequest.getUserName());
        }
        User user = new User();
        user.setEmail(signupRequest.getEmail());
        user.setUserName(signupRequest.getUserName());
        user.setPassword(signupRequest.getPassWord());
        user = userRepository.save(user);
        Set<String> sRole = signupRequest.getRole();
        Set<Role> roles = new HashSet<>();
        Role role = roleRepository.findRoleByName(ERole.ROLE_USER).orElseThrow(() -> new MasterDataIsNotFound("ERROR: Role data is not found."));
        if (CollectionUtils.isEmpty(sRole)) {
            roles.add(role);
        }else{

        }
        return user;
    }
}

