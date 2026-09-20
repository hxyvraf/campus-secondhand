# ============================================================
# 校园二手交易平台 - 数据库连接配置（首次运行用）
#
# 交互用法：双击项目根目录的「配置数据库.bat」
# 非交互用法（供启动脚本或自动化调用）：
#   powershell -NoProfile -ExecutionPolicy Bypass -File scripts\configure-database.ps1 `
#       -DbHost 127.0.0.1 -DbPort 3306 -DbUser root -DbPassword 123456 -DbName campus_trade
#
# 作用：生成 backend\config\application.yml，由 Spring Boot 以「外部配置」方式加载
#      （优先级高于 jar 内的 application.yml），只覆盖数据库连接与本机服务端口。
# 退出码：0 = 配置已写入；1 = 用户取消或写入失败
# ============================================================

[CmdletBinding()]
param(
    [string]$DbHost,
    [int]$DbPort = 0,
    [string]$DbUser,
    [string]$DbPassword,
    [string]$DbName,
    [int]$ServerPort = 0,
    [switch]$NonInteractive,
    [switch]$SkipCheck
)

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$BackendDir = Join-Path $ProjectRoot 'backend'
$ConfigDir = Join-Path $BackendDir 'config'
$ConfigPath = Join-Path $ConfigDir 'application.yml'

$Defaults = @{
    Host       = '127.0.0.1'
    Port       = 3306
    User       = 'root'
    Name       = 'campus_trade'
    ServerPort = 8080
}

function Write-Title {
    param([string]$Text)
    Write-Host ''
    Write-Host "===== $Text =====" -ForegroundColor Cyan
}

function Find-MysqlClient {
    $cmd = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $candidates = @(
        'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe',
        'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe',
        'C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe',
        'C:\Program Files (x86)\MySQL\MySQL Server 8.0\bin\mysql.exe'
    )
    foreach ($path in $candidates) {
        if (Test-Path -LiteralPath $path) { return $path }
    }
    return $null
}

