"""生成 Postman 集合与环境文件（Collection v2.1）。

输出：
    docs/postman/campus-trade.postman_collection.json
    docs/postman/campus-trade.postman_environment.json

集合按 8 个模块分组，覆盖正常流程、异常流程与边界值；每条请求都带断言
（HTTP 状态码 + 业务返回码 + 关键业务字段），并通过环境变量串联前后依赖。
"""

import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR = os.path.join(ROOT, "docs", "postman")

cases = []


def chk(label, js):
    return (label, js)


def bearer(token):
    return {"Authorization": "Bearer {{{{{0}}}}}".format(token)}


def case(folder, name, method, path, **kwargs):
    cases.append({
        "folder": folder,
        "name": name,
        "method": method,
        "path": path,
        "query": kwargs.get("query"),
        "json": kwargs.get("json"),
        "raw": kwargs.get("raw"),
        "formdata": kwargs.get("formdata"),
        "headers": kwargs.get("headers") or {},
        "status": kwargs.get("status"),
        "code": kwargs.get("code"),
        "checks": kwargs.get("checks") or [],
        "captures": kwargs.get("captures") or {},
        "pre": kwargs.get("pre"),
        "description": kwargs.get("description", ""),
    })


def ok(status=200, code=200, checks=(), captures=None):
    return dict(status=status, code=code, checks=list(checks), captures=captures or {})


# ============================ 01 注册登录 ============================
F = "01-注册登录"
case(F, "01 注册-新用户注册成功", "POST", "/api/auth/register",
     pre="pm.environment.set('newUsername', 'stu' + Date.now());",
     json={"username": "{{newUsername}}", "password": "123456", "nickname": "接口测试新同学",
           "phone": "13800138000", "email": "stu@campus.com", "school": "南昌职业大学"},
     description="用户名唯一；把新用户名写入环境变量供后续用例复用",
     **ok(checks=[chk("返回用户名与环境变量一致", "body.data.username === pm.environment.get('newUsername')"),
                  chk("新用户角色为 USER", "body.data.role === 'USER'"),
                  chk("响应不包含密码字段", "body.data.password === undefined")],
          captures={"newUserId": "String(body.data.id)"}))

case(F, "02 注册-用户名重复", "POST", "/api/auth/register",
     json={"username": "{{newUsername}}", "password": "123456", "nickname": "重复注册"},
     description="同一用户名重复注册返回 409/40904",
     **ok(409, 40904, [chk("提示用户名已存在", "body.message.includes('已存在')")]))

case(F, "03 注册-用户名过短（2 位）", "POST", "/api/auth/register",
     json={"username": "ab", "password": "123456", "nickname": "短用户名"},
     **ok(400, 40001, [chk("返回字段级错误明细", "Array.isArray(body.data) && body.data.length > 0")]))

case(F, "04 注册-用户名含特殊字符", "POST", "/api/auth/register",
     json={"username": "stu@01!", "password": "123456", "nickname": "特殊字符"},
     **ok(400, 40001))

case(F, "05 注册-密码过短（3 位）", "POST", "/api/auth/register",
     json={"username": "stu_shortpwd", "password": "123", "nickname": "短密码"},
     **ok(400, 40001))

case(F, "06 注册-昵称为空", "POST", "/api/auth/register",
     json={"username": "stu_nonick", "password": "123456", "nickname": ""},
     **ok(400, 40001))

case(F, "07 注册-邮箱格式错误", "POST", "/api/auth/register",
     json={"username": "stu_bademail", "password": "123456", "nickname": "邮箱错误", "email": "not-an-email"},
     **ok(400, 40001))

case(F, "08 注册-手机号格式错误", "POST", "/api/auth/register",
     json={"username": "stu_badphone", "password": "123456", "nickname": "手机号错误", "phone": "12345"},
     **ok(400, 40001))

case(F, "09 注册-请求体为空对象", "POST", "/api/auth/register",
     raw="{}",
     **ok(400, 40001))

case(F, "10 登录-买家登录成功", "POST", "/api/auth/login",
     json={"username": "buyer01", "password": "123456"},
     description="登录成功返回 JWT，写入环境变量 buyerToken",
     **ok(checks=[chk("返回 token", "typeof body.data.token === 'string' && body.data.token.length > 20"),
                  chk("tokenType 为 Bearer", "body.data.tokenType === 'Bearer'"),
                  chk("昵称为苏晴", "body.data.user.nickname === '苏晴'")],
          captures={"buyerToken": "body.data.token", "buyerUserId": "String(body.data.user.id)"}))

case(F, "11 登录-卖家 seller01 登录成功", "POST", "/api/auth/login",
     json={"username": "seller01", "password": "123456"},
     **ok(checks=[chk("昵称为林晓", "body.data.user.nickname === '林晓'")],
          captures={"sellerToken": "body.data.token", "sellerUserId": "String(body.data.user.id)"}))

case(F, "12 登录-卖家 seller02 登录成功", "POST", "/api/auth/login",
     json={"username": "seller02", "password": "123456"},
     **ok(checks=[chk("昵称为陈默", "body.data.user.nickname === '陈默'")],
          captures={"seller2Token": "body.data.token"}))

case(F, "13 登录-管理员登录成功", "POST", "/api/auth/login",
     json={"username": "admin", "password": "123456"},
     **ok(checks=[chk("角色为 ADMIN", "body.data.user.role === 'ADMIN'")],
          captures={"adminToken": "body.data.token"}))

case(F, "14 登录-新注册用户登录成功", "POST", "/api/auth/login",
     json={"username": "{{newUsername}}", "password": "123456"},
     **ok(captures={"newUserToken": "body.data.token"}))

case(F, "15 登录-密码错误", "POST", "/api/auth/login",
     json={"username": "buyer01", "password": "wrong-password"},
     **ok(401, 40102, [chk("提示用户名或密码错误", "body.message.includes('用户名或密码错误')")]))

case(F, "16 登录-用户名不存在", "POST", "/api/auth/login",
     json={"username": "not_exist_user", "password": "123456"},
     **ok(401, 40102))

case(F, "17 登录-用户名为空", "POST", "/api/auth/login",
     json={"username": "", "password": "123456"},
     **ok(400, 40001))

