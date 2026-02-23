# 检查前后端服务状态
Write-Host "检查服务状态..." -ForegroundColor Cyan

# 检查后端服务 (端口 5000)
$backendPort = Get-NetTCPConnection -LocalPort 5000 -ErrorAction SilentlyContinue
if ($backendPort) {
    Write-Host "✅ 后端服务已启动 (端口 5000)" -ForegroundColor Green
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:5000" -TimeoutSec 2 -ErrorAction Stop
        Write-Host "   HTTP 状态: $($response.StatusCode)" -ForegroundColor Green
    } catch {
        Write-Host "   ⚠️  服务启动中或未响应..." -ForegroundColor Yellow
    }
} else {
    Write-Host "❌ 后端服务未启动 (端口 5000)" -ForegroundColor Red
}

# 检查前端服务 (端口 3000)
$frontendPort = Get-NetTCPConnection -LocalPort 3000 -ErrorAction SilentlyContinue
if ($frontendPort) {
    Write-Host "✅ 前端服务已启动 (端口 3000)" -ForegroundColor Green
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:3000" -TimeoutSec 2 -ErrorAction Stop
        Write-Host "   HTTP 状态: $($response.StatusCode)" -ForegroundColor Green
    } catch {
        Write-Host "   ⚠️  服务启动中或未响应..." -ForegroundColor Yellow
    }
} else {
    Write-Host "❌ 前端服务未启动 (端口 3000)" -ForegroundColor Red
}

Write-Host "`n访问地址:" -ForegroundColor Cyan
Write-Host "  前端: http://localhost:3000" -ForegroundColor White
Write-Host "  后端: http://localhost:5000" -ForegroundColor White

