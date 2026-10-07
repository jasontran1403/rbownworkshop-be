package com.rbownworkshop.server.dto.response;

import com.rbownworkshop.server.dto.OrderResult;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResponse {
    private List<OrderResult> results;

    /** 3 số "đang xử lý" theo loại. */
    private ProcessingNumberResponse processingNumbers;

    /** Giữ lại cho tương thích ngược (= processingNumbers.normal). */
    private Integer currentProcessingNumber;

    private Integer totalOrders;
}
