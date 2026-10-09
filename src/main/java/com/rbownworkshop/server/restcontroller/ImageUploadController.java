package com.rbownworkshop.server.restcontroller;

import com.rbownworkshop.server.exception.ExcelParseException;
import com.rbownworkshop.server.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * POST /api/shop-orders/images  (multipart, field "file")
 *   → { "url": "http://host/uploads/images/<file>", "filename": "<file>" }
 *
 * Nằm dưới /api/shop-orders/** nên đã được whitelist bởi SecurityConfiguration.
 */
@RestController
@RequestMapping("/api/shop-orders/images")
@RequiredArgsConstructor
public class ImageUploadController {

    private final ImageStorageService storage;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        try {
            String filename = storage.saveImage(file);
            String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/images/")
                    .path(filename)
                    .toUriString();

            Map<String, String> body = new HashMap<>();
            body.put("url", url);
            body.put("filename", filename);
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            throw new ExcelParseException(e.getMessage());
        } catch (IOException e) {
            throw new ExcelParseException("Lỗi lưu ảnh: " + e.getMessage());
        }
    }
}