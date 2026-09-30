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
    private String servicePackage;
    private BigDecimal servicePrice;
    private String orderStatus;
    private LocalDate registerDate;
    private LocalDate completeDate;
    private String refundStatus;
    private String refundInfo;

    // Cột mới
    private String priorityRegister;
    private BigDecimal priorityFee;
    private String refundNoteStatus;
    private String orderNoteStatus;
}