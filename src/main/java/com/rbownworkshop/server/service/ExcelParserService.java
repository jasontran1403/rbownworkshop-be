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

@Service
public class ExcelParserService {

    private static final LinkedHashMap<String, String> SHEET_KEYWORD_MAPPING = new LinkedHashMap<>();
    static {
        SHEET_KEYWORD_MAPPING.put("uutienvip", "SUPER_VIP");
        SHEET_KEYWORD_MAPPING.put("uutien",    "VIP");
        SHEET_KEYWORD_MAPPING.put("khachorder", "NORMAL");
    }

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("yyyy-M-d"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
    };

    private static final int MAX_COLS = 26;

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
            if (isEmptyRow(row)) continue;

            String account = getString(row, 3);
            if (account == null || account.trim().isEmpty()) continue;

            String rawL = getCellString(row.getCell(11));
            String rawM = getCellString(row.getCell(12));

            DateOrientation orientation = detectOrientation(rawM);
            if (orientation == DateOrientation.UNKNOWN) {
                orientation = detectOrientation(rawL);
            }

            LocalDate orderDate = parseDateByOrientation(rawL, orientation);
            LocalDate deliveryDate = parseDateByOrientation(rawM, orientation);
            if (deliveryDate == null) {
                deliveryDate = parseFormulaDelivery(row, orderDate);
            }

            ShopOrder o = ShopOrder.builder()
                    .customerName(getString(row, 0))
                    .orderPlace(getString(row, 1))
                    .servicePackage(getString(row, 2))
                    .account(account.trim())
                    .password(getString(row, 4))
                    .protectionCode(formatProtectionCode(getString(row, 5)))
                    .regionIp(getString(row, 7))
                    .game20k(getString(row, 8))
                    .servicePrice(getBigDecimal(row, 9))
                    .actualAmount(getBigDecimal(row, 10))
                    .orderDate(orderDate)
                    .deliveryDate(deliveryDate)
                    .holdDays(getInteger(row, 13))
                    .regionTransferStatus(getString(row, 14))
                    .depositRefund(getString(row, 15))
                    .noteRBown(getString(row, 16))
                    .priorityRegister(getString(row, 18))
                    .priorityFee(getBigDecimal(row, 19))
                    .refundNoteStatus(getString(row, 20))
                    .orderNoteStatus(getString(row, 21))
                    .noteLogAcc(getString(row, 22))
                    .noteAddMoneyLogAcc(getString(row, 23))
                    .noteAccError(getString(row, 24))
                    .noteAddMoneyFixAcc(getString(row, 25))
                    .sheetType(sheetType)
                    .build();

            orders.add(o);
        }

        return orders;
    }

    private String formatProtectionCode(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return trimmed;
        return trimmed.replaceAll("\\s+", "\n");
    }

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

    private LocalDate parseFormulaDelivery(Row row, LocalDate orderDate) {
        Cell cell = row.getCell(12);
        if (cell == null) return null;
        try {
            if (cell.getCellType() != CellType.FORMULA) return null;
            String formula = cell.getCellFormula();
            if (formula.matches("(?i)^L\\d+\\s*\\+\\s*\\(?J\\d+\\s*/\\s*10000\\)?$")) {
                if (orderDate == null) return null;
                BigDecimal price = getBigDecimal(row, 9);
                if (price == null) return orderDate;
                int days = price.divide(BigDecimal.valueOf(10000), 0,
                        java.math.RoundingMode.DOWN).intValue();
                return orderDate.plusDays(days);
            }
        } catch (Exception ignored) {}
        return null;
    }

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

    private boolean isEmptyRow(Row row) {
        for (int c = 0; c < MAX_COLS; c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String v = getCellString(cell);
                if (v != null && !v.trim().isEmpty()) return false;
            }
        }
        return true;
    }

    private int detectHeaderRow(Sheet sheet) {
        int maxScan = Math.min(sheet.getLastRowNum(), 10);
        for (int i = 0; i <= maxScan; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String d = norm(getString(row, 3));
            String c = norm(getString(row, 2));
            String e = norm(getString(row, 4));
            if (d.contains("taikhoan") || c.contains("goidichvu") || e.contains("matkhau")) {
                return i;
            }
        }
        return -1;
    }

    private String norm(String s) {
        if (s == null) return "";
        String x = Normalizer.normalize(s, Normalizer.Form.NFD);
        x = x.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return x.toLowerCase().replaceAll("\\s+", "").trim();
    }

    private String normalizeSheetName(String name) {
        if (name == null) return "";
        String x = name
                .replace("\u200B", "").replace("\u200C", "")
                .replace("\u200D", "").replace("\uFEFF", "")
                .replace("\u00A0", " ");
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