param(
    [int]$WaitTimeoutSeconds = 420
)
$ErrorActionPreference = "Stop"
& (Join-Path $PSScriptRoot "backend\start-compose.ps1") -WaitTimeoutSeconds $WaitTimeoutSeconds