case(F, "18 登录-密码为空", "POST", "/api/auth/login",
     json={"username": "buyer01", "password": ""},
     **ok(400, 40001))

case(F, "19 当前用户-未携带 token", "GET", "/api/auth/me",
     **ok(401, 40101))

case(F, "20 当前用户-token 非法", "GET", "/api/auth/me",
     headers={"Authorization": "Bearer this.is.not.a.valid.token"},
     **ok(401, 40101))

case(F, "21 当前用户-查询成功", "GET", "/api/auth/me",
     headers=bearer("buyerToken"),
     **ok(checks=[chk("账号为 buyer01", "body.data.username === 'buyer01'"),
                  chk("本人可见手机号", "typeof body.data.phone === 'string'")]))

case(F, "22 修改密码-原密码错误", "PUT", "/api/auth/password",
     headers=bearer("buyerToken"),
     json={"oldPassword": "wrong-password", "newPassword": "abc123456"},
     **ok(400, 40001, [chk("提示原密码不正确", "body.message.includes('原密码不正确')")]))

case(F, "23 修改密码-新密码与原密码相同", "PUT", "/api/auth/password",
     headers=bearer("buyerToken"),
     json={"oldPassword": "123456", "newPassword": "123456"},
     **ok(400, 40001))

case(F, "24 修改密码-新密码过短", "PUT", "/api/auth/password",
     headers=bearer("buyerToken"),
     json={"oldPassword": "123456", "newPassword": "123"},
     **ok(400, 40001))

case(F, "25 退出登录", "POST", "/api/auth/logout",
     headers=bearer("buyerToken"),
     **ok(checks=[chk("返回成功", "body.code === 200")]))

# ============================ 02 用户 ============================
F = "02-用户"
case(F, "26 用户公开资料-查看他人资料", "GET", "/api/users/{{sellerUserId}}",
     **ok(checks=[chk("返回卖家昵称", "body.data.nickname === '林晓'"),
                  chk("他人手机号已脱敏", "body.data.phone === null || body.data.phone.indexOf('****') >= 0"),
                  chk("返回在售商品数量", "typeof body.data.onSaleCount === 'number'")]))

case(F, "27 用户公开资料-用户不存在", "GET", "/api/users/999999",
     **ok(404, 40401))

case(F, "28 用户公开资料-用户 ID 非数字", "GET", "/api/users/abc",
     **ok(400, 40001))

case(F, "29 查看某用户在售商品", "GET", "/api/users/{{sellerUserId}}/products",
     query={"page": "1", "size": "5"},
     **ok(checks=[chk("分页字段完整", "typeof body.data.total === 'number' && Array.isArray(body.data.records)"),
                  chk("只返回在售商品", "body.data.records.every(p => p.status === 'ON_SALE')")]))

case(F, "30 更新个人资料-修改昵称与学校", "PUT", "/api/users/me",
     headers=bearer("buyerToken"),
     json={"nickname": "苏晴", "school": "南昌职业大学", "phone": "13900000004", "email": "suqing@campus.com"},
     **ok(checks=[chk("昵称已更新", "body.data.nickname === '苏晴'")]))

case(F, "31 更新个人资料-手机号格式错误", "PUT", "/api/users/me",
     headers=bearer("buyerToken"),
     json={"nickname": "苏晴", "phone": "12345"},
     **ok(400, 40001))

case(F, "32 更新个人资料-昵称超长（21 字）", "PUT", "/api/users/me",
     headers=bearer("buyerToken"),
     json={"nickname": "测" * 21},
     **ok(400, 40001))

case(F, "33 更新个人资料-未登录", "PUT", "/api/users/me",
     json={"nickname": "匿名"},
     **ok(401, 40101))

# ============================ 03 分类 ============================
F = "03-分类"
case(F, "34 分类列表-查询成功", "GET", "/api/categories",
     **ok(checks=[chk("至少返回 6 个分类", "body.data.length >= 6"),
                  chk("按 sort 升序", "body.data.every((item, i, arr) => i === 0 || arr[i - 1].sort <= item.sort)")]))

case(F, "35 分类列表-请求方法不支持", "POST", "/api/categories",
     json={},
     **ok(405, 40500))

# ============================ 04 商品 ============================
F = "04-商品"
case(F, "36 商品列表-默认分页", "GET", "/api/products",
     **ok(checks=[chk("默认每页 10 条", "body.data.size === 10"),
                  chk("仅返回在售商品", "body.data.records.every(p => p.status === 'ON_SALE')"),
                  chk("分页字段完整", "['records', 'total', 'page', 'size', 'pages'].every(k => k in body.data)")]))

case(F, "37 商品列表-关键词搜索命中", "GET", "/api/products",
     query={"keyword": "教材", "size": "20"},
     **ok(checks=[chk("搜索结果不为空", "body.data.total >= 1"),
                  chk("标题或描述包含关键词", "body.data.records.every(p => (p.title + (p.description || '')).indexOf('教材') >= 0)")]))

case(F, "38 商品列表-关键词无结果", "GET", "/api/products",
     query={"keyword": "不存在的商品关键词xyz", "size": "20"},
     **ok(checks=[chk("总数为 0", "body.data.total === 0"),
                  chk("返回空数组", "body.data.records.length === 0")]))

case(F, "39 商品列表-按分类筛选", "GET", "/api/products",
     query={"categoryId": "2", "size": "20"},
     **ok(checks=[chk("全部分类 ID 为 2", "body.data.records.every(p => p.categoryId === 2)")]))

case(F, "40 商品列表-价格区间筛选", "GET", "/api/products",
     query={"minPrice": "20", "maxPrice": "60", "size": "50"},
     **ok(checks=[chk("价格都在 20-60 之间", "body.data.records.every(p => p.price >= 20 && p.price <= 60)")]))

case(F, "41 商品列表-价格升序排序", "GET", "/api/products",
     query={"sort": "priceAsc", "size": "20"},
     **ok(checks=[chk("价格升序", "body.data.records.every((p, i, arr) => i === 0 || arr[i - 1].price <= p.price)")]))

case(F, "42 商品列表-价格降序排序", "GET", "/api/products",
     query={"sort": "priceDesc", "size": "20"},
     **ok(checks=[chk("价格降序", "body.data.records.every((p, i, arr) => i === 0 || arr[i - 1].price >= p.price)")]))

