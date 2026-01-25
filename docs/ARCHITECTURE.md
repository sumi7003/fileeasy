# 系统架构文档 (System Architecture)

## 1. 总体架构图

```mermaid
graph TD
    subgraph "终端层 (Edge/Client)"
        Android[Android Player App]
        Browser[Web Browser (Optional)]
    end

    subgraph "接入层 (Gateway)"
        LB[Nginx Load Balancer]
        WS[WebSocket Server]
    end

    subgraph "业务层 (Business Layer)"
        API[NestJS REST API]
        Scheduler[Task Scheduler]
        MediaMgr[Media Manager]
    end

    subgraph "数据层 (Data Layer)"
        DB[(PostgreSQL)]
        Redis[(Redis Cache)]
        OSS[Object Storage (MinIO/S3)]
    end

    subgraph "管理层 (Management)"
        Admin[React Web Admin]
    end

    Android <-->|HTTP/WS| LB
    Admin <-->|HTTP| LB
    LB <--> API
    LB <--> WS
    API --> DB
    API --> Redis
    API --> OSS
    WS <--> Redis
```

## 2. 核心模块说明

### 2.1 服务端 (Server)
- **框架**: NestJS (Node.js)
- **通信协议**:
  - HTTP (REST): 用于管理后台的数据交互（CRUD）。
  - WebSocket (Socket.io): 用于服务端向 Android 终端实时推送指令（如截屏、立即插播、更新配置）。
- **鉴权**: JWT (JSON Web Token) + RBAC (基于角色的权限控制)。

### 2.2 终端 (Android Player)
- **技术**: Native Android (Kotlin)
- **播放引擎**: Google ExoPlayer (视频) + Glide/Coil (图片)。
- **保活机制**: 
  - 前台 Service 提升进程优先级。
  - `BootReceiver` 监听开机广播。
  - `AlarmManager` 定时心跳唤醒。
- **缓存策略**: 
  - 采用 LRU 策略管理本地存储空间。
  - 文件哈希校验 (MD5) 确保素材完整性。

### 2.3 数据库设计概要 (Schema Design)

#### Device (设备表)
- `id`: UUID
- `serial_number`: 硬件唯一标识
- `name`: 设备别名
- `status`: online | offline
- `last_heartbeat`: Timestamp
- `group_id`: FK -> Group

#### Media (素材表)
- `id`: UUID
- `url`: 存储地址
- `type`: image | video
- `duration`: 默认播放时长
- `hash`: MD5

#### Playlist (播放列表)
- `id`: UUID
- `items`: JSON Array (包含素材ID、顺序、播放时长)
- `schedule`: JSON (生效时间段)

## 3. 部署方案
所有服务容器化，通过 Docker Compose 编排。
Android 端通过 Gradle 构建生成 APK。

