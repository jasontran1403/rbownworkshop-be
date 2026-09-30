package com.rbownworkshop.server.service;

import com.rbownworkshop.server.exception.RateLimitException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    // Default: 20 request / 60s / key (dành cho /search public)
    private static final int DEFAULT_MAX_REQUESTS = 20;
    private static final long DEFAULT_WINDOW_MS = 60_000L;

    private final Map<String, Deque<Long>> store = new ConcurrentHashMap<>();

    /** Dùng limit mặc định. */
    public void check(String key) {
        check(key, DEFAULT_MAX_REQUESTS, DEFAULT_WINDOW_MS);
    }

    /**
     * Sliding window per key.
     * @param key         khóa (thường là "<scope>:<ip>")
     * @param maxRequests số request tối đa trong cửa sổ
     * @param windowMs    độ dài cửa sổ, tính bằng ms
     */
    public void check(String key, int maxRequests, long windowMs) {
        long now = Instant.now().toEpochMilli();
        Deque<Long> queue = store.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (queue) {
            while (!queue.isEmpty() && now - queue.peekFirst() > windowMs) {
                queue.pollFirst();
            }
            if (queue.size() >= maxRequests) {
                throw new RateLimitException(
                        "Thao tác quá nhanh, vui lòng chậm lại.");
            }
            queue.addLast(now);
        }
    }
}