package com.aibookkeeping.service.category;

import com.aibookkeeping.dto.CategoryRequest;
import com.aibookkeeping.entity.Category;
import com.aibookkeeping.mapper.CategoryMapper;
import com.aibookkeeping.vo.CategoryVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryVO> listCategories(Long userId, Integer type) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        // 系统预设分类 + 当前用户的自定义分类
        wrapper.and(w -> w.isNull(Category::getUserId).or().eq(Category::getUserId, userId));
        if (type != null) wrapper.eq(Category::getType, type);
        wrapper.orderByAsc(Category::getSortOrder, Category::getId);

        return categoryMapper.selectList(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryVO createCategory(CategoryRequest request, Long userId) {
        Category category = new Category();
        category.setUserId(userId);
        category.setName(request.getName());
        category.setType(request.getType());
        category.setIcon(request.getIcon());
        category.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        categoryMapper.insert(category);
        return convertToVO(category);
    }

    @Override
    public CategoryVO updateCategory(Long id, CategoryRequest request, Long userId) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        // 只能修改自己的自定义分类
        if (category.getUserId() != null && !category.getUserId().equals(userId)) {
            throw new RuntimeException("无权限修改此分类");
        }
        if (category.getUserId() == null) {
            throw new RuntimeException("系统预设分类不可修改");
        }
        category.setName(request.getName());
        category.setType(request.getType());
        category.setIcon(request.getIcon());
        category.setSortOrder(request.getSortOrder());
        categoryMapper.updateById(category);
        return convertToVO(category);
    }

    @Override
    public void deleteCategory(Long id, Long userId) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        if (category.getUserId() == null) {
            throw new RuntimeException("系统预设分类不可删除");
        }
        if (!category.getUserId().equals(userId)) {
            throw new RuntimeException("无权限删除此分类");
        }
        categoryMapper.deleteById(id);
    }

    private CategoryVO convertToVO(Category category) {
        return CategoryVO.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType())
                .icon(category.getIcon())
                .sortOrder(category.getSortOrder())
                .isSystem(category.getUserId() == null)
                .build();
    }
}
