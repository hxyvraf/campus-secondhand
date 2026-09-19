# 用 newman 执行 Postman 集合，生成 HTML 测试报告（需要先启动后端）
. (Join-Path $PSScriptRoot 'common.ps1')

Write-Title '校园二手交易平台 - 接口测试执行（newman）'

$collection = Join-Path $Script:ProjectRoot 'docs\postman\campus-trade.postman_collection.json'
$environment = Join-Path $Script:ProjectRoot 'docs\postman\campus-trade.postman_environment.json'
$reportDir = Join-Path $Script:ProjectRoot 'docs\测试执行证据'
$report = Join-Path $reportDir 'newman-report.html'
$jsonReport = Join-Path $reportDir 'newman-report.json'

if (-not (Test-Path $collection)) { throw "没有找到 Postman 集合文件：$collection" }

$newman = Get-Command newman.cmd -ErrorAction SilentlyContinue
if (-not $newman) { $newman = Get-Command newman -ErrorAction SilentlyContinue }
if (-not $newman) {
    Write-Host '没有找到 newman，正在安装（需要联网）…'
    npm install -g newman newman-reporter-htmlextra
    $newman = Get-Command newman.cmd -ErrorAction SilentlyContinue
}
if (-not $newman) { throw 'newman 安装失败，也可以直接双击「启动后端.bat」后用 Postman 手工导入集合执行' }

New-Item -ItemType Directory -Force -Path $reportDir | Out-Null

Write-Host "先在浏览器/命令行确认后端可访问：$Script:BackendUrl/api/categories"
if (-not (Wait-BackendReady -TimeoutSeconds 6)) {
    Write-Host '  提示：后端似乎没有启动，请先双击「启动后端.bat」。' -ForegroundColor Yellow
    exit 1
}

Write-Host ''
Write-Host '开始执行接口测试（约 1-3 分钟）…'
& $newman.Source run $collection -e $environment -r cli,htmlextra,json --reporter-htmlextra-export $report --reporter-json-export $jsonReport --timeout-request 15000
$code = $LASTEXITCODE

Write-Host ''
if (Test-Path $report) {
    Write-Host "HTML 报告：$report" -ForegroundColor Green
    Start-Process $report
}
if ($code -ne 0) {
    Write-Host '本次执行存在失败用例，请在报告中查看详情。' -ForegroundColor Yellow
} else {
    Write-Host '全部接口测试通过。' -ForegroundColor Green
}
