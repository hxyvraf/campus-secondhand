package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.trade.common.BusinessException;
import com.campus.trade.common.Labels;
import com.campus.trade.common.PageResult;
import com.campus.trade.common.Pagination;
import com.campus.trade.common.ResultCode;
import com.campus.trade.constant.ProductStatus;
import com.campus.trade.dto.ProductSaveRequest;
import com.campus.trade.entity.Favorite;
import com.campus.trade.entity.Product;
import com.campus.trade.entity.ProductImage;
import com.campus.trade.mapper.CategoryMapper;
import com.campus.trade.mapper.FavoriteMapper;
import com.campus.trade.mapper.ProductImageMapper;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.security.LoginUser;
import com.campus.trade.security.UserContext;
import com.campus.trade.vo.ProductVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 商品服务：发布、编辑、上下架、删除、搜索、详情 */
@Service
public class ProductService {

    private static final List<String> SORT_TYPES = List.of("latest", "priceAsc", "priceDesc", "hot");

    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final CategoryMapper categoryMapper;
    private final FavoriteMapper favoriteMapper;

    public ProductService(ProductMapper productMapper, ProductImageMapper productImageMapper,
                          CategoryMapper categoryMapper, FavoriteMapper favoriteMapper) {
        this.productMapper = productMapper;
        this.productImageMapper = productImageMapper;
        this.categoryMapper = categoryMapper;
        this.favoriteMapper = favoriteMapper;
    }

