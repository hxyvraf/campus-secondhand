package com.campus.trade.controller;

import com.campus.trade.common.PageResult;
import com.campus.trade.common.Result;
import com.campus.trade.security.UserContext;
import com.campus.trade.service.FavoriteService;
import com.campus.trade.vo.FavoriteVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 收藏模块 */
@Tag(name = "06-收藏", description = "收藏、取消收藏、我的收藏")
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @Operation(summary = "收藏商品", description = "重复收藏幂等处理：仍返回 200，但 message 提示已收藏；商品不存在返回 404/40401")
    @PostMapping("/{productId}")
    public Result<Map<String, Boolean>> add(@PathVariable Long productId) {
        boolean added = favoriteService.add(UserContext.userId(), productId);
        return Result.ok(added ? "收藏成功" : "你已经收藏过该商品", Map.of("favorited", true));
    }

    @Operation(summary = "取消收藏", description = "未收藏时幂等处理：返回 200，favorited 为 false")
    @DeleteMapping("/{productId}")
    public Result<Map<String, Boolean>> remove(@PathVariable Long productId) {
        boolean removed = favoriteService.remove(UserContext.userId(), productId);
        return Result.ok(removed ? "取消收藏成功" : "你尚未收藏该商品", Map.of("favorited", false));
    }

    @Operation(summary = "我的收藏列表", description = "登录后分页返回收藏的商品摘要")
    @GetMapping
    public Result<PageResult<FavoriteVO>> page(@RequestParam(required = false) Integer page,
                                               @RequestParam(required = false) Integer size) {
        return Result.ok(favoriteService.page(UserContext.userId(), page, size));
    }

    @Operation(summary = "查询是否已收藏某商品")
    @GetMapping("/{productId}/status")
    public Result<Map<String, Boolean>> status(@PathVariable Long productId) {
        return Result.ok(Map.of("favorited", favoriteService.status(UserContext.userId(), productId)));
    }
}
