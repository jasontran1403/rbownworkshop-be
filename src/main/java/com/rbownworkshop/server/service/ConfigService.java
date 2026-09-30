package com.rbownworkshop.server.service;

import com.rbownworkshop.server.entity.AppConfig;
import com.rbownworkshop.server.repository.AppConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConfigService {

    private static final String KEY_CURRENT_PROCESSING = "current_processing_number";

    private final AppConfigRepository repository;

    public Integer getCurrentProcessingNumber() {
        return repository.findById(KEY_CURRENT_PROCESSING)
                .map(c -> {
                    try {
                        return Integer.parseInt(c.getValue());
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .orElse(null);
    }

    @Transactional
    public Integer setCurrentProcessingNumber(Integer value) {
        if (value == null) {
            repository.deleteById(KEY_CURRENT_PROCESSING);
            return null;
        }
        AppConfig cfg = repository.findById(KEY_CURRENT_PROCESSING)
                .orElse(AppConfig.builder().key(KEY_CURRENT_PROCESSING).build());
        cfg.setValue(String.valueOf(value));
        repository.save(cfg);
        return value;
    }
}
