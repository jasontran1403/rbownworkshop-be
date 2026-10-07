package com.rbownworkshop.server.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResult {
    private Long id;
    private Integer sheetSequence;
    private String sheetType;

    // Tên gói / Giá
    private String servicePackage;
    private BigDecimal servicePrice;

    // Đăng ký ưu tiên / Phí ưu tiên
    private String priorityRegister;
    private BigDecimal priorityFee;

    // Đăng ký chọn vùng / Vùng chọn
    private String regionRegister;
    private String regionSelected;

    // Đăng ký chọn game / Game chọn
    private String gameRegister;
    private String gameSelected;

    // Ngày đăng ký
    private LocalDate registerDate;

    // Số ngày treo
    private Integer holdDays;

    // TT hoàn tiền / TT đơn
    private String refundNoteStatus;
    private String orderNoteStatus;
}
