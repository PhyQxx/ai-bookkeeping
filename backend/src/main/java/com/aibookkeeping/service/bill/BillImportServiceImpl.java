package com.aibookkeeping.service.bill;

import com.aibookkeeping.entity.Bill;
import com.aibookkeeping.entity.Category;
import com.aibookkeeping.mapper.BillMapper;
import com.aibookkeeping.mapper.CategoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillImportServiceImpl implements BillImportService {

    private final BillMapper billMapper;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importAlipayCsv(MultipartFile file, Long userId) {
        List<Bill> bills = new ArrayList<>();
        Map<String, Long> categoryMap = getCategoryMap();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), Charset.forName("GBK")))) {
            CSVParser parser = new CSVParserBuilder().withSeparator(',').withQuoteChar('\"').build();
            CSVReader csvReader = new CSVReaderBuilder(reader).withCSVParser(parser).build();

            String[] line;
            boolean dataStarted = false;
            while ((line = csvReader.readNext()) != null) {
                if (line.length > 0 && line[0].trim().startsWith("-----------")) {
                    dataStarted = !dataStarted;
                    continue;
                }
                if (!dataStarted || line.length < 10 || "类型".equals(line[0].trim())) {
                    continue;
                }

                // 支付宝 CSV 列索引 (估算): 0:时间, 1:分类, 2:交易对方, 3:商品, 4:收/支, 5:金额...
                String timeStr = line[0].trim();
                String categoryName = line[1].trim();
                String target = line[2].trim();
                String typeStr = line[4].trim();
                String amountStr = line[5].trim();
                String remark = line[3].trim() + " (" + target + ")";

                if ("不计收支".equals(typeStr)) continue;

                Bill bill = new Bill();
                bill.setUserId(userId);
                bill.setBillDate(LocalDateTime.parse(timeStr, dtf).toLocalDate());
                bill.setAmount(new BigDecimal(amountStr));
                bill.setType("支出".equals(typeStr) ? 2 : 1);
                bill.setRemark(remark);
                bill.setInputType(2); // 外部导入
                bill.setCategoryId(mapCategory(categoryName, bill.getType(), categoryMap));
                bill.setCreatedAt(LocalDateTime.now());
                bills.add(bill);
            }
        } catch (Exception e) {
            log.error("Failed to import Alipay CSV", e);
            throw new RuntimeException("解析支付宝账单失败: " + e.getMessage());
        }

        if (!bills.isEmpty()) {
            bills.forEach(billMapper::insert);
        }
        return bills.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importWechatCsv(MultipartFile file, Long userId) {
        List<Bill> bills = new ArrayList<>();
        Map<String, Long> categoryMap = getCategoryMap();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), Charset.forName("UTF-8")))) {
            CSVParser parser = new CSVParserBuilder().withSeparator(',').withQuoteChar('\"').build();
            CSVReader csvReader = new CSVReaderBuilder(reader).withCSVParser(parser).build();

            String[] line;
            boolean dataStarted = false;
            while ((line = csvReader.readNext()) != null) {
                if (line.length > 0 && "交易时间".equals(line[0].trim())) {
                    dataStarted = true;
                    continue;
                }
                if (!dataStarted || line.length < 6) continue;

                // 微信 CSV: 0:时间, 1:类型, 2:对方, 3:商品, 4:收支, 5:金额...
                String timeStr = line[0].trim();
                String target = line[2].trim();
                String product = line[3].trim();
                String typeStr = line[4].trim();
                String amountStr = line[5].trim().replace("¥", "");
                String remark = product + " (" + target + ")";

                if ("/".equals(typeStr)) continue;

                Bill bill = new Bill();
                bill.setUserId(userId);
                bill.setBillDate(LocalDateTime.parse(timeStr, dtf).toLocalDate());
                bill.setAmount(new BigDecimal(amountStr));
                bill.setType("支出".equals(typeStr) ? 2 : 1);
                bill.setRemark(remark);
                bill.setInputType(2);
                bill.setCategoryId(mapCategory(product, bill.getType(), categoryMap));
                bill.setCreatedAt(LocalDateTime.now());
                bills.add(bill);
            }
        } catch (Exception e) {
            log.error("Failed to import Wechat CSV", e);
            throw new RuntimeException("解析微信账单失败: " + e.getMessage());
        }

        if (!bills.isEmpty()) {
            bills.forEach(billMapper::insert);
        }
        return bills.size();
    }

    private Map<String, Long> getCategoryMap() {
        return categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getName, Category::getId, (v1, v2) -> v1));
    }

    private Long mapCategory(String externalCategory, Integer type, Map<String, Long> categoryMap) {
        // 简单模糊匹配或默认分类
        if (externalCategory == null || externalCategory.isEmpty()) return type == 2 ? categoryMap.get("其他支出") : categoryMap.get("其他收入");
        
        for (String ourCat : categoryMap.keySet()) {
            if (externalCategory.contains(ourCat) || ourCat.contains(externalCategory)) {
                return categoryMap.get(ourCat);
            }
        }
        
        // 兜底
        if (type == 2) {
             return categoryMap.getOrDefault("其他", categoryMap.values().iterator().next());
        } else {
             return categoryMap.getOrDefault("其他", categoryMap.values().iterator().next());
        }
    }
}
