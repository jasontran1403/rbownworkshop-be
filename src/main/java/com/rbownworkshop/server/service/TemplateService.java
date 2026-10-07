package com.rbownworkshop.server.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.util.List;

@Service
public class TemplateService {

    /** 16 cột khớp với Update_Form_Website.xlsx. */
    private static final String[] HEADERS = {
            "Tên Khách",            // A  (0)
            "Chỗ Order",            // B  (1)
            "Gói Dịch Vụ",          // C  (2)
            "Tài Khoản",            // D  (3)
            "Đăng Ký Ưu Tiên",      // E  (4)
            "Phí Ưu Tiên",          // F  (5)
            "Đăng Ký Chọn Vùng",    // G  (6)
            "Vùng Chọn",            // H  (7)
            "Đăng Ký Chọn Game",    // I  (8)
            "Game Chọn",            // J  (9)
            "Giá Chốt",             // K  (10)
            "Thực Nhận",            // L  (11)
            "Ngày Đặt",             // M  (12)
            "Số Ngày Treo",         // N  (13)
            "Tình Trạng Hoàn Tiền", // O  (14)
            "Tình Trạng Đơn"        // P  (15)
    };

    /** Vị trí cột ngày (0-based) để set style date. */
    private static final int DATE_COL = 12; // Ngày Đặt

    private static final List<String> SHEET_NAMES = List.of(
            "Khách Order", "Ưu Tiên", "Ưu Tiên Vip"
    );

    public void writeTemplate(OutputStream out) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            XSSFCellStyle headerStyle = wb.createCellStyle();
            XSSFFont headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setWrapText(true);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            XSSFCellStyle dateStyle = wb.createCellStyle();
            XSSFDataFormat df = wb.createDataFormat();
            dateStyle.setDataFormat(df.getFormat("mm/dd/yyyy"));

            for (String sheetName : SHEET_NAMES) {
                XSSFSheet sheet = wb.createSheet(sheetName);

                Row header = sheet.createRow(0);
                header.setHeightInPoints(60f);
                for (int i = 0; i < HEADERS.length; i++) {
                    Cell cell = header.createCell(i);
                    cell.setCellValue(HEADERS[i]);
                    cell.setCellStyle(headerStyle);
                }

                for (int i = 0; i < HEADERS.length; i++) {
                    sheet.setColumnWidth(i, 7500);
                }

                sheet.createFreezePane(0, 1);

                // Set style date cho cột Ngày Đặt (~500 dòng)
                for (int r = 1; r < 500; r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) row = sheet.createRow(r);
                    Cell d = row.getCell(DATE_COL);
                    if (d == null) d = row.createCell(DATE_COL);
                    d.setCellStyle(dateStyle);
                }
            }

            wb.write(out);
        }
    }
}
