# Cafe Queue System 启动指南

## 环境要求

- Docker Desktop
- Java 24
- Maven 3.9+
- Node.js 22+（仅本地前端开发需要）

## Docker 一键启动

在项目根目录执行：

```powershell
cd E:\ProgramProjects\SpringBoot\Cafe-Queue-System
Copy-Item backend\.env.example backend\.env
# 首次启动前请修改 backend\.env 中的 MYSQL_ROOT_PASSWORD、MYSQL_PASSWORD、JWT_SECRET
docker compose -f backend\docker-compose.yml up -d
```

访问地址：

- 前端：http://localhost:5173
- 后端网关：http://localhost:8080
- Nacos：http://localhost:8848
- MySQL：localhost:13306，用户和密码见 `backend\.env`
- Redis：localhost:6379

## 重新打包

后端：

```powershell
cd backend
mvn -DskipTests package
```

前端：

```powershell
cd frontend
npm install
npm run build
```

重新构建并启动 Docker：

```powershell
docker compose -f backend\docker-compose.yml up -d --build
```

## 常用排查命令

```powershell
docker compose -f backend\docker-compose.yml ps
docker logs cafequeue-gateway-service --tail 100
docker logs cafequeue-frontend --tail 100
docker compose -f backend\docker-compose.yml down
```

如果前端出现 502，通常是网关启动失败或 Nginx 缓存了旧网关地址，可执行：

```powershell
docker start cafequeue-gateway-service
docker restart cafequeue-frontend
```