function ConvertFrom-SecureToPlain {
    param([System.Security.SecureString]$Secure)
    if ($null -eq $Secure) { return '' }
    $bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($Secure)
    try { return [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr) }
    finally { [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
}

function ConvertTo-YamlQuoted {
    param([string]$Value)
    if ($null -eq $Value) { $Value = '' }
    $escaped = $Value.Replace('\', '\\').Replace('"', '\"')
    return '"' + $escaped + '"'
}

function Read-Port {
    param([string]$Label, [int]$DefaultValue)
    while ($true) {
        $answer = Read-Host ("{0}（直接回车 = {1}）" -f $Label, $DefaultValue)
        if ([string]::IsNullOrWhiteSpace($answer)) { return $DefaultValue }
        $parsed = 0
        if ([int]::TryParse($answer.Trim(), [ref]$parsed) -and $parsed -ge 1 -and $parsed -le 65535) { return $parsed }
        Write-Host '  端口必须是 1-65535 之间的数字，请重新输入。' -ForegroundColor Yellow
    }
}

function Read-ConnectionInputs {
    param([hashtable]$Current)
    $values = @{}

    $answer = Read-Host ("MySQL 地址 / 主机（直接回车 = {0}）" -f $Current.Host)
    if ([string]::IsNullOrWhiteSpace($answer)) { $values.Host = $Current.Host } else { $values.Host = $answer.Trim() }

    $values.Port = Read-Port -Label 'MySQL 端口' -DefaultValue $Current.Port

    $answer = Read-Host ("MySQL 账号（直接回车 = {0}）" -f $Current.User)
    if ([string]::IsNullOrWhiteSpace($answer)) { $values.User = $Current.User } else { $values.User = $answer.Trim() }

    if ($Current.Password) {
        $passwordPrompt = 'MySQL 密码（输入时不显示；直接回车 = 沿用上次输入的密码）'
    } else {
        $passwordPrompt = 'MySQL 密码（输入时不显示；没有密码直接回车）'
    }
    $secure = Read-Host -Prompt $passwordPrompt -AsSecureString
    $entered = ConvertFrom-SecureToPlain -Secure $secure
    if ([string]::IsNullOrEmpty($entered) -and $Current.Password) { $values.Password = $Current.Password }
    else { $values.Password = $entered }

    $answer = Read-Host ("数据库名（直接回车 = {0}）" -f $Current.Name)
    if ([string]::IsNullOrWhiteSpace($answer)) { $values.Name = $Current.Name } else { $values.Name = $answer.Trim() }

    $values.ServerPort = Read-Port -Label '本机服务端口（浏览器访问用）' -DefaultValue $Current.ServerPort

    return $values
}

function Test-DatabaseLogin {
    param([hashtable]$Values)
    $mysql = Find-MysqlClient
    if (-not $mysql) {
        return @{ Ok = $false; Skipped = $true; Message = '本机没有找到 mysql.exe，跳过连通性自检（不影响使用，由后端启动结果为准）' }
    }

    # 用 .NET 直接起进程，拿到 mysql 客户端的原始 stdout / stderr（避免 PowerShell 对错误流的额外包装）
    $mysqlArgs = @(
        ("--host={0}" -f $Values.Host),
        ("--port={0}" -f $Values.Port),
        ("--user={0}" -f $Values.User),
        '--connect-timeout=6',
        '--default-character-set=utf8mb4',
        '-N',
        '-B',
        '-e',
        'SELECT VERSION();'
    )

    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $mysql
    $startInfo.Arguments = (($mysqlArgs | ForEach-Object { '"' + $_ + '"' }) -join ' ')
    $startInfo.UseShellExecute = $false
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.CreateNoWindow = $true
    $startInfo.EnvironmentVariables['MYSQL_PWD'] = $Values.Password

    $process = $null
    try {
        $process = [System.Diagnostics.Process]::Start($startInfo)
        $stdout = $process.StandardOutput.ReadToEnd()
        $stderr = $process.StandardError.ReadToEnd()
        if (-not $process.WaitForExit(30000)) {
            $process.Kill()
            return @{ Ok = $false; Skipped = $false; Message = '连接 MySQL 超时（30 秒）' }
        }
        $exitCode = $process.ExitCode
    } catch {
        return @{ Ok = $false; Skipped = $false; Message = ('调用 mysql 客户端失败：{0}' -f $_.Exception.Message) }
    } finally {
        if ($process) { $process.Dispose() }
    }

    if ($exitCode -eq 0) {
        $version = (($stdout -split "`r?`n") | Where-Object { $_.Trim() } | Select-Object -First 1)
        return @{ Ok = $true; Skipped = $false; Message = $version }
    }

    $detail = (($stderr -split "`r?`n") | ForEach-Object { $_.Trim() } | Where-Object { $_ } | Select-Object -First 2) -join ' / '
    if (-not $detail) { $detail = 'MySQL 客户端没有返回详细信息' }
    return @{ Ok = $false; Skipped = $false; Message = $detail }
}

function Write-ConfigFile {
    param([hashtable]$Values)

    $url = 'jdbc:mysql://{0}:{1}/{2}?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai' -f $Values.Host, $Values.Port, $Values.Name

    $lines = @(
        '# ============================================================',
        '# 校园二手交易平台 - 本机运行配置（由「配置数据库.bat」自动生成）',
        '# 作用：只覆盖数据库连接与本机服务端口，其余配置仍以 jar 内的 application.yml 为准。',
        '# 换端口：改下面的 server.port（启动脚本会自动读取这个值）。',
        '# 换数据库：双击「配置数据库.bat」重新填写，或者删掉本文件后重新启动。',
        '# ============================================================',
        'server:',
        ('  port: {0}' -f $Values.ServerPort),
        '',
        'spring:',
        '  datasource:',
        ('    url: {0}' -f (ConvertTo-YamlQuoted $url)),
        ('    username: {0}' -f (ConvertTo-YamlQuoted $Values.User)),
        ('    password: {0}' -f (ConvertTo-YamlQuoted $Values.Password)),
        ''
    )

    if (-not (Test-Path -LiteralPath $ConfigDir)) {
        New-Item -ItemType Directory -Force -Path $ConfigDir | Out-Null
    }
    if (Test-Path -LiteralPath $ConfigPath) {
        Copy-Item -LiteralPath $ConfigPath -Destination ($ConfigPath + '.bak') -Force
    }

    $encoding = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($ConfigPath, ($lines -join "`r`n"), $encoding)
}

Write-Title '校园二手交易平台 - 数据库连接配置'

$current = @{
    Host       = if ($DbHost) { $DbHost } else { $Defaults.Host }
    Port       = if ($DbPort -gt 0) { $DbPort } else { $Defaults.Port }
    User       = if ($DbUser) { $DbUser } else { $Defaults.User }
    Password   = if ($PSBoundParameters.ContainsKey('DbPassword')) { $DbPassword } else { '' }
    Name       = if ($DbName) { $DbName } else { $Defaults.Name }
    ServerPort = if ($ServerPort -gt 0) { $ServerPort } else { $Defaults.ServerPort }
}

$hasAllInputs = $DbHost -and ($DbPort -gt 0) -and $DbUser -and $PSBoundParameters.ContainsKey('DbPassword') -and $DbName
if (-not $hasAllInputs -and $NonInteractive) {
    Write-Host '非交互模式下必须提供 -DbHost -DbPort -DbUser -DbPassword -DbName 全部参数。' -ForegroundColor Red
    exit 1
}

$saved = $false
for ($attempt = 1; $attempt -le 3; $attempt++) {
    if (-not $NonInteractive -and $attempt -gt 1) {
        Write-Host ''
        Write-Host '请根据下面的提示重新填写（回车 = 沿用上一次的值）' -ForegroundColor Yellow
    }

    if (-not $NonInteractive) {
        if ($hasAllInputs) {
            $values = $current
        } else {
            Write-Host ''
            Write-Host '请填写本机 MySQL 连接信息（直接回车使用括号里的默认值）' -ForegroundColor Gray
            $values = Read-ConnectionInputs -Current $current
        }
    } else {
        $values = $current
    }

    if (-not $SkipCheck) {
        Write-Host ''
        Write-Host '正在校验数据库连接…' -ForegroundColor Gray
        $check = Test-DatabaseLogin -Values $values
        if ($check.Skipped) {
            Write-Host $check.Message -ForegroundColor Yellow
            $saved = $true
            break
        }
        if (-not $check.Ok) {
            Write-Host ("连接失败：{0}" -f $check.Message) -ForegroundColor Red
            Write-Host '常见原因：1) MySQL 服务没有启动  2) 账号或密码不对  3) 端口不是 3306  4) MySQL 还没安装' -ForegroundColor Yellow
            if ($NonInteractive) { exit 1 }
            $current.Host = $values.Host
            $current.Port = $values.Port
            $current.User = $values.User
            $current.Password = $values.Password
            $current.Name = $values.Name
            $current.ServerPort = $values.ServerPort
            continue
        }
        Write-Host ("连接成功，MySQL 版本：{0}" -f $check.Message) -ForegroundColor Green
    }

    $saved = $true
    break
}

if (-not $saved) {
    Write-Host ''
    $answer = Read-Host '连续 3 次连接失败。仍然保存这份配置吗？保存后仍可能启动失败（输入 Y 保存，其它键取消）'
    if ($answer -notmatch '^\s*[Yy]') {
        Write-Host '已取消，未写入配置。' -ForegroundColor Yellow
        exit 1
    }
}

Write-ConfigFile -Values $values

Write-Title '配置完成'
Write-Host ("配置文件：{0}" -f $ConfigPath) -ForegroundColor Green
Write-Host ("数据库  ：{0}:{1}/{2}（账号 {3}）" -f $values.Host, $values.Port, $values.Name, $values.User)
Write-Host ("访问地址：http://localhost:{0}" -f $values.ServerPort) -ForegroundColor Green
Write-Host ''
Write-Host '下一步：双击根目录的「启动系统.bat」即可打开完整系统。'
exit 0
