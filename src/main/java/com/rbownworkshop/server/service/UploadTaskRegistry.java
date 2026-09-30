package com.rbownworkshop.server.service;

import com.rbownworkshop.server.dto.upload.UploadTask;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry cho upload tasks. Tự cleanup task cũ mỗi 5 phút.
 * Task đã DONE/ERROR được giữ 10 phút để FE có thời gian đọc kết quả.
 */
@Component
public class UploadTaskRegistry {

    private static final long RETENTION_MS = 10 * 60 * 1000L; // 10 phút

    private final Map<String, UploadTask> tasks = new ConcurrentHashMap<>();

    public UploadTask create(long estimatedMillis, int totalRows) {
        String id = UUID.randomUUID().toString();
        UploadTask task = UploadTask.builder()
                .taskId(id)
                .status("RUNNING")
                .estimatedMillis(estimatedMillis)
                .startedAt(Instant.now().toEpochMilli())
                .totalRows(totalRows)
                .savedRows(0)
                .build();
        tasks.put(id, task);
        return task;
    }

    public UploadTask get(String taskId) {
        return tasks.get(taskId);
    }

    public void updateSavedRows(String taskId, int savedRows) {
        UploadTask t = tasks.get(taskId);
        if (t != null) t.setSavedRows(savedRows);
    }

    public void complete(String taskId, int currentProcessingNumber) {
        UploadTask t = tasks.get(taskId);
        if (t == null) return;
        t.setStatus("DONE");
        t.setCompletedAt(Instant.now().toEpochMilli());
        t.setSavedRows(t.getTotalRows());
        t.setCurrentProcessingNumber(currentProcessingNumber);
    }

    public void fail(String taskId, String errorMessage) {
        UploadTask t = tasks.get(taskId);
        if (t == null) return;
        t.setStatus("ERROR");
        t.setCompletedAt(Instant.now().toEpochMilli());
        t.setErrorMessage(errorMessage);
    }

    /**
     * Xóa task đã kết thúc quá 10 phút.
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000L)
    public void cleanup() {
        long now = Instant.now().toEpochMilli();
        tasks.entrySet().removeIf(e -> {
            UploadTask t = e.getValue();
            Long done = t.getCompletedAt();
            return done != null && (now - done) > RETENTION_MS;
        });
    }
}