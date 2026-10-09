package com.rbownworkshop.server.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "shop_order", indexes = {
        @Index(name = "idx_account", columnList = "account"),
        @Index(name = "idx_order_date", columnList = "order_date"),
        @Index(name = "idx_sheet_type", columnList = "sheet_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "order_place", length = 255)
    private String orderPlace;

    @Column(name = "service_package", length = 255)
    private String servicePackage;

    @Column(name = "account", length = 255, nullable = false)
    private String account;

    @Column(name = "priority_register", length = 255)
    private String priorityRegister;

    @Column(name = "priority_fee", precision = 18, scale = 2)
    private BigDecimal priorityFee;

    @Column(name = "region_register", length = 50)
    private String regionRegister;

    @Column(name = "region_selected", length = 255)
    private String regionSelected;

    @Column(name = "game_register", length = 50)
    private String gameRegister;

    @Column(name = "game_selected", length = 255)
    private String gameSelected;

    @Column(name = "service_price", precision = 18, scale = 2)
    private BigDecimal servicePrice;

    @Column(name = "actual_amount", precision = 18, scale = 2)
    private BigDecimal actualAmount;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "hold_days")
    private Integer holdDays;

    @Column(name = "refund_note_status", length = 255)
    private String refundNoteStatus;

    @Column(name = "order_note_status", length = 255)
    private String orderNoteStatus;

    /**
     * STT RIÊNG của mỗi loại (sheetType), được đánh 1..N độc lập:
     *   - SUPER_VIP: 1..N  (Ưu tiên VIP)
     *   - VIP:       1..N  (Ưu tiên)
     *   - NORMAL:    1..N  (Khách Order)
     *
     * Trong mỗi loại, sắp xếp theo orderDate ASC (null xếp cuối).
     * Xem UploadAsyncService.process() để biết chi tiết.
     */
    @Column(name = "sheet_sequence")
    private Integer sheetSequence;

    @Column(name = "sheet_type", length = 20)
    private String sheetType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}