package com.demo.event.repository;

import com.demo.event.model.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.awt.print.Pageable;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Danh sách thông báo của user, phân trang, mới nhất trước
    Page<Notification> findByUserIdOrderBySentAtDesc(Long userId, Pageable pageable);

    // Đếm thông báo chưa đọc
    long countByUserIdAndIsReadFalse(Long userId);

    // Đánh dấu tất cả đã đọc
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") Long userId);
}
