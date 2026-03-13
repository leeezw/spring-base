# User Center Frontend

React 18 + Vite 控制台，用于对接 `scaffold-user-center` 提供的后端接口。

## 功能

- 登录并写入本地 Token
- 用户列表分页查询
- 角色列表展示
- 当前登录者 Session 列表

## 运行

```bash
npm install
npm run dev
# 构建
npm run build
```

开发服务器将通过 Vite 代理 `/api` 请求到 `http://localhost:8080`，可在 `vite.config.js` 中修改。
