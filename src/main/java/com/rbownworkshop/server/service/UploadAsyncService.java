package com.rbownworkshop.server.service;

import com.rbownworkshop.server.entity.ShopOrder;
import com.rbownworkshop.server.repository.ShopOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service riêng cho phần xử lý bất đồng bộ của upload.
 * QUAN TRỌNG: phải nằm trong class KHÁC với ShopOrderService,
 * vì @Async chỉ hoạt động khi được gọi thông qua Spring proxy —
 * gọi self-invocation trong cùng class sẽ bypass proxy và chạy đồng bộ.
 */
@Service
@RequiredArgsConstructor
public class UploadAsyncService {

    private final ShopOrderRepository repository;
    private final UploadTaskRegistry taskRegistry;

    private static final int PROGRESS_TICK = 10;

    /**
     * Truncate + insert data. Cập nhật savedRows liên tục vào registry.
     * Return void ngay lập tức, code chạy ở thread từ pool "uploadExecutor".
     */
    @Async("uploadExecutor")
    public void process(String taskId, List<ShopOrder> parsed) {
        try {
            repository.truncate();

            Map<String, Long> seqByType = new HashMap<>();
            LocalDateTime now = LocalDateTime.now();
            int saved = 0;

            for (ShopOrder o : parsed) {
                long nextSeq = seqByType.merge(o.getSheetType(), 1L, Long::sum);
                o.setSheetSequence((int) nextSeq);
                o.setCreatedAt(now);
                repository.save(o);
                saved++;
                if (saved % PROGRESS_TICK == 0) {
                    taskRegistry.updateSavedRows(taskId, saved);
                }
            }

            int currentMax = repository.findMaxId().intValue();
            taskRegistry.complete(taskId, currentMax);
        } catch (Exception e) {
            taskRegistry.fail(taskId, e.getMessage() != null ? e.getMessage() : "Lỗi không xác định");
        }
    }
}