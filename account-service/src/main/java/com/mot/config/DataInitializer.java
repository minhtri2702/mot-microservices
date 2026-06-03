package com.mot.config;

import com.mot.entity.Role;
import com.mot.enums.ERole;
import com.mot.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        // Initialize roles if they don't exist
        for (ERole role : ERole.values()) {
            if (roleRepository.findRoleByName(role).isEmpty()) {
                Role newRole = new Role();
                newRole.setId(role.ordinal() + 1); // Set ID manually: 1=ROLE_USER, 2=ROLE_ADMIN, 3=ROLE_MODERATOR
                newRole.setName(role);
                roleRepository.save(newRole);
                log.info("Created role: {} with id={}", role.name(), role.ordinal() + 1);
            }
        }
    }
}
