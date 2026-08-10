package com.demo.event.repository;

import com.demo.event.model.entity.Event;
import com.demo.event.model.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderBySentAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") Long userId);

    /**
     * Kiem tra event nay da tao notification cho ngay hom nay chua
     * (tranh Scheduler tao trung neu chay lai nhieu lan trong ngay).
     */
    @Query("SELECT COUNT(n) > 0 FROM Notification n WHERE n.event = :event"
         + " AND FUNCTION('DATE', n.sentAt) = :today")
    boolean existsByEventAndSentToday(@Param("event") Event event, @Param("today") LocalDate today);
}
