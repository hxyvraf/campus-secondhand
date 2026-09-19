# 公共函数：被其他脚本 dot-source 引用（数据库连接信息、MySQL 客户端查找、服务状态检测）

$Script:ProjectRoot = Split-Path -Parent $PSScriptRoot
$Script:BackendDir = Join-Path $Script:ProjectRoot 'backend'
$Script:FrontendDir = Join-Path $Script:ProjectRoot 'frontend'
$Script:JarPath = Join-Path $Script:BackendDir 'target\campus-trade.jar'
$Script:BackendUrl = 'http://localhost:8080'
$Script:FrontendUrl = 'http://localhost:5173'

# 数据库连接信息（与 backend/src/main/resources/application.yml 保持一致）
$Script:DbHost = '127.0.0.1'
$Script:DbPort = '3306'
$Script:DbUser = 'root'
$Script:DbPassword = '123456'
$Script:DbName = 'campus_trade'

function Find-MysqlClient {
    $cmd = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $candidates = @(
        'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe',
        'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe',
        'C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe'
    )
    foreach ($path in $candidates) {
        if (Test-Path $path) { return $path }
    }
    return $null
}

function Get-BackendProcess {
    Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
        Where-Object { $_.CommandLine -like '*campus-trade.jar*' }
}

function Stop-BackendService {
    $processes = Get-BackendProcess
    if (-not $processes) { return $false }
    foreach ($process in $processes) {
        Stop-Process -Id $process.ProcessId -Force -ErrorAction SilentlyContinue
        Write-Host "  已停止后端服务（进程 $($process.ProcessId)）"
    }
    Start-Sleep -Seconds 2
    return $true
}

function Get-FrontendProcess {
    Get-CimInstance Win32_Process -Filter "Name='node.exe'" -ErrorAction SilentlyContinue |
        Where-Object { $_.CommandLine -like '*vite*' -and $_.CommandLine -like '*campus-secondhand*' }
}

function Wait-BackendReady {
    param([int]$TimeoutSeconds = 90)
    for ($i = 1; $i -le ($TimeoutSeconds / 2); $i++) {
        Start-Sleep -Seconds 2
        try {
            $response = Invoke-RestMethod -Uri "$Script:BackendUrl/api/categories" -TimeoutSec 3
            if ($response.code -eq 200) { return $true }
        } catch { }
    }
    return $false
}

function Invoke-Mysql {
    param([string]$Sql, [string]$Database = '')
    $mysql = Find-MysqlClient
    if (-not $mysql) { throw '没有找到 mysql 客户端（mysql.exe），请确认 MySQL 已安装并加入 PATH' }
    $previous = $env:MYSQL_PWD
    $env:MYSQL_PWD = $Script:DbPassword
    try {
        $args = @("--host=$Script:DbHost", "--port=$Script:DbPort", "--user=$Script:DbUser", '--default-character-set=utf8mb4')
        if ($Database) { $args += $Database }
        $args += @('-e', $Sql)
        & $mysql @args
        if ($LASTEXITCODE -ne 0) { throw "mysql 执行失败（退出码 $LASTEXITCODE）" }
    } finally {
        if ($null -eq $previous) { Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue }
        else { $env:MYSQL_PWD = $previous }
    }
}

function Write-Title {
    param([string]$Text)
    Write-Host ''
    Write-Host "===== $Text =====" -ForegroundColor Cyan
}
