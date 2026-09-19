package com.campus.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 商品图片 */
@Data
@TableName("product_image")
public class ProductImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    /** 相对路径，例如 /uploads/20260919/xxx.jpg */
    private String url;

    private Integer sort;
}
