# Scaffold Project

现代化 Spring Boot 3 开发脚手架，提供开箱即用的企业级功能模块。

## 技术栈

- **Java 17** + **Spring Boot 3.2.5**
- **PostgreSQL 16** + **MyBatis-Plus 3.5.5**
- **Redis 7** (Session + 缓存 + 限流)
- **JWT** 认证
- **Knife4j** API 文档

## 模块结构

```
scaffold-project/
├── scaffold-common              # 通用基础 (Result/异常/工具类)
├── scaffold-auth-starter        # 认证 (JWT/Session/Token黑名单)
├── scaffold-mybatis-starter     # 数据层 (MyBatis-Plus + PostgreSQL)
├── scaffold-permission-starter  # 权限鉴权 (角色/权限/数据权限)
├── scaffold-log-starter         # 操作日志
├── scaffold-cache-starter       # 缓存 + 限流
└── scaffold-app                 # 主应用
```

## 核心功能

### 认证授权
- JWT Token 认证
- Redis Session 管理
- Token 黑名单
- 单点登录支持

### 权限控制
- `@RequiresRoles` - 角色校验
- `@RequiresPermissions` - 操作权限校验（支持通配符）
- `@DataScope` - 数据权限（全部/本部门/本部门及子部门/仅本人）

### 操作日志
- `@OperationLog` - 方法级操作日志
- 自动记录：请求参数、响应、IP、用户、耗时、成功/失败

### 限流
- `@RateLimit` - 限流注解
- 5种维度：IP/USER/TOKEN/GLOBAL/CUSTOM
- 3种算法：滑动窗口/固定窗口/令牌桶

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.6+
- PostgreSQL 16+
- Redis 7+

### 2. 数据库初始化

```sql
CREATE DATABASE scaffold_db;
CREATE USER scaffold_user WITH PASSWORD 'Scaffold@2026';
GRANT ALL PRIVILEGES ON DATABASE scaffold_db TO scaffold_user;
```

### 3. 配置

编辑 `scaffold-app/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/scaffold_db
    username: scaffold_user
    password: Scaffold@2026
  
  data:
    redis:
      host: localhost
      port: 6379

scaffold:
  auth:
    jwt:
      secret: your-secret-key
      expire: 7200
```

### 4. 构建运行

```bash
mvn clean package -DskipTests
java -jar scaffold-app/target/scaffold-app-1.0.0-SNAPSHOT.jar
```

访问：http://localhost:8090

默认账号：`admin` / `admin123`

## API 示例

### 登录
```bash
curl -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### 带权限的接口
```java
@RestController
@RequestMapping("/api/user")
public class UserController {
    
    @GetMapping("/list")
    @RequiresPermissions("system:user:query")
    @OperationLog(module = "用户管理", type = OperationType.QUERY, description = "查询用户列表")
    @RateLimit(limitType = LimitType.USER, count = 100, period = 60)
    public Result<List<User>> list() {
        // ...
    }
}
```

## 开发计划

- [ ] 用户中台模块
- [ ] 文件存储模块
- [ ] 代码生成器
- [ ] React 前端

## License

MIT
