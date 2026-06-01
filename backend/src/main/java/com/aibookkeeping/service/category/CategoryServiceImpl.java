package com.aibookkeeping.service.category;

import com.aibookkeeping.dto.CategoryRequest;
import com.aibookkeeping.entity.Category;
import com.aibookkeeping.exception.BusinessException;
import com.aibookkeeping.exception.ErrorCode;
import com.aibookkeeping.mapper.CategoryMapper;
import com.aibookkeeping.vo.CategoryVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY_PREFIX = "category:list:";
    private static final long CACHE_EXPIRE_HOURS = 24;

    @Override
    public List<CategoryVO> listCategories(Long userId, Integer type) {
        String cacheKey = CACHE_KEY_PREFIX + userId + ":" + (type == null ? "all" : type);
        
        // Try cache
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Category list cache hit for user: {}", userId);
            return (List<CategoryVO>) cached;
        }

        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.isNull(Category::getUserId).or().eq(Category::getUserId, userId));
        if (type != null) wrapper.eq(Category::getType, type);
        wrapper.orderByAsc(Category::getSortOrder, Category::getId);

        List<CategoryVO> list = categoryMapper.selectList(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        // Save to cache
        redisTemplate.opsForValue().set(cacheKey, list, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        return list;
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
        
        clearCache(userId);
        return convertToVO(category);
    }

    @Override
    public CategoryVO updateCategory(Long id, CategoryRequest request, Long userId) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        if (category.getUserId() == null) {
            throw new BusinessException(ErrorCode.CATEGORY_SYSTEM_CANNOT_MODIFY);
        }
        if (!category.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CATEGORY_NO_PERMISSION);
        }
        category.setName(request.getName());
        category.setType(request.getType());
        category.setIcon(request.getIcon());
        category.setSortOrder(request.getSortOrder());
        categoryMapper.updateById(category);
        
        clearCache(userId);
        return convertToVO(category);
    }

    @Override
    public void deleteCategory(Long id, Long userId) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        if (category.getUserId() == null) {
            throw new BusinessException(ErrorCode.CATEGORY_SYSTEM_CANNOT_DELETE);
        }
        if (!category.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CATEGORY_NO_PERMISSION);
        }
        categoryMapper.deleteById(id);
        
        clearCache(userId);
    }

    private void clearCache(Long userId) {
        String pattern = CACHE_KEY_PREFIX + userId + ":*";
        java.util.Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Cleared category cache for user: {}", userId);
        }
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
