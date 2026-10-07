package com.rbownworkshop.server.service;

import com.rbownworkshop.server.entity.ShopOrder;
import com.rbownworkshop.server.repository.ShopOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
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

    /** Rank để ưu tiên khi cùng ngày: SUPER_VIP (0) < VIP (1) < NORMAL (2). */
    private static final Map<String, Integer> TYPE_RANK = Map.of(
            "SUPER_VIP", 0,
            "VIP",       1,
            "NORMAL",    2
    );

    /**
     * Truncate + insert data. Cập nhật savedRows liên tục vào registry.
     * Return void ngay lập tức, code chạy ở thread từ pool "uploadExecutor".
     *
     * STT (sheetSequence) được đánh dùng chung cho CẢ 3 loại:
     *   1. Sort theo orderDate ASC (null xếp cuối)
     *   2. Cùng ngày: SUPER_VIP → VIP → NORMAL
     *   3. Đánh số 1..N theo thứ tự đã sort
     */
    @Async("uploadExecutor")
    public void process(String taskId, List<ShopOrder> parsed) {
        try {
            repository.truncate();

            // Sort toàn bộ danh sách để đánh STT dùng chung
            parsed.sort(
                    Comparator
                            // Null orderDate xếp cuối
                            .comparing(ShopOrder::getOrderDate,
                                    Comparator.nullsLast(Comparator.naturalOrder()))
                            // Cùng ngày: VIP ưu tiên hơn
                            .thenComparing((ShopOrder o) ->
                                    TYPE_RANK.getOrDefault(o.getSheetType(), Integer.MAX_VALUE))
            );

            LocalDateTime now = LocalDateTime.now();
            int saved = 0;
            int seq = 0;

            for (ShopOrder o : parsed) {
                seq++;
                o.setSheetSequence(seq);
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
