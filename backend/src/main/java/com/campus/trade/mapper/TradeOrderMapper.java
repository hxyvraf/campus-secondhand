package com.campus.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.trade.entity.TradeOrder;
import com.campus.trade.vo.OrderVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/** 订单 Mapper */
public interface TradeOrderMapper extends BaseMapper<TradeOrder> {

    String ORDER_COLUMNS = """
            SELECT o.id, o.order_no, o.product_id, o.buyer_id, o.seller_id, o.amount, o.status,
                   o.remark, o.cancel_reason, o.expire_at, o.created_at, o.confirmed_at,
                   o.finished_at, o.canceled_at,
                   p.title AS product_title, p.trade_place,
                   (SELECT pi.url FROM product_image pi WHERE pi.product_id = p.id
                     ORDER BY pi.sort ASC, pi.id ASC LIMIT 1) AS cover_image,
                   b.nickname AS buyer_nickname,
                   s.nickname AS seller_nickname
            FROM trade_order o
            JOIN product p ON p.id = o.product_id
            LEFT JOIN `user` b ON b.id = o.buyer_id
            LEFT JOIN `user` s ON s.id = o.seller_id
            """;

    /** 我的订单：role=buyer 看"我买到的"，role=seller 看"我卖出的"，管理员可查全部 */
    @Select("""
            <script>
            """ + ORDER_COLUMNS + """
            WHERE 1 = 1
            <if test="role == 'buyer'"> AND o.buyer_id = #{userId} </if>
            <if test="role == 'seller'"> AND o.seller_id = #{userId} </if>
            <if test="status != null and status != ''"> AND o.status = #{status} </if>
            ORDER BY o.created_at DESC, o.id DESC
            </script>
            """)
    IPage<OrderVO> selectOrderPage(IPage<OrderVO> page,
                                   @Param("userId") Long userId,
                                   @Param("role") String role,
                                   @Param("status") String status);

    /** 订单详情 */
    @Select(ORDER_COLUMNS + " WHERE o.id = #{id}")
    OrderVO selectOrderVOById(@Param("id") Long id);

    /** 待卖家确认且已超过确认期限的订单（供超时扫描任务使用） */
    @Select("""
            SELECT * FROM trade_order
            WHERE status = 'PENDING_CONFIRM' AND expire_at IS NOT NULL AND expire_at < #{now}
            ORDER BY id ASC
            """)
    List<TradeOrder> selectTimeoutOrders(@Param("now") LocalDateTime now);
}
