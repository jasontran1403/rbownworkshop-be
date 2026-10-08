package com.rbownworkshop.server.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Chạy khi app khởi động: DROP các cột cũ không còn dùng của bảng shop_order.
 * Dùng {@code ALTER TABLE ... DROP COLUMN IF EXISTS ...} (MySQL 8+).
 * Hibernate với {@code ddl-auto: update} KHÔNG tự drop cột khi field bị xoá
 * khỏi entity, nên phải làm bằng tay chỗ này.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SchemaMigrationService {

    private static final List<String> DROPPED_COLUMNS = List.of(
            "password",
            "protection_code",
            "region_ip",
            "game_20k",
            "delivery_date",
            "region_transfer_status",
            "deposit_refund",
            "note_r_bown",
            "note_log_acc",
            "note_add_money_log_acc",
            "note_acc_error",
            "note_add_money_fix_acc"
    );

    private final JdbcTemplate jdbc;

    @PostConstruct
    public void dropLegacyColumns() {
        for (String col : DROPPED_COLUMNS) {
            try {
                jdbc.execute("ALTER TABLE shop_order DROP COLUMN IF EXISTS `" + col + "`");
                log.info("[schema-migration] dropped column shop_order.{} (if existed)", col);
            } catch (Exception e) {
                log.warn("[schema-migration] could not drop column {}: {}", col, e.getMessage());
            }
        }

        // Mở rộng app_config.config_value để chứa HTML của thông báo (có thể dài)
        try {
            jdbc.execute("ALTER TABLE app_config MODIFY COLUMN config_value LONGTEXT");
            log.info("[schema-migration] extended app_config.config_value to LONGTEXT");
        } catch (Exception e) {
            log.warn("[schema-migration] could not extend app_config.config_value: {}", e.getMessage());
        }
    }
}
