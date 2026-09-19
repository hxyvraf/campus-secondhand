package com.campus.trade.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/** 分页参数校验：page 从 1 开始，size 1-100，非法直接返回 400/40001 */
public final class Pagination {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 100;

    private Pagination() {
    }

    public static <T> Page<T> of(Integer page, Integer size) {
        int p = page == null ? DEFAULT_PAGE : page;
        int s = size == null ? DEFAULT_SIZE : size;
        if (p < 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "页码必须大于等于 1");
        }
        if (s < 1 || s > MAX_SIZE) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "每页条数必须在 1-100 之间");
        }
        return new Page<>(p, s);
    }
}
