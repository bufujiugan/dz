SET NAMES utf8mb4;
SET time_zone = '+08:00';

INSERT INTO `user`(
    id, store_id, openid, unionid, nickname, avatar, phone, status,
    create_time, update_time, deleted
) VALUES
(10001, 10001, 'mock-openid-10001', NULL, '林小满', '', '13800138001', 0, DATE_SUB(NOW(), INTERVAL 42 DAY), NOW(), 0),
(10002, 10001, 'mock-openid-10002', NULL, '周末来一杯', '', '13800138002', 0, DATE_SUB(NOW(), INTERVAL 31 DAY), NOW(), 0),
(10003, 10001, 'mock-openid-10003', NULL, '陈先生', '', '13800138003', 0, DATE_SUB(NOW(), INTERVAL 20 DAY), NOW(), 0),
(10004, 10001, 'mock-openid-10004', NULL, '晚风', '', '13800138004', 0, DATE_SUB(NOW(), INTERVAL 12 DAY), NOW(), 0),
(10005, 10001, 'mock-openid-10005', NULL, '小鹿同学', '', '13800138005', 0, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW(), 0),
(10006, 10001, 'mock-openid-10006', NULL, '暂停使用用户', '', '13800138006', 1, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW(), 0)
ON DUPLICATE KEY UPDATE
    store_id = VALUES(store_id),
    nickname = VALUES(nickname),
    phone = VALUES(phone),
    status = VALUES(status),
    deleted = 0,
    update_time = NOW();

INSERT INTO user_account(
    id, user_id, balance_fen, points, frozen_points, version,
    create_time, update_time, deleted
) VALUES
(10001, 10001, 28600, 1680, 0, 0, DATE_SUB(NOW(), INTERVAL 42 DAY), NOW(), 0),
(10002, 10002, 8800, 620, 100, 0, DATE_SUB(NOW(), INTERVAL 31 DAY), NOW(), 0),
(10003, 10003, 52000, 2360, 0, 0, DATE_SUB(NOW(), INTERVAL 20 DAY), NOW(), 0),
(10004, 10004, 15600, 890, 0, 0, DATE_SUB(NOW(), INTERVAL 12 DAY), NOW(), 0),
(10005, 10005, 3000, 260, 50, 0, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW(), 0),
(10006, 10006, 0, 0, 0, 0, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW(), 0)
ON DUPLICATE KEY UPDATE
    balance_fen = VALUES(balance_fen),
    points = VALUES(points),
    frozen_points = VALUES(frozen_points),
    deleted = 0,
    update_time = NOW();

INSERT INTO store(
    id, name, address, phone, status, create_time, update_time, deleted
) VALUES
(10001, 'DZ 餐酒馆·滨江店', '滨江路 88 号 A 座 1 层', '021-68886666', 1, DATE_SUB(NOW(), INTERVAL 180 DAY), NOW(), 0)
ON DUPLICATE KEY UPDATE
    id = id;

-- 历史门店是否对小程序可见由管理后台维护，不在启动脚本中强制改状态。

