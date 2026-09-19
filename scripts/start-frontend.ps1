# 启动前端：首次自动 npm install，然后前台运行 Vite 开发服务器
. (Join-Path $PSScriptRoot 'common.ps1')

Write-Title '校园二手交易平台 - 启动前端服务'

Set-Location $Script:FrontendDir

if (-not (Test-Path (Join-Path $Script:FrontendDir 'node_modules'))) {
    Write-Host '未找到 node_modules，开始安装前端依赖（需要联网）…'
    npm install
    if ($LASTEXITCODE -ne 0) { throw 'npm install 失败' }
}

Write-Host ''
Write-Host "前端地址：$Script:FrontendUrl（Vite 会自动把 /api 请求代理到后端 $Script:BackendUrl）"
Write-Host '提示：请先在另一个窗口启动后端服务，否则页面上的数据会加载失败。'
Write-Host '按 Ctrl+C 或关闭本窗口即可停止前端。'
Write-Host ''

npm run dev
