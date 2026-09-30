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
    private Integer currentProcessingNumber;
    private Integer totalOrders;
}