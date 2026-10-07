package com.rbownworkshop.server.service;

import com.rbownworkshop.server.entity.ShopOrder;
import com.rbownworkshop.server.exception.ExcelParseException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Parser form mới (file Update_Form_Website.xlsx):
 *   16 cột, 3 sheet (Khách Order / Ưu Tiên / Ưu Tiên Vip).
 *
 *   MATCH THEO TÊN HEADER (normalized), KHÔNG theo vị trí cột.
 */
@Service
public class ExcelParserService {

    private static final LinkedHashMap<String, String> SHEET_KEYWORD_MAPPING = new LinkedHashMap<>();
    static {
        SHEET_KEYWORD_MAPPING.put("uutienvip", "SUPER_VIP");
        SHEET_KEYWORD_MAPPING.put("uutien",    "VIP");
        SHEET_KEYWORD_MAPPING.put("khachorder", "NORMAL");
    }

    /** Mapping từ header (normalized, bỏ dấu, lowercase, bỏ khoảng trắng) → tên field. */
    private static final Map<String, String> HEADER_TO_FIELD = new LinkedHashMap<>();
    static {
        HEADER_TO_FIELD.put("tenkhach",          "customerName");
        HEADER_TO_FIELD.put("choorder",          "orderPlace");
        HEADER_TO_FIELD.put("goidichvu",         "servicePackage");
        HEADER_TO_FIELD.put("taikhoan",          "account");
        HEADER_TO_FIELD.put("dangkyuutien",      "priorityRegister");
        HEADER_TO_FIELD.put("phiuutien",         "priorityFee");
        HEADER_TO_FIELD.put("dangkychonvung",    "regionRegister");
        HEADER_TO_FIELD.put("vungchon",          "regionSelected");
        HEADER_TO_FIELD.put("dangkychongame",    "gameRegister");
        HEADER_TO_FIELD.put("gamechon",          "gameSelected");
        HEADER_TO_FIELD.put("giachot",           "servicePrice");
        HEADER_TO_FIELD.put("thucnhan",          "actualAmount");
        HEADER_TO_FIELD.put("ngaydat",           "orderDate");
        HEADER_TO_FIELD.put("songaytreo",        "holdDays");
        HEADER_TO_FIELD.put("tinhtranghoantien", "refundNoteStatus");
        HEADER_TO_FIELD.put("tinhtrangdon",      "orderNoteStatus");
    }

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("yyyy-M-d"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
    };

    public List<ShopOrder> parse(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Map<String, Sheet> orderedSheets = new LinkedHashMap<>();
            Set<String> matched = new HashSet<>();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet s = workbook.getSheetAt(i);
                String type = matchSheetType(normalizeSheetName(s.getSheetName()));
                if (type == null || matched.contains(type)) continue;
                matched.add(type);
                orderedSheets.put(type, s);
            }

            if (orderedSheets.isEmpty()) {
                throw new ExcelParseException(
                        "Không tìm thấy sheet hợp lệ. Cần có: Ưu Tiên Vip, Ưu Tiên, Khách Order");
            }

            List<ShopOrder> result = new ArrayList<>();
            for (String type : List.of("SUPER_VIP", "VIP", "NORMAL")) {
                Sheet sheet = orderedSheets.get(type);
                if (sheet == null) continue;
                result.addAll(parseSheet(sheet, type));
            }

