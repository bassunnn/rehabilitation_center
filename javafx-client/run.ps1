Write-Host "Запуск JavaFX-приложения Реабилитационного центра..." -ForegroundColor Cyan
if (Get-Command mvn -ErrorAction SilentlyContinue) {
    mvn javafx:run
} elseif (Test-Path "E:\JetBrains\IntelliJIdea2026.1\plugins\maven\lib\maven3\bin\mvn.cmd") {
    & "E:\JetBrains\IntelliJIdea2026.1\plugins\maven\lib\maven3\bin\mvn.cmd" javafx:run
} else {
    Write-Error "Maven не найден в PATH или IntelliJ IDEA."
}

