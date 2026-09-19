# 后端接口冒烟脚本（父线程自用，交付物里不含）：
# 覆盖 5 大模块的正常流程、异常流程与边界场景，输出每条用例的真实状态码与业务码。
$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8080'
$script:pass = 0
$script:fail = 0
$results = New-Object System.Collections.ArrayList

function Invoke-Api {
    param([string]$Method, [string]$Path, $Body, [string]$Token, [string]$RawBody, [string]$ContentType)
    $url = "$base$Path"
    $client = New-Object System.Net.WebClient
    $client.Encoding = [System.Text.Encoding]::UTF8
    if ($Token) { $client.Headers.Add('Authorization', "Bearer $Token") }
    if ($ContentType) {
        $client.Headers.Add('Content-Type', $ContentType)
    } elseif ($Method -ne 'GET') {
        $client.Headers.Add('Content-Type', 'application/json; charset=utf-8')
    }
    try {
        if ($null -ne $Body) {
            $payload = ($Body | ConvertTo-Json -Depth 8 -Compress)
            $text = $client.UploadString($url, $Method, $payload)
        } elseif ($RawBody) {
            $text = $client.UploadString($url, $Method, $RawBody)
        } else {
            $text = if ($Method -eq 'GET') { $client.DownloadString($url) } else { $client.UploadString($url, $Method, '') }
        }
        return [pscustomobject]@{ status = 200; body = ($text | ConvertFrom-Json); raw = $text }
    } catch [System.Net.WebException] {
        $response = $_.Exception.Response
        if ($null -eq $response) { throw }
        $status = [int]$response.StatusCode
        $stream = $response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8)
        $text = $reader.ReadToEnd()
        $json = $null
        try { $json = $text | ConvertFrom-Json } catch { }
        return [pscustomobject]@{ status = $status; body = $json; raw = $text }
    } finally {
        $client.Dispose()
    }
}

function Check {
    param([string]$Name, $Actual, $Expect)
    $ok = ($Actual -eq $Expect)
    if ($ok) { $script:pass++ } else { $script:fail++ }
    $flag = if ($ok) { 'PASS' } else { 'FAIL' }
    $line = "[{0}] {1}  实际={2} 期望={3}" -f $flag, $Name, $Actual, $Expect
    Write-Host $line
    [void]$results.Add($line)
}

function CheckTrue {
    param([string]$Name, $Condition, [string]$Detail)
    $ok = [bool]$Condition
    if ($ok) { $script:pass++ } else { $script:fail++ }
    $flag = if ($ok) { 'PASS' } else { 'FAIL' }
    $line = "[{0}] {1}  {2}" -f $flag, $Name, $Detail
    Write-Host $line
    [void]$results.Add($line)
}

Write-Host '=== 1. 匿名接口与登录 ==='
$r = Invoke-Api GET '/api/categories'
Check '分类列表返回 200' $r.status 200
Check '分类业务码 200' $r.body.code 200
CheckTrue '分类数量不少于 6' ($r.body.data.Count -ge 6) "count=$($r.body.data.Count)"

$r = Invoke-Api GET '/api/products?page=1&size=5'
Check '商品列表匿名可访问' $r.status 200
Check '商品列表只返回 5 条' $r.body.data.records.Count 5
CheckTrue '在售商品总数大于 15' ($r.body.data.total -gt 15) "total=$($r.body.data.total)"

$r = Invoke-Api GET '/api/auth/me'
Check '未登录访问 /api/auth/me 返回 401' $r.status 401
Check '未登录业务码 40101' $r.body.code 40101

$r = Invoke-Api GET '/api/products/9999'
Check '商品不存在返回 404' $r.status 404
Check '商品不存在业务码 40401' $r.body.code 40401

$r = Invoke-Api POST '/api/auth/login' @{ username = 'buyer01'; password = 'wrong-password' }
Check '密码错误返回 401' $r.status 401
Check '密码错误业务码 40102' $r.body.code 40102

$login = Invoke-Api POST '/api/auth/login' @{ username = 'buyer01'; password = '123456' }
Check '买家登录成功' $login.status 200
$buyerToken = $login.body.data.token
CheckTrue '登录返回 token' ($buyerToken.Length -gt 20) 'token 长度正常'
Check '登录返回昵称' $login.body.data.user.nickname '苏晴'

