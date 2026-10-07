package com.rbownworkshop.server.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Singleton (id = 1) lưu trạng thái passcode của trang /management:
 *   - failedAttempts: số lần nhập sai liên tiếp
 *   - lockedAt:       thời điểm bị khóa (null = chưa bị khóa)
 *
 * Khi lockedAt + LOCK_MINUTES vẫn chưa qua → đang bị khóa.
 * Khi đã qua → xóa record, cho phép vào.
 */
@Entity
@Table(name = "management_access")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagementAccess {

    @Id
    private Long id;

    @Column(name = "failed_attempts", nullable = false)
    private Integer failedAttempts;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;
}
