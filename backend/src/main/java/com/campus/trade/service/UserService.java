package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.trade.common.BusinessException;
import com.campus.trade.common.PageResult;
import com.campus.trade.common.Pagination;
import com.campus.trade.common.ResultCode;
import com.campus.trade.constant.ProductStatus;
import com.campus.trade.constant.UserRole;
import com.campus.trade.dto.UpdateProfileRequest;
import com.campus.trade.entity.Product;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.vo.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** 用户服务：个人资料、用户公开主页、管理端用户管理 */
@Service
public class UserService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserMapper userMapper;
    private final ProductMapper productMapper;

    public UserService(UserMapper userMapper, ProductMapper productMapper) {
        this.userMapper = userMapper;
        this.productMapper = productMapper;
    }

    public User getUserOrThrow(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    /** 公开资料：手机号、邮箱只对本人和管理员可见 */
    public UserVO getPublicProfile(Long id, Long currentUserId, boolean admin) {
        User user = getUserOrThrow(id);
        boolean full = admin || (currentUserId != null && currentUserId.equals(id));
        return toVO(user, full);
    }

    @Transactional
    public UserVO updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getUserOrThrow(userId);
        if (request.nickname() != null && !request.nickname().isBlank()) {
            user.setNickname(request.nickname().trim());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone().trim());
        }
        if (request.email() != null) {
            user.setEmail(request.email().trim());
        }
        if (request.avatar() != null) {
            user.setAvatar(request.avatar().trim());
        }
        if (request.school() != null) {
            user.setSchool(request.school().trim());
        }
        userMapper.updateById(user);
        return toVO(user, true);
    }

    /** 管理端：用户列表（支持账号/昵称模糊搜索） */
    public PageResult<UserVO> adminPage(String keyword, Integer page, Integer size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(User::getUsername, keyword)
                        .or()
                        .like(User::getNickname, keyword))
                .orderByAsc(User::getId);
        return PageResult.of(userMapper.selectPage(Pagination.of(page, size), wrapper), u -> toVO(u, true));
    }

    /** 管理端：启用/禁用用户 */
    @Transactional
    public UserVO updateStatus(Long operatorId, Long targetId, Integer status) {
        User target = getUserOrThrow(targetId);
        if (targetId.equals(operatorId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "不能修改自己的账号状态");
        }
        if (UserRole.ADMIN.equals(target.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "不能修改管理员的账号状态");
        }
        User update = new User();
        update.setId(targetId);
        update.setStatus(status);
        userMapper.updateById(update);
        target.setStatus(status);
        return toVO(target, true);
    }

    public UserVO toVO(User user, boolean full) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setSchool(user.getSchool());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        if (user.getCreatedAt() != null) {
            vo.setCreatedAt(user.getCreatedAt().format(DATE_TIME));
        }
        if (full) {
            vo.setPhone(user.getPhone());
            vo.setEmail(user.getEmail());
        } else {
            // 公开主页对手机号做脱敏，邮箱不返回
            vo.setPhone(maskPhone(user.getPhone()));
        }
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getSellerId, user.getId())
                .eq(Product::getStatus, ProductStatus.ON_SALE.name())
                .select(Product::getId));
        vo.setOnSaleCount((long) products.size());
        return vo;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
