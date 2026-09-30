package com.rbownworkshop.server.dto.response;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadResponse {
    private int totalRows;
    private int savedRows;
    private int skippedRows;
    private List<String> errors;
    private Integer currentProcessingNumber;
}
