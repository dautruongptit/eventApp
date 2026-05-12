package com.demo.event.repository;

import com.demo.event.model.entity.Relative;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RelativeRepository extends JpaRepository<Relative, Long> {

    // Lấy tất cả người thân của user, hỗ trợ filter groupType và search tên
    @Query("SELECT r FROM Relative r WHERE r.user.id = :userId " +
            "AND (:groupType IS NULL OR r.groupType = :groupType) " +
            "AND (:search IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%',:search,'%'))) " +
            "ORDER BY r.name ASC")
    List<Relative> findByFilters(
            @Param("userId") Long userId,
            @Param("groupType") Relative.GroupType groupType,
            @Param("search") String search);

    // Đếm theo từng nhóm để hiển thị GroupSummary
    @Query("SELECT r.groupType, COUNT(r) FROM Relative r " +
            "WHERE r.user.id = :userId GROUP BY r.groupType")
    List<Object[]> countByGroupType(@Param("userId") Long userId);

    // Tìm người thân theo id + user (tránh truy cập chéo giữa users)
    Optional<Relative> findByIdAndUserId(Long id, Long userId);

    // Tổng số người thân theo user
    long countByUserId(Long userId);

    @Modifying
    @Query("UPDATE Relative r SET r.totalEvents = r.totalEvents + 1 WHERE r.id = :id")
    void incrementEventCount(@Param("id") Long relativeId);

    /** Giảm counter sự kiện khi xoá event (min 0). */
    @Modifying
    @Query("UPDATE Relative r SET r.totalEvents = r.totalEvents - 1 WHERE r.id = :id AND r.totalEvents > 0")
    void decrementRelativeEventCount(@Param("id") Long relativeId);

}
