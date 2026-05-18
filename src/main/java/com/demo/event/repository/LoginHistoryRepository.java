package com.demo.event.repository;

import com.demo.event.model.entity.LoginHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    // Lich su dang nhap cua user (phan trang, moi nhat truoc)
    Page<LoginHistory> findByUserIdOrderByLoginAtDesc(Long userId, Pageable pageable);

    // 5 lan dang nhap gan nhat (dung trong response profile)
    List<LoginHistory> findTop5ByUserIdOrderByLoginAtDesc(Long userId);

    // Dem so lan that bai trong khoang thoi gian (chong brute-force)
    @Query("SELECT COUNT(h) FROM LoginHistory h WHERE h.user.id = :userId AND h.isSuccess = false AND h.loginAt >= :since")
    long countFailuresSince(@Param("userId") Long userId,
                            @Param("since") LocalDateTime since);

    // Lay lich su tu IP cu the (phat hien dang nhap bat thuong)
    List<LoginHistory> findByIpAddressAndUserIdOrderByLoginAtDesc(
            String ipAddress, Long userId);
}
