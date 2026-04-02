package com.aibookkeeping.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryVO {

    private Long id;

    private String name;

    private Integer type;

    private String icon;

    private Integer sortOrder;

    /** 是否系统预设分类 */
    private Boolean isSystem;
}
