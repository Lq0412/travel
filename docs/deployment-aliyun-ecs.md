# 阿里云 ECS 部署说明

本文档对应当前仓库的生产部署方案：

- 服务器：阿里云 ECS Ubuntu 22.04
- 访问方式：公网 IP 直连
- 编排方式：Docker Compose
- 首版服务：`frontend`、`backend`、`mysql`、`redis`、`rabbitmq`、`milvus`

## 1. 服务器前置要求

阿里云安全组至少放行以下端口：

- `22`：SSH
- `80`：前端站点

首版不建议对公网开放以下端口：

- `3306`
- `6379`
- `5672`
- `15672`
- `19530`
- `9091`

这些端口通过容器内部网络访问即可。

## 2. 安装 Docker 和 Compose

```bash
sudo apt update
sudo apt install -y ca-certificates curl gnupg lsb-release
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable docker
sudo systemctl start docker
```

## 3. 上传项目

```bash
git clone <你的仓库地址>
cd travel
cp .env.example .env
```

## 4. 填写服务器环境变量

编辑根目录 `.env`：

```bash
nano .env
```

至少需要填写这些值：

- `MYSQL_ROOT_PASSWORD`
- `MYSQL_PASSWORD`
- `REDIS_PASSWORD`
- `RABBITMQ_DEFAULT_PASS`
- `MINIO_ROOT_PASSWORD`
- `DASHSCOPE_API_KEY`
- `TAVILY_API_KEY`
- `AMAP_KEY`
- `VITE_AMAP_KEY`
- `VITE_AMAP_SECURITY_CODE`
- `PEXELS_API_KEY`
- `COS_CLIENT_HOST`
- `COS_CLIENT_REGION`
- `COS_CLIENT_BUCKET`
- `COS_CLIENT_SECRETID`
- `COS_CLIENT_SECRETKEY`

说明：

- MySQL 首次启动时会自动导入 [travel.sql](C:/Users/Lq304/Desktop/travel/travel_backend/src/main/resources/sql/travel.sql)
- 只有在 `mysql-data` 卷为空时才会执行初始化 SQL

## 5. 启动服务

在仓库根目录执行：

```bash
docker compose up -d --build
```

查看启动状态：

```bash
docker compose ps
docker compose logs -f backend
```

## 6. 访问地址

首版使用公网 IP 访问：

- 前端首页：`http://8.163.101.247`

如果后续绑定域名，再补：

- HTTPS
- `443` 监听
- Nginx 证书配置

## 7. 常用运维命令

重建前后端镜像：

```bash
docker compose up -d --build frontend backend
```

查看日志：

```bash
docker compose logs -f frontend
docker compose logs -f backend
docker compose logs -f mysql
docker compose logs -f redis
docker compose logs -f rabbitmq
docker compose logs -f milvus
```

停止服务：

```bash
docker compose down
```

保留容器卷情况下重启：

```bash
docker compose down
docker compose up -d
```

## 8. 首版上线建议

- 先确认 `backend` 能连通 `mysql`、`redis`、`rabbitmq`、`milvus`
- 再在浏览器访问 `http://8.163.101.247`
- 如果 AI 工作台流式接口异常，优先检查 `backend` 日志和 DashScope Key
- 如果 Milvus 相关页面异常，先确认 `milvus`、`etcd`、`minio` 三个容器都已启动

## 9. 后续建议

- 绑定域名后再加 HTTPS
- 把仓库里已经出现过的真实密钥统一轮换
- 如果 ECS 内存偏小，可以考虑第二阶段再把 `milvus` 独立出去
