package com.demo.event.repository;

import com.demo.event.model.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Tim role theo ten.
     * Dung trong AuthService de gan ROLE_USER khi dang ky.
     */
    Optional<Role> findByName(String name);
}
