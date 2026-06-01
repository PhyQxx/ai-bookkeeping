package com.aibookkeeping.controller;

import com.aibookkeeping.entity.Bill;
import com.aibookkeeping.entity.Category;
import com.aibookkeeping.mapper.BillMapper;
import com.aibookkeeping.mapper.CategoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillExportController {

    private final BillMapper billMapper;
    private final CategoryMapper categoryMapper;

    @GetMapping("/export")
    public void exportBills(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer type,
            @RequestParam(defaultValue = "excel") String format,
            Authentication authentication,
            HttpServletResponse response) throws IOException {

        Long userId = (Long) authentication.getPrincipal();

        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Bill::getUserId, userId);
        if (startDate != null) wrapper.ge(Bill::getBillDate, LocalDate.parse(startDate));
        if (endDate != null) wrapper.le(Bill::getBillDate, LocalDate.parse(endDate));
        if (categoryId != null) wrapper.eq(Bill::getCategoryId, categoryId);
        if (type != null) wrapper.eq(Bill::getType, type);
        wrapper.orderByDesc(Bill::getBillDate, Bill::getCreatedAt);

        List<Bill> bills = billMapper.selectList(wrapper);
        Map<Long, String> categoryMap = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        if ("csv".equalsIgnoreCase(format)) {
            exportCsv(bills, categoryMap, response);
        } else if ("pdf".equalsIgnoreCase(format)) {
            exportPdf(bills, categoryMap, response);
        } else {
            exportExcel(bills, categoryMap, response);
        }
    }

    private void exportPdf(List<Bill> bills, Map<Long, String> categoryMap, HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=bills_report.pdf");

        try (Document document = new Document()) {
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            // 使用系统内置的中文字体或尝试加载
            BaseFont bfChinese = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(bfChinese, 18, Font.BOLD);
            Font headerFont = new Font(bfChinese, 12, Font.BOLD, Color.WHITE);
            Font normalFont = new Font(bfChinese, 10, Font.NORMAL);

            Paragraph title = new Paragraph("AI 智能记账 - 账单报告", titleFont);
            title.setAlignment(Paragraph.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setWidths(new float[]{15f, 15f, 10f, 15f, 30f, 15f});

            String[] headers = {"日期", "分类", "类型", "金额", "备注", "方式"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new Color(102, 126, 234));
                cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
                cell.setPadding(5);
                table.addCell(cell);
            }

            for (Bill bill : bills) {
                table.addCell(new PdfPCell(new Phrase(bill.getBillDate().toString(), normalFont)));
                table.addCell(new PdfPCell(new Phrase(categoryMap.getOrDefault(bill.getCategoryId(), "-"), normalFont)));
                table.addCell(new PdfPCell(new Phrase(bill.getType() == 1 ? "收入" : "支出", normalFont)));
                table.addCell(new PdfPCell(new Phrase(bill.getAmount().toString(), normalFont)));
                table.addCell(new PdfPCell(new Phrase(bill.getRemark() != null ? bill.getRemark() : "", normalFont)));
                table.addCell(new PdfPCell(new Phrase(bill.getInputType() == 1 ? "AI" : "手动", normalFont)));
            }

            document.add(table);
        } catch (DocumentException e) {
            log.error("Failed to generate PDF", e);
            throw new IOException("PDF 生成失败");
        }
    }

    private void exportExcel(List<Bill> bills, Map<Long, String> categoryMap, HttpServletResponse response) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("账单数据");

        // Header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Header row
        String[] headers = {"日期", "分类", "类型", "金额", "备注", "记录方式", "创建时间"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < bills.size(); i++) {
            Bill bill = bills.get(i);
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(bill.getBillDate() != null ? bill.getBillDate().toString() : "");
            row.createCell(1).setCellValue(categoryMap.getOrDefault(bill.getCategoryId(), "未分类"));
            row.createCell(2).setCellValue(bill.getType() == 1 ? "收入" : "支出");
            row.createCell(3).setCellValue(bill.getAmount() != null ? bill.getAmount().doubleValue() : 0);
            row.createCell(4).setCellValue(bill.getRemark() != null ? bill.getRemark() : "");
            row.createCell(5).setCellValue(bill.getInputType() == 1 ? "AI" : "手动");
            row.createCell(6).setCellValue(bill.getCreatedAt() != null ? bill.getCreatedAt().format(dtf) : "");
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=bills_export.xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    private void exportCsv(List<Bill> bills, Map<Long, String> categoryMap, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=bills_export.csv");

        try (OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8)) {
            // BOM for Excel compatibility
            writer.write('\uFEFF');
            writer.write("日期,分类,类型,金额,备注,记录方式,创建时间\n");

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (Bill bill : bills) {
                StringBuilder sb = new StringBuilder();
                sb.append(bill.getBillDate() != null ? bill.getBillDate().toString() : "").append(',');
                sb.append(categoryMap.getOrDefault(bill.getCategoryId(), "未分类")).append(',');
                sb.append(bill.getType() == 1 ? "收入" : "支出").append(',');
                sb.append(bill.getAmount() != null ? bill.getAmount().toPlainString() : "0").append(',');
                String remark = bill.getRemark() != null ? bill.getRemark().replace("\"", "\"\"") : "";
                sb.append("\"").append(remark).append("\",");
                sb.append(bill.getInputType() == 1 ? "AI" : "手动").append(',');
                sb.append(bill.getCreatedAt() != null ? bill.getCreatedAt().format(dtf) : "").append('\n');
                writer.write(sb.toString());
            }
        }
    }
}
