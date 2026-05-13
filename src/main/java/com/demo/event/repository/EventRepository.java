package com.demo.event.repository;

import com.demo.event.model.entity.Event;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    // Upcoming events: từ today đến future, sắp xếp theo ngày
    @Query("SELECT e FROM Event e WHERE e.user.id = :userId " +
            "AND e.eventDate BETWEEN :from AND :to " +
            "AND e.isActive = true ORDER BY e.eventDate ASC")
    List<Event> findUpcoming(@Param("userId") Long userId,
                             @Param("from") LocalDate from,
                             @Param("to") LocalDate to,
                             Pageable pageable);

    // Sự kiện bản thân (relativeId IS NULL) sắp tới
    @Query("SELECT e FROM Event e WHERE e.user.id = :userId " +
            "AND e.relative IS NULL AND e.eventDate >= :today " +
            "AND e.isActive = true ORDER BY e.eventDate ASC")
    List<Event> findMyUpcoming(@Param("userId") Long userId,
                               @Param("today") LocalDate today,
                               Pageable pageable);

    // Filter đa điều kiện cho màn hình Sự kiện
    @Query("SELECT e FROM Event e WHERE e.user.id = :userId " +
            "AND (:type IS NULL OR e.eventType = :type) " +
            "AND (:relativeId IS NULL OR e.relative.id = :relativeId) " +
            "AND (:month IS NULL OR MONTH(e.eventDate) = :month) " +
            "AND (:year IS NULL OR YEAR(e.eventDate) = :year) " +
            "AND e.isActive = true ORDER BY e.eventDate ASC")
    List<Event> findFiltered(
            @Param("userId") Long userId,
            @Param("type") Event.EventType type,
            @Param("relativeId") Long relativeId,
            @Param("month") Integer month,
            @Param("year") Integer year);

    // Sự kiện của người thân cụ thể
    List<Event> findByRelativeIdAndIsActiveTrueOrderByEventDateAsc(Long relativeId);

    // Các sự kiện cần nhắc hôm nay (dùng trong Scheduler)
    @Query("SELECT DISTINCT e FROM Event e " +
            "JOIN e.reminders r " +
            "WHERE e.isActive = true AND r.isEnabled = true " +
            "AND (DATEDIFF(e.eventDate, :today) = r.remindDaysBefore OR " +
            "     (r.remindHoursBefore IS NOT NULL AND e.eventDate = :today))")
    List<Event> findEventsNeedingReminderToday(@Param("today") LocalDate today);

    Optional<Event> findByIdAndUserId(Long id, Long userId);
}
