# 停止微服务全套（不含 Nacos / RocketMQ）
foreach ($port in 8080,8081,8082,8083,8084) {
    $c = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($c) { Stop-Process -Id $c.OwningProcess -Force; "停止 :$port (PID $($c.OwningProcess))" }
}