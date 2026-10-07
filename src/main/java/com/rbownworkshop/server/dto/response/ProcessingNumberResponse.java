package com.rbownworkshop.server.dto.response;

import lombok.*;

/**
 * 3 số "đang xử lý" — một cho mỗi loại đơn:
 *   superVip = Ưu tiên VIP
 *   vip      = Ưu tiên
 *   normal   = Thường (Khách Order)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessingNumberResponse {
    private Integer superVip;
    private Integer vip;
    private Integer normal;

    /** Giữ lại cho tương thích ngược — luôn trả về normal. */
    private Integer currentProcessingNumber;
}
