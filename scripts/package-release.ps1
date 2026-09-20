# ============================================================
# 打包「解压即用」发行包（zip）
#
# 用法：powershell -NoProfile -ExecutionPolicy Bypass -File scripts\package-release.ps1
# 可选参数：
#   -Version 1.0.0     版本号，决定 zip 文件名与包内根目录名（默认 1.0.0）
#   -OutputDir <目录>  输出目录（默认 <项目>\_release）
#   -Rebuild           打包前先执行 mvn -B clean package -DskipTests
#
# 产物：<OutputDir>\campus-secondhand-v<版本>-java17.zip
#       <OutputDir>\SHA256.txt、<OutputDir>\文件清单.txt
# 包内 = 全量源码 + docs + 预构建 jar（前端已内嵌）+ 双击入口 + 使用说明
# ============================================================

[CmdletBinding()]
param(
    [string]$Version = '1.0.0',
    [string]$OutputDir,
    [switch]$Rebuild
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem | Out-Null

function Write-Title { param([string]$Text) Write-Host ''; Write-Host "===== $Text =====" -ForegroundColor Cyan }
function Write-Ok { param([string]$Text) Write-Host ("  [OK] {0}" -f $Text) -ForegroundColor Green }

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$BackendDir = Join-Path $ProjectRoot 'backend'
$JarPath = Join-Path $BackendDir 'target\campus-trade.jar'
if (-not $OutputDir) { $OutputDir = Join-Path $ProjectRoot '_release' }

$PackageName = "campus-secondhand-$Version"
$ZipName = "campus-secondhand-v$Version-java17.zip"
$ZipPath = Join-Path $OutputDir $ZipName

function Get-JarInfo {
    param([string]$Path)
    $zip = [System.IO.Compression.ZipFile]::OpenRead($Path)
    try {
        $major = $null
        $classEntry = $zip.Entries | Where-Object { $_.FullName -eq 'BOOT-INF/classes/com/campus/trade/CampusTradeApplication.class' } | Select-Object -First 1
        if ($classEntry) {
            $stream = $classEntry.Open()
            try {
                $buffer = New-Object byte[] 8
                $read = $stream.Read($buffer, 0, 8)
                if ($read -eq 8) { $major = ([int]$buffer[6] * 256) + [int]$buffer[7] }
            } finally { $stream.Dispose() }
        }
        $staticFiles = ($zip.Entries | Where-Object { $_.FullName -like 'BOOT-INF/classes/static/*' -and $_.Length -gt 0 }).Count
        $seedImages = ($zip.Entries | Where-Object { $_.FullName -like 'BOOT-INF/classes/seed-images/*' -and $_.Length -gt 0 }).Count
        return @{ MajorVersion = $major; StaticFiles = $staticFiles; SeedImages = $seedImages }
    } finally { $zip.Dispose() }
}

function Get-PackageViolations {
    param([string]$Root)
    $violations = New-Object System.Collections.Generic.List[string]
    foreach ($item in (Get-ChildItem -LiteralPath $Root -Recurse -Force)) {
        $relative = $item.FullName.Substring($Root.Length).TrimStart('\')
        if ($relative -match '(^|\\)node_modules(\\|$)') { $violations.Add("多余目录：$relative") }
        elseif ($relative -match '(^|\\)\.git(\\|$)') { $violations.Add("多余目录：$relative") }
        elseif ($relative -match '(^|\\)_agent_scratch(\\|$)') { $violations.Add("多余目录：$relative") }
        elseif ($relative -match '(^|\\)backend\\uploads(\\|$)') { $violations.Add("运行时目录：$relative") }
        elseif ($relative -match '(^|\\)backend\\config(\\|$)') { $violations.Add("本机配置：$relative") }
        elseif (-not $item.PSIsContainer -and $item.Name -like '*.log') { $violations.Add("日志文件：$relative") }
    }
    return $violations
}

Write-Title '校园二手交易平台 - 打包发行版'

if ($Rebuild) {
    Write-Host '先重新构建后端（mvn -B clean package -DskipTests）…'
    Push-Location $BackendDir
    try {
        & mvn -B clean package -DskipTests
        if ($LASTEXITCODE -ne 0) { throw 'Maven 打包失败' }
    } finally { Pop-Location }
}

if (-not (Test-Path -LiteralPath $JarPath)) {
    throw "找不到 $JarPath，请先执行 backend 的 mvn clean package（或加上 -Rebuild 参数）"
}

$jarInfo = Get-JarInfo -Path $JarPath
if ($jarInfo.MajorVersion -ne 61) {
    throw ("jar 的字节码版本是 {0}（需要 61 = Java 17）。请确认 backend\pom.xml 里的 java.version 为 17 后重新打包。" -f $jarInfo.MajorVersion)
}
Write-Ok ("后端 jar 检查通过：字节码 Java 17，内嵌静态资源 {0} 个、示例图 {1} 张" -f $jarInfo.StaticFiles, $jarInfo.SeedImages)

$stageRoot = Join-Path ([System.IO.Path]::GetTempPath()) ('campus-release-' + [Guid]::NewGuid().ToString('N'))
$stagePackage = Join-Path $stageRoot $PackageName
New-Item -ItemType Directory -Force -Path $stagePackage | Out-Null

Write-Host '复制项目文件（排除 .git、node_modules、target、uploads、本机配置、日志）…'
$excludeDirs = @(
    (Join-Path $ProjectRoot '.git'),
    (Join-Path $ProjectRoot '.idea'),
    (Join-Path $ProjectRoot '_release'),
    (Join-Path $ProjectRoot '_agent_scratch'),
    (Join-Path $ProjectRoot 'frontend\node_modules'),
    (Join-Path $ProjectRoot 'backend\target'),
    (Join-Path $ProjectRoot 'backend\uploads'),
    (Join-Path $ProjectRoot 'backend\config')
)
$roboArgs = @($ProjectRoot, $stagePackage, '/E', '/R:1', '/W:1', '/NFL', '/NDL', '/NJH', '/NJS', '/NP', '/XF', '*.log', '/XD') + $excludeDirs
& robocopy @roboArgs | Out-Null
if ($LASTEXITCODE -ge 8) { throw "robocopy 复制失败（退出码 $LASTEXITCODE）" }

$jarTargetDir = Join-Path $stagePackage 'backend\target'
New-Item -ItemType Directory -Force -Path $jarTargetDir | Out-Null
Copy-Item -LiteralPath $JarPath -Destination (Join-Path $jarTargetDir 'campus-trade.jar') -Force
Write-Ok '预构建 jar 已放入 backend\target\campus-trade.jar'

$requiredFiles = @(
    '使用说明.txt',
    '启动系统.bat',
    '配置数据库.bat',
    'README.md',
    'backend\target\campus-trade.jar',
    'backend\src\main\resources\db\schema.sql',
    'backend\src\main\resources\db\data.sql',
    'scripts\configure-database.ps1',
    'scripts\start-standalone.ps1'
)
$missing = @()
foreach ($relative in $requiredFiles) {
    if (-not (Test-Path -LiteralPath (Join-Path $stagePackage $relative))) { $missing += $relative }
}
if ($missing.Count -gt 0) { throw ("包内缺少必要文件：{0}" -f ($missing -join '、')) }
Write-Ok '必要文件检查通过'

$violations = Get-PackageViolations -Root $stagePackage
if ($violations.Count -gt 0) {
    throw ("包内出现不应包含的内容：`r`n{0}" -f (($violations | Select-Object -First 10) -join "`r`n"))
}
Write-Ok '内容审计通过：无 node_modules / .git / uploads / 本机配置 / 日志'

if (-not (Test-Path -LiteralPath $OutputDir)) { New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null }
if (Test-Path -LiteralPath $ZipPath) { Remove-Item -LiteralPath $ZipPath -Force }

Write-Host '正在压缩（大约 1 分钟）…'

# 手工写入条目：zip 内路径统一用正斜杠（Windows PowerShell 的 CreateFromDirectory 会写成反斜杠，
# 部分解压工具会把整条路径当成一个文件名，因此这里自己控制）
$files = Get-ChildItem -LiteralPath $stagePackage -Recurse -File -Force | Sort-Object FullName
$zipStream = [System.IO.File]::Open($ZipPath, [System.IO.FileMode]::Create)
$archive = New-Object System.IO.Compression.ZipArchive($zipStream, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    foreach ($file in $files) {
        $relative = $file.FullName.Substring($stagePackage.Length).TrimStart('\').Replace('\', '/')
        $entry = $archive.CreateEntry(($PackageName + '/' + $relative), [System.IO.Compression.CompressionLevel]::Optimal)
        $entry.LastWriteTime = [System.DateTimeOffset]$file.LastWriteTime
        $entryStream = $entry.Open()
        try {
            $source = [System.IO.File]::OpenRead($file.FullName)
            try { $source.CopyTo($entryStream) } finally { $source.Dispose() }
        } finally { $entryStream.Dispose() }
    }
    foreach ($dir in (Get-ChildItem -LiteralPath $stagePackage -Recurse -Directory -Force)) {
        $hasChild = Get-ChildItem -LiteralPath $dir.FullName -Force | Select-Object -First 1
        if (-not $hasChild) {
            $relativeDir = $dir.FullName.Substring($stagePackage.Length).TrimStart('\').Replace('\', '/')
            $archive.CreateEntry(($PackageName + '/' + $relativeDir + '/')) | Out-Null
        }
    }
} finally {
    $archive.Dispose()
    $zipStream.Dispose()
}

$totalBytes = ($files | Measure-Object Length -Sum).Sum
$manifestLines = New-Object System.Collections.Generic.List[string]
$manifestLines.Add(("校园二手交易平台 发行包文件清单 - {0}" -f $PackageName))
$manifestLines.Add(("生成时间：{0}    文件数：{1}    原始大小：{2:N1} MB" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $files.Count, ($totalBytes / 1MB)))
$manifestLines.Add('')
foreach ($file in $files) {
    $manifestLines.Add(("{0,12:N0}  {1}" -f $file.Length, $file.FullName.Substring($stagePackage.Length).TrimStart('\')))
}
$utf8Bom = New-Object System.Text.UTF8Encoding($true)
[System.IO.File]::WriteAllText((Join-Path $OutputDir '文件清单.txt'), ($manifestLines -join "`r`n"), $utf8Bom)

$zipInfo = Get-Item -LiteralPath $ZipPath
$hash = (Get-FileHash -LiteralPath $ZipPath -Algorithm SHA256).Hash
[System.IO.File]::WriteAllText((Join-Path $OutputDir 'SHA256.txt'), ("{0}  {1}`r`n{2}  大小：{3:N1} MB`r`n" -f $hash, $zipInfo.Name, 'SHA256', ($zipInfo.Length / 1MB)), $utf8Bom)

$tempRoot = [System.IO.Path]::GetTempPath()
if ($stageRoot.StartsWith($tempRoot, [System.StringComparison]::OrdinalIgnoreCase) -and (Test-Path -LiteralPath $stageRoot)) {
    Remove-Item -LiteralPath $stageRoot -Recurse -Force
}

Write-Title '打包完成'
Write-Host ("压缩包：{0}" -f $ZipPath) -ForegroundColor Green
Write-Host ("大小  ：{0:N1} MB（包内 {1} 个文件 / 原始 {2:N1} MB）" -f ($zipInfo.Length / 1MB), $files.Count, ($totalBytes / 1MB))
Write-Host ("SHA256：{0}" -f $hash)
Write-Host ("清单  ：{0}" -f (Join-Path $OutputDir '文件清单.txt'))
Write-Host ''
Write-Host '把这个 zip 上传到网络仓库附件 / 网盘即可；使用者解压后双击「启动系统.bat」。'