    public Product getProductOrThrow(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在或已删除");
        }
        return product;
    }

    /** 首页商品列表：关键词、分类、价格区间、排序、分页 */
    public PageResult<ProductVO> search(String keyword, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                       String sort, Integer page, Integer size) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "最低价格不能大于最高价格");
        }
        if (sort != null && !sort.isBlank() && !SORT_TYPES.contains(sort)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "排序方式只支持：latest、priceAsc、priceDesc、hot");
        }
        IPage<ProductVO> result = productMapper.searchProducts(Pagination.of(page, size), trim(keyword), categoryId,
                minPrice, maxPrice, null, null, true, sort);
        finish(result.getRecords());
        return PageResult.of(result, v -> v);
    }

    /** 我发布的商品（含已下架、交易中、已售出） */
    public PageResult<ProductVO> mine(Long sellerId, String status, Integer page, Integer size) {
        checkStatusParam(status);
        IPage<ProductVO> result = productMapper.searchProducts(Pagination.of(page, size), null, null,
                null, null, sellerId, trim(status), false, "latest");
        finish(result.getRecords());
        return PageResult.of(result, v -> v);
    }

    /** 某个用户主页展示的在售商品（匿名可访问） */
    public PageResult<ProductVO> onSaleOfUser(Long sellerId, Integer page, Integer size) {
        IPage<ProductVO> result = productMapper.searchProducts(Pagination.of(page, size), null, null,
                null, null, sellerId, null, true, "latest");
        finish(result.getRecords());
        return PageResult.of(result, v -> v);
    }

    /** 管理端商品列表：可查全部状态 */
    public PageResult<ProductVO> adminPage(String keyword, String status, Integer page, Integer size) {
        checkStatusParam(status);
        IPage<ProductVO> result = productMapper.searchProducts(Pagination.of(page, size), trim(keyword), null,
                null, null, null, trim(status), false, "latest");
        finish(result.getRecords());
        return PageResult.of(result, v -> v);
    }

    /** 商品详情：图片列表、是否已收藏、浏览量 +1 */
    @Transactional
    public ProductVO detail(Long id) {
        ProductVO vo = productMapper.selectProductVOById(id);
        if (vo == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在或已删除");
        }
        List<String> images = productImageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getProductId, id)
                        .orderByAsc(ProductImage::getSort)
                        .orderByAsc(ProductImage::getId))
                .stream().map(ProductImage::getUrl).toList();
        vo.setImages(images.isEmpty() && vo.getCoverImage() != null ? List.of(vo.getCoverImage()) : images);
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, id)
                .setSql("view_count = view_count + 1"));
        finish(List.of(vo));
        return vo;
    }

    /** 发布商品 */
    @Transactional
    public Long create(Long sellerId, ProductSaveRequest request) {
        checkCategory(request.categoryId());
        Product product = new Product();
        product.setSellerId(sellerId);
        applyRequest(product, request);
        product.setStatus(ProductStatus.ON_SALE.name());
        product.setViewCount(0);
        product.setFavoriteCount(0);
        product.setDeleted(0);
        productMapper.insert(product);
        saveImages(product.getId(), request.imageUrls());
        return product.getId();
    }

    /** 编辑商品：仅本人可编辑，且只有在售/已下架商品可编辑 */
    @Transactional
    public void update(Long id, Long userId, ProductSaveRequest request) {
        Product product = getProductOrThrow(id);
        if (!product.getSellerId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能编辑自己发布的商品");
        }
        if (!List.of(ProductStatus.ON_SALE.name(), ProductStatus.OFF_SHELF.name()).contains(product.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "交易中或已售出的商品不能编辑");
        }
        checkCategory(request.categoryId());
        applyRequest(product, request);
        productMapper.updateById(product);
        productImageMapper.delete(new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, id));
        saveImages(id, request.imageUrls());
    }

    /** 上架 / 下架 */
    @Transactional
    public void updateStatus(Long id, Long userId, String targetStatus) {
        Product product = getProductOrThrow(id);
        if (!product.getSellerId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能操作自己发布的商品");
        }
        if (ProductStatus.ON_SALE.name().equals(targetStatus)) {
            if (!ProductStatus.OFF_SHELF.name().equals(product.getStatus())) {
                throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "只有已下架的商品才能重新上架");
            }
        } else if (!ProductStatus.ON_SALE.name().equals(product.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "只有在售商品才能下架");
        }
        Product update = new Product();
        update.setId(id);
        update.setStatus(targetStatus);
        productMapper.updateById(update);
    }

    /** 删除商品（逻辑删除） */
    @Transactional
    public void delete(Long id, Long userId) {
        Product product = getProductOrThrow(id);
        if (!product.getSellerId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能删除自己发布的商品");
        }
        if (ProductStatus.LOCKED.name().equals(product.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "商品正在交易中，请先处理订单");
        }
        productMapper.deleteById(id);
    }

    /** 管理端：强制下架违规商品 */
    @Transactional
    public void offShelf(Long id) {
        Product product = getProductOrThrow(id);
        if (!List.of(ProductStatus.ON_SALE.name(), ProductStatus.OFF_SHELF.name()).contains(product.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "交易中或已售出的商品不能强制下架");
        }
        Product update = new Product();
        update.setId(id);
        update.setStatus(ProductStatus.OFF_SHELF.name());
        productMapper.updateById(update);
    }

    private void applyRequest(Product product, ProductSaveRequest request) {
        product.setTitle(request.title().trim());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setOriginalPrice(request.originalPrice());
        product.setCategoryId(request.categoryId());
        product.setConditionLevel(request.conditionLevel());
        product.setTradePlace(request.tradePlace());
    }

    private void saveImages(Long productId, List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return;
        }
        int sort = 0;
        for (String url : urls) {
            if (url == null || url.isBlank()) {
                continue;
            }
            ProductImage image = new ProductImage();
            image.setProductId(productId);
            image.setUrl(url.trim());
            image.setSort(sort++);
            productImageMapper.insert(image);
        }
    }

    private void checkCategory(Long categoryId) {
        if (categoryMapper.selectById(categoryId) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "商品分类不存在");
        }
    }

    private void checkStatusParam(String status) {
        if (status != null && !status.isBlank() && !ProductStatus.isValid(status)) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "商品状态只能为：ON_SALE、LOCKED、SOLD、OFF_SHELF");
        }
    }

    /** 补充中文状态名与"是否已收藏"，供列表和详情复用 */
    private void finish(List<ProductVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        records.forEach(vo -> vo.setStatusLabel(Labels.product(vo.getStatus())));
        LoginUser loginUser = UserContext.get();
        if (loginUser == null) {
            return;
        }
        List<Long> ids = records.stream().map(ProductVO::getId).toList();
        Set<Long> favoritedIds = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, loginUser.userId())
                        .in(Favorite::getProductId, ids))
                .stream().map(Favorite::getProductId).collect(Collectors.toSet());
        records.forEach(vo -> vo.setFavorited(favoritedIds.contains(vo.getId())));
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
