# Xplay - 企业级数字标牌系统

> 🚀 **当前推荐架构**: Android Host Mode (一主多从) | 零成本 | 支持30+台设备

---

## ⚠️ 重要提示

### 当前推荐方案: Android Host Mode

**如果你有多台Android Pad，强烈推荐使用一主多从架构：**

- ✅ **零额外成本** - 不需要购买服务器或树莓派
- ✅ **配置简单** - 30分钟完成所有配置
- ✅ **性能充足** - 单台主Pad可支持30+台从Pad
- ✅ **维护方便** - 只需要管理Android应用

**快速开始**: 查看 [主Pad设计文档](docs/MASTER_PAD_DESIGN.md)

### NestJS独立服务器已废弃

NestJS服务器代码**已移至废弃目录**：

- ❌ **不推荐使用** - 除非设备数量>50台
- 📁 **已移至** - `apps/_deprecated_server/` 目录
- 📖 **详见文档** - [废弃说明](docs/DEPRECATED_NESTJS_SERVER.md)
- 🔄 **如需重启** - [完整重启指南](apps/_deprecated_server/README_DEPRECATED.md)

**原因**: 维护双服务端架构成本高，Android Host Mode已足够满足大多数场景。

---

## 🚀 快速开始 (Android Host Mode)

### 准备工作
- 多台Android Pad (推荐4GB+ RAM)
- 同一Wi-Fi网络

### 配置步骤 (10分钟)

**1. 配置主Pad (管理中枢)**
```
1. 安装Xplay App到性能最好的Pad
2. 打开App → 开启"服务端角色"
3. 记录主Pad IP地址 (如: 192.168.1.100)
4. 点击"查看系统监控" → 看到监控面板
5. 点击"管理素材与播放列表" → 进入管理后台
```

**2. 配置从Pad (播放器)**
```
1. 安装Xplay App到其他所有Pad
2. 打开App → 输入主Pad IP: 192.168.1.100
3. 自动连接并注册
4. 在主Pad监控面板中看到设备上线
```

**3. 开始使用**
```
1. 主Pad管理后台 → 上传素材
2. 创建播放列表
3. 分配给从Pad
4. 从Pad自动开始播放 (30秒内)
```

**详细文档**: [主Pad设计文档](docs/MASTER_PAD_DESIGN.md)

---

## 🧪 旧文档: NestJS服务器测试指南 (已废弃)

<details>
<summary>⚠️ 点击展开 - 仅供参考，不推荐使用</summary>

## 1. 极速测试指南 (如何立刻看到效果)

如果您目前在本地电脑运行服务端，手机/模拟器运行客户端，请按以下步骤操作：

### 第一步：获取电脑 IP 并配置 App
由于手机无法识别 `localhost`，必须使用局域网 IP。
1. **查 IP**: 在电脑终端运行 `ifconfig | grep "inet " | grep -v 127.0.0.1` (Mac) 或 `ipconfig` (Win)。假设是 `192.168.1.5`。
2. **改代码**: 修改 `apps/android-player/src/main/java/com/xplay/player/data/api/NetworkModule.kt` 第 9 行：
   ```kotlin
   private const val BASE_URL = "http://192.168.1.5:3000/api/v1/"
   ```
3. **重装 App**:
   ```bash
   cd apps/android-player && ./gradlew assembleDebug
   adb -s <您的设备ID> install -r ./build/outputs/apk/debug/XplayPlayer-debug.apk
   ```

### 第二步：启动服务
1. **服务端 (已设为 SQLite 模式，无需数据库)**:
   ```bash
   pnpm --filter @xplay/server start:dev
   ```
2. **管理后台**:
   ```bash
   pnpm --filter @xplay/web-admin dev
   ```

### 第三步：全链路闭环测试
1. **打开后台**: 浏览器访问 `http://localhost:3001`。
2. **上传**: 在 [素材库] 上传一个图片或视频。
3. **编排**: 在 [播放列表] 新建一个列表并加入该素材。
4. **上线**: 在手机上打开 "Xplay Player"，观察后台 [设备管理] 刷新，看到手机上线。
5. **发布**: 点击设备操作栏的 [分配节目]，选择刚才的列表。
6. **成功**: 观察手机，30秒内应开始播放。

---

## 🏗 2. 服务端部署说明书 (正式环境)

正式部署建议使用 **Docker + PostgreSQL**，以保证 50+ 台设备的稳定性。

### 1. 准备工作
- 服务器安装 Docker 和 Docker Compose。
- 将代码上传至服务器。

### 2. 修改配置 (从开发切到生产)
⚠️ **此步骤仅适用于NestJS服务器（已废弃）**

