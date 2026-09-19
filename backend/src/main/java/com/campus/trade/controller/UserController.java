package com.campus.trade.controller;

import com.campus.trade.common.PageResult;
import com.campus.trade.common.Result;
import com.campus.trade.dto.UpdateProfileRequest;
import com.campus.trade.security.UserContext;
import com.campus.trade.service.ProductService;
import com.campus.trade.service.UserService;
import com.campus.trade.vo.ProductVO;
import com.campus.trade.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 用户资料模块 */
@Tag(name = "02-用户", description = "用户公开主页、个人资料维护")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final ProductService productService;

    public UserController(UserService userService, ProductService productService) {
        this.userService = userService;
        this.productService = productService;
    }

    @Operation(summary = "查看用户公开资料", description = "匿名可访问；非本人查看时手机号脱敏、邮箱不返回；用户不存在返回 404/40401")
    @GetMapping("/{id}")
    public Result<UserVO> profile(@PathVariable Long id) {
        Long currentUserId = UserContext.get() == null ? null : UserContext.get().userId();
        return Result.ok(userService.getPublicProfile(id, currentUserId, UserContext.isAdmin()));
    }

    @Operation(summary = "更新个人资料", description = "登录后可修改昵称、手机号、邮箱、头像、学校")
    @PutMapping("/me")
    public Result<UserVO> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.ok("资料更新成功", userService.updateProfile(UserContext.userId(), request));
    }

    @Operation(summary = "查看某个用户在售的商品", description = "匿名可访问，分页返回")
    @GetMapping("/{id}/products")
    public Result<PageResult<ProductVO>> userProducts(@PathVariable Long id,
                                                      @RequestParam(required = false) Integer page,
                                                      @RequestParam(required = false) Integer size) {
        userService.getUserOrThrow(id);
        return Result.ok(productService.onSaleOfUser(id, page, size));
    }
}
