# Scaffold Project

一个基于 Spring Boot 3.x 的企业级多租户 SaaS 脚手架项目，开箱即用。

## 技术栈

- **Java 17**
- **Spring Boot 3.1.11**
- **MyBatis-Plus 3.5.5** - 数据库操作
- **PostgreSQL 16** - 关系型数据库
- **Redis** - 缓存与会话管理
- **JWT** - 无状态认证
- **Knife4j** - API 文档

## 核心功能

### 1. 认证授权 (scaffold-auth-starter)
- ✅ JWT Token 认证
- ✅ Session 管理（Redis）
- ✅ Token 黑名单
- ✅ 认证过滤器
- ✅ `@AllowAnonymous` 匿名访问注解

### 2. 权限控制 (scaffold-permission-starter)
- ✅ `@RequiresRoles` 角色验证
- ✅ `@RequiresPermissions` 权限验证（支持通配符 `*`）
- ✅ `@DataScope` 数据权限
- ✅ 超级管理员自动放行（`*:*:*`）

### 3. 操作日志 (scaffold-log-starter)
- ✅ `@OperationLog` 操作日志注解
- ✅ 自动记录请求/响应/IP/用户/耗时
- ✅ 支持多种操作类型（增删改查等）

### 4. 限流控制 (scaffold-cache-starter)
- ✅ `@RateLimit` 限流注解
- ✅ 5 种限流维度：IP / USER / TOKEN / GLOBAL / CUSTOM
- ✅ 3 种限流算法：滑动窗口 / 固定窗口 / 令牌桶
- ✅ Lua 脚本原子操作

### 5. 多租户 SaaS (scaffold-mybatis-starter)
- ✅ 共享数据库 + 共享表 + tenant_id 隔离
- ✅ MyBatis-Plus 多租户插件自动注入 `WHERE tenant_id = ?`
- ✅ 租户上下文管理（ThreadLocal）
- ✅ 登录时租户识别（租户编码）
- ✅ 数据完全隔离，支持百万级租户

### 6. 用户中台 (scaffold-user-center)
- ✅ 用户管理（CRUD + 分页查询）
- ✅ 角色管理（CRUD + 分页查询）
- ✅ 权限管理（CRUD + 树形结构）
- ✅ 菜单管理（CRUD + 树形结构）
- ✅ 部门管理（CRUD + 树形结构）
- ✅ 关联关系管理（用户-角色、角色-权限、角色-菜单）

## 项目结构

```
scaffold-project/
├── scaffold-common/              # 公共模块
│   ├── Result<T>                # 统一响应
│   ├── GlobalExceptionHandler   # 全局异常处理
│   └── JsonUtils                # JSON 工具类
├── scaffold-auth-starter/        # 认证模块
│   ├── JwtUtils                 # JWT 工具
│   ├── LoginUser                # 登录用户信息
│   ├── AuthenticationFilter     # 认证过滤器
│   └── SessionService           # Session 管理
├── scaffold-mybatis-starter/     # MyBatis-Plus 配置
│   ├── BaseEntity               # 基础实体（逻辑删除+审计字段）
│   ├── MyMetaObjectHandler      # 自动填充
│   ├── TenantContext            # 租户上下文
│   └── CustomTenantHandler      # 多租户插件
├── scaffold-permission-starter/  # 权限模块
│   ├── @RequiresRoles           # 角色注解
│   ├── @RequiresPermissions     # 权限注解
│   ├── @DataScope               # 数据权限注解
│   └── PermissionAspect         # 权限切面
├── scaffold-log-starter/         # 日志模块
│   ├── @OperationLog            # 操作日志注解
│   └── OperationLogAspect       # 日志切面
├── scaffold-cache-starter/       # 缓存模块
│   ├── @RateLimit               # 限流注解
│   └── RateLimitAspect          # 限流切面
├── scaffold-user-center/         # 用户中台
│   ├── entity/                  # 实体类
│   ├── mapper/                  # Mapper 接口
│   ├── service/                 # 服务层
│   └── controller/              # 控制器
└── scaffold-app/                 # 主应用
    ├── ScaffoldApplication      # 启动类
    └── controller/              # 业务控制器
```

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.6+
- PostgreSQL 12+
- Redis 5.0+

### 2. 数据库初始化

```bash
# 创建数据库
createdb scaffold_db

# 执行初始化脚本
psql -d scaffold_db -f sql/init_user_center.sql
psql -d scaffold_db -f sql/upgrade_multi_tenant.sql
```

### 3. 配置文件

修改 `scaffold-app/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/scaffold_db
    username: your_username
    password: your_password
  
  data:
    redis:
      host: localhost
      port: 6379
      password: your_redis_password  # 如果有密码
```