$loginSeller = Invoke-Api POST '/api/auth/login' @{ username = 'seller01'; password = '123456' }
$sellerToken = $loginSeller.body.data.token
$loginSeller2 = Invoke-Api POST '/api/auth/login' @{ username = 'seller02'; password = '123456' }
$seller2Token = $loginSeller2.body.data.token
$loginAdmin = Invoke-Api POST '/api/auth/login' @{ username = 'admin'; password = '123456' }
$adminToken = $loginAdmin.body.data.token
Check '卖家/管理员登录成功' ($loginSeller.status + $loginSeller2.status + $loginAdmin.status) 600

$r = Invoke-Api GET '/api/auth/me' $null $buyerToken
Check '携带 token 获取当前用户' $r.body.data.username 'buyer01'

Write-Host ''
Write-Host '=== 2. 注册与参数校验 ==='
$suffix = Get-Random -Minimum 1000 -Maximum 9999
$newUser = "stu$suffix"
$r = Invoke-Api POST '/api/auth/register' @{ username = $newUser; password = '123456'; nickname = '新同学'; phone = '13800138000'; email = 'stu@campus.com'; school = '南昌职业大学' }
Check '新用户注册成功' $r.status 200
Check '注册返回用户名' $r.body.data.username $newUser

$r = Invoke-Api POST '/api/auth/register' @{ username = $newUser; password = '123456'; nickname = '重复注册' }
Check '重复用户名返回 409' $r.status 409
Check '重复用户名业务码 40904' $r.body.code 40904

$r = Invoke-Api POST '/api/auth/register' @{ username = 'ab'; password = '123'; nickname = '' }
Check '注册参数非法返回 400' $r.status 400
Check '参数错误业务码 40001' $r.body.code 40001
CheckTrue '参数错误返回字段明细' ($r.body.data.Count -ge 3) "明细条数=$($r.body.data.Count)"

Write-Host ''
Write-Host '=== 3. 商品发布与管理 ==='
$newProduct = Invoke-Api POST '/api/products' @{ title = '冒烟测试商品-接口自动化'; description = '由冒烟脚本创建'; price = 12.34; originalPrice = 30; categoryId = 2; conditionLevel = '九成新'; tradePlace = '图书馆一楼' } $seller2Token
Check '发布商品成功' $newProduct.status 200
$productId = $newProduct.body.data.id
CheckTrue '发布返回商品 ID' ($productId -gt 20) "id=$productId"

$r = Invoke-Api POST '/api/products' @{ title = '负价格商品'; price = -1; categoryId = 1; conditionLevel = '全新' } $seller2Token
Check '价格为负数返回 400' $r.status 400
$r = Invoke-Api POST '/api/products' @{ title = '零价格商品'; price = 0; categoryId = 1; conditionLevel = '全新' } $seller2Token
Check '价格为 0 返回 400' $r.status 400
$r = Invoke-Api POST '/api/products' @{ title = '超范围价格'; price = 1000000; categoryId = 1; conditionLevel = '全新' } $seller2Token
Check '价格超上限返回 400' $r.status 400
$r = Invoke-Api POST '/api/products' @{ title = ('测' * 51); price = 10; categoryId = 1; conditionLevel = '全新' } $seller2Token
Check '标题超长返回 400' $r.status 400
$r = Invoke-Api POST '/api/products' @{ title = '成色非法'; price = 10; categoryId = 1; conditionLevel = '五成新' } $seller2Token
Check '成色非法返回 400' $r.status 400
$r = Invoke-Api POST '/api/products' @{ title = '分类不存在'; price = 10; categoryId = 9999; conditionLevel = '全新' } $seller2Token
Check '分类不存在返回 400' $r.status 400

$r = Invoke-Api PUT "/api/products/$productId" @{ title = '冒烟测试商品-被他人修改'; price = 5; categoryId = 2; conditionLevel = '八成新' } $buyerToken
Check '修改他人商品返回 403' $r.status 403
Check '越权业务码 40301' $r.body.code 40301

$r = Invoke-Api PATCH "/api/products/$productId/status" @{ status = 'OFF_SHELF' } $seller2Token
Check '商品下架成功' $r.status 200
$r = Invoke-Api PATCH "/api/products/$productId/status" @{ status = 'OFF_SHELF' } $seller2Token
Check '重复下架返回 409' $r.status 409
Check '状态非法流转业务码 40902' $r.body.code 40902
$r = Invoke-Api PATCH "/api/products/$productId/status" @{ status = 'ON_SALE' } $seller2Token
Check '商品重新上架成功' $r.status 200
$r = Invoke-Api PATCH "/api/products/$productId/status" @{ status = 'SOLD' } $seller2Token
Check '非法目标状态返回 400' $r.status 400

