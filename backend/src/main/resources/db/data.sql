-- ============================================================
-- 校园二手交易平台 初始数据（可重复执行：INSERT IGNORE + 固定主键）
-- 测试账号密码统一为 123456（BCrypt 加密存储）
-- ============================================================

-- 测试账号：admin 管理员 / seller01、seller02 卖家 / buyer01 买家
INSERT IGNORE INTO `user` (`id`, `username`, `password`, `nickname`, `phone`, `email`, `school`, `role`, `status`) VALUES
  (1, 'admin',    '$2a$10$e97QMMTkiGpQlopsxAHyNeshZST1Z808OWA5jKe.myZYGvLt3mDfK', '平台管理员', '13900000001', 'admin@campus.com',   '南昌职业大学', 'ADMIN', 1),
  (2, 'seller01', '$2a$10$e97QMMTkiGpQlopsxAHyNeshZST1Z808OWA5jKe.myZYGvLt3mDfK', '林晓',       '13900000002', 'linxiao@campus.com', '南昌职业大学', 'USER',  1),
  (3, 'seller02', '$2a$10$e97QMMTkiGpQlopsxAHyNeshZST1Z808OWA5jKe.myZYGvLt3mDfK', '陈默',       '13900000003', 'chenmo@campus.com',  '南昌职业大学', 'USER',  1),
  (4, 'buyer01',  '$2a$10$e97QMMTkiGpQlopsxAHyNeshZST1Z808OWA5jKe.myZYGvLt3mDfK', '苏晴',       '13900000004', 'suqing@campus.com',  '南昌职业大学', 'USER',  1);

-- 商品分类
INSERT IGNORE INTO `category` (`id`, `name`, `sort`) VALUES
  (1, '教材书籍', 1),
  (2, '数码电子', 2),
  (3, '生活用品', 3),
  (4, '服饰鞋包', 4),
  (5, '运动户外', 5),
  (6, '其他闲置', 6);

-- 初始商品：在售 16 件 + 已下架 1 件 + 已售出 1 件 + 交易中 1 件 + 在售 1 件
INSERT IGNORE INTO `product`
  (`id`, `seller_id`, `category_id`, `title`, `description`, `price`, `original_price`, `condition_level`, `trade_place`, `status`, `view_count`, `favorite_count`) VALUES
  (1,  2, 1, '九成新《软件测试技术》教材', '只翻过前几章，无笔记无划线，配套习题册一起送。', 18.00, 45.00, '九成新', '三号宿舍楼下', 'ON_SALE', 36, 2),
  (2,  2, 1, '《Java 程序设计》第 3 版', '课程考完就闲置了，书角轻微磨损，内页干净。', 15.50, 39.80, '八成新', '三号宿舍楼下', 'ON_SALE', 21, 1),
  (3,  2, 2, '罗技 M170 无线鼠标', '大一买的，换了新的所以出掉，接收器和电池都在。', 35.00, 79.00, '九成新', '图书馆一楼', 'ON_SALE', 58, 3),
  (4,  2, 2, '金士顿 32G U 盘', '读写正常，外壳有一点点划痕，不影响使用。', 22.00, 45.00, '八成新', '图书馆一楼', 'ON_SALE', 17, 0),
  (5,  2, 3, '宿舍小台灯（三档亮度）', '毕业学长留下的，带 USB 接口，可充电。', 25.00, 59.00, '八成新', '三号宿舍楼下', 'ON_SALE', 42, 1),
  (6,  2, 3, '折叠晾衣架', '宿舍阳台用，折叠后不占地方，无损坏。', 12.00, 29.90, '八成新', '五号宿舍楼下', 'ON_SALE', 9,  0),
  (7,  2, 4, '耐克运动短裤（L 码）', '买大了只穿过两次，已洗干净。', 45.00, 129.00, '九成新', '体育馆门口', 'ON_SALE', 31, 2),
  (8,  2, 5, '迪卡侬羽毛球拍一对', '含拍套，线还很好，适合入门。', 68.00, 158.00, '八成新', '体育馆门口', 'ON_SALE', 25, 1),
  (9,  3, 1, '《计算机网络》谢希仁 第 7 版', '考研复习用过，重点章节有铅笔笔记，可擦。', 20.00, 49.00, '八成新', '二号教学楼', 'ON_SALE', 44, 4),
  (10, 3, 1, '英语四级真题（近 5 年）', '写过一部分，答案单独装订，整套齐全。', 10.00, 32.00, '七成新及以下', '二号教学楼', 'ON_SALE', 29, 1),
  (11, 3, 2, '小米移动电源 10000mAh', '容量正常，支持快充，附带原装线。', 55.00, 109.00, '九成新', '一号食堂门口', 'ON_SALE', 63, 5),
  (12, 3, 2, '机械键盘 87 键（青轴）', '手感很好，宿舍怕吵所以出掉，键帽齐全。', 120.00, 299.00, '八成新', '一号食堂门口', 'ON_SALE', 71, 6),
  (13, 3, 3, '宿舍收纳箱（大号两个）', '搬宿舍剩下的，无破损，可小刀。', 30.00, 69.00, '八成新', '七号宿舍楼下', 'ON_SALE', 14, 0),
  (14, 3, 3, '电热水壶 1.5L', '宿舍可用功率，烧水正常，已清洁。', 28.00, 79.00, '八成新', '七号宿舍楼下', 'ON_SALE', 33, 2),
  (15, 3, 4, '帆布双肩包', '容量大能装电脑，只有一点点脏，可水洗。', 40.00, 119.00, '八成新', '图书馆一楼', 'ON_SALE', 19, 0),
  (16, 3, 5, '瑜伽垫（加厚 8mm）', '用过几次，已清洁消毒，送收纳绑带。', 32.00, 89.00, '九成新', '体育馆门口', 'ON_SALE', 26, 1),
  (17, 2, 6, '宿舍小风扇（已下架示例）', '这件商品用于演示"已下架"状态。', 15.00, 39.00, '八成新', '三号宿舍楼下', 'OFF_SHELF', 8, 0),
  (18, 3, 2, '罗技 K380 蓝牙键盘（已售出示例）', '这件商品用于演示"已售出"状态。', 99.00, 199.00, '九成新', '图书馆一楼', 'SOLD', 48, 2),
  (19, 2, 1, '《数据结构》严蔚敏（交易中示例）', '有买家已下单，用于演示"交易中"状态与订单流程。', 26.00, 45.00, '八成新', '三号宿舍楼下', 'LOCKED', 30, 1),
  (20, 3, 6, '多肉植物两盆（带花盆）', '养得挺好，换校区带不走，带花盆一起送。', 18.00, 45.00, '全新', '七号宿舍楼下', 'ON_SALE', 12, 0);

