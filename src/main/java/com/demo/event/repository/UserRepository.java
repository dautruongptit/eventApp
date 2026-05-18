package com.demo.event.repository;

import com.demo.event.model.entity.User;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndIsActiveTrue(String email);

    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE User u SET u.totalEvents = u.totalEvents + 1 WHERE u.id = :id")
    void incrementEventCount(@Param("id") Long userId);

    @Modifying
    @Query("UPDATE User u SET u.totalRelatives = u.totalRelatives + 1 WHERE u.id = :id")
    void incrementRelativeCount(@Param("id") Long userId);

    @Modifying
    @Query("UPDATE User u SET u.totalEvents = u.totalEvents - 1 WHERE u.id = :id AND u.totalEvents > 0")
    void decrementEventCount(@Param("id") Long userId);

    @Modifying
    @Query("UPDATE User u SET u.totalRelatives = u.totalRelatives - 1 WHERE u.id = :id AND u.totalRelatives > 0")
    void decrementRelativeCount(@Param("id") Long userId);
    Optional<User> findByEmail(String email);

}
