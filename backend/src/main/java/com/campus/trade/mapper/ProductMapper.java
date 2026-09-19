package com.campus.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.trade.entity.Product;
import com.campus.trade.vo.ProductVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/** 商品 Mapper（列表查询走自定义 JOIN，避免多次单表查询） */
public interface ProductMapper extends BaseMapper<Product> {

    String BASE_COLUMNS = """
            SELECT p.id, p.seller_id, p.category_id, p.title, p.description, p.price, p.original_price,
                   p.condition_level, p.trade_place, p.status, p.view_count, p.favorite_count, p.created_at,
                   c.name AS category_name,
                   u.nickname AS seller_nickname,
                   u.avatar AS seller_avatar,
                   (SELECT pi.url FROM product_image pi WHERE pi.product_id = p.id
                     ORDER BY pi.sort ASC, pi.id ASC LIMIT 1) AS cover_image
            FROM product p
            LEFT JOIN category c ON c.id = p.category_id
            LEFT JOIN `user` u ON u.id = p.seller_id
            """;

    /** 商品搜索/筛选/分页：兼顾首页列表、我的商品、用户主页在售商品、管理端商品列表 */
    @Select("""
            <script>
            """ + BASE_COLUMNS + """
            WHERE p.deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (p.title LIKE CONCAT('%', #{keyword}, '%') OR p.description LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="categoryId != null"> AND p.category_id = #{categoryId} </if>
            <if test="minPrice != null"> AND p.price &gt;= #{minPrice} </if>
            <if test="maxPrice != null"> AND p.price &lt;= #{maxPrice} </if>
            <if test="sellerId != null"> AND p.seller_id = #{sellerId} </if>
            <if test="status != null and status != ''"> AND p.status = #{status} </if>
            <if test="onlyOnSale != null and onlyOnSale"> AND p.status = 'ON_SALE' </if>
            <choose>
              <when test="sort == 'priceAsc'"> ORDER BY p.price ASC, p.id DESC </when>
              <when test="sort == 'priceDesc'"> ORDER BY p.price DESC, p.id DESC </when>
              <when test="sort == 'hot'"> ORDER BY p.favorite_count DESC, p.view_count DESC, p.id DESC </when>
              <otherwise> ORDER BY p.created_at DESC, p.id DESC </otherwise>
            </choose>
            </script>
            """)
    IPage<ProductVO> searchProducts(IPage<ProductVO> page,
                                    @Param("keyword") String keyword,
                                    @Param("categoryId") Long categoryId,
                                    @Param("minPrice") BigDecimal minPrice,
                                    @Param("maxPrice") BigDecimal maxPrice,
                                    @Param("sellerId") Long sellerId,
                                    @Param("status") String status,
                                    @Param("onlyOnSale") Boolean onlyOnSale,
                                    @Param("sort") String sort);

    /** 商品详情 */
    @Select(BASE_COLUMNS + " WHERE p.id = #{id} AND p.deleted = 0")
    ProductVO selectProductVOById(@Param("id") Long id);
}
