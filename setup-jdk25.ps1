# Powershell.exe  -ExecutionPolicy Bypass -File setup-jdk25.ps1

# Set JDK 25 as JAVA_HOME for this Maven build
$env:JAVA_HOME = "C:\green\jdk-25.0.1+8"
$env:MAVEN_HOME = "C:\green\apache-maven-3.9.4"
$env:PATH = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:PATH"

Write-Host "Using JDK: $env:JAVA_HOME" -ForegroundColor Green
Write-Host "Java Version:" -ForegroundColor Green
& "$env:JAVA_HOME\bin\java.exe" -version

Write-Host "`nMaven Version:" -ForegroundColor Green
& "$env:MAVEN_HOME\bin\mvn.cmd" --version

# Optionally, you can specify a custom settings.xml if needed
# mvn -s d:\03_projects\suk\asu-trading-analysis\.mvn-local-settings.xml
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Environment Ready!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "You can now run Maven commands, for example:" -ForegroundColor Yellow
Write-Host "  mvn.cmd clean compile" -ForegroundColor White
Write-Host "  mvn.cmd clean package -DskipTests" -ForegroundColor White
Write-Host "`nTo build and run:" -ForegroundColor Yellow
Write-Host "  mvn.cmd -q package -DskipTests" -ForegroundColor White