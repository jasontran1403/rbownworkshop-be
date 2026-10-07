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
    private final UploadAsyncService uploadAsyncService;

    private static final long TRUNCATE_BASE_MS = 500L;
    private static final long INSERT_MS_PER_ROW = 20L;

    public UploadStartResponse startUpload(MultipartFile file) {
        List<ShopOrder> parsed = parserService.parse(file);
        int rowCount = parsed.size();
        long estimatedMillis = TRUNCATE_BASE_MS + (long) rowCount * INSERT_MS_PER_ROW;

        UploadTask task = taskRegistry.create(estimatedMillis, rowCount);
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

        ProcessingNumberResponse numbers = configService.getAll();

        List<OrderResult> results = found.stream()
                .map(this::toOrderResult)
                .collect(Collectors.toList());

        return SearchResponse.builder()
                .results(results)
                .processingNumbers(numbers)
                .currentProcessingNumber(numbers.getNormal())
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
                .priorityRegister(o.getPriorityRegister())
                .priorityFee(o.getPriorityFee())
                .regionRegister(o.getRegionRegister())
                .regionSelected(o.getRegionSelected())
                .gameRegister(o.getGameRegister())
                .gameSelected(o.getGameSelected())
                .registerDate(o.getOrderDate())
                .holdDays(o.getHoldDays())
                .refundNoteStatus(o.getRefundNoteStatus())
                .orderNoteStatus(o.getOrderNoteStatus())
                .build();
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

        if (patch.getCustomerName()    != null) existing.setCustomerName(patch.getCustomerName());
        if (patch.getOrderPlace()      != null) existing.setOrderPlace(patch.getOrderPlace());
        if (patch.getServicePackage()  != null) existing.setServicePackage(patch.getServicePackage());
        if (patch.getAccount()         != null) existing.setAccount(patch.getAccount());
        if (patch.getPriorityRegister()!= null) existing.setPriorityRegister(patch.getPriorityRegister());
        if (patch.getPriorityFee()     != null) existing.setPriorityFee(patch.getPriorityFee());
        if (patch.getRegionRegister()  != null) existing.setRegionRegister(patch.getRegionRegister());
        if (patch.getRegionSelected()  != null) existing.setRegionSelected(patch.getRegionSelected());
        if (patch.getGameRegister()    != null) existing.setGameRegister(patch.getGameRegister());
        if (patch.getGameSelected()    != null) existing.setGameSelected(patch.getGameSelected());
        if (patch.getServicePrice()    != null) existing.setServicePrice(patch.getServicePrice());
        if (patch.getActualAmount()    != null) existing.setActualAmount(patch.getActualAmount());
        if (patch.getOrderDate()       != null) existing.setOrderDate(patch.getOrderDate());
        if (patch.getHoldDays()        != null) existing.setHoldDays(patch.getHoldDays());
        if (patch.getRefundNoteStatus()!= null) existing.setRefundNoteStatus(patch.getRefundNoteStatus());
        if (patch.getOrderNoteStatus() != null) existing.setOrderNoteStatus(patch.getOrderNoteStatus());
        if (patch.getSheetType()       != null) existing.setSheetType(patch.getSheetType());
        if (patch.getSheetSequence()   != null) existing.setSheetSequence(patch.getSheetSequence());

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
