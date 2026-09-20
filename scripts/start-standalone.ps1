# ============================================================
# 校园二手交易平台 - 免构建启动（使用包内已经打包好的 jar，无需 Maven / Node / 联网）
#
# 交互用法：双击项目根目录的「启动系统.bat」
# 调试/自动化用法：
#   powershell -NoProfile -ExecutionPolicy Bypass -File scripts\start-standalone.ps1 -Foreground -NoBrowser
#
# 启动前会依次自检：jar 是否存在 → Java 版本是否 >= 17 → 数据库配置是否存在（缺失则自动进入配置问答）
# → 服务端口是否被占用；随后运行 java -jar，并自动打开浏览器。
# ============================================================

[CmdletBinding()]
param(
    [switch]$Foreground,
    [switch]$NoBrowser,
    [int]$WaitSeconds = 120
)

. (Join-Path $PSScriptRoot 'common.ps1')

$ConfigDir = Join-Path $Script:BackendDir 'config'
$ConfigPath = Join-Path $ConfigDir 'application.yml'
$ConfigArg = '--spring.config.additional-location=optional:file:./config/'
$RequiredJavaMajor = 17

function Get-ServerPortFromConfig {
    param([string]$Path)
    $port = 8080
    if (-not (Test-Path -LiteralPath $Path)) { return $port }
    $inServerBlock = $false
    foreach ($line in (Get-Content -LiteralPath $Path)) {
        if ($line -match '^\s*server:\s*(#.*)?$') { $inServerBlock = $true; continue }
        if ($inServerBlock) {
            if ($line -match '^\s+port:\s*(\d+)') { return [int]$Matches[1] }
            if ($line -match '^\S') { $inServerBlock = $false }
        }
    }
    return $port
}

function Get-JavaMajorVersion {
    $java = Get-Command java.exe -ErrorAction SilentlyContinue
    if (-not $java) { return $null }
    $output = & java -version 2>&1
    $first = ($output | Select-Object -First 1)
    if ($first -match 'version "(\d+)(?:\.(\d+))?') {
        $major = [int]$Matches[1]
        if ($major -eq 1 -and $Matches[2]) { $major = [int]$Matches[2] }
        return $major
    }
    return $null
}

function Test-BackendResponding {
    param([int]$Port)
    try {
        $response = Invoke-RestMethod ("http://localhost:{0}/api/categories" -f $Port) -TimeoutSec 3
        return ($response.code -eq 200)
    } catch {
        return $false
    }
}

function Test-PortBusy {
    param([int]$Port)
    # netstat 不依赖 CIM，在受限环境与各版本 Windows 上都可用
    $netstat = netstat -ano | Select-String (":{0}\s+\S+\s+.*LISTENING" -f $Port)
    if ($netstat) { return $true }
    if (Get-Command Get-NetTCPConnection -ErrorAction SilentlyContinue) {
        $listeners = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
        if ($listeners) { return $true }
    }
    return $false
}

function Wait-BackendUp {
    param([int]$Port, [int]$TimeoutSeconds)
    $url = "http://localhost:{0}/api/categories" -f $Port
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Seconds 2
        try {
            $response = Invoke-RestMethod -Uri $url -TimeoutSec 3
            if ($response.code -eq 200) { return $true }
        } catch { }
    }
    return $false
}

Write-Title '校园二手交易平台 - 启动（免构建模式）'

$port = Get-ServerPortFromConfig -Path $ConfigPath

if (-not (Test-Path -LiteralPath $Script:JarPath)) {
    Write-Host '没有找到 backend\target\campus-trade.jar。' -ForegroundColor Red
    Write-Host '这个包应当自带 jar；如果你拿到的是纯源码，请先安装 JDK 17+ 与 Maven，再双击「重新打包后端.bat」。' -ForegroundColor Yellow
    exit 1
}

$javaMajor = Get-JavaMajorVersion
if (-not $javaMajor) {
    Write-Host '没有检测到 Java。请先安装 JDK 17 或更高版本，并确保 java 命令在 PATH 中。' -ForegroundColor Red
    Write-Host '下载地址（选 Windows x64 的 .msi 安装包）：https://adoptium.net/zh-CN/temurin/releases/?version=17&os=windows' -ForegroundColor Yellow
    Write-Host '安装后请重新打开本窗口再试一次。' -ForegroundColor Yellow
    exit 1
}
if ($javaMajor -lt $RequiredJavaMajor) {
    Write-Host ("当前 Java 版本是 {0}，本项目需要 JDK {1} 或更高版本。" -f $javaMajor, $RequiredJavaMajor) -ForegroundColor Red
    Write-Host '下载地址（选 Windows x64 的 .msi 安装包）：https://adoptium.net/zh-CN/temurin/releases/?version=17&os=windows' -ForegroundColor Yellow
    exit 1
}
Write-Host ("环境检查：Java {0} 已就绪" -f $javaMajor) -ForegroundColor Green