            if (result.isEmpty()) {
                throw new ExcelParseException("Không có dòng dữ liệu hợp lệ trong file");
            }
            return result;
        } catch (ExcelParseException e) {
            throw e;
        } catch (Exception e) {
            throw new ExcelParseException("Lỗi đọc file excel: " + e.getMessage(), e);
        }
    }

    private List<ShopOrder> parseSheet(Sheet sheet, String sheetType) {
        List<ShopOrder> orders = new ArrayList<>();
        int headerRow = detectHeaderRow(sheet);
        if (headerRow < 0) return orders;

        // Build mapping field → column index cho sheet này
        Map<String, Integer> fieldToCol = extractFieldColumns(sheet.getRow(headerRow));

        // Tối thiểu phải có cột "Tài Khoản" để biết dòng là data
        Integer accountCol = fieldToCol.get("account");
        if (accountCol == null) return orders;

        int lastRow = sheet.getLastRowNum();
        boolean skipNextDataRow = false;

        for (int i = headerRow + 1; i <= lastRow; i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                skipNextDataRow = false;
                continue;
            }

            if (isRowMergedFromA(sheet, i)) continue;

            String colA = getString(row, 0);
            if (isSummaryRow(colA)) {
                skipNextDataRow = true;
                continue;
            }
            if (skipNextDataRow) {
                skipNextDataRow = false;
                continue;
            }
            if (isEmptyRow(row, fieldToCol)) continue;

            String account = getString(row, accountCol);
            if (account == null || account.trim().isEmpty()) continue;

            ShopOrder.ShopOrderBuilder b = ShopOrder.builder().sheetType(sheetType);

            b.customerName  (strCol(row, fieldToCol, "customerName"));
            b.orderPlace    (strCol(row, fieldToCol, "orderPlace"));
            b.servicePackage(strCol(row, fieldToCol, "servicePackage"));
            b.account       (account.trim());
            b.priorityRegister(strCol(row, fieldToCol, "priorityRegister"));
            b.priorityFee   (bdCol(row, fieldToCol, "priorityFee"));
            b.regionRegister(strCol(row, fieldToCol, "regionRegister"));
            b.regionSelected(strCol(row, fieldToCol, "regionSelected"));
            b.gameRegister  (strCol(row, fieldToCol, "gameRegister"));
            b.gameSelected  (strCol(row, fieldToCol, "gameSelected"));
            b.servicePrice  (bdCol(row, fieldToCol, "servicePrice"));
            b.actualAmount  (bdCol(row, fieldToCol, "actualAmount"));
            b.orderDate     (dateCol(row, fieldToCol, "orderDate"));
            b.holdDays      (intCol(row, fieldToCol, "holdDays"));
            b.refundNoteStatus(strCol(row, fieldToCol, "refundNoteStatus"));
            b.orderNoteStatus (strCol(row, fieldToCol, "orderNoteStatus"));

            orders.add(b.build());
        }

        return orders;
    }

    // ============================================================
    //  Header detection / mapping
    // ============================================================

    private Map<String, Integer> extractFieldColumns(Row headerRow) {
        Map<String, Integer> out = new HashMap<>();
        if (headerRow == null) return out;
        short last = headerRow.getLastCellNum();
        for (int c = 0; c < last; c++) {
            String raw = getCellString(headerRow.getCell(c));
            if (raw == null) continue;
            String key = norm(raw);
            String field = HEADER_TO_FIELD.get(key);
            if (field != null && !out.containsKey(field)) {
                out.put(field, c);
            }
        }
        return out;
    }

    private int detectHeaderRow(Sheet sheet) {
        int maxScan = Math.min(sheet.getLastRowNum(), 10);
        for (int i = 0; i <= maxScan; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            // Row được coi là header nếu đủ ≥ 4 header khớp
            Map<String, Integer> cols = extractFieldColumns(row);
            if (cols.size() >= 4 && cols.containsKey("account")) return i;
        }
        return -1;
    }

    // ============================================================
    //  Date orientation (giữ nguyên logic cũ để đoán M/D vs D/M)
    // ============================================================

    private enum DateOrientation { MDY, DMY, UNKNOWN }

    private DateOrientation detectOrientation(String raw) {
        if (raw == null) return DateOrientation.UNKNOWN;
        String s = raw.trim();
        if (s.contains(" ")) s = s.substring(0, s.indexOf(' '));
        if (s.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) return DateOrientation.MDY;

        String[] p = s.split("[/\\-]");
        if (p.length != 3) return DateOrientation.UNKNOWN;
        try {
            int a = Integer.parseInt(p[0].trim());
            int b = Integer.parseInt(p[1].trim());
            if (a > 12 && b <= 12) return DateOrientation.DMY;
            if (b > 12 && a <= 12) return DateOrientation.MDY;
            return DateOrientation.UNKNOWN;
        } catch (Exception e) {
            return DateOrientation.UNKNOWN;
        }
    }

    private LocalDate parseDateByOrientation(String raw, DateOrientation orientation) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim();
        if (s.contains(" ")) s = s.substring(0, s.indexOf(' '));

        if (s.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
            try { return LocalDate.parse(s); } catch (Exception e) { return null; }
        }

        String[] p = s.split("[/\\-]");
        if (p.length != 3) return tryParse(s);

        int a, b, y;
        try {
            a = Integer.parseInt(p[0].trim());
            b = Integer.parseInt(p[1].trim());
            y = Integer.parseInt(p[2].trim());
        } catch (Exception e) {
            return tryParse(s);
        }

        switch (orientation) {
            case MDY -> {
                try { return LocalDate.of(y, a, b); } catch (Exception ignored) {}
                try { return LocalDate.of(y, b, a); } catch (Exception ignored) {}
            }
            case DMY -> {
                try { return LocalDate.of(y, b, a); } catch (Exception ignored) {}
                try { return LocalDate.of(y, a, b); } catch (Exception ignored) {}
            }
            case UNKNOWN -> {
                if (a >= 1 && a <= 12 && b >= 1 && b <= 31) {
                    try { return LocalDate.of(y, a, b); } catch (Exception ignored) {}
                }
                if (b >= 1 && b <= 12 && a >= 1 && a <= 31) {
                    try { return LocalDate.of(y, b, a); } catch (Exception ignored) {}
                }
            }
        }
        return null;
    }

    // ============================================================
    //  Row helpers
    // ============================================================

    private boolean isRowMergedFromA(Sheet sheet, int rowIndex) {
        for (int m = 0; m < sheet.getNumMergedRegions(); m++) {
            CellRangeAddress region = sheet.getMergedRegion(m);
            if (rowIndex >= region.getFirstRow() && rowIndex <= region.getLastRow()) {
                if (region.getFirstColumn() == 0 && region.getLastColumn() >= 3) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSummaryRow(String colA) {
        if (colA == null) return false;
        String n = norm(colA);
        return n.startsWith("doanhthu")
                || n.startsWith("ngay")
                || n.equals("stt") || n.equals("tenkhach")
                || n.contains("loinhuan")
                || n.contains("hoankhach")
                || n.contains("chiacophan");
    }

    private boolean isEmptyRow(Row row, Map<String, Integer> fieldToCol) {
        for (Integer c : fieldToCol.values()) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String v = getCellString(cell);
                if (v != null && !v.trim().isEmpty()) return false;
            }
        }
        return true;
    }

    // ============================================================
    //  Cell → field helpers
    // ============================================================

    private String strCol(Row row, Map<String, Integer> cols, String field) {
        Integer c = cols.get(field);
        if (c == null) return null;
        return getString(row, c);
    }

    private BigDecimal bdCol(Row row, Map<String, Integer> cols, String field) {
        Integer c = cols.get(field);
        if (c == null) return null;
        return getBigDecimal(row, c);
    }

    private Integer intCol(Row row, Map<String, Integer> cols, String field) {
        Integer c = cols.get(field);
        if (c == null) return null;
        return getInteger(row, c);
    }

    private LocalDate dateCol(Row row, Map<String, Integer> cols, String field) {
        Integer c = cols.get(field);
        if (c == null) return null;
        String raw = getCellString(row.getCell(c));
        DateOrientation orientation = detectOrientation(raw);
        return parseDateByOrientation(raw, orientation);
    }

    // ============================================================
    //  Utils
    // ============================================================

    private String norm(String s) {
        if (s == null) return "";
        // Quan trọng: Java NFD KHÔNG decompose "Đ"/"đ" (U+0110/U+0111) vì
        // chúng không có canonical decomposition. Phải replace tay trước.
        String pre = s.replace('Đ', 'D').replace('đ', 'd');
        String x = Normalizer.normalize(pre, Normalizer.Form.NFD);
        x = x.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return x.toLowerCase().replaceAll("\\s+", "").trim();
    }

    private String normalizeSheetName(String name) {
        if (name == null) return "";
        String x = name
                .replace("​", "").replace("‌", "")
                .replace("‍", "").replace("﻿", "")
                .replace(" ", " ");
        x = Normalizer.normalize(x, Normalizer.Form.NFD);
        x = x.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return x.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private String matchSheetType(String normalizedName) {
        for (Map.Entry<String, String> e : SHEET_KEYWORD_MAPPING.entrySet()) {
            if (normalizedName.contains(e.getKey())) return e.getValue();
        }
        return null;
    }

    private String getString(Row row, int idx) {
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        return getCellString(cell);
    }

    private String getCellString(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.FORMULA) {
            CellType cachedType = cell.getCachedFormulaResultType();
            switch (cachedType) {
                case STRING -> { return cell.getStringCellValue(); }
                case NUMERIC -> {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        var dt = cell.getLocalDateTimeCellValue();
                        if (dt != null) return dt.toLocalDate().toString();
                    }
                    double d = cell.getNumericCellValue();
                    if (d == Math.floor(d) && !Double.isInfinite(d)) {
                        return String.valueOf((long) d);
                    }
                    return BigDecimal.valueOf(d).toPlainString();
                }
                case BOOLEAN -> { return String.valueOf(cell.getBooleanCellValue()); }
                default -> { return null; }
            }
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    var dt = cell.getLocalDateTimeCellValue();
                    if (dt != null) yield dt.toLocalDate().toString();
                }
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d) && !Double.isInfinite(d)) yield String.valueOf((long) d);
                yield BigDecimal.valueOf(d).toPlainString();
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case BLANK -> null;
            default -> null;
        };
    }

    private BigDecimal getBigDecimal(Row row, int idx) {
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
            if (cell.getCellType() == CellType.FORMULA
                    && cell.getCachedFormulaResultType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
            String s = getCellString(cell);
            if (s == null || s.isBlank()) return null;
            s = s.replaceAll("[^0-9-]", "");
            if (s.isBlank() || s.equals("-")) return null;
            return new BigDecimal(s);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer getInteger(Row row, int idx) {
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            }
            if (cell.getCellType() == CellType.FORMULA
                    && cell.getCachedFormulaResultType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            }
            String s = getCellString(cell);
            if (s == null || s.isBlank()) return null;
            s = s.replaceAll("[^0-9-]", "");
            if (s.isBlank() || s.equals("-")) return null;
            return Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate tryParse(String s) {
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try { return LocalDate.parse(s, fmt); } catch (Exception ignored) {}
        }
        try { return LocalDate.parse(s); } catch (Exception ignored) {}
        return null;
    }
}