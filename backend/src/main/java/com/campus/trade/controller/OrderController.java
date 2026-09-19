package com.campus.trade.controller;

import com.campus.trade.common.PageResult;
import com.campus.trade.common.Result;
import com.campus.trade.dto.OrderCancelRequest;
import com.campus.trade.dto.OrderCreateRequest;
import com.campus.trade.security.UserContext;
import com.campus.trade.service.OrderService;
import com.campus.trade.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 订单管理模块 */
@Tag(name = "07-订单", description = "下单、订单列表、订单详情、卖家确认、买家完成、取消订单")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "提交订单",
            description = "下单后商品变成交易中(LOCKED)；不能购买自己的商品(409/40903)；"
                    + "同一买家对同一商品重复下单返回 409/40901；商品不可交易返回 409/40903")
    @PostMapping
    public Result<OrderVO> create(@Valid @RequestBody OrderCreateRequest request) {
        return Result.ok("下单成功，等待卖家确认", orderService.create(UserContext.userId(), request));
    }

    @Operation(summary = "我的订单列表",
            description = "role=buyer 我买到的（默认），role=seller 我卖出的，管理员可用 all；status 可选："
                    + "PENDING_CONFIRM / CONFIRMED / COMPLETED / CANCELED / TIMEOUT")
    @GetMapping
    public Result<PageResult<OrderVO>> page(@RequestParam(required = false) String role,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer size) {
        return Result.ok(orderService.page(UserContext.userId(), role, status, page, size));
    }

    @Operation(summary = "订单详情", description = "仅买卖双方或管理员可查看，越权返回 403/40301；订单不存在返回 404/40401")
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.ok(orderService.detail(id, UserContext.userId(), UserContext.isAdmin()));
    }

    @Operation(summary = "卖家确认订单",
            description = "仅卖家可操作(403/40301)；只有待卖家确认的订单才能确认，状态非法返回 409/40902")
    @PostMapping("/{id}/confirm")
    public Result<OrderVO> confirm(@PathVariable Long id) {
        return Result.ok("已确认订单，请尽快完成交易", orderService.confirm(id, UserContext.userId()));
    }

    @Operation(summary = "买家确认完成",
            description = "仅买家可操作(403/40301)；只有交易中的订单才能完成，完成后商品状态变为已售出，状态非法返回 409/40902")
    @PostMapping("/{id}/finish")
    public Result<OrderVO> finish(@PathVariable Long id) {
        return Result.ok("交易已完成", orderService.finish(id, UserContext.userId()));
    }

    @Operation(summary = "取消订单",
            description = "买卖双方均可取消；待确认/交易中的订单可取消，其余状态返回 409/40902；取消后商品重新上架")
    @PostMapping("/{id}/cancel")
    public Result<OrderVO> cancel(@PathVariable Long id, @Valid @RequestBody(required = false) OrderCancelRequest request) {
        String reason = request == null ? null : request.reason();
        return Result.ok("订单已取消", orderService.cancel(id, UserContext.userId(), reason));
    }
}
