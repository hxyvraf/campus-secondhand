# 重新打包后端（会先停掉正在运行的后端服务，避免 jar 被占用）
. (Join-Path $PSScriptRoot 'common.ps1')

Write-Title '校园二手交易平台 - 重新打包后端'

if (Get-BackendProcess) {
    Write-Host '检测到后端服务正在运行，先停止它…'
    Stop-BackendService | Out-Null
}

Push-Location $Script:BackendDir
try {
    mvn -B clean package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw 'Maven 打包失败' }
} finally {
    Pop-Location
}

Write-Host ''
Write-Host "打包完成：$Script:JarPath" -ForegroundColor Green
Write-Host '双击「启动后端.bat」即可启动新版本。'
