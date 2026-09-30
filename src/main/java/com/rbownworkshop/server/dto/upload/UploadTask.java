package com.rbownworkshop.server.dto.upload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Trạng thái của 1 upload task (in-memory).
 * Được trả về cho FE qua GET /api/shop-orders/upload/status/{taskId}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UploadTask {
    private String taskId;
    private String status;            // "RUNNING" | "DONE" | "ERROR"
    private long estimatedMillis;     // dự kiến tổng thời gian xử lý
    private long startedAt;           // epoch ms
    private Long completedAt;         // epoch ms, null nếu chưa xong
    private int totalRows;
    private int savedRows;
    private Integer currentProcessingNumber; // sau khi DONE
    private String errorMessage;      // khi ERROR
}