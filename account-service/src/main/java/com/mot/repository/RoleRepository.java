package com.mot.repository;

import com.fasterxml.jackson.databind.node.LongNode;
import com.mot.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findRoleByName(Enum Name);

}
