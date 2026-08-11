package com.demo.event.repository;

import com.demo.event.model.entity.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventCategoryRepository extends JpaRepository<EventCategory, Long> {

    Optional<EventCategory> findByCode(String code);

    /** Danh mục hệ thống + danh mục riêng của user, sắp theo sortOrder. */
    List<EventCategory> findByIsSystemTrueOrUser_IdOrderBySortOrderAsc(Long userId);
}
