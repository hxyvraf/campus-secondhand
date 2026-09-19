# 重置数据：删除 campus_trade 数据库并重启后端，恢复到初始数据（6 个分类 / 4 个账号 / 20 件商品）
. (Join-Path $PSScriptRoot 'common.ps1')

Write-Title '校园二手交易平台 - 重置数据'

Write-Host '该操作会删除数据库 campus_trade 中的全部数据（包括你测试时注册的账号、发布的商品、订单）。'
$answer = Read-Host '确认继续请输入 Y，其他任意键取消'
if ($answer -ne 'Y' -and $answer -ne 'y') {
    Write-Host '已取消。'
    exit 0
}

Write-Host ''
Write-Host '1/3 停止后端服务…'
Stop-BackendService | Out-Null

Write-Host '2/3 删除数据库…'
Invoke-Mysql -Sql "DROP DATABASE IF EXISTS $Script:DbName;"
Write-Host "  数据库 $Script:DbName 已删除"

Write-Host '3/3 重新启动后端（会自动重建表结构并写入初始数据）…'
if (-not (Test-Path $Script:JarPath)) {
    Write-Host '  未找到 jar，请先双击「启动后端.bat」完成首次打包。' -ForegroundColor Yellow
    exit 1
}

Start-Process -FilePath 'powershell' -ArgumentList @('-NoExit', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', (Join-Path $PSScriptRoot 'start-backend.ps1'))
if (Wait-BackendReady -TimeoutSeconds 120) {
    Write-Host ''
    Write-Host '重置完成，数据已恢复到初始状态。' -ForegroundColor Green
    Write-Host "可用账号：admin / seller01 / seller02 / buyer01，密码均为 123456"
} else {
    Write-Host '后端重启超时，请查看后端窗口日志。' -ForegroundColor Yellow
}
