package com.aibookkeeping.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiParsePreviewVO {

    /** 解析后的金额 */
    private BigDecimal amount;

    /** 推荐分类名称 */
    private String category;

    /** 推荐分类ID */
    private Long categoryId;

    /** 解析后的日期 */
    private String date;

    /** 解析后的备注 */
    private String remark;

    /** 候选分类列表（供用户选择） */
    private List<CategoryCandidate> candidates;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryCandidate {
        private Long id;
        private String name;
        private Integer type;
    }
}
