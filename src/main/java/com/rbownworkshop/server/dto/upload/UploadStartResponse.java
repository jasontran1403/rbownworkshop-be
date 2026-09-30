package com.rbownworkshop.server.dto.upload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response trả ngay khi FE gọi POST /upload — task đã bắt đầu chạy nền.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadStartResponse {
    private String taskId;
    private long estimatedMillis;
    private int totalRows;
}