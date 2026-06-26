# Cafe Queue System

校园智能饮品排队与取餐系统，包含 Vue 前端、Spring Boot 微服务后端、MySQL、Redis 和 Nacos。项目支持优惠券领取、下单、支付确认、制作排队、取餐码展示、通知与设备调度等流程。

## 技术栈

- 前端：Vue 3、Vite、Nginx
- 后端：Spring Boot、Spring Cloud Gateway、Maven
- 中间件：MySQL 5.7、Redis 5、Nacos 2.4.3
- 部署：Docker Compose

## 目录结构

```text
backend/    后端微服务、公共模块、Docker Compose 配置
frontend/   前端页面与 Nginx 配置
```

主要后端服务：

- `gateway-service`：统一网关、登录令牌、鉴权、路由转发
- `order-service`：订单创建、支付回调、订单状态流转
- `recipe-inventory-service`：配方库存与库存预占
- `vending-queue-service`：制作排队与等待时间估算
- `notification-service`：学生通知
- `marketing-coupon-service`：优惠券发放、锁定与核销
- `vending-device-service`：咖啡机设备状态与制作指令

## 启动方式

环境要求：

- Docker Desktop
- Java 24
- Maven 3.9+
- Node.js 22+（仅前端本地开发需要）

首次启动前，在项目根目录执行：

```powershell
Copy-Item backend\.env.example backend\.env
```

然后修改 `backend\.env` 中的配置项：

```text
MYSQL_ROOT_PASSWORD=你的 MySQL root 密码
MYSQL_USER=cafequeue
MYSQL_PASSWORD=你的业务数据库密码
JWT_SECRET=你的 JWT 密钥
```

一键启动：

```powershell
docker compose up -d --build
```

访问地址：

- 前端：http://localhost:5173
- 后端网关：http://localhost:8080
- Nacos：http://localhost:8848
- MySQL：localhost:13306
- Redis：localhost:6379

## 常用命令

查看容器状态：

```powershell
docker compose ps
```

查看网关日志：

```powershell
docker logs cafequeue-gateway-service --tail 100
```

停止项目：

```powershell
docker compose down
```

后端单独打包：

```powershell
cd backend
mvn -DskipTests package
```

前端单独构建：

```powershell
cd frontend
npm install
npm run build
```

## 注意事项

- 不要提交 `backend\.env`，仓库只保留 `backend\.env.example`。
- 如果前端出现 502，通常是网关未启动完成，可以先查看 `cafequeue-gateway-service` 日志。
- 如果端口被占用，请先停止旧容器或修改 `backend\docker-compose.yml` 中的端口映射。
