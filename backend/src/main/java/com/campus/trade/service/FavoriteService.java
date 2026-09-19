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
import com.campus.trade.entity.Favorite;
import com.campus.trade.entity.Product;
import com.campus.trade.mapper.FavoriteMapper;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.vo.FavoriteVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 收藏服务 */
@Service
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final ProductMapper productMapper;
    private final ProductService productService;
    private final MessageService messageService;

    public FavoriteService(FavoriteMapper favoriteMapper, ProductMapper productMapper,
                           ProductService productService, MessageService messageService) {
        this.favoriteMapper = favoriteMapper;
        this.productMapper = productMapper;
        this.productService = productService;
        this.messageService = messageService;
    }

    /** 收藏商品；重复收藏视为幂等成功，返回 false 表示之前已收藏过 */
    @Transactional
    public boolean add(Long userId, Long productId) {
        Product product = productService.getProductOrThrow(productId);
        Favorite exists = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId));
        if (exists != null) {
            return false;
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        favoriteMapper.insert(favorite);
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .setSql("favorite_count = favorite_count + 1"));
        if (!product.getSellerId().equals(userId)) {
            messageService.push(product.getSellerId(), MessageType.FAVORITE, "商品被收藏",
                    "有同学收藏了你发布的商品《" + shortTitle(product.getTitle()) + "》", productId);
        }
        return true;
    }

    /** 取消收藏；未收藏过时同样幂等成功，返回 false 表示本来就没有收藏 */
    @Transactional
    public boolean remove(Long userId, Long productId) {
        Favorite exists = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId));
        if (exists == null) {
            return false;
        }
        favoriteMapper.deleteById(exists.getId());
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .gt(Product::getFavoriteCount, 0)
                .setSql("favorite_count = favorite_count - 1"));
        return true;
    }

    public PageResult<FavoriteVO> page(Long userId, Integer page, Integer size) {
        IPage<FavoriteVO> result = favoriteMapper.selectFavoritePage(Pagination.of(page, size), userId);
        result.getRecords().forEach(vo -> vo.setStatusLabel(Labels.product(vo.getStatus())));
        return PageResult.of(result, v -> v);
    }

    public boolean status(Long userId, Long productId) {
        productService.getProductOrThrow(productId);
        Long count = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId));
        return count != null && count > 0;
    }

    private String shortTitle(String title) {
        if (title == null) {
            return "";
        }
        return title.length() > 20 ? title.substring(0, 20) + "..." : title;
    }
}
