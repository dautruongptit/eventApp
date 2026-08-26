package com.demo.event.repository;

import com.demo.event.model.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderBySentAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") Long userId);

    /** Thông báo gần nhất của 1 reminder — ReminderScheduler dùng để biết
     * lần bắn trước (của reminder lặp lại) đã được đọc hay chưa. */
    Optional<Notification> findFirstByReminderIdOrderBySentAtDesc(Long reminderId);
}
