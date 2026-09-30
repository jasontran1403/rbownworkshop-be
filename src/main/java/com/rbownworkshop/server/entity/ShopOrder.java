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

    @Column(name = "password", length = 255)
    private String password;

    @Lob
    @Column(name = "protection_code", columnDefinition = "TEXT")
    private String protectionCode;

    @Column(name = "priority_register", length = 255)
    private String priorityRegister;

    @Column(name = "priority_fee", precision = 18, scale = 2)
    private BigDecimal priorityFee;

    @Column(name = "region_ip", length = 255)
    private String regionIp;

    @Column(name = "game_20k", length = 255)
    private String game20k;

    @Column(name = "service_price", precision = 18, scale = 2)
    private BigDecimal servicePrice;

    @Column(name = "actual_amount", precision = 18, scale = 2)
    private BigDecimal actualAmount;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "hold_days")
    private Integer holdDays;

    @Column(name = "region_transfer_status", length = 255)
    private String regionTransferStatus;

    @Column(name = "deposit_refund", length = 255)
    private String depositRefund;

    @Column(name = "note_r_bown", columnDefinition = "TEXT")
    private String noteRBown;

    @Column(name = "refund_note_status", length = 255)
    private String refundNoteStatus;

    @Column(name = "order_note_status", length = 255)
    private String orderNoteStatus;

    @Column(name = "note_log_acc", columnDefinition = "TEXT")
    private String noteLogAcc;

    @Column(name = "note_add_money_log_acc", columnDefinition = "TEXT")
    private String noteAddMoneyLogAcc;

    @Column(name = "note_acc_error", columnDefinition = "TEXT")
    private String noteAccError;

    @Column(name = "note_add_money_fix_acc", columnDefinition = "TEXT")
    private String noteAddMoneyFixAcc;

    @Column(name = "sheet_sequence")
    private Integer sheetSequence;

    @Column(name = "sheet_type", length = 20)
    private String sheetType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}