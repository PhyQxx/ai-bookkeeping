package com.aibookkeeping.service.category;

import com.aibookkeeping.dto.CategoryRequest;
import com.aibookkeeping.entity.Category;
import com.aibookkeeping.vo.CategoryVO;

import java.util.List;

public interface CategoryService {

    /**
     * 获取分类列表（系统预设 + 用户自定义）
     */
    List<CategoryVO> listCategories(Long userId, Integer type);

    /**
     * 创建自定义分类
     */
    CategoryVO createCategory(CategoryRequest request, Long userId);

    /**
     * 更新分类
     */
    CategoryVO updateCategory(Long id, CategoryRequest request, Long userId);

    /**
     * 删除自定义分类
     */
    void deleteCategory(Long id, Long userId);
}
