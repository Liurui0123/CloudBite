# 启动微服务全套：Nacos(需先手动起) -> user/menu/order/ai -> gateway
# 用法： .\start-all.ps1
$ErrorActionPreference = "Stop"
$p = "E:\dsh-code\cloudbite"
$java = "E:\develop\jdk21\bin\java.exe"

if (-not (Test-NetConnection 127.0.0.1 -Port 8848 -WarningAction SilentlyContinue).TcpTestSucceeded) {
    Write-Host "Nacos 未启动，请先运行： E:\develop\nacos\bin\startup.cmd -m standalone" -ForegroundColor Yellow
}
if (-not (Test-NetConnection 127.0.0.1 -Port 9876 -WarningAction SilentlyContinue).TcpTestSucceeded) {
    Write-Host "RocketMQ 未启动（延迟关单会失效），可运行 cloudBite\start-mq.ps1" -ForegroundColor Yellow
}

$occ = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($occ) { throw "8080 已被占用（可能是单体版后端），请先停掉再启动微服务" }

foreach ($n in @("user","menu","order","ai")) {
    Start-Process -FilePath $java -ArgumentList @("-jar", "$p\cloudbite-$n\target\cloudbite-$n-1.0.0.jar") -WorkingDirectory $p -WindowStyle Hidden
    Start-Sleep -Seconds 2
}
Start-Sleep -Seconds 25
Start-Process -FilePath $java -ArgumentList @("-jar", "$p\cloudbite-gateway\target\cloudbite-gateway-1.0.0.jar") -WorkingDirectory $p -WindowStyle Hidden
Start-Sleep -Seconds 15
foreach ($port in 8080,8081,8082,8083,8084) {
    "{0}: {1}" -f $port, $(if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) { "UP" } else { "down" })
}
Write-Host "网关: http://localhost:8080  文档: http://localhost:8080/doc.html" -ForegroundColor Green