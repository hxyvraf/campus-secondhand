package com.campus.trade.controller;

import com.campus.trade.common.PageResult;
import com.campus.trade.common.Result;
import com.campus.trade.dto.IdResponse;
import com.campus.trade.dto.ProductSaveRequest;
import com.campus.trade.dto.ProductStatusRequest;
import com.campus.trade.security.UserContext;
import com.campus.trade.service.ProductService;
import com.campus.trade.vo.ProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/** 商品发布与管理模块 */
@Tag(name = "04-商品", description = "发布、搜索、详情、编辑、上下架、删除、我发布的商品")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "发布商品", description = "登录后可发布；价格范围 0.01-999999.99；标题 1-50 字；图片最多 5 张")
    @PostMapping
    public Result<IdResponse> create(@Valid @RequestBody ProductSaveRequest request) {
        Long id = productService.create(UserContext.userId(), request);
        return Result.ok("发布成功", new IdResponse(id));
    }

    @Operation(summary = "商品列表（搜索）",
            description = "匿名可访问，只返回在售商品。支持关键词（标题/描述模糊）、分类、价格区间、排序（latest/priceAsc/priceDesc/hot）与分页。"
                    + "价格区间非法（最低价大于最高价）返回 400/40001")
    @GetMapping
    public Result<PageResult<ProductVO>> search(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Long categoryId,
                                                @RequestParam(required = false) BigDecimal minPrice,
                                                @RequestParam(required = false) BigDecimal maxPrice,
                                                @RequestParam(required = false) String sort,
                                                @RequestParam(required = false) Integer page,
                                                @RequestParam(required = false) Integer size) {
        return Result.ok(productService.search(keyword, categoryId, minPrice, maxPrice, sort, page, size));
    }

    @Operation(summary = "我发布的商品", description = "登录后使用，可按状态筛选，返回全部状态（含已下架、交易中、已售出）")
    @GetMapping("/mine")
    public Result<PageResult<ProductVO>> mine(@RequestParam(required = false) String status,
                                              @RequestParam(required = false) Integer page,
                                              @RequestParam(required = false) Integer size) {
        return Result.ok(productService.mine(UserContext.userId(), status, page, size));
    }

    @Operation(summary = "商品详情", description = "匿名可访问；返回图片列表与当前登录用户是否已收藏；每次调用浏览量 +1；商品不存在返回 404/40401")
    @GetMapping("/{id}")
    public Result<ProductVO> detail(@PathVariable Long id) {
        return Result.ok(productService.detail(id));
    }

    @Operation(summary = "编辑商品", description = "仅发布者本人可编辑，非本人返回 403/40301；交易中或已售出商品不可编辑，返回 409/40902")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProductSaveRequest request) {
        productService.update(id, UserContext.userId(), request);
        return Result.ok("修改成功", null);
    }

    @Operation(summary = "商品上架/下架", description = "status 只能为 ON_SALE 或 OFF_SHELF；非法流转返回 409/40902")
    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody ProductStatusRequest request) {
        productService.updateStatus(id, UserContext.userId(), request.status());
        return Result.ok("操作成功", null);
    }

    @Operation(summary = "删除商品", description = "逻辑删除，仅本人可删除；交易中的商品不可删除，返回 409/40902")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id, UserContext.userId());
        return Result.ok("删除成功", null);
    }
}