case(F, "43 商品列表-收藏最多排序", "GET", "/api/products",
     query={"sort": "hot", "size": "10"},
     **ok(checks=[chk("收藏数降序", "body.data.records.every((p, i, arr) => i === 0 || arr[i - 1].favoriteCount >= p.favoriteCount)")]))

case(F, "44 商品列表-每页最大条数边界 100", "GET", "/api/products",
     query={"page": "1", "size": "100"},
     **ok(checks=[chk("每页条数为 100", "body.data.size === 100")]))

case(F, "45 商品列表-页码为 0（非法边界）", "GET", "/api/products",
     query={"page": "0"},
     **ok(400, 40001, [chk("提示页码不合法", "body.message.indexOf('页码') >= 0")]))

case(F, "46 商品列表-每页条数超上限 101", "GET", "/api/products",
     query={"size": "101"},
     **ok(400, 40001))

case(F, "47 商品列表-每页条数为 0", "GET", "/api/products",
     query={"size": "0"},
     **ok(400, 40001))

case(F, "48 商品列表-页码非数字", "GET", "/api/products",
     query={"page": "abc"},
     **ok(400, 40001))

case(F, "49 商品列表-最低价大于最高价", "GET", "/api/products",
     query={"minPrice": "100", "maxPrice": "1"},
     **ok(400, 40001, [chk("提示价格区间不合法", "body.message.indexOf('最低价格') >= 0")]))

case(F, "50 商品列表-排序方式非法", "GET", "/api/products",
     query={"sort": "unknown"},
     **ok(400, 40001))

case(F, "51 商品列表-超出总页数返回空列表", "GET", "/api/products",
     query={"page": "999", "size": "10"},
     **ok(checks=[chk("返回空数组", "body.data.records.length === 0"),
                  chk("总数仍为正数", "body.data.total > 0")]))

case(F, "52 商品详情-查询成功", "GET", "/api/products/1",
     **ok(checks=[chk("商品 ID 正确", "body.data.id === 1"),
                  chk("返回图片列表", "Array.isArray(body.data.images) && body.data.images.length > 0"),
                  chk("返回卖家昵称", "typeof body.data.sellerNickname === 'string'"),
                  chk("返回状态中文名", "body.data.statusLabel === '在售'")],
          captures={"seededProductId": "body.data.id", "seededViewCount": "body.data.viewCount"}))

case(F, "53 商品详情-浏览量自增", "GET", "/api/products/1",
     **ok(checks=[chk("浏览量比上次 +1 以上", "body.data.viewCount > Number(pm.environment.get('seededViewCount'))")]))

case(F, "54 商品详情-商品不存在", "GET", "/api/products/999999",
     **ok(404, 40401))

case(F, "55 商品详情-商品 ID 非数字", "GET", "/api/products/abc",
     **ok(400, 40001))

case(F, "56 发布商品-未登录", "POST", "/api/products",
     json={"title": "未登录发布", "price": 10, "categoryId": 1, "conditionLevel": "全新"},
     **ok(401, 40101))

case(F, "57 发布商品-卖家发布成功", "POST", "/api/products",
     headers=bearer("sellerToken"),
     json={"title": "接口测试商品-自动化创建", "description": "由 Postman 集合创建，用于验证发布、下单与订单状态流转。",
           "price": 66.60, "originalPrice": 199.00, "categoryId": 2, "conditionLevel": "九成新",
           "tradePlace": "图书馆一楼", "imageUrls": ["/seed/p01.png"]},
     description="发布成功后把商品 ID 写入环境变量，后续下单用例依赖它",
     **ok(checks=[chk("返回商品 ID", "typeof body.data.id === 'number'")],
          captures={"productId": "String(body.data.id)"}))

case(F, "58 发布商品-价格下限边界 0.01", "POST", "/api/products",
     headers=bearer("sellerToken"),
     json={"title": "接口测试-价格下限 0.01", "price": 0.01, "categoryId": 1, "conditionLevel": "全新"},
     **ok(checks=[chk("发布成功", "body.code === 200")],
          captures={"minPriceProductId": "String(body.data.id)"}))

case(F, "59 发布商品-价格上限边界 999999.99", "POST", "/api/products",
     headers=bearer("sellerToken"),
     json={"title": "接口测试-价格上限 999999.99", "price": 999999.99, "categoryId": 1, "conditionLevel": "全新"},
     **ok(checks=[chk("发布成功", "body.code === 200")],
          captures={"maxPriceProductId": "String(body.data.id)"}))

case(F, "60 发布商品-价格低于下限 0.001", "POST", "/api/products",
     headers=bearer("sellerToken"),
     json={"title": "接口测试-价格过低", "price": 0.001, "categoryId": 1, "conditionLevel": "全新"},
     **ok(400, 40001))

case(F, "61 发布商品-价格为负数", "POST", "/api/products",
     headers=bearer("sellerToken"),
     json={"title": "接口测试-负价格", "price": -1, "categoryId": 1, "conditionLevel": "全新"},
     **ok(400, 40001, [chk("提示价格范围", "body.data.some(item => item.field === 'price')")]))

case(F, "62 发布商品-价格为 0", "POST", "/api/products",
     headers=bearer("sellerToken"),
     json={"title": "接口测试-零价格", "price": 0, "categoryId": 1, "conditionLevel": "全新"},
     **ok(400, 40001))

case(F, "63 发布商品-价格超过上限", "POST", "/api/products",
     headers=bearer("sellerToken"),
     json={"title": "接口测试-价格超高", "price": 1000000, "categoryId": 1, "conditionLevel": "全新"},
     **ok(400, 40001))

case(F, "64 发布商品-标题为空", "POST", "/api/products",
     headers=bearer("sellerToken"),
     json={"title": "", "price": 10, "categoryId": 1, "conditionLevel": "全新"},
     **ok(400, 40001))

case(F, "65 发布商品-标题 50 字（边界内）", "POST", "/api/products",
     headers=bearer("sellerToken"),
     json={"title": "测" * 50, "price": 10, "categoryId": 1, "conditionLevel": "全新"},
     **ok(checks=[chk("发布成功", "body.code === 200")]))

