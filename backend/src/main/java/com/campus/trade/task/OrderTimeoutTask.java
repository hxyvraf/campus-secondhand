package com.campus.trade.task;

import com.campus.trade.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 订单超时关闭定时任务：默认每分钟扫描一次，超时时长由 app.order.timeout-minutes 控制 */
@Component
public class OrderTimeoutTask {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutTask.class);

    private final OrderService orderService;

    public OrderTimeoutTask(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(cron = "${app.order.scan-cron:0 * * * * ?}")
    public void scan() {
        int closed = orderService.scanTimeoutOrders();
        if (closed > 0) {
            log.info("订单超时扫描完成，自动关闭 {} 个订单", closed);
        }
    }
}
