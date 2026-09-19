# 把前端打包进后端：前端构建产物复制到 backend/src/main/resources/static，实现单端口访问
. (Join-Path $PSScriptRoot 'common.ps1')

Write-Title '校园二手交易平台 - 打包前端进后端'

Set-Location $Script:FrontendDir
if (-not (Test-Path (Join-Path $Script:FrontendDir 'node_modules'))) {
    Write-Host '未找到 node_modules，先安装依赖…'
    npm install
    if ($LASTEXITCODE -ne 0) { throw 'npm install 失败' }
}

Write-Host '1/3 构建前端（npm run build）…'
npm run build
if ($LASTEXITCODE -ne 0) { throw '前端构建失败' }

$staticDir = Join-Path $Script:BackendDir 'src\main\resources\static'
Write-Host '2/3 复制 dist 到后端 static 目录…'
if (Test-Path $staticDir) { Remove-Item $staticDir -Recurse -Force }
New-Item -ItemType Directory -Force -Path $staticDir | Out-Null
Copy-Item -Path (Join-Path $Script:FrontendDir 'dist\*') -Destination $staticDir -Recurse -Force

if (Get-BackendProcess) {
    Write-Host '  检测到后端在运行，先停止它以便重新打包…'
    Stop-BackendService | Out-Null
}

Write-Host '3/3 重新打包后端（此时前端已内置）…'
Push-Location $Script:BackendDir
try {
    mvn -B clean package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw 'Maven 打包失败' }
} finally {
    Pop-Location
}

Write-Host ''
Write-Host "完成！启动后端后直接访问 $Script:BackendUrl 即可打开完整页面（不需要单独启动前端）。" -ForegroundColor Green