case(F, "66 发布商品-标题 51 字（超边界）", "POST", "/api/products",
     headers=bearer("sellerToken"),
     json={"title": "测" * 51, "price": 10, "categoryId": 1, "conditionLevel": "全新"},
     **ok(400, 40001))

case(F, "67 发布商品-成色非法", "POST", "/api/products",
     headers=bearer("sellerToken"),
     json={"title": "接口测试-成色非法", "price": 10, "categoryId": 1, "conditionLevel": "五成新"},
     **ok(400, 40001))

case(F, "68 发布商品-分类不存在", "POST", "/api/products",
     headers=bearer("sellerToken"),
     json={"title": "接口测试-分类不存在", "price": 10, "categoryId": 999999, "conditionLevel": "全新"},
     **ok(400, 40001, [chk("提示分类不存在", "body.message.indexOf('分类') >= 0")]))

case(F, "69 发布商品-图片超过 5 张", "POST", "/api/products",
     headers=bearer("sellerToken"),
     json={"title": "接口测试-图片超限", "price": 10, "categoryId": 1, "conditionLevel": "全新",
           "imageUrls": ["/seed/p01.png"] * 6},
     **ok(400, 40001))

case(F, "70 编辑商品-修改他人商品（越权）", "PUT", "/api/products/{{productId}}",
     headers=bearer("buyerToken"),
     json={"title": "越权修改", "price": 1, "categoryId": 1, "conditionLevel": "全新"},
     **ok(403, 40301, [chk("提示只能编辑自己的商品", "body.message.indexOf('自己') >= 0")]))

case(F, "71 编辑商品-本人修改成功", "PUT", "/api/products/{{productId}}",
     headers=bearer("sellerToken"),
     json={"title": "接口测试商品-已修改", "description": "修改后的描述", "price": 88.80,
           "originalPrice": 199.00, "categoryId": 2, "conditionLevel": "八成新",
           "tradePlace": "三号宿舍楼下", "imageUrls": ["/seed/p02.png"]},
     **ok(checks=[chk("修改成功", "body.code === 200")]))

case(F, "72 编辑商品-商品不存在", "PUT", "/api/products/999999",
     headers=bearer("sellerToken"),
     json={"title": "不存在", "price": 1, "categoryId": 1, "conditionLevel": "全新"},
     **ok(404, 40401))

case(F, "73 商品下架-本人操作成功", "PATCH", "/api/products/{{productId}}/status",
     headers=bearer("sellerToken"),
     json={"status": "OFF_SHELF"},
     **ok(checks=[chk("下架成功", "body.code === 200")]))

case(F, "74 商品下架-重复下架（状态非法流转）", "PATCH", "/api/products/{{productId}}/status",
     headers=bearer("sellerToken"),
     json={"status": "OFF_SHELF"},
     **ok(409, 40902))

case(F, "75 商品上架-重新上架成功", "PATCH", "/api/products/{{productId}}/status",
     headers=bearer("sellerToken"),
     json={"status": "ON_SALE"},
     **ok(checks=[chk("上架成功", "body.code === 200")]))

case(F, "76 商品上下架-目标状态非法", "PATCH", "/api/products/{{productId}}/status",
     headers=bearer("sellerToken"),
     json={"status": "SOLD"},
     **ok(400, 40001))

case(F, "77 商品上下架-操作他人商品", "PATCH", "/api/products/{{productId}}/status",
     headers=bearer("buyerToken"),
     json={"status": "OFF_SHELF"},
     **ok(403, 40301))

case(F, "78 我发布的商品-查询成功", "GET", "/api/products/mine",
     headers=bearer("sellerToken"),
     query={"page": "1", "size": "20"},
     **ok(checks=[chk("包含刚发布的商品", "body.data.records.some(p => String(p.id) === pm.environment.get('productId'))")]))

case(F, "79 我发布的商品-按状态筛选", "GET", "/api/products/mine",
     headers=bearer("sellerToken"),
     query={"status": "ON_SALE", "size": "20"},
     **ok(checks=[chk("只返回在售商品", "body.data.records.every(p => p.status === 'ON_SALE')")]))

case(F, "80 我发布的商品-状态参数非法", "GET", "/api/products/mine",
     headers=bearer("sellerToken"),
     query={"status": "UNKNOWN"},
     **ok(400, 40001))

case(F, "81 我发布的商品-未登录", "GET", "/api/products/mine",
     **ok(401, 40101))

case(F, "82 上传图片-上传 png 成功", "POST", "/api/files/images",
     headers=bearer("sellerToken"),
     formdata=[{"key": "file", "type": "file", "src": "docs/postman/testdata/test-upload.png"}],
     **ok(checks=[chk("返回图片地址", "typeof body.data.url === 'string' && body.data.url.indexOf('/uploads/') === 0")],
          captures={"uploadedImageUrl": "body.data.url"}))

case(F, "83 上传图片-文件类型不支持（txt）", "POST", "/api/files/images",
     headers=bearer("sellerToken"),
     formdata=[{"key": "file", "type": "file", "src": "docs/postman/testdata/test-upload.txt"}],
     **ok(400, 40002, [chk("提示文件类型不支持", "body.message.indexOf('文件类型') >= 0")]))

case(F, "84 上传图片-文件超过 5MB", "POST", "/api/files/images",
     headers=bearer("sellerToken"),
     formdata=[{"key": "file", "type": "file", "src": "docs/postman/testdata/test-upload-6mb.png"}],
     **ok(400, 40003))

case(F, "85 上传图片-未登录", "POST", "/api/files/images",
     formdata=[{"key": "file", "type": "file", "src": "docs/postman/testdata/test-upload.png"}],
     **ok(401, 40101))

# ============================ 05 收藏 ============================
F = "05-收藏"
case(F, "86 收藏商品-首次收藏成功", "POST", "/api/favorites/{{productId}}",
     headers=bearer("buyerToken"),
     **ok(checks=[chk("收藏状态为 true", "body.data.favorited === true")]))

case(F, "87 收藏商品-重复收藏幂等", "POST", "/api/favorites/{{productId}}",
     headers=bearer("buyerToken"),
     **ok(checks=[chk("提示已收藏过", "body.message.indexOf('已经收藏') >= 0")]))

