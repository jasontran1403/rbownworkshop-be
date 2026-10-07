package com.rbownworkshop.server.restcontroller;

import com.rbownworkshop.server.service.ManagementAccessService;
import com.rbownworkshop.server.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * API cho passcode của route /management.
 *   GET  /api/management-access/status
 *   POST /api/management-access/verify   body: { "passcode": "..." }
 */
@RestController
@RequestMapping("/api/management-access")
@RequiredArgsConstructor
public class ManagementAccessController {

    private static final int VERIFY_MAX = 10;
    private static final long VERIFY_WIN = 10_000L; // 10s

    private final ManagementAccessService service;
    private final RateLimitService rateLimitService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(service.getStatus());
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        String ip = getClientIp(request);
        rateLimitService.check("mgmt-verify:" + ip, VERIFY_MAX, VERIFY_WIN);
        String passcode = body == null ? null : body.get("passcode");
        return ResponseEntity.ok(service.verify(passcode));
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
