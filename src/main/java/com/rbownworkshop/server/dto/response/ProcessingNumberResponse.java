package com.rbownworkshop.server.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessingNumberResponse {
    private Integer currentProcessingNumber;
}
