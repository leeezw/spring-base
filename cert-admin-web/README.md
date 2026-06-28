# cert-admin-web

SSL 证书自动化管理控制台（前端服务），技术栈：Vite + React + TypeScript + Ant Design + Ant Design Pro。

## 开发

```bash
npm install
npm run dev      # http://localhost:5173，已将 /api 代理到后端 http://localhost:8090
```

## 构建

```bash
npm run build    # 产物在 dist/，可用任意静态服务器（Nginx）托管
```

## 说明

- 登录调用后端 `/api/auth/login`（脚手架二阶段登录，默认租户 `default`），token 存于 localStorage，后续请求自动带 `Authorization: Bearer`。
- 页面：概览 / 证书管理 / ACME 账户 / DNS 服务商 / 部署目标 / 任务日志。
- 后端 API 见 Knife4j 文档：`http://localhost:8090/doc.html`。