case(F, "88 收藏商品-商品不存在", "POST", "/api/favorites/999999",
     headers=bearer("buyerToken"),
     **ok(404, 40401))

case(F, "89 收藏商品-商品 ID 非数字", "POST", "/api/favorites/abc",
     headers=bearer("buyerToken"),
     **ok(400, 40001))

case(F, "90 收藏商品-未登录", "POST", "/api/favorites/1",
     **ok(401, 40101))

case(F, "91 收藏状态-查询已收藏", "GET", "/api/favorites/{{productId}}/status",
     headers=bearer("buyerToken"),
     **ok(checks=[chk("收藏状态为 true", "body.data.favorited === true")]))

case(F, "92 我的收藏列表-查询成功", "GET", "/api/favorites",
     headers=bearer("buyerToken"),
     query={"page": "1", "size": "20"},
     **ok(checks=[chk("包含刚收藏的商品", "body.data.records.some(item => String(item.productId) === pm.environment.get('productId'))"),
                  chk("返回商品摘要字段", "body.data.records.every(item => 'title' in item && 'price' in item)")]))

case(F, "93 我的收藏列表-未登录", "GET", "/api/favorites",
     **ok(401, 40101))

case(F, "94 取消收藏-成功", "DELETE", "/api/favorites/{{productId}}",
     headers=bearer("buyerToken"),
     **ok(checks=[chk("收藏状态为 false", "body.data.favorited === false")]))

case(F, "95 取消收藏-未收藏时幂等", "DELETE", "/api/favorites/{{productId}}",
     headers=bearer("buyerToken"),
     **ok(checks=[chk("提示尚未收藏", "body.message.indexOf('尚未收藏') >= 0")]))

case(F, "96 收藏状态-取消后为 false", "GET", "/api/favorites/{{productId}}/status",
     headers=bearer("buyerToken"),
     **ok(checks=[chk("收藏状态为 false", "body.data.favorited === false")]))

# ============================ 06 订单 ============================
F = "06-订单"
case(F, "97 下单-买家下单成功", "POST", "/api/orders",
     headers=bearer("buyerToken"),
     json={"productId": "{{productId}}", "remark": "接口测试下单：今晚 7 点图书馆一楼交易"},
     description="下单后商品被锁定为交易中，订单 ID 写入环境变量",
     **ok(checks=[chk("订单状态为待卖家确认", "body.data.status === 'PENDING_CONFIRM'"),
                  chk("金额等于商品售价", "Number(body.data.amount) === 88.8"),
                  chk("返回订单号", "typeof body.data.orderNo === 'string' && body.data.orderNo.indexOf('C') === 0"),
                  chk("返回超时时间点", "typeof body.data.expireAt === 'string'")],
          captures={"orderId": "String(body.data.id)"}))

case(F, "98 下单-商品状态变为交易中", "GET", "/api/products/{{productId}}",
     **ok(checks=[chk("商品状态为 LOCKED", "body.data.status === 'LOCKED'"),
                  chk("状态中文名为交易中", "body.data.statusLabel === '交易中'")]))

case(F, "99 下单-重复下单被拦截（防重复提交）", "POST", "/api/orders",
     headers=bearer("buyerToken"),
     json={"productId": "{{productId}}", "remark": "重复提交"},
     description="对应简历中的严重缺陷场景：重复提交不能生成重复订单",
     **ok(409, 40901, [chk("提示请勿重复提交", "body.message.indexOf('重复') >= 0")]))

case(F, "100 下单-购买自己发布的商品", "POST", "/api/orders",
     headers=bearer("sellerToken"),
     json={"productId": "{{productId}}"},
     **ok(409, 40903, [chk("提示不能购买自己的商品", "body.message.indexOf('自己') >= 0")]))

case(F, "101 下单-其他买家购买已锁定商品", "POST", "/api/orders",
     headers=bearer("seller2Token"),
     json={"productId": "{{productId}}"},
     **ok(409, 40903, [chk("提示商品不可交易", "body.message.indexOf('不可交易') >= 0")]))

case(F, "102 下单-商品不存在", "POST", "/api/orders",
     headers=bearer("buyerToken"),
     json={"productId": 999999},
     **ok(404, 40401))

case(F, "103 下单-缺少商品 ID", "POST", "/api/orders",
     headers=bearer("buyerToken"),
     json={"remark": "没有商品 ID"},
     **ok(400, 40001))

case(F, "104 下单-留言超长（201 字）", "POST", "/api/orders",
     headers=bearer("buyerToken"),
     json={"productId": "{{minPriceProductId}}", "remark": "测" * 201},
     **ok(400, 40001))

case(F, "105 下单-未登录", "POST", "/api/orders",
     json={"productId": 1},
     **ok(401, 40101))

case(F, "106 订单列表-买家视角", "GET", "/api/orders",
     headers=bearer("buyerToken"),
     query={"role": "buyer", "size": "20"},
     **ok(checks=[chk("包含刚创建的订单", "body.data.records.some(o => String(o.id) === pm.environment.get('orderId'))"),
                  chk("返回订单号与商品标题", "body.data.records.every(o => 'orderNo' in o && 'productTitle' in o)")]))

case(F, "107 订单列表-卖家视角", "GET", "/api/orders",
     headers=bearer("sellerToken"),
     query={"role": "seller", "size": "20"},
     **ok(checks=[chk("包含刚创建的订单", "body.data.records.some(o => String(o.id) === pm.environment.get('orderId'))")]))

case(F, "108 订单列表-按状态筛选", "GET", "/api/orders",
     headers=bearer("buyerToken"),
     query={"role": "buyer", "status": "PENDING_CONFIRM", "size": "20"},
     **ok(checks=[chk("只返回待确认订单", "body.data.records.every(o => o.status === 'PENDING_CONFIRM')")]))

case(F, "109 订单列表-角色参数非法", "GET", "/api/orders",
     headers=bearer("buyerToken"),
     query={"role": "unknown"},
     **ok(400, 40001))

case(F, "110 订单列表-状态参数非法", "GET", "/api/orders",
     headers=bearer("buyerToken"),
     query={"status": "UNKNOWN"},
     **ok(400, 40001))

case(F, "111 订单列表-未登录", "GET", "/api/orders",
     **ok(401, 40101))