INSERT INTO category(
    id, store_id, name, sort, create_time, update_time, deleted
) VALUES
(10001, 10001, '精酿啤酒', 10, NOW(), NOW(), 0),
(10002, 10001, '经典鸡尾酒', 20, NOW(), NOW(), 0),
(10003, 10001, '佐酒小食', 30, NOW(), NOW(), 0),
(10004, 10001, '无酒精饮品', 40, NOW(), NOW(), 0),
(10005, 10001, '卡券售卖', 50, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    store_id = VALUES(store_id),
    name = VALUES(name),
    sort = VALUES(sort),
    deleted = 0,
    update_time = NOW();

INSERT INTO product(
    id, store_id, category_id, name, main_image, images, description,
    status, recommended, create_time, update_time, deleted
) VALUES
(10001, 10001, 10001, '海盐小麦精酿', '/uploads/products/sea-salt-wheat-beer.jpg', JSON_ARRAY('/uploads/products/sea-salt-wheat-beer.jpg'), '清爽小麦香气，带有轻柔海盐尾韵。', 1, 1, DATE_SUB(NOW(), INTERVAL 60 DAY), NOW(), 0),
(10002, 10001, 10001, '柑橘 IPA', '/uploads/products/citrus-ipa.jpg', JSON_ARRAY('/uploads/products/citrus-ipa.jpg'), '柑橘与热带水果香气，苦度平衡。', 1, 1, DATE_SUB(NOW(), INTERVAL 55 DAY), NOW(), 0),
(10003, 10001, 10002, '落日金汤力', '/uploads/products/sunset-gin-tonic.jpg', JSON_ARRAY('/uploads/products/sunset-gin-tonic.jpg'), '金酒、汤力水与葡萄柚的清新组合。', 1, 1, DATE_SUB(NOW(), INTERVAL 45 DAY), NOW(), 0),
(10004, 10001, 10002, '桂花威士忌酸', '/uploads/products/osmanthus-whisky-sour.jpg', JSON_ARRAY('/uploads/products/osmanthus-whisky-sour.jpg'), '威士忌酸加入桂花香气，酸甜顺滑。', 1, 0, DATE_SUB(NOW(), INTERVAL 40 DAY), NOW(), 0),
(10005, 10001, 10003, '黑椒牛肉粒', '/uploads/products/black-pepper-beef.jpg', JSON_ARRAY('/uploads/products/black-pepper-beef.jpg'), '现炒黑椒牛肉粒，适合搭配精酿。', 1, 1, DATE_SUB(NOW(), INTERVAL 35 DAY), NOW(), 0),
(10006, 10001, 10003, '松露薯条', '/uploads/products/truffle-fries.jpg', JSON_ARRAY('/uploads/products/truffle-fries.jpg'), '酥脆薯条搭配松露酱。', 1, 0, DATE_SUB(NOW(), INTERVAL 28 DAY), NOW(), 0),
(10007, 10001, 10004, '青提气泡饮', '/uploads/products/green-grape-soda.jpg', JSON_ARRAY('/uploads/products/green-grape-soda.jpg'), '青提、柠檬与气泡水，无酒精。', 1, 0, DATE_SUB(NOW(), INTERVAL 18 DAY), NOW(), 0),
(10008, 10001, 10003, '季节限定拼盘', '/mock/season-plate.jpg', JSON_ARRAY('/mock/season-plate.jpg'), '季节限定商品，当前暂时下架。', 0, 0, DATE_SUB(NOW(), INTERVAL 10 DAY), NOW(), 0),
(10009, 10001, 10005, '柑橘 IPA 兑换券', '/uploads/products/citrus-ipa.jpg', JSON_ARRAY('/uploads/products/citrus-ipa.jpg'), '购买后自动存入会员卡券，可提交核销申请。', 1, 1, DATE_SUB(NOW(), INTERVAL 8 DAY), NOW(), 0),
(10010, 10001, 10005, '双人微醺套餐券', '/uploads/products/sunset-gin-tonic.jpg', JSON_ARRAY('/uploads/products/sunset-gin-tonic.jpg'), '2杯特调加精选小食，适合周末到店核销。', 1, 1, DATE_SUB(NOW(), INTERVAL 7 DAY), NOW(), 0),
(10011, 10001, 10005, '100元代金券', '/uploads/products/black-pepper-beef.jpg', JSON_ARRAY('/uploads/products/black-pepper-beef.jpg'), '满300元可用，到店核销可抵扣。', 1, 1, DATE_SUB(NOW(), INTERVAL 6 DAY), NOW(), 0),
(10012, 10001, 10005, '招牌特调兑换券', '/uploads/products/osmanthus-whisky-sour.jpg', JSON_ARRAY('/uploads/products/osmanthus-whisky-sour.jpg'), '可兑换单杯指定酒水，周末可用。', 1, 1, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW(), 0)
ON DUPLICATE KEY UPDATE
    store_id = VALUES(store_id),
    category_id = VALUES(category_id),
    name = VALUES(name),
    main_image = VALUES(main_image),
    images = VALUES(images),
    description = VALUES(description),
    status = VALUES(status),
    recommended = VALUES(recommended),
    deleted = 0,
    update_time = NOW();

INSERT INTO product_sku(
    id, product_id, spec_name, price_fen, stock, sales,
    create_time, update_time, deleted
) VALUES
(10001, 10001, '500ml', 3200, 86, 124, NOW(), NOW(), 0),
(10002, 10002, '500ml', 3600, 62, 98, NOW(), NOW(), 0),
(10003, 10003, '标准杯', 4800, 999, 76, NOW(), NOW(), 0),
(10004, 10004, '标准杯', 5200, 999, 45, NOW(), NOW(), 0),
(10005, 10005, '标准份', 5800, 38, 67, NOW(), NOW(), 0),
(10006, 10006, '标准份', 3200, 55, 83, NOW(), NOW(), 0),
(10007, 10007, '标准杯', 2600, 999, 39, NOW(), NOW(), 0),
(10008, 10008, '双人份', 8800, 0, 12, NOW(), NOW(), 0),
(10009, 10009, '1 张', 3600, 999, 18, NOW(), NOW(), 0),
(10010, 10010, '套餐券 1 张', 16800, 999, 9, NOW(), NOW(), 0),
(10011, 10011, '代金券 1 张', 8800, 999, 21, NOW(), NOW(), 0),
(10012, 10012, '兑换券 1 张', 3900, 999, 15, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    product_id = VALUES(product_id),
    spec_name = VALUES(spec_name),
    price_fen = VALUES(price_fen),
    stock = VALUES(stock),
    sales = VALUES(sales),
    deleted = 0,
    update_time = NOW();

INSERT INTO coupon_category(
    id, store_id, name, sort, status, create_time, update_time, deleted
) VALUES
(10001, 10001, '套餐券', 10, 1, NOW(), NOW(), 0),
(10002, 10001, '酒水券', 20, 1, NOW(), NOW(), 0),
(10003, 10001, '代金券', 30, 1, NOW(), NOW(), 0),
(10004, 10001, '储值券', 40, 1, NOW(), NOW(), 0),
(10005, 10001, '活动券', 50, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    store_id = VALUES(store_id),
    name = VALUES(name),
    sort = VALUES(sort),
    status = VALUES(status),
    deleted = 0,
    update_time = NOW();

INSERT INTO `order`(
    id, order_no, user_id, store_id, total_fen, pay_type, status,
    pay_time, cancel_time, remark, prepay_requested,
    create_time, update_time, deleted
) VALUES
(10001, 'DZMOCK202606100001', 10001, 10001, 6800, 'WECHAT', 'CREATED', NULL, NULL, '靠窗位置', 1, DATE_SUB(NOW(), INTERVAL 8 MINUTE), NOW(), 0),
(10002, 'DZMOCK202606100002', 10002, 10001, 10600, 'WECHAT', 'PAID', DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL, '少冰', 1, DATE_SUB(NOW(), INTERVAL 130 MINUTE), NOW(), 0),
(10003, 'DZMOCK202606100003', 10003, 10001, 9000, 'BALANCE', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, '', 0, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW(), 0),
(10004, 'DZMOCK202606100004', 10004, 10001, 3200, 'WECHAT', 'CANCELLED', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), '用户取消', 0, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW(), 0),
(10005, 'DZMOCK202606100005', 10001, 10001, 8400, 'WECHAT', 'REFUNDED', DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, '已完成退款', 1, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW(), 0),
(10006, 'DZMOCK202606100006', 10005, 10001, 5800, 'POINTS', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 4 DAY), NULL, '积分支付演示', 0, DATE_SUB(NOW(), INTERVAL 4 DAY), NOW(), 0),
(10007, 'DZMOCK202606100007', 10003, 10001, 10000, 'WECHAT', 'REFUNDING', DATE_SUB(NOW(), INTERVAL 5 DAY), NULL, '退款处理中', 1, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW(), 0)
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    store_id = VALUES(store_id),
    total_fen = VALUES(total_fen),
    pay_type = VALUES(pay_type),
    status = VALUES(status),
    pay_time = VALUES(pay_time),
    cancel_time = VALUES(cancel_time),
    remark = VALUES(remark),
    prepay_requested = VALUES(prepay_requested),
    create_time = VALUES(create_time),
    deleted = 0,
    update_time = NOW();

INSERT INTO order_item(
    id, order_id, sku_id, product_name, spec_name, price_fen, quantity,
    create_time, update_time, deleted
) VALUES
(10001, 10001, 10001, '海盐小麦精酿', '500ml', 3200, 1, NOW(), NOW(), 0),
(10002, 10001, 10006, '松露薯条', '标准份', 3200, 1, NOW(), NOW(), 0),
(10003, 10002, 10003, '落日金汤力', '标准杯', 4800, 1, NOW(), NOW(), 0),
(10004, 10002, 10005, '黑椒牛肉粒', '标准份', 5800, 1, NOW(), NOW(), 0),
(10005, 10003, 10002, '柑橘 IPA', '500ml', 3600, 1, NOW(), NOW(), 0),
(10006, 10003, 10004, '桂花威士忌酸', '标准杯', 5200, 1, NOW(), NOW(), 0),
(10007, 10004, 10001, '海盐小麦精酿', '500ml', 3200, 1, NOW(), NOW(), 0),
(10008, 10005, 10004, '桂花威士忌酸', '标准杯', 5200, 1, NOW(), NOW(), 0),
(10009, 10005, 10006, '松露薯条', '标准份', 3200, 1, NOW(), NOW(), 0),
(10010, 10006, 10005, '黑椒牛肉粒', '标准份', 5800, 1, NOW(), NOW(), 0),
(10011, 10007, 10003, '落日金汤力', '标准杯', 4800, 1, NOW(), NOW(), 0),
(10012, 10007, 10004, '桂花威士忌酸', '标准杯', 5200, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    order_id = VALUES(order_id),
    sku_id = VALUES(sku_id),
    product_name = VALUES(product_name),
    spec_name = VALUES(spec_name),
    price_fen = VALUES(price_fen),
    quantity = VALUES(quantity),
    deleted = 0,
    update_time = NOW();

INSERT INTO payment_record(
    id, order_no, transaction_id, amount_fen, openid, trade_state,
    notify_raw, verify_result, query_result, notify_count,
    create_time, update_time, deleted
) VALUES
(10001, 'DZMOCK202606100002', 'WXMOCK202606100002', 10600, 'mock-openid-10002', 'SUCCESS', '{"source":"mock","state":"SUCCESS"}', 'PASS', '{"trade_state":"SUCCESS"}', 1, DATE_SUB(NOW(), INTERVAL 2 HOUR), NOW(), 0),
(10002, 'DZMOCK202606100004', 'WXMOCK202606100004', 3200, 'mock-openid-10004', 'CLOSED', '{"source":"mock","state":"CLOSED"}', 'PASS', '{"trade_state":"CLOSED"}', 1, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW(), 0),
(10003, 'DZMOCK202606100005', 'WXMOCK202606100005', 8400, 'mock-openid-10001', 'REFUND', '{"source":"mock","state":"REFUND"}', 'PASS', '{"trade_state":"REFUND"}', 2, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW(), 0),
(10004, 'DZMOCK202606100007', 'WXMOCK202606100007', 10000, 'mock-openid-10003', 'SUCCESS', '{"source":"mock","state":"SUCCESS"}', 'PASS', '{"trade_state":"SUCCESS"}', 1, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW(), 0)
ON DUPLICATE KEY UPDATE
    transaction_id = VALUES(transaction_id),
    amount_fen = VALUES(amount_fen),
    openid = VALUES(openid),
    trade_state = VALUES(trade_state),
    notify_raw = VALUES(notify_raw),
    verify_result = VALUES(verify_result),
    query_result = VALUES(query_result),
    notify_count = VALUES(notify_count),
    deleted = 0,
    update_time = NOW();

INSERT INTO recharge_tier(
    id, pay_fen, bonus_fen, label, status, sort,
    create_time, update_time, deleted
) VALUES
(10001, 10000, 0, '充 100 元', 1, 10, NOW(), NOW(), 0),
(10002, 30000, 3000, '充 300 元赠 30 元', 1, 20, NOW(), NOW(), 0),
(10003, 50000, 8000, '充 500 元赠 80 元', 1, 30, NOW(), NOW(), 0),
(10004, 100000, 20000, '充 1000 元赠 200 元', 1, 40, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    pay_fen = VALUES(pay_fen),
    bonus_fen = VALUES(bonus_fen),
    label = VALUES(label),
    status = VALUES(status),
    sort = VALUES(sort),
    deleted = 0,
    update_time = NOW();

INSERT INTO recharge_order(
    id, recharge_no, user_id, tier_id, pay_fen, bonus_fen, status,
    credited, prepay_requested, create_time, update_time, deleted
) VALUES
(10001, 'RCMOCK202606100001', 10001, 10002, 30000, 3000, 'PAID', 1, 1, DATE_SUB(NOW(), INTERVAL 6 DAY), NOW(), 0),
(10002, 'RCMOCK202606100002', 10002, 10001, 10000, 0, 'CREATED', 0, 1, DATE_SUB(NOW(), INTERVAL 3 HOUR), NOW(), 0),
(10003, 'RCMOCK202606100003', 10003, 10003, 50000, 8000, 'PAID', 1, 1, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW(), 0),
(10004, 'RCMOCK202606100004', 10004, 10001, 10000, 0, 'PAID', 0, 1, DATE_SUB(NOW(), INTERVAL 1 HOUR), NOW(), 0)
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    tier_id = VALUES(tier_id),
    pay_fen = VALUES(pay_fen),
    bonus_fen = VALUES(bonus_fen),
    status = VALUES(status),
    credited = VALUES(credited),
    prepay_requested = VALUES(prepay_requested),
    create_time = VALUES(create_time),
    deleted = 0,
    update_time = NOW();

INSERT INTO points_request(
    id, type, user_id, points, remark, voucher_images, status,
    auditor_id, audit_remark, audit_time, before_points, after_points,
    create_time, update_time, deleted
) VALUES
(10001, 'DEPOSIT', 10001, 200, '线下活动积分补录', JSON_ARRAY('/mock/voucher-1.jpg'), 'PENDING', NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 3 HOUR), NOW(), 0),
(10002, 'DEPOSIT', 10002, 100, '生日活动积分', JSON_ARRAY('/mock/voucher-2.jpg'), 'PENDING', NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW(), 0),
(10003, 'WITHDRAW', 10003, 50, '积分兑换核销', JSON_ARRAY(), 'PENDING', NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW(), 0),
(10004, 'DEPOSIT', 10004, 300, '品牌联名活动', JSON_ARRAY('/mock/voucher-4.jpg'), 'APPROVED', 1, '凭证有效，审核通过', DATE_SUB(NOW(), INTERVAL 2 DAY), 590, 890, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW(), 0),
(10005, 'DEPOSIT', 10005, 500, '消费凭证补录', JSON_ARRAY('/mock/voucher-5.jpg'), 'REJECTED', 1, '凭证信息不完整', DATE_SUB(NOW(), INTERVAL 1 DAY), 260, 260, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW(), 0)
ON DUPLICATE KEY UPDATE
    type = VALUES(type),
    user_id = VALUES(user_id),
    points = VALUES(points),
    remark = VALUES(remark),
    voucher_images = VALUES(voucher_images),
    status = VALUES(status),
    auditor_id = VALUES(auditor_id),
    audit_remark = VALUES(audit_remark),
    audit_time = VALUES(audit_time),
    before_points = VALUES(before_points),
    after_points = VALUES(after_points),
    create_time = VALUES(create_time),
    deleted = 0,
    update_time = NOW();

INSERT INTO account_log(
    id, user_id, asset_type, change_type, change_value,
    before_value, after_value, biz_no, operator, remark,
    create_time, update_time, deleted
) VALUES
(10001, 10001, 'BALANCE', 'RECHARGE', 33000, 0, 33000, 'RCMOCK202606100001', 'WECHAT_MOCK', '充值到账', DATE_SUB(NOW(), INTERVAL 6 DAY), NOW(), 0),
(10002, 10001, 'BALANCE', 'PAY_BALANCE', -4400, 33000, 28600, 'DZMOCK-ASSET-10001', 'USER:10001', '余额支付', DATE_SUB(NOW(), INTERVAL 5 DAY), NOW(), 0),
(10003, 10002, 'POINTS', 'POINTS_ADD', 100, 520, 620, 'POINTSMOCK10002', 'ADMIN:1', '活动积分', DATE_SUB(NOW(), INTERVAL 4 DAY), NOW(), 0),
(10004, 10003, 'BALANCE', 'RECHARGE', 58000, 0, 58000, 'RCMOCK202606100003', 'WECHAT_MOCK', '充值及赠送到账', DATE_SUB(NOW(), INTERVAL 2 DAY), NOW(), 0),
(10005, 10003, 'BALANCE', 'PAY_BALANCE', -6000, 58000, 52000, 'DZMOCK-ASSET-10003', 'USER:10003', '余额消费', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW(), 0),
(10006, 10004, 'POINTS', 'POINTS_ADD', 300, 590, 890, 'POINTSMOCK10004', 'ADMIN:1', '审核通过', DATE_SUB(NOW(), INTERVAL 2 DAY), NOW(), 0),
(10007, 10005, 'POINTS', 'POINTS_FREEZE', -50, 310, 260, 'POINTSMOCK10005', 'SYSTEM', '兑换冻结', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW(), 0)
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    asset_type = VALUES(asset_type),
    change_type = VALUES(change_type),
    change_value = VALUES(change_value),
    before_value = VALUES(before_value),
    after_value = VALUES(after_value),
    biz_no = VALUES(biz_no),
    operator = VALUES(operator),
    remark = VALUES(remark),
    create_time = VALUES(create_time),
    deleted = 0,
    update_time = NOW();

INSERT INTO operation_log(
    id, admin_id, module, action, params_digest, ip, cost_ms,
    create_time, update_time, deleted
) VALUES
(10001, 1, 'PRODUCT', 'CREATE', '{"productId":10001,"source":"mock"}', '127.0.0.1', 36, DATE_SUB(NOW(), INTERVAL 7 DAY), NOW(), 0),
(10002, 1, 'PRODUCT', 'CHANGE_STATUS', '{"productId":10008,"status":0}', '127.0.0.1', 18, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW(), 0),
(10003, 1, 'ORDER', 'COMPLETE', '{"orderNo":"DZMOCK202606100003"}', '127.0.0.1', 42, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW(), 0),
(10004, 1, 'POINTS', 'AUDIT', '{"requestId":10004,"approve":true}', '127.0.0.1', 51, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW(), 0),
(10005, 1, 'POINTS', 'AUDIT', '{"requestId":10005,"approve":false}', '127.0.0.1', 47, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW(), 0),
(10006, 1, 'RECHARGE', 'MANUAL_CREDIT', '{"rechargeNo":"RCMOCK202606100001"}', '127.0.0.1', 63, DATE_SUB(NOW(), INTERVAL 6 DAY), NOW(), 0),
(10007, 1, 'HOME', 'UPDATE_ANNOUNCEMENT', '{"storeId":10001}', '127.0.0.1', 21, DATE_SUB(NOW(), INTERVAL 4 DAY), NOW(), 0),
(10008, 1, 'RECONCILE', 'DAILY', '{"date":"2026-06-09","mode":"MOCK"}', '127.0.0.1', 15, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW(), 0)
ON DUPLICATE KEY UPDATE
    admin_id = VALUES(admin_id),
    module = VALUES(module),
    action = VALUES(action),
    params_digest = VALUES(params_digest),
    ip = VALUES(ip),
    cost_ms = VALUES(cost_ms),
    create_time = VALUES(create_time),
    deleted = 0,
    update_time = NOW();

INSERT INTO home_content(
    id, store_id, announcement, create_time, update_time, deleted
) VALUES
(10001, 10001, '欢迎来到 DZ 餐酒馆，当前为 Mock 演示环境，所有业务数据均可用于后台功能体验。', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    announcement = VALUES(announcement),
    deleted = 0,
    update_time = NOW();

INSERT INTO activity(
    id, store_id, title, image_url, sort, status, create_time, update_time, deleted
) VALUES
(10001, 10001, '周三微醺夜', '/uploads/products/sunset-gin-tonic.jpg', 30, 1, NOW(), NOW(), 0),
(10002, 10001, '精酿第二杯半价', '/uploads/products/citrus-ipa.jpg', 20, 1, NOW(), NOW(), 0),
(10003, 10001, '会员专属活动预告', '/uploads/products/sea-salt-wheat-beer.jpg', 10, 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    store_id = VALUES(store_id),
    title = VALUES(title),
    image_url = VALUES(image_url),
    sort = VALUES(sort),
    status = VALUES(status),
    deleted = 0,
    update_time = NOW();

INSERT INTO coupon_template(
    id, store_id, category_id, name, image_url, description, sale_product_id,
    purchase_valid_days, gift_valid_days, status,
    create_time, update_time, deleted
) VALUES
(10001, 10001, 10002, '柑橘 IPA 兑换券', '/uploads/products/citrus-ipa.jpg',
 '可兑换柑橘 IPA 一杯，核销申请经管理员确认后生效。', 10009, 180, 30, 1,
 NOW(), NOW(), 0),
(10002, 10001, 10005, '会员赠饮券', '/uploads/products/sea-salt-wheat-beer.jpg',
 '后台会员活动赠送券。', NULL, 180, 30, 1,
 NOW(), NOW(), 0),
(10003, 10001, 10001, '双人微醺套餐券', '/uploads/products/sunset-gin-tonic.jpg',
 '2杯特调加精选小食，周末可用，需要到店核销。', 10010, 120, 30, 1,
 NOW(), NOW(), 0),
(10004, 10001, 10003, '100元代金券', '/uploads/products/black-pepper-beef.jpg',
 '满300元可用，可与部分活动叠加，到店核销。', 10011, 120, 30, 1,
 NOW(), NOW(), 0),
(10005, 10001, 10002, '招牌特调兑换券', '/uploads/products/osmanthus-whisky-sour.jpg',
 '单杯指定酒水兑换券，适合今晚尝鲜。', 10012, 90, 30, 1,
 NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    store_id = VALUES(store_id),
    category_id = VALUES(category_id),
    name = VALUES(name),
    image_url = VALUES(image_url),
    description = VALUES(description),
    sale_product_id = VALUES(sale_product_id),
    purchase_valid_days = VALUES(purchase_valid_days),
    gift_valid_days = VALUES(gift_valid_days),
    status = VALUES(status),
    deleted = 0,
    update_time = NOW();

INSERT INTO user_coupon(
    id, coupon_no, template_id, user_id, store_id, coupon_name, image_url,
    source_type, source_no, status, expire_time, used_time,
    create_time, update_time, deleted
) VALUES
(10001, 'CPMOCK10001', 10002, 10001, 10001, '会员赠饮券',
 '/uploads/products/sea-salt-wheat-beer.jpg', 'GIFT', 'MOCK_GIFT_10001',
 'UNUSED', DATE_ADD(NOW(), INTERVAL 25 DAY), NULL, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW(), 0),
(10002, 'CPMOCK10002', 10001, 10001, 10001, '柑橘 IPA 兑换券',
 '/uploads/products/citrus-ipa.jpg', 'PURCHASE', 'MOCK_PURCHASE_10001',
 'REDEEM_PENDING', DATE_ADD(NOW(), INTERVAL 120 DAY), NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW(), 0),
(10003, 'CPMOCK10003', 10002, 10002, 10001, '会员赠饮券',
 '/uploads/products/sea-salt-wheat-beer.jpg', 'GIFT', 'MOCK_GIFT_10002',
 'USED', DATE_ADD(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY),
 DATE_SUB(NOW(), INTERVAL 20 DAY), NOW(), 0),
(10004, 'CPMOCKDEMO01', 10002, 1, 10001, '会员赠饮券',
 '/uploads/products/sea-salt-wheat-beer.jpg', 'GIFT', 'MOCK_GIFT_DEMO',
 'UNUSED', DATE_ADD(NOW(), INTERVAL 25 DAY), NULL,
 DATE_SUB(NOW(), INTERVAL 1 DAY), NOW(), 0)
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id),
    store_id = VALUES(store_id),
    coupon_name = VALUES(coupon_name),
    image_url = VALUES(image_url),
    status = VALUES(status),
    expire_time = VALUES(expire_time),
    used_time = VALUES(used_time),
    deleted = 0,
    update_time = NOW();

INSERT INTO coupon_redeem_request(
    id, user_coupon_id, user_id, status, remark, auditor_id,
    audit_remark, audit_time, create_time, update_time, deleted
) VALUES
(10001, 10002, 10001, 'PENDING', '今晚到店使用', NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 30 MINUTE), NOW(), 0),
(10002, 10003, 10002, 'APPROVED', '到店核销', 1, '已确认使用',
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NOW(), 0)
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    auditor_id = VALUES(auditor_id),
    audit_remark = VALUES(audit_remark),
    audit_time = VALUES(audit_time),
    deleted = 0,
    update_time = NOW();

INSERT INTO system_config(config_key, config_value, description, deleted) VALUES
('gameplay.description', '积分由酒馆管理员发放，用于排行榜荣誉展示。需要取出积分时，可在会员中心提交申请，管理员审核后生效。赠送卡券有效期为 30 天。', '小程序玩法说明', 0),
('points.halving.enabled', 'false', '是否启用积分定时减半', 0),
('points.halving.intervalDays', '7', '积分减半间隔自然日', 0),
('points.halving.time', '00:00', '积分减半触发时间', 0),
('coupon.gift.validDays', '30', '后台赠送卡券有效天数', 0)
ON DUPLICATE KEY UPDATE
    config_value = VALUES(config_value),
    description = VALUES(description),
    deleted = 0,
    update_time = NOW();

INSERT INTO store_operation_config(
    id, store_id, business_end_time, home_slogan, hero_image,
    gameplay_description, menu_title, points_halving_enabled,
    points_halving_day, points_halving_time, points_halving_last_period,
    create_time, update_time, deleted
) VALUES (
    10001, 10001, '02:00', '今晚，慢一点。',
    '/uploads/products/sunset-gin-tonic.jpg',
    '积分由本店管理员发放，仅用于本店排行榜荣誉展示。积分按后台配置的自然日周期自动减半，四舍五入。需要取出积分时，可在会员中心提交申请，审核通过后生效。赠送卡券有效期为 30 天。',
    '今晚酒单', 0, 7, '00:00', '', NOW(), NOW(), 0
)
ON DUPLICATE KEY UPDATE
    business_end_time = VALUES(business_end_time),
    home_slogan = VALUES(home_slogan),
    hero_image = VALUES(hero_image),
    gameplay_description = VALUES(gameplay_description),
    menu_title = VALUES(menu_title),
    points_halving_enabled = VALUES(points_halving_enabled),
    points_halving_day = VALUES(points_halving_day),
    points_halving_time = VALUES(points_halving_time),
    deleted = 0,
    update_time = NOW();

INSERT INTO user_store_points(
    id, user_id, store_id, points, frozen_points, version,
    create_time, update_time, deleted
) VALUES
(10001, 10001, 10001, 1680, 0, 0, NOW(), NOW(), 0),
(10002, 10002, 10001, 620, 100, 0, NOW(), NOW(), 0),
(10003, 10003, 10001, 2360, 0, 0, NOW(), NOW(), 0),
(10004, 10004, 10001, 890, 0, 0, NOW(), NOW(), 0),
(10005, 10005, 10001, 260, 50, 0, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    points = VALUES(points),
    frozen_points = VALUES(frozen_points),
    deleted = 0,
    update_time = NOW();

INSERT INTO user_store_points(
    user_id, store_id, points, frozen_points, version,
    create_time, update_time, deleted
) VALUES
(1, 10001, 1680, 0, 0, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    points = VALUES(points),
    frozen_points = VALUES(frozen_points),
    deleted = 0,
    update_time = NOW();

INSERT INTO store_points_request(
    id, store_id, user_id, type, points, remark, status,
    auditor_id, audit_remark, audit_time, before_points, after_points,
    create_time, update_time, deleted
) VALUES
(10001, 10001, 10002, 'WITHDRAW', 100, '柜台取出积分', 'PENDING',
 NULL, NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 HOUR), NOW(), 0),
(10002, 10001, 10004, 'WITHDRAW', 50, '活动积分取出', 'APPROVED',
 1, '已完成柜台交付', DATE_SUB(NOW(), INTERVAL 1 DAY), 940, 890,
 DATE_SUB(NOW(), INTERVAL 1 DAY), NOW(), 0)
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    auditor_id = VALUES(auditor_id),
    audit_remark = VALUES(audit_remark),
    audit_time = VALUES(audit_time),
    before_points = VALUES(before_points),
    after_points = VALUES(after_points),
    deleted = 0,
    update_time = NOW();

INSERT INTO store_points_log(
    id, store_id, user_id, change_type, change_value,
    before_value, after_value, biz_no, operator, remark,
    create_time, update_time, deleted
) VALUES
(10001, 10001, 10001, 'POINTS_CHANGE', 1680, 0, 1680,
 'MOCK_STORE_POINTS_10001', 'ADMIN:1', '管理员发放本店积分',
 DATE_SUB(NOW(), INTERVAL 7 DAY), NOW(), 0),
(10002, 10001, 10004, 'POINTS_CHANGE', -50, 940, 890,
 'MOCK_STORE_POINTS_10004', 'ADMIN:1', '取积分审核通过',
 DATE_SUB(NOW(), INTERVAL 1 DAY), NOW(), 0)
ON DUPLICATE KEY UPDATE
    change_value = VALUES(change_value),
    before_value = VALUES(before_value),
    after_value = VALUES(after_value),
    operator = VALUES(operator),
    remark = VALUES(remark),
    deleted = 0,
    update_time = NOW();