if (-not (Test-Path -LiteralPath $ConfigPath)) {
    Write-Host ''
    Write-Host '还没有配置数据库，现在开始首次配置（只需填一次）…' -ForegroundColor Cyan
    & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'configure-database.ps1')
    if (-not (Test-Path -LiteralPath $ConfigPath)) {
        Write-Host ''
        Write-Host '数据库还没有配置完成，启动已取消。' -ForegroundColor Yellow
        exit 1
    }
    $port = Get-ServerPortFromConfig -Path $ConfigPath
}

if (Test-BackendResponding -Port $port) {
    $localUrl = "http://localhost:{0}" -f $port
    Write-Host ("服务已经在运行：{0}" -f $localUrl) -ForegroundColor Yellow
    Write-Host '如果要重新启动，先关闭标题为「校园二手交易平台 - 服务窗口」的那个窗口再试。' -ForegroundColor Gray
    if (-not $NoBrowser) { Start-Process $localUrl }
    exit 0
}

if (Test-PortBusy -Port $port) {
    Write-Host ("端口 {0} 已被其它程序占用，服务无法启动。" -f $port) -ForegroundColor Red
    Write-Host '解决办法：双击「配置数据库.bat」，把最后一项「本机服务端口」改成 8081 之类的空闲端口，再重新启动。' -ForegroundColor Yellow
    exit 1
}

Write-Host ("服务端口：{0}" -f $port)
Write-Host '首次启动会自动创建数据库、建表并写入初始数据，请耐心等待 10-30 秒…'

if ($Foreground) {
    Set-Location -LiteralPath $Script:BackendDir
    & java '-jar' 'target\campus-trade.jar' $ConfigArg
    exit $LASTEXITCODE
}

$escapedBackendDir = $Script:BackendDir.Replace("'", "''")
$innerCommand = @(
    "`$Host.UI.RawUI.WindowTitle = '校园二手交易平台 - 服务窗口（关闭本窗口即停止服务）'"
    "Set-Location -LiteralPath '$escapedBackendDir'"
    "Write-Host '后端服务正在运行，关闭本窗口即停止服务。' -ForegroundColor Cyan"
    "java -jar 'target\campus-trade.jar' '$ConfigArg'"
) -join '; '

# 注意：这里必须手工拼引号（Start-Process 的 -ArgumentList 在路径含空格时会把参数拆散）
$startInfo = New-Object System.Diagnostics.ProcessStartInfo
$startInfo.FileName = 'powershell.exe'
$startInfo.Arguments = '-NoExit -NoProfile -ExecutionPolicy Bypass -Command "' + $innerCommand + '"'
$startInfo.UseShellExecute = $true
[System.Diagnostics.Process]::Start($startInfo) | Out-Null
Write-Host '已在新窗口启动后端服务。' -ForegroundColor Green

if (Wait-BackendUp -Port $port -TimeoutSeconds $WaitSeconds) {
    $localUrl = "http://localhost:{0}" -f $port
    Write-Host ''
    Write-Host ("启动成功：{0}" -f $localUrl) -ForegroundColor Green
    Write-Host ("接口文档：{0}/swagger-ui.html" -f $localUrl)
    Write-Host '测试账号：admin（管理员）/ seller01（卖家）/ buyer01（买家），密码都是 123456'
    Write-Host '停止服务：关闭标题为「校园二手交易平台 - 服务窗口」的那个窗口。' -ForegroundColor Gray
    if (-not $NoBrowser) { Start-Process $localUrl }
} else {
    Write-Host ''
    Write-Host '等待后端就绪超时，服务可能启动失败。' -ForegroundColor Yellow
    Write-Host '请查看标题为「校园二手交易平台 - 服务窗口」的窗口里的报错信息。' -ForegroundColor Yellow
    Write-Host '常见原因：MySQL 服务没有启动、数据库账号密码不对（双击「配置数据库.bat」重填）、端口被占用。' -ForegroundColor Yellow
    exit 1
}

exit 0
