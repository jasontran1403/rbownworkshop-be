package com.rbownworkshop.server.service;

import com.rbownworkshop.server.dto.OrderResult;
import com.rbownworkshop.server.dto.response.*;
import com.rbownworkshop.server.dto.upload.UploadStartResponse;
import com.rbownworkshop.server.dto.upload.UploadTask;
import com.rbownworkshop.server.entity.ShopOrder;
import com.rbownworkshop.server.exception.ExcelParseException;
import com.rbownworkshop.server.repository.ShopOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShopOrderService {

    private final ShopOrderRepository repository;
    private final ExcelParserService parserService;
    private final ConfigService configService;
    private final UploadTaskRegistry taskRegistry;
    private final UploadAsyncService uploadAsyncService; // ← inject async service riêng

    private static final long TRUNCATE_BASE_MS = 500L;
    private static final long INSERT_MS_PER_ROW = 20L;

    /**
     * Bắt đầu upload:
     * 1) Parse file sync (nhanh) — nếu lỗi throw ngay để FE biết.
     * 2) Tính estimate, tạo task.
     * 3) Fire async job (chạy nền, KHÔNG đợi).
     * 4) Trả taskId + estimate ngay lập tức.
     */
    public UploadStartResponse startUpload(MultipartFile file) {
        List<ShopOrder> parsed = parserService.parse(file);
        int rowCount = parsed.size();
        long estimatedMillis = TRUNCATE_BASE_MS + (long) rowCount * INSERT_MS_PER_ROW;

        UploadTask task = taskRegistry.create(estimatedMillis, rowCount);

        // Gọi async service qua Spring proxy — return ngay lập tức
        uploadAsyncService.process(task.getTaskId(), parsed);

        return UploadStartResponse.builder()
                .taskId(task.getTaskId())
                .estimatedMillis(estimatedMillis)
                .totalRows(rowCount)
                .build();
    }

    public UploadTask getUploadStatus(String taskId) {
        return taskRegistry.get(taskId);
    }

    // ================================================================

    public SearchResponse search(String account, String sheetType) {
        List<ShopOrder> found = (sheetType == null || sheetType.isBlank())
                ? repository.findByAccountIgnoreCase(account)
                : repository.findByAccountAndSheetType(account, sheetType);

        Integer current = configService.getCurrentProcessingNumber();

        List<OrderResult> results = found.stream()
                .map(this::toOrderResult)
                .collect(Collectors.toList());

        return SearchResponse.builder()
                .results(results)
                .currentProcessingNumber(current)
                .totalOrders(found.size())
                .build();
    }

    private OrderResult toOrderResult(ShopOrder o) {
        return OrderResult.builder()
                .id(o.getId())
                .sheetSequence(o.getSheetSequence())
                .sheetType(o.getSheetType())
                .servicePackage(o.getServicePackage())
                .servicePrice(o.getServicePrice())
                .orderStatus(determineStatus(o))
                .registerDate(o.getOrderDate())
                .completeDate(o.getDeliveryDate())
                .refundStatus(determineRefundStatus(o))
                .refundInfo(o.getDepositRefund())
                .priorityRegister(o.getPriorityRegister())
                .priorityFee(o.getPriorityFee())
                .refundNoteStatus(o.getRefundNoteStatus())
                .orderNoteStatus(o.getOrderNoteStatus())
                .build();
    }

    private String determineStatus(ShopOrder o) {
        if (o.getDeliveryDate() != null) return "hoàn thành";
        if (o.getHoldDays() != null && o.getHoldDays() > 0) return "đang treo";
        return "chưa treo";
    }

    private String determineRefundStatus(ShopOrder o) {
        if (o.getDepositRefund() == null || o.getDepositRefund().isBlank()) return "không hoàn";
        String v = o.getDepositRefund().toLowerCase();
        if (v.contains("đã hoàn") || v.contains("hoàn rồi") || v.contains("done")) return "đã hoàn";
        return "chờ hoàn";
    }

    public List<ShopOrder> searchForManagement(String keyword, String sheetType) {
        return repository.searchForManagement(
                (keyword == null || keyword.isBlank()) ? null : keyword,
                (sheetType == null || sheetType.isBlank()) ? null : sheetType
        );
    }

    public List<ShopOrder> searchForManagementPaged(String keyword, String sheetType, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        return repository.searchForManagementPaged(
                (keyword == null || keyword.isBlank()) ? null : keyword,
                (sheetType == null || sheetType.isBlank()) ? null : sheetType,
                pageable
        );
    }

    @Transactional
    public ShopOrder update(Long id, ShopOrder patch) {
        ShopOrder existing = repository.findById(id)
                .orElseThrow(() -> new ExcelParseException("Không tìm thấy đơn id=" + id));

        if (patch.getCustomerName() != null) existing.setCustomerName(patch.getCustomerName());
        if (patch.getOrderPlace() != null) existing.setOrderPlace(patch.getOrderPlace());
        if (patch.getServicePackage() != null) existing.setServicePackage(patch.getServicePackage());
        if (patch.getAccount() != null) existing.setAccount(patch.getAccount());
        if (patch.getPassword() != null) existing.setPassword(patch.getPassword());
        if (patch.getProtectionCode() != null) existing.setProtectionCode(patch.getProtectionCode());
        if (patch.getPriorityRegister() != null) existing.setPriorityRegister(patch.getPriorityRegister());
        if (patch.getPriorityFee() != null) existing.setPriorityFee(patch.getPriorityFee());
        if (patch.getRegionIp() != null) existing.setRegionIp(patch.getRegionIp());
        if (patch.getGame20k() != null) existing.setGame20k(patch.getGame20k());
        if (patch.getServicePrice() != null) existing.setServicePrice(patch.getServicePrice());
        if (patch.getActualAmount() != null) existing.setActualAmount(patch.getActualAmount());
        if (patch.getOrderDate() != null) existing.setOrderDate(patch.getOrderDate());
        if (patch.getDeliveryDate() != null) existing.setDeliveryDate(patch.getDeliveryDate());
        if (patch.getHoldDays() != null) existing.setHoldDays(patch.getHoldDays());
        if (patch.getRegionTransferStatus() != null) existing.setRegionTransferStatus(patch.getRegionTransferStatus());
        if (patch.getDepositRefund() != null) existing.setDepositRefund(patch.getDepositRefund());
        if (patch.getNoteRBown() != null) existing.setNoteRBown(patch.getNoteRBown());
        if (patch.getRefundNoteStatus() != null) existing.setRefundNoteStatus(patch.getRefundNoteStatus());
        if (patch.getOrderNoteStatus() != null) existing.setOrderNoteStatus(patch.getOrderNoteStatus());
        if (patch.getNoteLogAcc() != null) existing.setNoteLogAcc(patch.getNoteLogAcc());
        if (patch.getNoteAddMoneyLogAcc() != null) existing.setNoteAddMoneyLogAcc(patch.getNoteAddMoneyLogAcc());
        if (patch.getNoteAccError() != null) existing.setNoteAccError(patch.getNoteAccError());
        if (patch.getNoteAddMoneyFixAcc() != null) existing.setNoteAddMoneyFixAcc(patch.getNoteAddMoneyFixAcc());
        if (patch.getSheetType() != null) existing.setSheetType(patch.getSheetType());
        if (patch.getSheetSequence() != null) existing.setSheetSequence(patch.getSheetSequence());

        return repository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ExcelParseException("Không tìm thấy đơn id=" + id);
        }
        repository.deleteById(id);
    }
}