$r = Invoke-Api GET '/api/products/mine' $null $seller2Token
Check '我发布的商品可查询' $r.status 200
CheckTrue '我发布的商品包含刚发布的' (($r.body.data.records | Where-Object { $_.id -eq $productId }).Count -eq 1) "records=$($r.body.data.records.Count)"

Write-Host ''
Write-Host '=== 4. 搜索、收藏与分页边界 ==='
$r = Invoke-Api GET '/api/products?keyword=教材&size=20'
CheckTrue '关键词搜索命中结果不为空' ($r.body.data.total -ge 1) "total=$($r.body.data.total)"
$r = Invoke-Api GET '/api/products?categoryId=2&size=20'
$otherCategory = @($r.body.data.records | Where-Object { $_.categoryId -ne 2 })
CheckTrue '按分类筛选生效' ($otherCategory.Count -eq 0) "records=$($r.body.data.records.Count) 非本分类=$($otherCategory.Count)"
$r = Invoke-Api GET '/api/products?minPrice=30&maxPrice=60&sort=priceAsc&size=20'
CheckTrue '价格区间与升序排序生效' (($r.body.data.records | Measure-Object -Property price -Minimum).Minimum -ge 30) "最小值=$((($r.body.data.records | Measure-Object -Property price -Minimum).Minimum))"
$r = Invoke-Api GET '/api/products?minPrice=100&maxPrice=1'
Check '最低价大于最高价返回 400' $r.status 400
$r = Invoke-Api GET '/api/products?page=0'
Check '页码为 0 返回 400' $r.status 400
$r = Invoke-Api GET '/api/products?size=101'
Check '每页超过 100 条返回 400' $r.status 400
$r = Invoke-Api GET '/api/products?page=999&size=10'
Check '超出总页数返回 200 空列表' $r.status 200
Check '超页返回空 records' $r.body.data.records.Count 0
$r = Invoke-Api GET '/api/products?sort=unknown'
Check '非法排序参数返回 400' $r.status 400

$r = Invoke-Api POST '/api/favorites/1' $null $buyerToken
CheckTrue '收藏商品（首次或已收藏都返回 200）' ($r.status -eq 200) "status=$($r.status) message=$($r.body.message)"
$r = Invoke-Api POST '/api/favorites/1' $null $buyerToken
Check '重复收藏幂等返回 200' $r.status 200
CheckTrue '重复收藏提示已收藏' ($r.body.message -like '*已经收藏*') $r.body.message
$r = Invoke-Api GET '/api/favorites/1/status' $null $buyerToken
Check '查询收藏状态为 true' $r.body.data.favorited $true
$r = Invoke-Api DELETE '/api/favorites/1' $null $buyerToken
Check '取消收藏成功' $r.status 200
$r = Invoke-Api GET '/api/favorites/1/status' $null $buyerToken
Check '取消后收藏状态为 false' $r.body.data.favorited $false
$r = Invoke-Api POST '/api/favorites/99999' $null $buyerToken
Check '收藏不存在的商品返回 404' $r.status 404
$r = Invoke-Api GET '/api/favorites' $null $buyerToken
Check '我的收藏列表可查询' $r.status 200

Write-Host ''
Write-Host '=== 5. 订单全流程 ==='
$r = Invoke-Api POST '/api/orders' @{ productId = $productId; remark = '冒烟测试下单' } $buyerToken
Check '买家下单成功' $r.status 200
$orderId = $r.body.data.id
Check '新订单状态为待卖家确认' $r.body.data.status 'PENDING_CONFIRM'
$r = Invoke-Api GET "/api/products/$productId"
Check '下单后商品变为交易中' $r.body.data.status 'LOCKED'

$r = Invoke-Api POST '/api/orders' @{ productId = $productId; remark = '重复下单' } $buyerToken
Check '重复下单返回 409' $r.status 409
Check '重复下单业务码 40901' $r.body.code 40901

$r = Invoke-Api POST '/api/orders' @{ productId = $productId } $seller2Token
Check '购买自己的商品返回 409' $r.status 409
Check '商品不可交易业务码 40903' $r.body.code 40903

$r = Invoke-Api GET "/api/orders/$orderId" $null $seller2Token
Check '卖家可查看订单详情' $r.status 200
$r = Invoke-Api GET "/api/orders/$orderId" $null $sellerToken
Check '无关用户查看订单返回 403' $r.status 403