case(F, "112 订单详情-买家查看自己的订单", "GET", "/api/orders/{{orderId}}",
     headers=bearer("buyerToken"),
     **ok(checks=[chk("订单 ID 正确", "String(body.data.id) === pm.environment.get('orderId')"),
                  chk("返回买卖双方昵称", "typeof body.data.buyerNickname === 'string' && typeof body.data.sellerNickname === 'string'")]))

case(F, "113 订单详情-卖家查看", "GET", "/api/orders/{{orderId}}",
     headers=bearer("sellerToken"),
     **ok(checks=[chk("查询成功", "body.code === 200")]))

case(F, "114 订单详情-无关用户越权查看", "GET", "/api/orders/{{orderId}}",
     headers=bearer("seller2Token"),
     **ok(403, 40301, [chk("提示只能查看相关订单", "body.message.indexOf('订单') >= 0")]))

case(F, "115 订单详情-订单不存在", "GET", "/api/orders/999999",
     headers=bearer("buyerToken"),
     **ok(404, 40401))

case(F, "116 卖家确认-买家越权确认", "POST", "/api/orders/{{orderId}}/confirm",
     headers=bearer("buyerToken"),
     **ok(403, 40301))

case(F, "117 卖家确认-卖家确认成功", "POST", "/api/orders/{{orderId}}/confirm",
     headers=bearer("sellerToken"),
     **ok(checks=[chk("订单状态变为交易中", "body.data.status === 'CONFIRMED'"),
                  chk("记录确认时间", "typeof body.data.confirmedAt === 'string'")]))

case(F, "118 卖家确认-重复确认", "POST", "/api/orders/{{orderId}}/confirm",
     headers=bearer("sellerToken"),
     **ok(409, 40902, [chk("提示状态不允许", "body.message.indexOf('只有') >= 0 || body.message.indexOf('不允许') >= 0")]))

case(F, "119 买家完成-卖家越权完成", "POST", "/api/orders/{{orderId}}/finish",
     headers=bearer("sellerToken"),
     **ok(403, 40301))

case(F, "120 买家完成-买家确认完成", "POST", "/api/orders/{{orderId}}/finish",
     headers=bearer("buyerToken"),
     **ok(checks=[chk("订单状态为已完成", "body.data.status === 'COMPLETED'"),
                  chk("记录完成时间", "typeof body.data.finishedAt === 'string'")]))

case(F, "121 买家完成-商品状态变为已售出", "GET", "/api/products/{{productId}}",
     **ok(checks=[chk("商品状态为 SOLD", "body.data.status === 'SOLD'")]))

case(F, "122 取消订单-已完成订单不可取消", "POST", "/api/orders/{{orderId}}/cancel",
     headers=bearer("buyerToken"),
     json={"reason": "完成后想取消"},
     **ok(409, 40902))

case(F, "123 取消订单-重新下单（取消流程准备）", "POST", "/api/orders",
     headers=bearer("buyerToken"),
     json={"productId": "{{minPriceProductId}}", "remark": "取消流程测试"},
     **ok(checks=[chk("下单成功", "body.code === 200")],
          captures={"cancelOrderId": "String(body.data.id)", "cancelProductId": "String(body.data.productId)"}))

case(F, "124 取消订单-买家取消成功", "POST", "/api/orders/{{cancelOrderId}}/cancel",
     headers=bearer("buyerToken"),
     json={"reason": "临时不想要了"},
     **ok(checks=[chk("订单状态为已取消", "body.data.status === 'CANCELED'"),
                  chk("记录取消原因", "body.data.cancelReason === '临时不想要了'")]))

case(F, "125 取消订单-商品回到在售状态", "GET", "/api/products/{{cancelProductId}}",
     **ok(checks=[chk("商品状态为 ON_SALE", "body.data.status === 'ON_SALE'")]))

case(F, "126 取消订单-重复取消", "POST", "/api/orders/{{cancelOrderId}}/cancel",
     headers=bearer("buyerToken"),
     json={"reason": "再次取消"},
     **ok(409, 40902))

case(F, "127 取消订单-订单不存在", "POST", "/api/orders/999999/cancel",
     headers=bearer("buyerToken"),
     json={"reason": "不存在"},
     **ok(404, 40401))

# ============================ 07 消息 ============================
F = "07-消息"
case(F, "128 消息列表-卖家消息列表", "GET", "/api/messages",
     headers=bearer("sellerToken"),
     query={"page": "1", "size": "20"},
     **ok(checks=[chk("消息字段完整", "body.data.records.every(m => 'title' in m && 'content' in m && 'isRead' in m)")]))

case(F, "129 消息列表-只看未读", "GET", "/api/messages",
     headers=bearer("sellerToken"),
     query={"isRead": "0", "size": "20"},
     **ok(checks=[chk("只返回未读消息", "body.data.records.every(m => m.isRead === 0)")]))

case(F, "130 消息列表-按类型筛选订单消息", "GET", "/api/messages",
     headers=bearer("sellerToken"),
     query={"type": "ORDER", "size": "20"},
     **ok(checks=[chk("只返回订单消息", "body.data.records.every(m => m.type === 'ORDER')")]))

case(F, "131 消息列表-未登录", "GET", "/api/messages",
     **ok(401, 40101))

case(F, "132 未读消息数-查询成功", "GET", "/api/messages/unread-count",
     headers=bearer("sellerToken"),
     **ok(checks=[chk("返回 count 字段", "typeof body.data.count === 'number'")],
          captures={"sellerUnread": "body.data.count"}))

case(F, "133 标记已读-标记他人消息", "PUT", "/api/messages/1/read",
     headers=bearer("seller2Token"),
     **ok(403, 40301, [chk("提示只能操作自己的消息", "body.message.indexOf('自己') >= 0")]))

case(F, "134 标记已读-消息不存在", "PUT", "/api/messages/999999/read",
     headers=bearer("sellerToken"),
     **ok(404, 40401))

case(F, "135 标记已读-标记单条成功", "PUT", "/api/messages/1/read",
     headers=bearer("sellerToken"),
     **ok(checks=[chk("返回成功", "body.code === 200")]))

case(F, "136 全部已读-执行成功", "PUT", "/api/messages/read-all",
     headers=bearer("sellerToken"),
     **ok(checks=[chk("返回更新条数", "typeof body.data.updated === 'number'")]))

