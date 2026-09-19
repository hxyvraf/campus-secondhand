package com.campus.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.trade.entity.Favorite;
import com.campus.trade.vo.FavoriteVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 收藏 Mapper */
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /** 我的收藏列表（联表返回商品摘要） */
    @Select("""
            SELECT f.product_id, p.title, p.price, p.status, f.created_at AS favorited_at,
                   u.nickname AS seller_nickname,
                   (SELECT pi.url FROM product_image pi WHERE pi.product_id = p.id
                     ORDER BY pi.sort ASC, pi.id ASC LIMIT 1) AS cover_image
            FROM favorite f
            JOIN product p ON p.id = f.product_id AND p.deleted = 0
            LEFT JOIN `user` u ON u.id = p.seller_id
            WHERE f.user_id = #{userId}
            ORDER BY f.created_at DESC, f.id DESC
            """)
    IPage<FavoriteVO> selectFavoritePage(IPage<FavoriteVO> page, @Param("userId") Long userId);
}
