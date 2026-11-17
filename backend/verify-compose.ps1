$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$compose = Join-Path $root "docker-compose.yml"

Write-Host "Compose path: $compose"
Write-Host ""
Write-Host "Expected markers:" -ForegroundColor Cyan
Get-Content $compose | Select-String -Pattern "REHAB_CENTER_COMPOSE_V3|mvn -f|spring-boot:run|jansi.tmpdir|restart:"

Write-Host ""
Write-Host "Rendered effective command:" -ForegroundColor Cyan
docker compose -f $compose config | Select-String -Pattern "package|java -jar|spring-boot:run|jansi|restart"