case(F, "137 全部已读-未读数归零", "GET", "/api/messages/unread-count",
     headers=bearer("sellerToken"),
     **ok(checks=[chk("未读数为 0", "body.data.count === 0")]))

# ============================ 08 管理端 ============================
F = "08-管理端"
case(F, "138 管理端商品列表-管理员查询", "GET", "/api/admin/products",
     headers=bearer("adminToken"),
     query={"page": "1", "size": "20"},
     **ok(checks=[chk("返回商品数据", "body.data.records.length > 0")]))

case(F, "139 管理端商品列表-普通用户访问", "GET", "/api/admin/products",
     headers=bearer("buyerToken"),
     **ok(403, 40303, [chk("提示需要管理员权限", "body.message.indexOf('管理员') >= 0")]))

case(F, "140 管理端商品列表-未登录访问", "GET", "/api/admin/products",
     **ok(401, 40101))

case(F, "141 管理端商品列表-按状态筛选已下架", "GET", "/api/admin/products",
     headers=bearer("adminToken"),
     query={"status": "OFF_SHELF", "size": "20"},
     **ok(checks=[chk("只返回已下架商品", "body.data.records.every(p => p.status === 'OFF_SHELF')")]))

case(F, "142 管理端商品列表-关键词搜索", "GET", "/api/admin/products",
     headers=bearer("adminToken"),
     query={"keyword": "接口测试", "size": "20"},
     **ok(checks=[chk("搜索到接口测试商品", "body.data.total >= 1")]))

case(F, "143 强制下架-商品不存在", "PUT", "/api/admin/products/999999/off-shelf",
     headers=bearer("adminToken"),
     **ok(404, 40401))

case(F, "144 强制下架-管理员下架商品成功", "PUT", "/api/admin/products/{{maxPriceProductId}}/off-shelf",
     headers=bearer("adminToken"),
     **ok(checks=[chk("返回成功", "body.code === 200")]))

case(F, "145 强制下架-普通用户无权操作", "PUT", "/api/admin/products/{{minPriceProductId}}/off-shelf",
     headers=bearer("buyerToken"),
     **ok(403, 40303))

case(F, "146 用户列表-管理员查询", "GET", "/api/admin/users",
     headers=bearer("adminToken"),
     query={"page": "1", "size": "20"},
     **ok(checks=[chk("至少包含 4 个初始账号", "body.data.total >= 4"),
                  chk("返回角色与状态字段", "body.data.records.every(u => 'role' in u && 'status' in u)")]))

case(F, "147 用户列表-按关键词搜索", "GET", "/api/admin/users",
     headers=bearer("adminToken"),
     query={"keyword": "buyer01"},
     **ok(checks=[chk("搜索到 buyer01", "body.data.records.some(u => u.username === 'buyer01')")]))

case(F, "148 用户禁用-管理员不能禁用自己", "PUT", "/api/admin/users/1/status",
     headers=bearer("adminToken"),
     json={"status": 0},
     **ok(403, 40301, [chk("提示不能修改自己", "body.message.indexOf('自己') >= 0")]))

case(F, "149 用户禁用-禁用新注册用户", "PUT", "/api/admin/users/{{newUserId}}/status",
     headers=bearer("adminToken"),
     json={"status": 0},
     **ok(checks=[chk("状态为已禁用", "body.data.status === 0")]))

case(F, "150 用户禁用-被禁用用户无法调用接口", "GET", "/api/auth/me",
     headers=bearer("newUserToken"),
     **ok(403, 40302, [chk("提示账号已被禁用", "body.message.indexOf('禁用') >= 0")]))

case(F, "151 用户禁用-被禁用用户无法登录", "POST", "/api/auth/login",
     json={"username": "{{newUsername}}", "password": "123456"},
     **ok(403, 40302))

case(F, "152 用户启用-重新启用成功", "PUT", "/api/admin/users/{{newUserId}}/status",
     headers=bearer("adminToken"),
     json={"status": 1},
     **ok(checks=[chk("状态为正常", "body.data.status === 1")]))

case(F, "153 用户启用-启用后可正常访问", "GET", "/api/auth/me",
     headers=bearer("newUserToken"),
     **ok(checks=[chk("恢复正常访问", "body.code === 200")]))

case(F, "154 用户禁用-状态值非法", "PUT", "/api/admin/users/{{newUserId}}/status",
     headers=bearer("adminToken"),
     json={"status": 5},
     **ok(400, 40001))

case(F, "155 分类维护-新增分类成功", "POST", "/api/admin/categories",
     headers=bearer("adminToken"),
     json={"name": "接口测试分类-{{$timestamp}}", "sort": 88},
     **ok(checks=[chk("返回分类 ID", "typeof body.data.id === 'number'")],
          captures={"categoryId": "String(body.data.id)"}))

case(F, "156 分类维护-新增分类名称重复", "POST", "/api/admin/categories",
     headers=bearer("adminToken"),
     json={"name": "教材书籍", "sort": 1},
     **ok(400, 40001, [chk("提示分类名称已存在", "body.message.indexOf('已存在') >= 0")]))

case(F, "157 分类维护-修改分类成功", "PUT", "/api/admin/categories/{{categoryId}}",
     headers=bearer("adminToken"),
     json={"name": "接口测试分类-已修改", "sort": 89},
     **ok(checks=[chk("名称已更新", "body.data.name === '接口测试分类-已修改'")]))

case(F, "158 分类维护-删除有商品的分类", "DELETE", "/api/admin/categories/1",
     headers=bearer("adminToken"),
     **ok(409, 40902, [chk("提示分类下有商品", "body.message.indexOf('商品') >= 0")]))

case(F, "159 分类维护-删除空分类成功", "DELETE", "/api/admin/categories/{{categoryId}}",
     headers=bearer("adminToken"),
     **ok(checks=[chk("返回成功", "body.code === 200")]))

case(F, "160 分类维护-普通用户无权新增", "POST", "/api/admin/categories",
     headers=bearer("buyerToken"),
     json={"name": "越权分类", "sort": 1},
     **ok(403, 40303))

