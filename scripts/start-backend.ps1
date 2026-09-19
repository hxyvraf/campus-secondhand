# 启动后端：jar 不存在时先自动打包，然后前台运行（关掉窗口即停止服务）
. (Join-Path $PSScriptRoot 'common.ps1')

Write-Title '校园二手交易平台 - 启动后端服务'

$existing = Get-BackendProcess
if ($existing) {
    Write-Host "后端已经在运行（进程 $($existing.ProcessId | Select-Object -First 1)），地址：$Script:BackendUrl" -ForegroundColor Yellow
    Write-Host '如需重启，请先关闭之前运行后端的窗口。'
    exit 0
}

if (-not (Test-Path $Script:JarPath)) {
    Write-Host '未找到 target\campus-trade.jar，开始打包（首次会联网下载 Maven 依赖）…'
    Push-Location $Script:BackendDir
    try {
        mvn -B package -DskipTests
        if ($LASTEXITCODE -ne 0) { throw 'Maven 打包失败' }
    } finally {
        Pop-Location
    }
} else {
    Write-Host "使用已有的 jar：$Script:JarPath"
}

Write-Host ''
Write-Host '首次启动会自动创建 campus_trade 数据库、建表并写入初始数据。'
Write-Host "接口地址：$Script:BackendUrl   Swagger 文档：$Script:BackendUrl/swagger-ui.html"
Write-Host '按 Ctrl+C 或直接关闭本窗口即可停止服务。'
Write-Host ''

Push-Location $Script:BackendDir
try {
    java -jar 'target\campus-trade.jar'
} finally {
    Pop-Location
}
