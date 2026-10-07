package com.rbownworkshop.server.service;

import com.rbownworkshop.server.entity.ManagementAccess;
import com.rbownworkshop.server.repository.ManagementAccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Logic:
 *   - Passcode đúng: xóa record (nếu có) → allowed=true.
 *   - Passcode sai: tăng failedAttempts; nếu ≥ MAX_ATTEMPTS → set lockedAt=now.
 *   - Khi check status: nếu có lockedAt và chưa qua LOCK_MINUTES → đang khóa;
 *     nếu đã qua → xóa record, trả allowed=true.
 */
@Service
@RequiredArgsConstructor
public class ManagementAccessService {

    public static final String PASSCODE = "Dg280520$";
    public static final int MAX_ATTEMPTS = 5;
    public static final int LOCK_MINUTES = 10;
    private static final long SINGLETON_ID = 1L;

    private final ManagementAccessRepository repo;

    /** GET status — có đang khóa không. */
    @Transactional
    public Map<String, Object> getStatus() {
        ManagementAccess rec = repo.findById(SINGLETON_ID).orElse(null);
        return buildStatus(rec, null);
    }

    /** POST verify passcode. */
    @Transactional
    public Map<String, Object> verify(String passcode) {
        ManagementAccess rec = repo.findById(SINGLETON_ID).orElse(null);

        // Nếu đang khóa → không cho verify
        if (isLocked(rec)) {
            Map<String, Object> out = buildStatus(rec, null);
            out.put("success", false);
            out.put("reason", "LOCKED");
            return out;
        }

        if (Objects.equals(passcode, PASSCODE)) {
            // Đúng → clear record
            if (rec != null) repo.delete(rec);
            Map<String, Object> out = new HashMap<>();
            out.put("success", true);
            out.put("locked", false);
            return out;
        }

        // Sai → tăng failedAttempts
        if (rec == null) {
            rec = ManagementAccess.builder()
                    .id(SINGLETON_ID)
                    .failedAttempts(0)
                    .lockedAt(null)
                    .build();
        }
        int attempts = (rec.getFailedAttempts() == null ? 0 : rec.getFailedAttempts()) + 1;
        rec.setFailedAttempts(attempts);
        if (attempts >= MAX_ATTEMPTS) {
            rec.setLockedAt(LocalDateTime.now());
        }
        repo.save(rec);

        Map<String, Object> out = buildStatus(rec, attempts);
        out.put("success", false);
        out.put("reason", "WRONG_PASSCODE");
        return out;
    }

    // ============================================================
    //  Helpers
    // ============================================================

    private boolean isLocked(ManagementAccess rec) {
        if (rec == null || rec.getLockedAt() == null) return false;
        LocalDateTime unlockAt = rec.getLockedAt().plusMinutes(LOCK_MINUTES);
        if (LocalDateTime.now().isBefore(unlockAt)) {
            return true;
        }
        // Đã quá hạn → xóa record (auto-unlock)
        repo.delete(rec);
        return false;
    }

    /**
     * Build status map. Nếu record có lockedAt chưa quá hạn → trả locked=true + remainingSeconds.
     * Nếu đã quá hạn → xóa record, trả locked=false.
     */
    private Map<String, Object> buildStatus(ManagementAccess rec, Integer attempts) {
        Map<String, Object> out = new HashMap<>();
        if (rec == null) {
            out.put("locked", false);
            out.put("failedAttempts", 0);
            out.put("remainingAttempts", MAX_ATTEMPTS);
            return out;
        }

        if (rec.getLockedAt() != null) {
            LocalDateTime unlockAt = rec.getLockedAt().plusMinutes(LOCK_MINUTES);
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(unlockAt)) {
                long remainingSec = Duration.between(now, unlockAt).getSeconds();
                out.put("locked", true);
                out.put("remainingSeconds", remainingSec);
                out.put("lockMinutes", LOCK_MINUTES);
                return out;
            }
            // Đã hết khóa → xóa record
            repo.delete(rec);
            out.put("locked", false);
            out.put("failedAttempts", 0);
            out.put("remainingAttempts", MAX_ATTEMPTS);
            return out;
        }

        int used = attempts != null ? attempts
                : (rec.getFailedAttempts() == null ? 0 : rec.getFailedAttempts());
        out.put("locked", false);
        out.put("failedAttempts", used);
        out.put("remainingAttempts", Math.max(0, MAX_ATTEMPTS - used));
        return out;
    }
}
