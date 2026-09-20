[CmdletBinding()]
param(
    [ValidateRange(1, 65535)]
    [int]$Port = 8080
)

$projectRoot = Split-Path -Parent $PSScriptRoot
$environmentFile = Join-Path $projectRoot '.env'
$mavenWrapper = Join-Path $projectRoot 'mvnw.cmd'

if (-not (Test-Path -LiteralPath $environmentFile)) {
    Write-Error 'Missing .env. Copy .env.example to .env and fill DB_URL, DB_USERNAME, DB_PASSWORD, and JWT_SECRET.'
    exit 1
}

Get-Content -LiteralPath $environmentFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith('#') -and $line -match '^([^=]+)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim(), 'Process')
    }
}

$requiredVariables = @('DB_URL', 'DB_USERNAME', 'DB_PASSWORD', 'JWT_SECRET')
$missingVariables = $requiredVariables | Where-Object { [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_, 'Process')) }

if ($missingVariables) {
    Write-Error ("Missing required .env values: " + ($missingVariables -join ', '))
    exit 1
}

if (-not $env:DB_URL.StartsWith('jdbc:postgresql://')) {
    Write-Error 'DB_URL must start with jdbc:postgresql://'
    exit 1
}

if (-not (Test-Path -LiteralPath $mavenWrapper)) {
    Write-Error 'mvnw.cmd was not found. Run this script from the QuickSeat repository.'
    exit 1
}

Set-Location -LiteralPath $projectRoot
Write-Host "Starting QuickSeat on port $Port..."
& $mavenWrapper spring-boot:run "-Dspring-boot.run.arguments=--server.port=$Port"
exit $LASTEXITCODE
