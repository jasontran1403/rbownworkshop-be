package com.rbownworkshop.server.service;

import com.rbownworkshop.server.entity.ShopOrder;
import com.rbownworkshop.server.repository.ShopOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
     *
     * STT (sheetSequence) được đánh ĐỘC LẬP cho TỪNG LOẠI (sheetType):
     *   - SUPER_VIP (Ưu tiên VIP): 1..N
     *   - VIP       (Ưu tiên):     1..N
     *   - NORMAL    (Khách Order): 1..N
     *
     * KHÔNG sort lại — giữ ĐÚNG thứ tự dòng trên Excel (sau khi ExcelParserService
     * đã lọc header / dòng tổng kết / dòng rỗng). Nhờ vậy:
     *   dòng Excel #N (bỏ header) ↔ STT = N trong loại tương ứng.
     */
    @Async("uploadExecutor")
    public void process(String taskId, List<ShopOrder> parsed) {
        try {
            repository.truncate();

            // 1. Group theo sheetType, GIỮ NGUYÊN thứ tự parse gốc trong mỗi group.
            Map<String, List<ShopOrder>> byType = new LinkedHashMap<>();
            for (ShopOrder o : parsed) {
                String type = o.getSheetType() == null ? "" : o.getSheetType();
                byType.computeIfAbsent(type, k -> new ArrayList<>()).add(o);
            }

            // 2. Mỗi group đánh STT 1..N theo đúng thứ tự Excel (không sort).
            for (Map.Entry<String, List<ShopOrder>> e : byType.entrySet()) {
                List<ShopOrder> group = e.getValue();
                int seq = 0;
                for (ShopOrder o : group) {
                    seq++;
                    o.setSheetSequence(seq);
                }
            }

            // 3. Save theo đúng thứ tự STT đã đánh (từng group) → id DB tăng
            //    cùng chiều với sheetSequence trong mỗi loại.
            LocalDateTime now = LocalDateTime.now();
            int saved = 0;

            for (List<ShopOrder> group : byType.values()) {
                for (ShopOrder o : group) {
                    o.setCreatedAt(now);
                    repository.save(o);
                    saved++;
                    if (saved % PROGRESS_TICK == 0) {
                        taskRegistry.updateSavedRows(taskId, saved);
                    }
                }
            }

            int currentMax = repository.findMaxId().intValue();
            taskRegistry.complete(taskId, currentMax);
        } catch (Exception e) {
            taskRegistry.fail(taskId, e.getMessage() != null ? e.getMessage() : "Lỗi không xác định");
        }
    }
}