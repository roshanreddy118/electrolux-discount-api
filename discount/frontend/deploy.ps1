# Build React app
Write-Host "`n🏗️  Building React frontend..." -ForegroundColor Cyan
npm run build

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Build failed!" -ForegroundColor Red
    exit 1
}

# Create static directory if it doesn't exist
$staticDir = "..\app\src\main\resources\static"
if (!(Test-Path $staticDir)) {
    New-Item -ItemType Directory -Path $staticDir -Force | Out-Null
}

# Clean old files
Write-Host "🧹 Cleaning old files..." -ForegroundColor Yellow
Get-ChildItem -Path $staticDir -Recurse | Remove-Item -Force -Recurse -ErrorAction SilentlyContinue

# Copy new build
Write-Host "📦 Copying build to Ktor..." -ForegroundColor Cyan
Copy-Item -Path "dist\*" -Destination $staticDir -Recurse -Force

Write-Host "`n✅ Frontend deployed successfully!" -ForegroundColor Green
Write-Host "📍 Location: app\src\main\resources\static\" -ForegroundColor Gray
Write-Host "🚀 Run '.\gradlew.bat run' from discount folder and open http://localhost:8082/`n" -ForegroundColor Gray
