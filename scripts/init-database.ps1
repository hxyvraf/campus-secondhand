# 手动初始化数据库：不需要启动后端，直接用 mysql 客户端执行 schema.sql 与 data.sql
. (Join-Path $PSScriptRoot 'common.ps1')

Write-Title '校园二手交易平台 - 初始化数据库'

$schema = Join-Path $Script:BackendDir 'src\main\resources\db\schema.sql'
$data = Join-Path $Script:BackendDir 'src\main\resources\db\data.sql'

Write-Host '1/4 创建数据库（如果不存在）…'
Invoke-Mysql -Sql "CREATE DATABASE IF NOT EXISTS $Script:DbName DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"

Write-Host '2/4 执行建表脚本 schema.sql…'
Invoke-Mysql -Database $Script:DbName -Sql "source $($schema -replace '\\', '/')"

Write-Host '3/4 写入初始数据 data.sql…'
Invoke-Mysql -Database $Script:DbName -Sql "source $($data -replace '\\', '/')"

Write-Host '4/4 校验数据…'
Invoke-Mysql -Database $Script:DbName -Sql @"
SELECT '用户' AS 表名, COUNT(*) AS 条数 FROM ``user``
UNION ALL SELECT '分类', COUNT(*) FROM category
UNION ALL SELECT '商品', COUNT(*) FROM product
UNION ALL SELECT '商品图片', COUNT(*) FROM product_image
UNION ALL SELECT '收藏', COUNT(*) FROM favorite
UNION ALL SELECT '订单', COUNT(*) FROM trade_order
UNION ALL SELECT '消息', COUNT(*) FROM message;
"@

Write-Host ''
Write-Host '数据库初始化完成。' -ForegroundColor Green
