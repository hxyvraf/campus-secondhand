package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.trade.common.BusinessException;
import com.campus.trade.common.ResultCode;
import com.campus.trade.dto.CategoryRequest;
import com.campus.trade.entity.Category;
import com.campus.trade.entity.Product;
import com.campus.trade.mapper.CategoryMapper;
import com.campus.trade.mapper.ProductMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 分类服务 */
@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;

    public CategoryService(CategoryMapper categoryMapper, ProductMapper productMapper) {
        this.categoryMapper = categoryMapper;
        this.productMapper = productMapper;
    }

    public List<Category> list() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
    }

    public Category getOrThrow(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分类不存在");
        }
        return category;
    }

    @Transactional
    public Category create(CategoryRequest request) {
        Long exists = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getName, request.name()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分类名称已存在");
        }
        Category category = new Category();
        category.setName(request.name());
        category.setSort(request.sort() == null ? 99 : request.sort());
        categoryMapper.insert(category);
        return category;
    }

    @Transactional
    public Category update(Long id, CategoryRequest request) {
        Category category = getOrThrow(id);
        Long exists = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getName, request.name())
                .ne(Category::getId, id));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分类名称已存在");
        }
        category.setName(request.name());
        if (request.sort() != null) {
            category.setSort(request.sort());
        }
        categoryMapper.updateById(category);
        return category;
    }

    @Transactional
    public void delete(Long id) {
        getOrThrow(id);
        Long productCount = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getCategoryId, id));
        if (productCount != null && productCount > 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR,
                    "该分类下还有 " + productCount + " 件商品，无法删除");
        }
        categoryMapper.deleteById(id);
    }
}
