# DZ 餐饮娱乐小程序后端

基于 JDK 17、Spring Boot 3.2、MyBatis-Plus 3.5、MySQL 8 的 Maven 多模块项目。
项目按用户最新要求不使用 Redis，当前微信登录、支付、订阅消息和对账默认运行在 mock 模式。

## 工程结构

```text
dz/
├── dz-common/   统一响应、异常、JWT、注解、枚举、工具
├── dz-dao/      实体、Mapper、Mapper.xml、schema.sql
├── dz-service/  业务服务、账户引擎、订单、支付、充值、积分
├── dz-api/      小程序端 API，端口 8080
├── dz-admin/    管理后台 API，端口 8081
└── http/        主流程 HTTP 调用示例
```

## 无 Redis 方案

- 幂等：`idempotent_record` 唯一键 + 过期时间，替代 Redis SETNX。
- 首页公告：`home_content` 表存储。
- 订阅任务：`notify_task` 表持久化并重试 3 次。
- 微信 access token：`wechat_access_token` 表持久化缓存，提前 2 分钟刷新。
- Docker Compose 只启动 MySQL、`dz-api`、`dz-admin`。

## 环境变量

必填：

| 变量 | 说明 |
|---|---|
| `DB_USERNAME` | MySQL 用户名 |
| `DB_PASSWORD` | MySQL 密码 |
| `JWT_SECRET` | JWT HMAC 密钥，至少 32 字符 |
| `ADMIN_INITIAL_PASSWORD` | 首次启动时创建 `admin` 管理员使用的初始密码，不会输出到日志 |

可选：

| 变量 | 默认值/说明 |
|---|---|
| `DB_URL` | 本机 `dz` 数据库连接 |
| `WECHAT_MOCK_ENABLED` | `true` |
| `WECHAT_AUTH_MOCK_ENABLED` | `true`，设为 `false` 后单独启用真实微信登录 |
| `UPLOAD_ROOT` | `F:/works/dz/uploads` |
| `WECHAT_APP_ID` | 真实微信配置 |
| `WECHAT_APP_SECRET` | 真实微信配置 |
| `WECHAT_MCH_ID` | 微信支付商户号 |
| `WECHAT_MERCHANT_PRIVATE_KEY_PATH` | 商户私钥路径 |
| `WECHAT_MERCHANT_SERIAL_NUMBER` | 商户证书序列号 |
| `WECHAT_API_V3_KEY` | APIv3 密钥 |

敏感值未写入代码或 `application.yml`。

## 本地启动

1. 安装 JDK 17+、Maven 3.9+、MySQL 8。
2. 创建数据库：`CREATE DATABASE dz CHARACTER SET utf8mb4;`
3. 设置环境变量。
4. 构建：

```powershell
mvn clean package
```

5. 启动小程序端：

```powershell
java -jar dz-api\target\dz-api-1.0.0-SNAPSHOT.jar
```

6. 启动管理端：

```powershell
java -jar dz-admin\target\dz-admin-1.0.0-SNAPSHOT.jar
```

启动时自动执行 `dz-dao/src/main/resources/sql/schema.sql`。管理端首次启动会创建
`admin`，随机初始密码仅打印一次到管理端日志。

Swagger：

- 小程序端：http://localhost:8080/swagger-ui.html
- 管理端：http://localhost:8081/swagger-ui.html

## Docker Compose

先完成 Maven 打包，然后从 `.env.example` 创建本地 `.env` 并填写安全值：

```powershell
mvn clean package
docker compose up --build
```

## Mock 规则

- 小程序登录 `code=demo` 固定映射到初始化用户 `mock-openid-demo`。
- 其他 code 映射到 `mock-openid-{code}`。
- 支付通知必须携带请求头 `X-Mock-Signature: valid`。
- mock 支付通知体仍会校验订单金额和 openid。
- 初始化商品 SKU：`1`（2800 分）、`2`（3600 分）。
- 初始化用户余额 10000 分、积分 1000。

完整示例见 `http/main-flow.http`。

## 资金与并发规则

- 金额统一使用 `Long`/`BIGINT` 分。
- `AccountService` 是余额、积分、冻结积分变更的唯一入口。
- 账户更新使用 `version` 乐观锁，失败最多重试 3 次。
- 每次账户变更记录 `account_log` 的变更前后值。
- 库存使用 `stock >= quantity` 条件更新，避免超卖。
- 订单状态只能通过 `OrderStateMachine` 校验后流转。
- 支付通知校验签名、数据库金额、订单用户 openid。
- 支付、充值、积分审核同时使用 MySQL 幂等记录和业务唯一约束。

## 定时任务

- 每 60 秒取消创建超过 15 分钟的未支付订单并回补库存。
- 每 10 秒发送待处理订阅消息，失败最多重试 3 次。

## 测试

```powershell
mvn test
```

已覆盖：

- 10 线程幂等请求只执行一次。
- 余额 100 并发两次扣 60，只成功一次。
- 库存 10 并发 20 次下单，成功恰好 10 次。
- 微信支付重复通知只处理一次。
- 积分重复审核只处理一次。

## 微信登录

真实 `code2Session` 和手机号授权已接入。配置 `WECHAT_APP_ID`、
`WECHAT_APP_SECRET`，并将 `WECHAT_AUTH_MOCK_ENABLED=false` 后启用。
手机号通过小程序 `getPhoneNumber` 授权绑定，不使用短信验证码。

## 待接真实微信支付

正式上线前必须完成：

1. 基于官方 `wechatpay-java` SDK 实现真实 JSAPI 下单、回调验签解密、查单、关单、退款和账单下载。
2. 配置订阅消息 templateId，并完成模板字段映射。
3. 支付查单每 30 秒兜底与退款回调需接入真实微信结果。
4. 对账当前返回 mock 空差异，真实账单接入后写入 `reconcile_diff`。
5. 生产环境应在网关限制支付回调来源，并补充集成测试、压测和数据库备份策略。

真实支付未接入前，不得用于真实资金交易。
