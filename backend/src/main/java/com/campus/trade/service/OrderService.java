package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.trade.common.BusinessException;
import com.campus.trade.common.Labels;
import com.campus.trade.common.PageResult;
import com.campus.trade.common.Pagination;
import com.campus.trade.common.ResultCode;
import com.campus.trade.constant.MessageType;
import com.campus.trade.constant.OrderStatus;
import com.campus.trade.constant.ProductStatus;
import com.campus.trade.dto.OrderCreateRequest;
import com.campus.trade.entity.Product;
import com.campus.trade.entity.TradeOrder;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.mapper.TradeOrderMapper;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.vo.OrderVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** 订单服务：下单、确认、完成、取消、超时关闭 */
@Service
public class OrderService {

    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final TradeOrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final ProductService productService;
    private final MessageService messageService;
    private final int timeoutMinutes;

    public OrderService(TradeOrderMapper orderMapper, ProductMapper productMapper, UserMapper userMapper,
                        ProductService productService, MessageService messageService,
                        @Value("${app.order.timeout-minutes:30}") int timeoutMinutes) {
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
        this.productService = productService;
        this.messageService = messageService;
        this.timeoutMinutes = timeoutMinutes;
    }

    /** 下单：校验商品可交易、防止重复下单（同一买家对同一商品只能有一个进行中的订单） */
    @Transactional
    public OrderVO create(Long buyerId, OrderCreateRequest request) {
        Product product = productService.getProductOrThrow(request.productId());
        if (product.getSellerId().equals(buyerId)) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_TRADABLE, "不能购买自己发布的商品");
        }
        // 先判断重复下单，再判断商品状态：同一个买家重复提交时要给出更精确的 40901 提示，
        // 而不是被"商品已锁定"的 40903 覆盖（这是重复提交类缺陷的常见修复点）。
        Long activeOrders = orderMapper.selectCount(new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getBuyerId, buyerId)
                .eq(TradeOrder::getProductId, product.getId())
                .in(TradeOrder::getStatus, List.of(OrderStatus.PENDING_CONFIRM.name(), OrderStatus.CONFIRMED.name())));
        if (activeOrders != null && activeOrders > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_ORDER);
        }
        if (!ProductStatus.ON_SALE.name().equals(product.getStatus())) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_TRADABLE,
                    "商品当前不可交易（当前状态：" + Labels.product(product.getStatus()) + "）");
        }
        // 乐观更新抢占商品：仅当商品仍在售时才锁定成功，避免并发下重复下单
        int locked = productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, product.getId())
                .eq(Product::getStatus, ProductStatus.ON_SALE.name())
                .set(Product::getStatus, ProductStatus.LOCKED.name()));
        if (locked == 0) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_TRADABLE, "商品已被其他同学抢先下单");
        }

        LocalDateTime now = LocalDateTime.now();
        TradeOrder order = new TradeOrder();
        order.setOrderNo(generateOrderNo(now));
        order.setProductId(product.getId());
        order.setBuyerId(buyerId);
        order.setSellerId(product.getSellerId());
        order.setAmount(product.getPrice());
        order.setStatus(OrderStatus.PENDING_CONFIRM.name());
        order.setRemark(request.remark());
        order.setExpireAt(now.plusMinutes(timeoutMinutes));
        orderMapper.insert(order);

        User buyer = userMapper.selectById(buyerId);
        messageService.push(product.getSellerId(), MessageType.ORDER, "收到新订单",
                "买家【" + nickname(buyer) + "】拍下了你的商品《" + shortTitle(product.getTitle())
                        + "》，请在 " + timeoutMinutes + " 分钟内确认，超时订单将自动关闭。", order.getId());
        return detailVO(order.getId());
    }

    /** 我的订单：role=buyer 我买到的 / role=seller 我卖出的，管理员可选 all */
    public PageResult<OrderVO> page(Long userId, String role, String status, Integer page, Integer size) {
        String normalizedRole = normalizeRole(role);
        if (status != null && !status.isBlank() && !isValidStatus(status)) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "订单状态只能为：PENDING_CONFIRM、CONFIRMED、COMPLETED、CANCELED、TIMEOUT");
        }
        IPage<OrderVO> result = orderMapper.selectOrderPage(Pagination.of(page, size), userId, normalizedRole,
                status == null || status.isBlank() ? null : status);
        result.getRecords().forEach(vo -> vo.setStatusLabel(Labels.order(vo.getStatus())));
        return PageResult.of(result, v -> v);
    }

    /** 订单详情：仅买卖双方或管理员可见 */
    public OrderVO detail(Long orderId, Long userId, boolean admin) {
        TradeOrder order = getOrderOrThrow(orderId);
        if (!admin && !order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能查看与自己相关的订单");
        }
        return detailVO(orderId);
    }

    /** 卖家确认订单 */
    @Transactional
    public OrderVO confirm(Long orderId, Long sellerId) {
        TradeOrder order = getOrderOrThrow(orderId);
        if (!order.getSellerId().equals(sellerId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有卖家可以确认订单");
        }
        if (!OrderStatus.PENDING_CONFIRM.name().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR,
                    "只有待卖家确认的订单才能确认（当前状态：" + Labels.order(order.getStatus()) + "）");
        }
        TradeOrder update = new TradeOrder();
        update.setId(orderId);
        update.setStatus(OrderStatus.CONFIRMED.name());
        update.setConfirmedAt(LocalDateTime.now());
        orderMapper.updateById(update);

        messageService.push(order.getBuyerId(), MessageType.ORDER, "卖家已确认订单",
                "卖家【" + nickname(userMapper.selectById(sellerId)) + "】已确认订单，请尽快完成线下交易，"
                        + "交易完成后记得点击「确认完成」。", orderId);
        return detailVO(orderId);
    }

    /** 买家确认完成，商品标记为已售出 */
    @Transactional
    public OrderVO finish(Long orderId, Long buyerId) {
        TradeOrder order = getOrderOrThrow(orderId);
        if (!order.getBuyerId().equals(buyerId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有买家可以确认完成");
        }
        if (!OrderStatus.CONFIRMED.name().equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR,
                    "只有交易中的订单才能确认完成（当前状态：" + Labels.order(order.getStatus()) + "）");
        }
        TradeOrder update = new TradeOrder();
        update.setId(orderId);
        update.setStatus(OrderStatus.COMPLETED.name());
        update.setFinishedAt(LocalDateTime.now());
        orderMapper.updateById(update);
        updateProductStatus(order.getProductId(), ProductStatus.SOLD.name());

        messageService.push(order.getSellerId(), MessageType.ORDER, "订单已完成",
                "买家【" + nickname(userMapper.selectById(buyerId)) + "】已确认完成交易，商品已标记为已售出。", orderId);
        return detailVO(orderId);
    }

    /** 取消订单：买家或卖家都可以取消，商品回到在售状态 */
    @Transactional
    public OrderVO cancel(Long orderId, Long userId, String reason) {
        TradeOrder order = getOrderOrThrow(orderId);
        boolean isBuyer = order.getBuyerId().equals(userId);
        boolean isSeller = order.getSellerId().equals(userId);
        if (!isBuyer && !isSeller) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能取消与自己相关的订单");
        }
        String finalReason = reason == null || reason.isBlank()
                ? (isBuyer ? "买家取消" : "卖家取消") : reason.trim();
        doCancel(order, finalReason);

        String who = isBuyer ? "买家" : "卖家";
        messageService.push(isBuyer ? order.getSellerId() : order.getBuyerId(), MessageType.ORDER, "订单已取消",
                who + "【" + nickname(userMapper.selectById(userId)) + "】取消了订单，原因："
                        + finalReason + "。商品已重新上架。", orderId);
        return detailVO(orderId);
    }

    /** 管理端强制取消订单（管理员不是买卖双方，单独走这条路径） */
    @Transactional
    public OrderVO adminCancel(Long orderId, String reason) {
        TradeOrder order = getOrderOrThrow(orderId);
        String finalReason = reason == null || reason.isBlank() ? "管理员强制取消" : reason.trim();
        doCancel(order, finalReason);
        messageService.push(order.getBuyerId(), MessageType.ORDER, "订单已取消",
                "管理员取消了订单，原因：" + finalReason + "。商品已重新上架。", orderId);
        messageService.push(order.getSellerId(), MessageType.ORDER, "订单已取消",
                "管理员取消了订单，原因：" + finalReason + "。商品已重新上架。", orderId);
        return detailVO(orderId);
    }

    /** 取消/关闭订单的公共逻辑：状态校验 + 商品回到在售 */
    private void doCancel(TradeOrder order, String reason) {
        if (!OrderStatus.isActive(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR,
                    "只有待确认或交易中的订单才能取消（当前状态：" + Labels.order(order.getStatus()) + "）");
        }
        TradeOrder update = new TradeOrder();
        update.setId(order.getId());
        update.setStatus(OrderStatus.CANCELED.name());
        update.setCancelReason(reason);
        update.setCanceledAt(LocalDateTime.now());
        orderMapper.updateById(update);
        // 商品回到在售状态，允许其他同学继续购买
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, order.getProductId())
                .eq(Product::getStatus, ProductStatus.LOCKED.name())
                .set(Product::getStatus, ProductStatus.ON_SALE.name()));
    }

    /** 扫描超过确认时限的订单并自动关闭（定时任务 / 管理端手动触发都会调用） */
    @Transactional
    public int scanTimeoutOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<TradeOrder> timeoutOrders = orderMapper.selectTimeoutOrders(now);
        for (TradeOrder order : timeoutOrders) {
            TradeOrder update = new TradeOrder();
            update.setId(order.getId());
            update.setStatus(OrderStatus.TIMEOUT.name());
            update.setCancelReason("卖家超时未确认，系统自动关闭");
            update.setCanceledAt(now);
            orderMapper.updateById(update);
            productMapper.update(null, new LambdaUpdateWrapper<Product>()
                    .eq(Product::getId, order.getProductId())
                    .eq(Product::getStatus, ProductStatus.LOCKED.name())
                    .set(Product::getStatus, ProductStatus.ON_SALE.name()));
            messageService.push(order.getBuyerId(), MessageType.ORDER, "订单超时关闭",
                    "卖家超时未确认，订单已自动关闭，商品重新上架。", order.getId());
            messageService.push(order.getSellerId(), MessageType.ORDER, "订单超时关闭",
                    "订单因超时未确认被系统自动关闭，商品已重新上架。", order.getId());
        }
        return timeoutOrders.size();
    }

    public TradeOrder getOrderOrThrow(Long orderId) {
        TradeOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private OrderVO detailVO(Long orderId) {
        OrderVO vo = orderMapper.selectOrderVOById(orderId);
        if (vo != null) {
            vo.setStatusLabel(Labels.order(vo.getStatus()));
        }
        return vo;
    }

    private void updateProductStatus(Long productId, String status) {
        Product update = new Product();
        update.setId(productId);
        update.setStatus(status);
        productMapper.updateById(update);
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "buyer";
        }
        if (!List.of("buyer", "seller", "all").contains(role)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "角色参数只支持：buyer、seller、all");
        }
        return role;
    }

    private boolean isValidStatus(String status) {
        try {
            OrderStatus.valueOf(status);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String generateOrderNo(LocalDateTime now) {
        return "C" + now.format(ORDER_NO_FORMAT) + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    private String nickname(User user) {
        return user == null ? "未知用户" : user.getNickname();
    }

    private String shortTitle(String title) {
        if (title == null) {
            return "";
        }
        return title.length() > 20 ? title.substring(0, 20) + "..." : title;
    }
}
