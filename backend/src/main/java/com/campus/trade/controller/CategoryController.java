package com.campus.trade.controller;

import com.campus.trade.common.Result;
import com.campus.trade.entity.Category;
import com.campus.trade.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 商品分类模块 */
@Tag(name = "03-分类", description = "分类列表（管理端的分类维护接口见「06-管理端」）")
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "分类列表", description = "匿名可访问，按 sort 升序返回")
    @GetMapping
    public Result<List<Category>> list() {
        return Result.ok(categoryService.list());
    }
}
