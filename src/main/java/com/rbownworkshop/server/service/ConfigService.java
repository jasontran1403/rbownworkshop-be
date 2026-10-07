package com.rbownworkshop.server.service;

import com.rbownworkshop.server.dto.response.ProcessingNumberResponse;
import com.rbownworkshop.server.entity.AppConfig;
import com.rbownworkshop.server.repository.AppConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConfigService {

    private static final String KEY_NORMAL    = "current_processing_number";          // giữ cũ cho NORMAL
    private static final String KEY_VIP       = "current_processing_number_vip";
    private static final String KEY_SUPER_VIP = "current_processing_number_super_vip";

    /** Map sheetType → config key. */
    private static final Map<String, String> KEY_BY_TYPE = Map.of(
            "NORMAL",    KEY_NORMAL,
            "VIP",       KEY_VIP,
            "SUPER_VIP", KEY_SUPER_VIP
    );

    private final AppConfigRepository repository;

    // ============================================================
    //  3 loại
    // ============================================================

    public ProcessingNumberResponse getAll() {
        return ProcessingNumberResponse.builder()
                .superVip(readInt(KEY_SUPER_VIP))
                .vip(readInt(KEY_VIP))
                .normal(readInt(KEY_NORMAL))
                .currentProcessingNumber(readInt(KEY_NORMAL))
                .build();
    }

    @Transactional
    public ProcessingNumberResponse setByType(String sheetType, Integer value) {
        String key = KEY_BY_TYPE.get(sheetType);
        if (key == null) {
            throw new IllegalArgumentException("sheetType không hợp lệ: " + sheetType);
        }
        writeInt(key, value);
        return getAll();
    }

    // ============================================================
    //  Backwards-compat API (dùng key NORMAL)
    // ============================================================

    public Integer getCurrentProcessingNumber() {
        return readInt(KEY_NORMAL);
    }

    @Transactional
    public Integer setCurrentProcessingNumber(Integer value) {
        writeInt(KEY_NORMAL, value);
        return value;
    }

    // ============================================================
    //  Helpers
    // ============================================================

    private Integer readInt(String key) {
        return repository.findById(key)
                .map(c -> {
                    try { return Integer.parseInt(c.getValue()); }
                    catch (NumberFormatException e) { return null; }
                })
                .orElse(null);
    }

    private void writeInt(String key, Integer value) {
        if (value == null) {
            repository.deleteById(key);
            return;
        }
        AppConfig cfg = repository.findById(key)
                .orElse(AppConfig.builder().key(key).build());
        cfg.setValue(String.valueOf(value));
        repository.save(cfg);
    }
}