case(F, "161 订单超时扫描-管理员手动触发", "POST", "/api/admin/orders/timeout-scan",
     headers=bearer("adminToken"),
     description="与后台定时任务同一段逻辑；配合 SQL 把订单 expire_at 改成过去时间即可验证超时关闭",
     **ok(checks=[chk("返回关闭订单数", "typeof body.data.closedCount === 'number'")]))

case(F, "162 订单超时扫描-普通用户无权触发", "POST", "/api/admin/orders/timeout-scan",
     headers=bearer("buyerToken"),
     **ok(403, 40303))


def build_request(case_item):
    path_parts = [part for part in case_item["path"].split("/") if part]
    raw_url = "{{baseUrl}}" + case_item["path"]
    if case_item["query"]:
        raw_url += "?" + "&".join("{0}={1}".format(k, v) for k, v in case_item["query"].items())

    url = {"raw": raw_url, "host": ["{{baseUrl}}"], "path": path_parts}
    if case_item["query"]:
        url["query"] = [{"key": k, "value": str(v)} for k, v in case_item["query"].items()]

    headers = [{"key": "Accept", "value": "application/json"}]
    for key, value in case_item["headers"].items():
        headers.append({"key": key, "value": value})

    body = None
    if case_item["json"] is not None:
        body = {"mode": "raw", "raw": json.dumps(case_item["json"], ensure_ascii=False, indent=2),
                "options": {"raw": {"language": "json"}}}
        headers.append({"key": "Content-Type", "value": "application/json"})
    elif case_item["raw"] is not None:
        body = {"mode": "raw", "raw": case_item["raw"], "options": {"raw": {"language": "json"}}}
        headers.append({"key": "Content-Type", "value": "application/json"})
    elif case_item["formdata"]:
        body = {"mode": "formdata", "formdata": case_item["formdata"]}

    request = {"method": case_item["method"], "header": headers, "url": url}
    if body:
        request["body"] = body
    if case_item["description"]:
        request["description"] = case_item["description"]

    exec_lines = []
    if case_item["status"]:
        exec_lines.append("pm.test('HTTP 状态码 {0}', function () {{".format(case_item["status"]))
        exec_lines.append("  pm.response.to.have.status({0});".format(case_item["status"]))
        exec_lines.append("});")
        exec_lines.append("")
    exec_lines.append("const body = pm.response.json();")
    if case_item["code"]:
        exec_lines.append("pm.test('业务返回码 {0}', function () {{".format(case_item["code"]))
        exec_lines.append("  pm.expect(body.code).to.eql({0});".format(case_item["code"]))
        exec_lines.append("});")
    for label, js in case_item["checks"]:
        exec_lines.append("pm.test('{0}', function () {{".format(label))
        exec_lines.append("  pm.expect({0}).to.be.true;".format(js))
        exec_lines.append("});")
    if case_item["captures"]:
        exec_lines.append("")
        for var, expression in case_item["captures"].items():
            exec_lines.append("pm.environment.set('{0}', {1});".format(var, expression))

    events = [{"listen": "test", "script": {"type": "text/javascript", "exec": exec_lines}}]
    if case_item["pre"]:
        events.insert(0, {"listen": "prerequest",
                          "script": {"type": "text/javascript", "exec": [case_item["pre"]]}})

    return {"name": case_item["name"], "request": request, "event": events, "response": []}


DESCRIPTION = (
    "校园二手交易平台（Vue 3 + Spring Boot + MySQL）功能测试用接口集合。\n\n"
    "- 执行顺序：请按文件夹顺序整体运行（认证 → 用户 → 分类 → 商品 → 收藏 → 订单 → 消息 → 管理端），"
    "集合内部通过环境变量串联 token、商品 ID、订单 ID。\n"
    "- 每条请求都带断言：HTTP 状态码 + 业务返回码 + 关键业务字段。\n"
    "- 上传图片用例依赖 docs/postman/testdata 下的测试素材，请在项目根目录执行 newman（或双击「跑接口测试-newman.bat」）。\n"
    "- 超时关闭场景：先用 SQL 把某订单的 expire_at 改成过去时间（见 docs/测试方案.md），再调用管理端超时扫描接口。\n"
)


def main():
    folders = {}
    order = []
    for case_item in cases:
        folder = case_item["folder"]
        if folder not in folders:
            folders[folder] = []
            order.append(folder)
        folders[folder].append(build_request(case_item))

    collection = {
        "info": {
            "_postman_id": "campus-trade-collection-2026",
            "name": "校园二手交易平台-接口测试集合",
            "description": DESCRIPTION,
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
        },
        "item": [{"name": folder, "item": folders[folder]} for folder in order],
        "variable": [{"key": "baseUrl", "value": "http://localhost:8080"}],
    }

    environment_keys = ["baseUrl", "buyerToken", "sellerToken", "seller2Token", "adminToken",
                        "newUserToken", "newUsername", "newUserId", "buyerUserId", "sellerUserId",
                        "productId", "minPriceProductId", "maxPriceProductId", "cancelOrderId",
                        "cancelProductId", "orderId", "categoryId", "uploadedImageUrl",
                        "seededProductId", "seededViewCount", "sellerUnread"]
    environment = {
        "id": "campus-trade-local-env",
        "name": "校园二手交易平台-本地环境",
        "values": [{"key": key, "value": "http://localhost:8080" if key == "baseUrl" else "",
                    "type": "default", "enabled": True} for key in environment_keys],
        "_postman_variable_scope": "environment",
    }

    os.makedirs(OUT_DIR, exist_ok=True)
    collection_path = os.path.join(OUT_DIR, "campus-trade.postman_collection.json")
    environment_path = os.path.join(OUT_DIR, "campus-trade.postman_environment.json")
    with open(collection_path, "w", encoding="utf-8") as fh:
        json.dump(collection, fh, ensure_ascii=False, indent=2)
    with open(environment_path, "w", encoding="utf-8") as fh:
        json.dump(environment, fh, ensure_ascii=False, indent=2)

    total = sum(len(items) for items in folders.values())
    print("已生成集合：{0}".format(collection_path))
    print("已生成环境：{0}".format(environment_path))
    print("文件夹 {0} 个，请求 {1} 个".format(len(order), total))
    for folder in order:
        print("  {0}: {1} 个请求".format(folder, len(folders[folder])))


if __name__ == "__main__":
    main()
