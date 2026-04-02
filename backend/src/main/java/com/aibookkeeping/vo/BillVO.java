package com.aibookkeeping.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillVO {

    private Long id;

    private BigDecimal amount;

    private Integer type;

    private String typeName;

    private Long categoryId;

    private String categoryName;

    private LocalDate billDate;

    private String remark;

    private Integer inputType;

    private LocalDateTime createdAt;
}