### 4. 启动应用

```bash
# 编译打包
mvn clean package -DskipTests

# 运行
java -jar scaffold-app/target/scaffold-app-1.0.0-SNAPSHOT.jar
```

应用启动后访问：
- API 文档：http://localhost:8090/doc.html
- Swagger UI：http://localhost:8090/swagger-ui/index.html

### 5. 默认账号

**默认租户：**
- 租户编码：`default`
- 租户名称：默认租户

**管理员账号：**
- 用户名：`admin`
- 密码：`admin123`
- 权限：超级管理员（`*:*:*`）

## API 示例

### 登录

```bash
POST /api/auth/login
Content-Type: application/json

{
  "tenantCode": "default",
  "username": "admin",
  "password": "admin123"
}
```

响应：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "token": "eyJhbGciOiJIUzM4NCJ9...",
    "userInfo": {
      "userId": 1,
      "tenantId": 1,
      "username": "admin",
      "nickname": "超级管理员",
      "roles": ["1"],
      "permissions": ["*:*:*"]
    }
  }
}
```

### 查询用户列表

```bash
GET /api/system/user/page?pageNum=1&pageSize=10
Authorization: Bearer {token}
```

## 多租户使用

### 1. 租户隔离原理

- 所有业务表包含 `tenant_id` 字段
- MyBatis-Plus 插件自动在 SQL 中注入 `WHERE tenant_id = ?`
- 登录时通过租户编码识别租户
- 请求期间通过 ThreadLocal 维护租户上下文

### 2. 创建新租户

```bash
POST /api/system/tenant/save
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "tenantCode": "company001",
  "tenantName": "示例公司",
  "contactName": "张三",
  "contactPhone": "13800138000",
  "contactEmail": "zhangsan@example.com",
  "accountCount": 100,
  "status": 1
}
```

### 3. 租户登录

```bash
POST /api/auth/login
Content-Type: application/json

{
  "tenantCode": "company001",
  "username": "user001",
  "password": "password123"
}
```

## 权限使用

### 1. 角色验证

```java
@RequiresRoles("admin")
@GetMapping("/admin/dashboard")
public Result<String> adminDashboard() {
    return Result.success("管理员面板");
}
```

### 2. 权限验证

```java
@RequiresPermissions("system:user:delete")
@DeleteMapping("/user/{id}")
public Result<Void> deleteUser(@PathVariable Long id) {
    userService.removeById(id);
    return Result.success();
}
```

### 3. 操作日志

```java
@OperationLog(value = "删除用户", type = OperationType.DELETE)
@DeleteMapping("/user/{id}")
public Result<Void> deleteUser(@PathVariable Long id) {
    userService.removeById(id);
    return Result.success();
}
```

### 4. 限流控制

```java
@RateLimit(
    dimension = RateLimitDimension.IP,
    algorithm = RateLimitAlgorithm.SLIDING_WINDOW,
    limit = 10,
    period = 60
)
@GetMapping("/public/data")
public Result<String> getPublicData() {
    return Result.success("公开数据");
}
```

## 部署

### systemd 服务（推荐）

创建 `/etc/systemd/system/scaffold.service`：

```ini
[Unit]
Description=Scaffold Project Spring Boot Application
After=network.target

[Service]
Type=simple
User=your_user
WorkingDirectory=/path/to/project
ExecStart=/usr/bin/java -Xmx512m -jar /path/to/scaffold-app-1.0.0-SNAPSHOT.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启动服务：
```bash
sudo systemctl daemon-reload
sudo systemctl enable scaffold
sudo systemctl start scaffold
sudo systemctl status scaffold
```

## 开发指南

### 添加新模块

1. 创建 Maven 模块
2. 添加依赖到父 POM
3. 实现业务逻辑
4. 在 `scaffold-app` 中引入

### 自定义权限

1. 在 `sys_permission` 表中添加权限记录
2. 分配权限给角色
3. 使用 `@RequiresPermissions` 注解保护接口

### 扩展多租户

- 忽略表：修改 `CustomTenantHandler.IGNORE_TABLES`
- 自定义租户识别：修改 `AuthController.login` 方法

## 技术亮点

1. **模块化设计**：各功能模块独立，可按需引入
2. **多租户架构**：支持百万级租户，成本低，扩展性好
3. **自动化处理**：审计字段自动填充，租户隔离自动注入
4. **安全可靠**：JWT + Redis Session，Token 黑名单机制
5. **开箱即用**：完整的用户中台，快速搭建 SaaS 应用

## 许可证

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request！

## 联系方式

- GitHub: https://github.com/leeezw/spring-base
