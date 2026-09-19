# 一键启动：分别弹出「后端」「前端」两个窗口
. (Join-Path $PSScriptRoot 'common.ps1')

Write-Title '校园二手交易平台 - 一键启动'

$backendScript = Join-Path $PSScriptRoot 'start-backend.ps1'
$frontendScript = Join-Path $PSScriptRoot 'start-frontend.ps1'

Start-Process -FilePath 'powershell' -ArgumentList @('-NoExit', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $backendScript)
Write-Host '已在新的窗口启动后端…'
Start-Sleep -Seconds 5
Start-Process -FilePath 'powershell' -ArgumentList @('-NoExit', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $frontendScript)
Write-Host '已在新的窗口启动前端…'

Write-Host ''
Write-Host '等待后端就绪（首次启动需要建库建表，约 10-20 秒）…'
if (Wait-BackendReady -TimeoutSeconds 120) {
    Write-Host "后端就绪：$Script:BackendUrl" -ForegroundColor Green
} else {
    Write-Host '后端启动较慢或失败，请查看「后端」窗口中的日志。' -ForegroundColor Yellow
}

Write-Host ''
Write-Host "前端地址：$Script:FrontendUrl" -ForegroundColor Green
Write-Host '浏览器会自动打开该地址，若没有打开请手动访问。'
Start-Process $Script:FrontendUrl
