package com.aibookkeeping.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiParseVO {

    /** 解析是否成功 */
    private Boolean success;

    /** 解析后的金额 */
    private java.math.BigDecimal amount;

    /** 解析后的分类名称 */
    private String category;

    /** 解析后的分类ID */
    private Long categoryId;

    /** 解析后的日期 */
    private String date;

    /** 解析后的备注 */
    private String remark;

    /** 错误信息（解析失败时） */
    private String errorMessage;

    /** 已创建的账单ID（解析成功并自动记账时） */
    private Long billId;
}
