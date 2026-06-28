# 证书自动化工具 — 没有测试环境也能验证

核心结论：**这套工具不需要专门的测试环境**。
- **签发验证**用 Let's Encrypt **staging（测试目录）+ 你现有的真实域名**：签出的是格式完全一样、但浏览器不信任的证书，**不限频、对线上零影响、零风险**。验证通过后把 ACME 账户切到生产目录重签即可。
- **运行**只需要 PostgreSQL + Redis。下面给两条路径：A 用你现有的数据库；B 本机用 Docker 一键起。

---

## 服务器过期 / 数据库连不上 怎么办？

原默认配置连的是远程库 `129.211.62.23`。服务器过期后那台 PostgreSQL 和 Redis 都没了，应用起不来。这套工具可以**完全脱离那台服务器**，按成本从低到高三条路：

1. **本机 Docker（首选，零成本、零改动）**：见下面【路径 B】，`docker compose up -d` 在自己电脑起 PG+Redis。
2. **免费云数据库（想常驻又不想买服务器）**：用 Neon/Supabase 免费 PostgreSQL + Upstash 免费 Redis，复制 `application-cloud.yml.example` 为 `application-cloud.yml`、填好连接串后：
   ```bash
   mvn -pl scaffold-app -am spring-boot:run -Dspring-boot.run.profiles=cloud
   ```
3. **新服务器/续费**：长期方案——证书**自动续期是定时任务，需要一台常驻主机**才能在到期前自动跑。打包 `mvn -pl scaffold-app -am package` 得到 jar，配好 PG+Redis 与 `CERT_CRYPTO_SECRET`，`java -jar` 跑起来并设开机自启即可。

> ⚠️ 关键：**Redis 也必须换一个能连的**（登录/令牌依赖它），只改数据库不够。

---

## 0. 零依赖：先跑单元测试（不需要任何外部服务）

```bash
mvn -pl scaffold-cert -am test
```
验证加解密（`CryptoService`）与域名解析（`DomainUtils`）等核心逻辑，应输出 `Tests run: 11, Failures: 0`。

---

## 路径 A：用你现有的数据库（推荐，最省事）

1. 对现有库执行建表脚本（只新增 6 张 cert 表 + 权限种子，幂等、可重复执行）：
   ```bash
   psql "$YOUR_PG_URL" -f sql/cert_automation.sql
   ```
2. 设置加密主密钥（务必 >=32 位）并启动后端：
   ```bash
   export CERT_CRYPTO_SECRET="your-strong-32+-char-secret-xxxxxxx"
   mvn -pl scaffold-app -am spring-boot:run
   ```
3. 打开接口文档确认 cert 接口已注册：`http://localhost:8090/doc.html`
4. 跳到下面【第 3 节：staging 零风险签发验证】。

> 用现有库时，登录用你系统里已有的管理员账号即可（超级管理员角色 `ADMIN` 已被授予全部 `cert:*` 权限）。

---

## 路径 B：本机 Docker 全量（本机什么都没有时）

1. 起 PostgreSQL + Redis：
   ```bash
   docker compose up -d
   ```
2. 按顺序导入基础 schema 与种子（脚手架既有脚本），最后导入 cert 脚本：
   ```bash
   PG="postgresql://scaffold_user:scaffold_pass@localhost:5432/scaffold_db"
   psql "$PG" -f sql/init_user_center.sql
   psql "$PG" -f sql/upgrade_multi_tenant.sql
   psql "$PG" -f sql/fix_sys_role_unique_constraint.sql
   psql "$PG" -f sql/sys_position.sql
   psql "$PG" -f sql/sys_employee.sql
   psql "$PG" -f sql/insert_test_data_fixed.sql   # 注意用 _fixed 版本
   psql "$PG" -f sql/cert_automation.sql
   ```
3. 启用 local profile（`application-local.yml` 按仓库约定被 gitignore，从模板复制即可）：
   ```bash
   cp scaffold-app/src/main/resources/application-local.yml.example \
      scaffold-app/src/main/resources/application-local.yml
   mvn -pl scaffold-app -am spring-boot:run -Dspring-boot.run.profiles=local
   ```
4. 前端：
   ```bash
   cd cert-admin-web && npm install && npm run dev   # http://localhost:5173
   ```
   用种子数据里的管理员账号登录（见 `sql/insert_test_data_fixed.sql`）。

---

## 3. staging 零风险签发验证（核心）

> 前提：你的域名 DNS 解析在西部数码，并有其 API 账号/密钥。

1. **建 ACME 账户**（控制台「ACME 账户 → 新增账户」）：
   - CA 类型：Let's Encrypt
   - **勾选「使用测试目录(staging)」** ← 关键，零风险
   - 填邮箱
2. **建 DNS 服务商**（「DNS 服务商 → 新增」）：
   - 类型：西部数码
   - 填 `账号` + `API 密码/密钥`
3. **新建签发**（「证书管理 → 新建签发」）：
   - 域名：填一个**不影响线上的子域名**，如 `test.你的域名.com`
   - 选上面的 ACME 账户 + DNS 服务商，密钥算法默认 RSA2048
   - 部署目标先留空（纯验证签发）
   - 提交
4. **看结果**：
   - 「任务日志」该条 `ISSUE` 状态应为 `SUCCESS`，展开可见全过程
   - 「证书管理」出现该域名记录，状态 `ACTIVE`，有到期时间
   - 数据库 `cert_certificate` 有记录、`cert_task_log` 为 `SUCCESS`
   - 因为是 staging，证书签发机构是 `(STAGING) Let's Encrypt`，浏览器不信任属正常
5. **测部署（可选）**：建一个部署目标（Nginx/宝塔/七牛云），点「测试」确认连通；再回证书点「部署」。
6. **切生产**：staging 全流程 OK 后，把 ACME 账户的「使用测试目录」取消（或新建一个生产账户），对真实域名重新「新建签发」，得到浏览器/小程序信任的正式证书并自动部署。

---

## 常见问题

- **DNS 验证失败 / 超时**：检查西部数码凭证、域名 zone 是否正确；可适当调大 `cert.acme.dns-propagation-seconds`（默认 30s）。
- **西部数码接口报错**：`WestDnsProvider` 的 API 端点以官方最新文档为准，可在 DNS 服务商凭证里加 `endpoint` 字段覆盖默认地址。
- **生产限频**：Let's Encrypt 生产环境对同一域名有签发频率限制，联调务必先用 staging。
- **加密密钥**：`CERT_CRYPTO_SECRET` 一旦用于加密存储凭证，后续不可更换，否则已存凭证无法解密。上线前请用强密钥并妥善保管。