-- 商品配图：使用 resources/seed-images 下的示例图（由 scripts/generate_placeholder_images.py 生成，通过 /seed/** 访问）
INSERT IGNORE INTO `product_image` (`id`, `product_id`, `url`, `sort`) VALUES
  (1,  1,  '/seed/p01.png', 0),
  (2,  2,  '/seed/p02.png', 0),
  (3,  3,  '/seed/p03.png', 0),
  (4,  4,  '/seed/p04.png', 0),
  (5,  5,  '/seed/p05.png', 0),
  (6,  6,  '/seed/p06.png', 0),
  (7,  7,  '/seed/p07.png', 0),
  (8,  8,  '/seed/p08.png', 0),
  (9,  9,  '/seed/p01.png', 0),
  (10, 10, '/seed/p02.png', 0),
  (11, 11, '/seed/p03.png', 0),
  (12, 12, '/seed/p04.png', 0),
  (13, 13, '/seed/p05.png', 0),
  (14, 14, '/seed/p06.png', 0),
  (15, 15, '/seed/p07.png', 0),
  (16, 16, '/seed/p08.png', 0),
  (17, 17, '/seed/p02.png', 0),
  (18, 18, '/seed/p03.png', 0),
  (19, 19, '/seed/p01.png', 0),
  (20, 20, '/seed/p06.png', 0);

-- 收藏数据：buyer01 收藏了 3 件商品
INSERT IGNORE INTO `favorite` (`id`, `user_id`, `product_id`) VALUES
  (1, 4, 1),
  (2, 4, 12),
  (3, 4, 20);

-- 订单数据：1 个待卖家确认（30 分钟后超时）+ 1 个已完成
INSERT IGNORE INTO `trade_order`
  (`id`, `order_no`, `product_id`, `buyer_id`, `seller_id`, `amount`, `status`, `remark`, `expire_at`, `created_at`, `confirmed_at`, `finished_at`) VALUES
  (1, 'C202609190000001001', 19, 4, 2, 26.00, 'PENDING_CONFIRM', '今晚 7 点在三号宿舍楼下交易可以吗？', DATE_ADD(NOW(), INTERVAL 30 MINUTE), NOW(), NULL, NULL),
  (2, 'C202609180000002002', 18, 4, 3, 99.00, 'COMPLETED', '明天下午图书馆一楼', DATE_ADD(NOW(), INTERVAL -1 DAY), DATE_ADD(NOW(), INTERVAL -1 DAY), DATE_ADD(NOW(), INTERVAL -1 DAY), DATE_ADD(NOW(), INTERVAL -23 HOUR));

-- 站内消息
INSERT IGNORE INTO `message` (`id`, `user_id`, `type`, `title`, `content`, `related_id`, `is_read`) VALUES
  (1, 2, 'ORDER',    '收到新订单', '买家【苏晴】拍下了你的商品《数据结构》严蔚敏（交易中示例），请在 30 分钟内确认，超时订单将自动关闭。', 1, 0),
  (2, 4, 'FAVORITE', '收藏成功',   '你收藏了商品《九成新《软件测试技术》教材》，降价时我们会提醒你。', 1, 1),
  (3, 3, 'ORDER',    '订单已完成', '买家【苏晴】已确认完成交易，商品已标记为已售出。', 2, 1),
  (4, 4, 'SYSTEM',   '欢迎使用校园二手交易平台', '这里是你的消息中心，下单、订单状态变化、商品被收藏等都会在这里提醒你。', NULL, 0);
