package com.campus.trade.controller;

import com.campus.trade.common.PageResult;
import com.campus.trade.common.Result;
import com.campus.trade.entity.Message;
import com.campus.trade.security.UserContext;
import com.campus.trade.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 消息通知模块 */
@Tag(name = "08-消息", description = "站内消息列表、未读数、标记已读")
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @Operation(summary = "消息列表", description = "分页返回当前登录用户的消息；isRead 可选 0 未读 / 1 已读；type 可选 ORDER/FAVORITE/SYSTEM")
    @GetMapping
    public Result<PageResult<Message>> page(@RequestParam(required = false) Integer isRead,
                                            @RequestParam(required = false) String type,
                                            @RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer size) {
        return Result.ok(messageService.page(UserContext.userId(), isRead, type, page, size));
    }

    @Operation(summary = "未读消息数量", description = "前端每 30 秒轮询该接口刷新消息红点")
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount() {
        return Result.ok(Map.of("count", messageService.unreadCount(UserContext.userId())));
    }

    @Operation(summary = "标记单条消息为已读", description = "只能操作自己的消息，他人消息返回 403/40301；消息不存在返回 404/40401")
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        messageService.markRead(UserContext.userId(), id);
        return Result.ok("已标记为已读", null);
    }

    @Operation(summary = "全部标记为已读", description = "返回本次更新的条数")
    @PutMapping("/read-all")
    public Result<Map<String, Integer>> markAllRead() {
        int updated = messageService.markAllRead(UserContext.userId());
        return Result.ok("已全部标记为已读", Map.of("updated", updated));
    }
}