$r = Invoke-Api POST "/api/orders/$orderId/confirm" $null $buyerToken
Check '买家确认订单返回 403' $r.status 403
$r = Invoke-Api POST "/api/orders/$orderId/confirm" $null $seller2Token
Check '卖家确认订单成功' $r.status 200
Check '订单状态变为交易中' $r.body.data.status 'CONFIRMED'
$r = Invoke-Api POST "/api/orders/$orderId/confirm" $null $seller2Token
Check '重复确认返回 409' $r.status 409
$r = Invoke-Api POST "/api/orders/$orderId/finish" $null $seller2Token
Check '卖家确认完成返回 403' $r.status 403
$r = Invoke-Api POST "/api/orders/$orderId/finish" $null $buyerToken
Check '买家确认完成成功' $r.status 200
Check '订单状态变为已完成' $r.body.data.status 'COMPLETED'
$r = Invoke-Api GET "/api/products/$productId"
Check '完成后商品变为已售出' $r.body.data.status 'SOLD'
$r = Invoke-Api POST "/api/orders/$orderId/cancel" @{ reason = '想取消' } $buyerToken
Check '已完成订单不能取消' $r.status 409
Check '状态非法流转业务码 40902' $r.body.code 40902

$r = Invoke-Api GET '/api/orders?role=buyer&size=20' $null $buyerToken
Check '我的订单（买家视角）可查询' $r.status 200
$r = Invoke-Api GET '/api/orders?role=seller&status=COMPLETED&size=20' $null $seller2Token
CheckTrue '卖家已完成订单不为空' ($r.body.data.total -ge 1) "total=$($r.body.data.total)"

$cancelProduct = Invoke-Api POST '/api/products' @{ title = '冒烟测试-取消订单用商品'; description = '取消流程用'; price = 8.88; categoryId = 6; conditionLevel = '八成新' } $seller2Token
$cancelProductId = $cancelProduct.body.data.id
$cancelOrder = Invoke-Api POST '/api/orders' @{ productId = $cancelProductId } $buyerToken
$cancelOrderId = $cancelOrder.body.data.id
$r = Invoke-Api POST "/api/orders/$cancelOrderId/cancel" @{ reason = '临时不想要了' } $buyerToken
Check '买家取消订单成功' $r.status 200
Check '订单状态变为已取消' $r.body.data.status 'CANCELED'
$r = Invoke-Api GET "/api/products/$cancelProductId"
Check '取消后商品回到在售' $r.body.data.status 'ON_SALE'

Write-Host ''
Write-Host '=== 6. 消息通知 ==='
$r = Invoke-Api GET '/api/messages/unread-count' $null $seller2Token
Check '未读消息数可查询' $r.status 200
$unread = $r.body.data.count
CheckTrue '收到下单/取消等消息' ($unread -ge 1) "未读=$unread"
$r = Invoke-Api GET '/api/messages?size=20' $null $seller2Token
Check '消息列表可查询' $r.status 200
$messageId = $r.body.data.records[0].id
$r = Invoke-Api PUT "/api/messages/$messageId/read" $null $seller2Token
Check '标记单条已读成功' $r.status 200
$r = Invoke-Api PUT "/api/messages/$messageId/read" $null $buyerToken
Check '标记他人消息返回 403' $r.status 403
$r = Invoke-Api PUT '/api/messages/read-all' $null $seller2Token
Check '全部已读成功' $r.status 200
$r = Invoke-Api GET '/api/messages/unread-count' $null $seller2Token
Check '全部已读后未读数为 0' $r.body.data.count 0

Write-Host ''
Write-Host '=== 7. 管理端与账号状态 ==='
$r = Invoke-Api GET '/api/admin/users' $null $buyerToken
Check '普通用户访问管理端返回 403' $r.status 403
Check '需要管理员业务码 40303' $r.body.code 40303
$r = Invoke-Api GET '/api/admin/users?keyword=buyer' $null $adminToken
Check '管理员查询用户列表成功' $r.status 200
CheckTrue '搜索到 buyer01' (($r.body.data.records | Where-Object { $_.username -eq 'buyer01' }).Count -eq 1) "records=$($r.body.data.records.Count)"
$r = Invoke-Api GET '/api/admin/products?status=OFF_SHELF' $null $adminToken
Check '管理员按状态查询商品成功' $r.status 200
CheckTrue '存在已下架商品' ($r.body.data.total -ge 1) "total=$($r.body.data.total)"
$r = Invoke-Api PUT '/api/admin/users/1/status' @{ status = 0 } $adminToken
Check '管理员不能禁用自己返回 403' $r.status 403

