package com.rbownworkshop.server.restcontroller;

import com.rbownworkshop.server.dto.response.ProcessingNumberResponse;
import com.rbownworkshop.server.dto.response.SearchResponse;
import com.rbownworkshop.server.dto.upload.UploadStartResponse;
import com.rbownworkshop.server.dto.upload.UploadTask;
import com.rbownworkshop.server.entity.ShopOrder;
import com.rbownworkshop.server.service.ConfigService;
import com.rbownworkshop.server.service.RateLimitService;
import com.rbownworkshop.server.service.ShopOrderService;
import com.rbownworkshop.server.service.TemplateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shop-orders")
@RequiredArgsConstructor
public class ShopOrderController {

    private final ShopOrderService service;
    private final ConfigService configService;
    private final RateLimitService rateLimitService;
    private final TemplateService templateService;

    private static final int SEARCH_MAX  = 5;
    private static final long SEARCH_WIN = 1_000L;

    @GetMapping("/processing-number")
    public ResponseEntity<ProcessingNumberResponse> getProcessingNumber() {
        return ResponseEntity.ok(ProcessingNumberResponse.builder()
                .currentProcessingNumber(configService.getCurrentProcessingNumber())
                .build());
    }

    @PutMapping("/processing-number")
    public ResponseEntity<ProcessingNumberResponse> updateProcessingNumber(
            @RequestParam(value = "value", required = false) Integer value) {
        Integer updated = configService.setCurrentProcessingNumber(value);
        return ResponseEntity.ok(ProcessingNumberResponse.builder()
                .currentProcessingNumber(updated)
                .build());
    }

    /**
     * Trả ngay taskId + estimate. Server tiếp tục truncate + insert ở thread riêng.
     * FE poll /upload/status/{taskId} để lấy progress + kết quả cuối.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadStartResponse> upload(@RequestParam("file") MultipartFile file) {
        UploadStartResponse res = service.startUpload(file);
        return ResponseEntity.accepted().body(res);
    }

    @GetMapping("/upload/status/{taskId}")
    public ResponseEntity<UploadTask> uploadStatus(@PathVariable String taskId) {
        UploadTask task = service.getUploadStatus(taskId);
        if (task == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(task);
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(
            @RequestParam("account") String account,
            @RequestParam(value = "sheetType", required = false) String sheetType,
            HttpServletRequest request) {
        String ip = getClientIp(request);
        rateLimitService.check("search:" + ip, SEARCH_MAX, SEARCH_WIN);
        return ResponseEntity.ok(service.search(account, sheetType));
    }

    @GetMapping("/management/search")
    public ResponseEntity<List<ShopOrder>> managementSearch(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sheetType", required = false) String sheetType,
            HttpServletRequest request) {
        String ip = getClientIp(request);
        rateLimitService.check("mgmt-search:" + ip, SEARCH_MAX, SEARCH_WIN);
        return ResponseEntity.ok(service.searchForManagement(keyword, sheetType));
    }

    @GetMapping("/management/search-paged")
    public ResponseEntity<Map<String, Object>> managementSearchPaged(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sheetType", required = false) String sheetType,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size,
            HttpServletRequest request) {
        String ip = getClientIp(request);
        rateLimitService.check("mgmt-search-paged:" + ip, SEARCH_MAX, SEARCH_WIN);

        List<ShopOrder> items = service.searchForManagementPaged(keyword, sheetType, page, size);
        Map<String, Object> resp = new HashMap<>();
        resp.put("items", items);
        resp.put("page", page);
        resp.put("size", size);
        resp.put("hasMore", items.size() == size);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/management/{id}")
    public ResponseEntity<ShopOrder> update(@PathVariable Long id, @RequestBody ShopOrder patch) {
        return ResponseEntity.ok(service.update(id, patch));
    }

    @DeleteMapping("/management/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"shop-order-template.xlsx\"");
        try {
            templateService.writeTemplate(response.getOutputStream());
        } catch (Exception e) {
            throw new IOException("Lỗi tạo template: " + e.getMessage(), e);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}