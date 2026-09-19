package com.campus.trade.controller;

import com.campus.trade.common.PageResult;
import com.campus.trade.common.Result;
import com.campus.trade.dto.CategoryRequest;
import com.campus.trade.dto.IdResponse;
import com.campus.trade.dto.UserStatusRequest;
import com.campus.trade.entity.Category;
import com.campus.trade.security.UserContext;
import com.campus.trade.service.CategoryService;
import com.campus.trade.service.OrderService;
import com.campus.trade.service.ProductService;
import com.campus.trade.service.UserService;
import com.campus.trade.vo.OrderVO;
import com.campus.trade.vo.ProductVO;
import com.campus.trade.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 管理端模块：非管理员访问统一返回 403/40303 */
@Tag(name = "09-管理端", description = "商品管理、用户管理、分类维护、订单超时扫描（均需管理员账号 admin）")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ProductService productService;
    private final UserService userService;
    private final CategoryService categoryService;
    private final OrderService orderService;

    public AdminController(ProductService productService, UserService userService,
                           CategoryService categoryService, OrderService orderService) {
        this.productService = productService;
        this.userService = userService;
        this.categoryService = categoryService;
        this.orderService = orderService;
    }

    @Operation(summary = "商品管理列表", description = "可按关键词、状态筛选全部商品（含已下架、交易中、已售出、已售出）")
    @GetMapping("/products")
    public Result<PageResult<ProductVO>> products(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) Integer page,
                                                  @RequestParam(required = false) Integer size) {
        UserContext.requireAdmin();
        return Result.ok(productService.adminPage(keyword, status, page, size));
    }

    @Operation(summary = "强制下架商品", description = "交易中或已售出的商品不能强制下架，返回 409/40902")
    @PutMapping("/products/{id}/off-shelf")
    public Result<Void> offShelf(@PathVariable Long id) {
        UserContext.requireAdmin();
        productService.offShelf(id);
        return Result.ok("已强制下架", null);
    }

    @Operation(summary = "用户列表", description = "支持账号/昵称模糊搜索")
    @GetMapping("/users")
    public Result<PageResult<UserVO>> users(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer size) {
        UserContext.requireAdmin();
        return Result.ok(userService.adminPage(keyword, page, size));
    }

    @Operation(summary = "启用/禁用用户",
            description = "status=1 启用、0 禁用；不能修改自己或管理员的账号状态(403/40301)；被禁用的用户立即无法调用需要登录的接口(403/40302)")
    @PutMapping("/users/{id}/status")
    public Result<UserVO> updateUserStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        UserContext.requireAdmin();
        return Result.ok("用户状态已更新", userService.updateStatus(UserContext.userId(), id, request.status()));
    }

    @Operation(summary = "强制取消订单", description = "管理员可将待确认/交易中的订单强制取消，取消后商品重新上架")
    @PostMapping("/orders/{id}/cancel")
    public Result<OrderVO> cancelOrder(@PathVariable Long id) {
        UserContext.requireAdmin();
        return Result.ok("订单已强制取消", orderService.adminCancel(id, "管理员强制取消"));
    }

    @Operation(summary = "新增分类")
    @PostMapping("/categories")
    public Result<IdResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        UserContext.requireAdmin();
        Category category = categoryService.create(request);
        return Result.ok("分类新增成功", new IdResponse(category.getId()));
    }

    @Operation(summary = "修改分类")
    @PutMapping("/categories/{id}")
    public Result<Category> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        UserContext.requireAdmin();
        return Result.ok("分类修改成功", categoryService.update(id, request));
    }

    @Operation(summary = "删除分类", description = "该分类下仍有商品时返回 409/40902")
    @DeleteMapping("/categories/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        UserContext.requireAdmin();
        categoryService.delete(id);
        return Result.ok("分类删除成功", null);
    }

    @Operation(summary = "手动触发订单超时扫描",
            description = "与后台定时任务逻辑一致，用于测试超时场景：不必等待定时任务，调一次即可关闭所有超时订单。返回本次关闭的订单数")
    @PostMapping("/orders/timeout-scan")
    public Result<Map<String, Integer>> timeoutScan() {
        UserContext.requireAdmin();
        return Result.ok("超时扫描完成", Map.of("closedCount", orderService.scanTimeoutOrders()));
    }
}
