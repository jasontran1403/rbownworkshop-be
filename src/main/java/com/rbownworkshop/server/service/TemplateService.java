package com.rbownworkshop.server.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.util.List;

@Service
public class TemplateService {

    private static final String[] HEADERS = {
            "Tên Khách",                    // A  (0)
            "Chỗ Order",                    // B  (1)
            "Gói Dịch Vụ",                  // C  (2)
            "Tài Khoản",                    // D  (3)
            "Mật Khẩu",                     // E  (4)
            "Mã Bảo Vệ",                    // F  (5)
            "Đăng Ký Ưu Tiên",              // G  (6)
            "IP vùng 20k chọn vùng",        // H  (7)
            "Chọn game 20k",                // I  (8)
            "Giá Chốt",                     // J  (9)
            "Thực Nhận",                    // K  (10)
            "Ngày Đặt",                     // L  (11)
            "Ngày Giao",                    // M  (12)
            "Số Ngày Treo",                 // N  (13)
            "Tình Trạng Chuyển Vùng",       // O  (14)
            "Hoàn Cọc",                     // P  (15)
            "Ghi Chú R Bown",               // Q  (16)
            "",                             // R  (17) - trống
            "Đăng Ký Ưu Tiên",              // S  (18)
            "Phí Ưu Tiên",                  // T  (19)
            "Tình Trạng Hoàn Tiền",         // U  (20)
            "Tình Trạng Đơn",               // V  (21)
            "Ghi Chú Log Acc",              // W  (22)
            "Ghi Chú Cộng Tiền Log Acc",    // X  (23)
            "Ghi Chú Acc Lỗi",              // Y  (24)
            "Ghi Chú Cộng Tiền Fix Lỗi Acc" // Z  (25)
    };

    private static final List<String> SHEET_NAMES = List.of(
            "Khách Order", "Ưu Tiên", "Ưu Tiên Vip"
    );

    public void writeTemplate(OutputStream out) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            // Header style: in đậm, nền cam, chữ trắng
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

            // Date style: mm/dd/yyyy
            XSSFCellStyle dateStyle = wb.createCellStyle();
            XSSFDataFormat df = wb.createDataFormat();
            dateStyle.setDataFormat(df.getFormat("mm/dd/yyyy"));

            for (String sheetName : SHEET_NAMES) {
                XSSFSheet sheet = wb.createSheet(sheetName);

                // Header
                Row header = sheet.createRow(0);
                header.setHeightInPoints(60f); // gấp ~4 lần chiều cao mặc định
                for (int i = 0; i < HEADERS.length; i++) {
                    Cell cell = header.createCell(i);
                    cell.setCellValue(HEADERS[i]);
                    cell.setCellStyle(headerStyle);
                }

                // Width: x1.5 so với bản cũ
                for (int i = 0; i < HEADERS.length; i++) {
                    sheet.setColumnWidth(i, 7500);
                }
                sheet.setColumnWidth(5, 12000);   // F - Mã bảo vệ
                sheet.setColumnWidth(6, 12000);   // G - Đăng Ký Ưu Tiên
                sheet.setColumnWidth(16, 10500);  // Q - Ghi chú R Bown
                sheet.setColumnWidth(22, 10500);  // W - Ghi chú log acc
                sheet.setColumnWidth(23, 10500);  // X - Ghi chú cộng tiền log acc
                sheet.setColumnWidth(24, 10500);  // Y - Ghi chú acc lỗi
                sheet.setColumnWidth(25, 10500);  // Z - Ghi chú cộng tiền fix lỗi acc

                // Freeze row đầu tiên
                sheet.createFreezePane(0, 1);

                // Set sẵn style date cho 2 cột L (11) và M (12) cho ~500 dòng
                for (int r = 1; r < 500; r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) row = sheet.createRow(r);
                    Cell l = row.getCell(11);
                    if (l == null) l = row.createCell(11);
                    l.setCellStyle(dateStyle);
                    Cell m = row.getCell(12);
                    if (m == null) m = row.createCell(12);
                    m.setCellStyle(dateStyle);
                }
            }

            wb.write(out);
        }
    }
}