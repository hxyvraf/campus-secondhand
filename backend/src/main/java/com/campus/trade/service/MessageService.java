package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.trade.common.BusinessException;
import com.campus.trade.common.PageResult;
import com.campus.trade.common.Pagination;
import com.campus.trade.common.ResultCode;
import com.campus.trade.constant.MessageType;
import com.campus.trade.entity.Message;
import com.campus.trade.mapper.MessageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 站内消息服务 */
@Service
public class MessageService {

    private final MessageMapper messageMapper;

    public MessageService(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    /** 发送一条站内消息（下单、确认、完成、取消、被收藏等业务动作调用） */
    public void push(Long userId, MessageType type, String title, String content, Long relatedId) {
        Message message = new Message();
        message.setUserId(userId);
        message.setType(type.name());
        message.setTitle(title);
        message.setContent(content);
        message.setRelatedId(relatedId);
        message.setIsRead(0);
        messageMapper.insert(message);
    }

    public PageResult<Message> page(Long userId, Integer isRead, String type, Integer page, Integer size) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getUserId, userId)
                .eq(isRead != null, Message::getIsRead, isRead)
                .eq(type != null && !type.isBlank(), Message::getType, type)
                .orderByDesc(Message::getCreatedAt)
                .orderByDesc(Message::getId);
        return PageResult.of(messageMapper.selectPage(Pagination.of(page, size), wrapper), m -> m);
    }

    public long unreadCount(Long userId) {
        return messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getUserId, userId)
                .eq(Message::getIsRead, 0));
    }

    @Transactional
    public void markRead(Long userId, Long messageId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "消息不存在");
        }
        if (!message.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能操作自己的消息");
        }
        if (message.getIsRead() != null && message.getIsRead() == 1) {
            return;
        }
        Message update = new Message();
        update.setId(messageId);
        update.setIsRead(1);
        messageMapper.updateById(update);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return messageMapper.update(null, new LambdaUpdateWrapper<Message>()
                .eq(Message::getUserId, userId)
                .eq(Message::getIsRead, 0)
                .set(Message::getIsRead, 1));
    }
}