$r = Invoke-Api PUT '/api/admin/users/4/status' @{ status = 0 } $adminToken
Check '管理员禁用 buyer01 成功' $r.status 200
$r = Invoke-Api GET '/api/auth/me' $null $buyerToken
Check '被禁用用户接口返回 403' $r.status 403
Check '账号禁用业务码 40302' $r.body.code 40302
$r = Invoke-Api POST '/api/auth/login' @{ username = 'buyer01'; password = '123456' }
Check '被禁用用户无法登录' $r.status 403
$r = Invoke-Api PUT '/api/admin/users/4/status' @{ status = 1 } $adminToken
Check '重新启用 buyer01 成功' $r.status 200
$r = Invoke-Api GET '/api/auth/me' $null $buyerToken
Check '启用后恢复正常访问' $r.status 200

$r = Invoke-Api POST '/api/admin/orders/timeout-scan' $null $adminToken
Check '管理员手动触发超时扫描' $r.status 200

Write-Host ''
Write-Host '=== 8. 分类维护与图片上传 ==='
# 分类名带随机后缀，保证脚本可重复执行，不会因为上次残留数据互相干扰
$catName = "冒烟测试分类$suffix"
$r = Invoke-Api POST '/api/admin/categories' @{ name = $catName; sort = 88 } $adminToken
Check '新增分类成功' $r.status 200
$categoryId = $r.body.data.id
$r = Invoke-Api POST '/api/admin/categories' @{ name = $catName; sort = 88 } $adminToken
Check '重复分类名返回 400' $r.status 400
$r = Invoke-Api PUT "/api/admin/categories/$categoryId" @{ name = "$catName-改"; sort = 89 } $adminToken
Check '修改分类成功' $r.status 200
$r = Invoke-Api DELETE "/api/admin/categories/$categoryId" $null $adminToken
Check '删除空分类成功' $r.status 200
$r = Invoke-Api DELETE '/api/admin/categories/1' $null $adminToken
Check '删除有商品的分类返回 409' $r.status 409

$material = 'C:\Users\hxyvraf\Desktop\campus-secondhand\docs\测试素材\test-upload.png'
$uploadClient = New-Object System.Net.WebClient
$uploadClient.Headers.Add('Authorization', "Bearer $seller2Token")
$uploadResult = $uploadClient.UploadFile("$base/api/files/images", $material)
$uploadJson = [System.Text.Encoding]::UTF8.GetString($uploadResult) | ConvertFrom-Json
if ($uploadJson.code -eq 200) { $script:pass++; Write-Host "[PASS] 上传 png 图片成功 url=$($uploadJson.data.url)" } else { $script:fail++; Write-Host "[FAIL] 上传 png 图片失败" }
$uploadClient.Dispose()

$badClient = New-Object System.Net.WebClient
$badClient.Headers.Add('Authorization', "Bearer $seller2Token")
try {
    $badClient.UploadFile("$base/api/files/images", 'C:\Users\hxyvraf\Desktop\campus-secondhand\docs\测试素材\test-upload.txt') | Out-Null
    $script:fail++; Write-Host '[FAIL] 上传 txt 未被拦截'
} catch [System.Net.WebException] {
    $status = [int]$_.Exception.Response.StatusCode
    if ($status -eq 400) { $script:pass++; Write-Host '[PASS] 上传 txt 文件返回 400（40002 文件类型不支持）' } else { $script:fail++; Write-Host "[FAIL] 上传 txt 返回 $status" }
}
$badClient.Dispose()

$bigClient = New-Object System.Net.WebClient
$bigClient.Headers.Add('Authorization', "Bearer $seller2Token")
try {
    $bigClient.UploadFile("$base/api/files/images", 'C:\Users\hxyvraf\Desktop\campus-secondhand\docs\测试素材\test-upload-6mb.png') | Out-Null
    $script:fail++; Write-Host '[FAIL] 6MB 文件未被拦截'
} catch [System.Net.WebException] {
    $status = [int]$_.Exception.Response.StatusCode
    if ($status -eq 400) { $script:pass++; Write-Host '[PASS] 超过 5MB 的图片返回 400（40003 大小超限）' } else { $script:fail++; Write-Host "[FAIL] 6MB 文件返回 $status" }
}
$bigClient.Dispose()

Write-Host ''
Write-Host "================ 冒烟结果：PASS=$script:pass FAIL=$script:fail ================"
$results | Set-Content -Path 'C:\Users\hxyvraf\Desktop\campus-secondhand\docs\测试执行证据\smoke-result.txt' -Encoding UTF8
if ($script:fail -gt 0) { exit 1 }
