package com.rbownworkshop.server.service;

import com.rbownworkshop.server.entity.ShopOrder;
import com.rbownworkshop.server.repository.ShopOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
     *
     * STT (sheetSequence) được đánh RIÊNG cho TỪNG LOẠI:
     *   - Trong mỗi loại (SUPER_VIP / VIP / NORMAL):
     *       1. Sort theo orderDate ASC (null xếp cuối)
     *       2. Đánh số 1..N theo thứ tự đã sort
     *   - 3 loại độc lập, mỗi loại đều bắt đầu từ 1.
     */
    @Async("uploadExecutor")
    public void process(String taskId, List<ShopOrder> parsed) {
        try {
            repository.truncate();

            // Group theo sheetType rồi đánh STT riêng từng nhóm
            Map<String, List<ShopOrder>> byType = new LinkedHashMap<>();
            for (ShopOrder o : parsed) {
                String type = o.getSheetType() == null ? "" : o.getSheetType();
                byType.computeIfAbsent(type, k -> new ArrayList<>()).add(o);
            }

            for (List<ShopOrder> group : byType.values()) {
                group.sort(
                        Comparator.comparing(ShopOrder::getOrderDate,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                );
                int seq = 0;
                for (ShopOrder o : group) {
                    seq++;
                    o.setSheetSequence(seq);
                }
            }

            LocalDateTime now = LocalDateTime.now();
            int saved = 0;

            for (ShopOrder o : parsed) {
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
