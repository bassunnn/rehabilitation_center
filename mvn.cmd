@echo off
if exist "%~dp0\..\..\..\..\..\Program Files\Java\jdk-25" (
    rem java ok
)
"E:\JetBrains\IntelliJIdea2026.1\plugins\maven\lib\maven3\bin\mvn.cmd" %*

