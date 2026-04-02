package com.aibookkeeping.controller;

import com.aibookkeeping.dto.CategoryRequest;
import com.aibookkeeping.exception.Result;
import com.aibookkeeping.service.category.CategoryService;
import com.aibookkeeping.vo.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category")
@Tag(name = "分类管理", description = "分类列表、新增、编辑、删除")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/list")
    @Operation(summary = "获取分类列表")
    public Result<List<CategoryVO>> listCategories(
            @RequestParam(required = false) Integer type,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<CategoryVO> list = categoryService.listCategories(userId, type);
        return Result.success(list);
    }

    @PostMapping
    @Operation(summary = "创建自定义分类")
    public Result<CategoryVO> createCategory(@Valid @RequestBody CategoryRequest request,
                                              Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        CategoryVO vo = categoryService.createCategory(request, userId);
        return Result.success(vo);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新分类")
    public Result<CategoryVO> updateCategory(@PathVariable Long id,
                                              @Valid @RequestBody CategoryRequest request,
                                              Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        CategoryVO vo = categoryService.updateCategory(id, request, userId);
        return Result.success(vo);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除自定义分类")
    public Result<Void> deleteCategory(@PathVariable Long id,
                                        Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        categoryService.deleteCategory(id, userId);
        return Result.success();
    }
}