如重启NestJS，需修改 `apps/_deprecated_server/src/app.module.ts`（或恢复后的 `apps/server/src/app.module.ts`），将 `TypeOrmModule` 改回 PostgreSQL 配置。

### 3. 使用 Docker 一键部署
在项目根目录执行：
```bash
# 构建镜像并后台启动
docker-compose up -d --build
```
该命令会自动启动：
- **PostgreSQL**: 端口 5432，持久化数据在 `postgres_data` 卷。
- **NestJS Server**: 端口 3000。

### 4. 静态资源持久化
⚠️ **NestJS服务器**: 上传的素材存储在 `apps/_deprecated_server/uploads/` 目录（或恢复后的 `apps/server/uploads/`）。

**Android Host Mode**: 素材存储在主Pad内部 `/data/data/com.xplay.player/files/uploads/`。
**注意**: 在 Docker 环境下，请确保 `docker-compose.yml` 中配置了该目录的 Volume 映射，否则容器重启素材会丢失。

### 5. Nginx 反向代理 (推荐配置)
为了支持域名和 80 端口，建议配置 Nginx：
```nginx
server {
    listen 80;
    server_name xplay.yourdomain.com;

    location / {
        proxy_pass http://127.0.0.1:3001; # 指向 Web Admin
    }

    location /api {
        proxy_pass http://127.0.0.1:3000; # 指向 Server API
    }

    location /uploads {
        proxy_pass http://127.0.0.1:3000; # 指向 Server 静态资源
    }
}
```

---

## 🛠 代码与项目管理规范

- **代码风格**: 统一使用 Prettier 格式化 (`pnpm format`)。
- **代码评审**: 核心逻辑（如下发引擎）必须经过 Review。
- **测试**: 涉及 50 台设备的高并发心跳，已通过轻量化 DTO 优化。

</details>

---

## 📚 文档导航

### 推荐阅读 (新架构)
1. **[主Pad设计文档](docs/MASTER_PAD_DESIGN.md)** ⭐ 最重要
   - 一主多从架构详解
   - 配置步骤和监控面板
   - 性能测试数据

2. **[高性能Pad方案](docs/HIGH_PERFORMANCE_PAD_SOLUTION.md)**
   - 详细实施指南
   - 批量配置脚本
   - 主备切换方案

3. **[详细流程分析](docs/DETAILED_FLOW_ANALYSIS.md)**
   - 服务端和客户端完整流程
   - 易出问题的地方标注
   - 调试工具和技巧

4. **[故障排查指南](docs/TROUBLESHOOTING.md)**
   - 5分钟快速定位问题
   - 常见问题解决方案
   - 诊断脚本

### 参考文档 (旧架构)
- [NestJS服务器废弃说明](docs/DEPRECATED_NESTJS_SERVER.md) ⚠️
- [完整重启指南](apps/_deprecated_server/README_DEPRECATED.md) ⚠️

---

## 🎯 架构选择建议

### 快速决策

```
你有多台Android Pad吗？
  └─ 是 → 使用 Android Host Mode ✅
  └─ 否 → 需要购买设备
        └─ 预算有限 → 购买1台高性能Pad作为主Pad ✅
        └─ 预算充足 → 可选择树莓派/服务器 ⚠️

设备数量？
  └─ ≤30台 → Android Host Mode ✅
  └─ 31-50台 → Android Host Mode (主备) ✅
  └─ >50台 → 考虑NestJS服务器 ⚠️

在局域网内使用？
  └─ 是 → Android Host Mode ✅
  └─ 否 → 需要云端管理 → NestJS + 云服务器 ⚠️
```

### 成本对比

| 方案 | 初始投入 | 月度成本 | 维护难度 | 推荐度 |
|-----|---------|---------|---------|-------|
| Android Host Mode | 0元 | 0元 | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| NestJS + 树莓派 | 300元 | 0元 | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| NestJS + 云服务器 | 0元 | 120元 | ⭐⭐⭐⭐⭐ | ⭐⭐ |

**结论**: 90%的场景推荐使用Android Host Mode

---

## 💬 获取帮助

### 常见问题
- 配置问题: 查看 [主Pad设计文档](docs/MASTER_PAD_DESIGN.md)
- 运行问题: 查看 [故障排查指南](docs/TROUBLESHOOTING.md)
- NestJS相关: 查看 [废弃说明](docs/DEPRECATED_NESTJS_SERVER.md)

### 联系方式
- GitHub Issues: [提交问题]
- 技术社区: [待补充]
- 企业支持: [待补充]

---

## 📄 版权说明

基于 [MIT License](./LICENSE) 开源。
参考项目: [xplay.io](https://github.com/wendal/xplay.io)

**最后更新**: 2026-01-16  
**当前版本**: v2.0 (Android Host Mode)  
**维护者**: 开发团队
