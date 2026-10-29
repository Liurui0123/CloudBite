# CloudBite 云餐 · 前端（Vue 3）

## 启动

```powershell
cd E:\点餐系统\code\cloudBite
npm run dev
```

打开 <http://localhost:5173>，账号 `xiaoming / 123456`，管理员 `admin / admin123`。

依赖已安装好（node_modules 已存在），后端默认跑在 `http://localhost:8080`，代理配置见 `vite.config.js`。

## 页面

- `/login`　登录 / 注册
- `/`　点餐首页（AI 推荐 + 实时热销榜）
- `/orders`　我的订单，`/orders/:orderNo` 订单详情
- `/admin`　商家后台：数据看板、菜品管理、订单管理