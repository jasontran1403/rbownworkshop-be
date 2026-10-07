package com.rbownworkshop.server.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Sau khi update form import (file Update_Form_Website.xlsx):
 *   Giữ lại các cột khớp header:
 *     Tên Khách, Chỗ Order, Gói Dịch Vụ, Tài Khoản,
 *     Đăng Ký Ưu Tiên, Phí Ưu Tiên,
 *     Đăng Ký Chọn Vùng, Vùng Chọn,
 *     Đăng Ký Chọn Game, Game Chọn,
 *     Giá Chốt, Thực Nhận,
 *     Ngày Đặt, Số Ngày Treo,
 *     Tình Trạng Hoàn Tiền, Tình Trạng Đơn
 *
 *   Bỏ hẳn: password, protectionCode, regionIp (thay bằng regionRegister+regionSelected),
 *           game20k (thay bằng gameRegister+gameSelected), deliveryDate,
 *           regionTransferStatus, depositRefund, noteRBown,
 *           noteLogAcc, noteAddMoneyLogAcc, noteAccError, noteAddMoneyFixAcc.
 *
 *   Các cột bị bỏ sẽ được DROP bằng SchemaMigrationService khi app khởi động.
 */
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

    /** Đăng Ký Chọn Vùng (Có/Không). */
    @Column(name = "region_register", length = 50)
    private String regionRegister;

    /** Vùng Chọn (text). */
    @Column(name = "region_selected", length = 255)
    private String regionSelected;

    /** Đăng Ký Chọn Game (Có/Không). */
    @Column(name = "game_register", length = 50)
    private String gameRegister;

    /** Game Chọn (text). */
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
     * STT dùng chung giữa cả 3 loại, sắp xếp theo (orderDate ASC, sheetType rank ASC).
     * Rank: SUPER_VIP = 0, VIP = 1, NORMAL = 2.
     */
    @Column(name = "sheet_sequence")
    private Integer sheetSequence;

    @Column(name = "sheet_type", length = 20)
    private String sheetType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
