# 阿里云 ECS 部署指南

## 架构概览

- 服务器：阿里云 ECS Ubuntu 22.04
- 编排：Docker Compose
- 镜像仓库：阿里云容器镜像服务 ACR（个人版，免费）
- 服务：frontend、backend、mysql、redis、rabbitmq、milvus + etcd + minio

## 1. 服务器前置要求

- 最低配置：2C4G（Milvus 吃内存，建议 4G 以上）
- 系统：Ubuntu 22.04

安全组放行：
- `22`：SSH
- `80`：前端站点
- `443`：HTTPS（绑定域名后）

以下端口**不要**对公网开放，容器内网通信即可：
- 3306（MySQL）、6379（Redis）、5672（RabbitMQ）、19530（Milvus）

## 2. 安装 Docker

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

## 3. ACR 镜像仓库信息

- 区域：华南3（广州）
- 命名空间：`lq_travel`
- 用户名：`nick1539634772`
- 仓库地址：

| 仓库 | 公网地址 |
|------|----------|
| backend | `crpi-4dquy2tjlozfjo6n.cn-guangzhou.personal.cr.aliyuncs.com/lq_travel/backend` |
| frontend | `crpi-4dquy2tjlozfjo6n.cn-guangzhou.personal.cr.aliyuncs.com/lq_travel/frontend` |

ECS 内网推送（免流量）：`crpi-4dquy2tjlozfjo6n-vpc.cn-guangzhou.personal.cr.aliyuncs.com`

## 4. 本地构建并推送镜像

```bash
# 登录 ACR
docker login --username=nick1539634772 crpi-4dquy2tjlozfjo6n.cn-guangzhou.personal.cr.aliyuncs.com

# 构建镜像（本地执行）
docker compose build

# 打标签并推送
docker tag travel-backend crpi-4dquy2tjlozfjo6n.cn-guangzhou.personal.cr.aliyuncs.com/lq_travel/backend:latest
docker tag travel-frontend crpi-4dquy2tjlozfjo6n.cn-guangzhou.personal.cr.aliyuncs.com/lq_travel/frontend:latest
docker push crpi-4dquy2tjlozfjo6n.cn-guangzhou.personal.cr.aliyuncs.com/lq_travel/backend:latest
docker push crpi-4dquy2tjlozfjo6n.cn-guangzhou.personal.cr.aliyuncs.com/lq_travel/frontend:latest
```

后续更新代码后，重复 build → tag → push 即可。

## 5. 服务器部署

```bash
# 克隆仓库
git clone https://github.com/Lq0412/travel.git
cd travel

# 创建环境变量文件
cp .env.example .env
nano .env  # 填写生产环境的密码和 API Key

# 登录 ACR（服务器也需要登录才能拉私有镜像）
docker login --username=nick1539634772 crpi-4dquy2tjlozfjo6n.cn-guangzhou.personal.cr.aliyuncs.com

# 拉取镜像并启动
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

## 6. 数据初始化

首次部署时，MySQL 会自动执行 `travel.sql` 建表。

如果有备份数据需要导入：

```bash
docker exec -i travel-mysql mysql -uroot -p你的密码 < travel-data-backup.sql
```

## 7. 常用运维命令

```bash
# 查看所有服务状态
docker compose -f docker-compose.prod.yml ps

# 查看后端日志
docker compose -f docker-compose.prod.yml logs -f backend

# 更新部署（本地 push 新镜像后）
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d

# 重建单个服务
docker compose -f docker-compose.prod.yml up -d --force-recreate backend

# 停止所有服务
docker compose -f docker-compose.prod.yml down

# 停止并清除数据卷（慎用，会丢失数据库数据）
docker compose -f docker-compose.prod.yml down -v
```

## 8. 文件说明

| 文件 | 用途 |
|------|------|
| `docker-compose.yml` | 本地开发，从源码 build |
| `docker-compose.prod.yml` | 生产部署，从 ACR 拉取镜像 |
| `.env.example` | 环境变量模板 |
| `.env` | 实际环境变量（不提交 Git） |
| `travel-data-backup.sql` | 数据库备份 |

## 9. 注意事项

- `.env` 包含密码和 API Key，不要提交到 Git（已在 .gitignore 中）
- 生产环境密码请重新生成，不要使用开发环境的密码
- 服务器内存不足时，Milvus 可能启动失败，可用 `AI_RAG_MILVUS_ENABLED=false` 关闭
- 后续绑域名加 HTTPS，可用 Caddy 或 Nginx + Let's Encrypt
