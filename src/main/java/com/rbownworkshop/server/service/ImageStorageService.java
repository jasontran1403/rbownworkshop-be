package com.rbownworkshop.server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Quản lý file ảnh trên disk:
 *  - saveImage(file): lưu file → trả về filename (chỉ phần sau /uploads/images/)
 *  - deleteByFilename(filename): xóa 1 file
 *  - extractFilenamesFromHtml(html): parse HTML, lấy các filename ảnh thuộc hệ thống
 *  - cleanupOrphans(oldHtml, newHtml): xóa những ảnh có trong oldHtml mà không còn trong newHtml
 */
@Service
@Slf4j
public class ImageStorageService {

    @Value("${app.upload.image-dir}")
    private String imageDir;

    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXT =
            Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> ALLOWED_MIME =
            Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    /** Match URL dạng ".../uploads/images/<filename>" trong HTML. */
    private static final Pattern IMG_URL_PATTERN = Pattern.compile(
            "/uploads/images/([A-Za-z0-9_\\-]+\\.(?:jpg|jpeg|png|gif|webp))",
            Pattern.CASE_INSENSITIVE
    );

    private Path dir;

    @PostConstruct
    public void init() {
        try {
            dir = Paths.get(imageDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            log.info("Image storage dir: {}", dir);
        } catch (IOException e) {
            throw new RuntimeException("Không tạo được thư mục lưu ảnh: " + imageDir, e);
        }
    }

    // ============================================================
    //  Save
    // ============================================================

    public String saveImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Thiếu file ảnh.");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("Ảnh quá lớn (tối đa 5MB).");
        }
        String ct = file.getContentType();
        if (ct == null || !ALLOWED_MIME.contains(ct.toLowerCase())) {
            throw new IllegalArgumentException("Chỉ chấp nhận JPG/PNG/GIF/WEBP.");
        }
        String ext = extractExt(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("Đuôi file không hợp lệ.");
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path target = dir.resolve(filename).normalize();
        // chống path traversal
        if (!target.startsWith(dir)) {
            throw new IllegalArgumentException("Tên file không hợp lệ.");
        }
        file.transferTo(target.toFile());
        return filename;
    }

    // ============================================================
    //  Delete / cleanup
    // ============================================================

    public void deleteByFilename(String filename) {
        if (filename == null || filename.isBlank()) return;
        try {
            Path target = dir.resolve(filename).normalize();
            if (!target.startsWith(dir)) return;    // không cho xóa ngoài folder
            boolean deleted = Files.deleteIfExists(target);
            if (deleted) log.info("Deleted orphan image: {}", filename);
        } catch (IOException e) {
            log.warn("Không xóa được ảnh {}: {}", filename, e.getMessage());
        }
    }

    /** Trả về set các filename của ảnh thuộc hệ thống xuất hiện trong HTML. */
    public Set<String> extractFilenamesFromHtml(String html) {
        if (html == null || html.isEmpty()) return Collections.emptySet();
        Set<String> out = new HashSet<>();
        Matcher m = IMG_URL_PATTERN.matcher(html);
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }

    /** Xóa những ảnh có trong oldHtml mà không còn trong newHtml. */
    public void cleanupOrphans(String oldHtml, String newHtml) {
        Set<String> oldSet = extractFilenamesFromHtml(oldHtml);
        if (oldSet.isEmpty()) return;
        Set<String> newSet = extractFilenamesFromHtml(newHtml);
        for (String fn : oldSet) {
            if (!newSet.contains(fn)) {
                deleteByFilename(fn);
            }
        }
    }

    // ============================================================
    //  Helpers
    // ============================================================

    private String extractExt(String name) {
        if (name == null) return "";
        int i = name.lastIndexOf('.');
        if (i < 0 || i >= name.length() - 1) return "";
        return name.substring(i + 1).toLowerCase();
    }
}