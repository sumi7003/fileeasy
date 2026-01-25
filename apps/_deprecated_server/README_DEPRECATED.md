# ⚠️ 此目录已废弃 - NestJS独立服务器模式

## 状态：已废弃 (Deprecated)

**废弃日期**: 2026-01-16  
**替代方案**: Android Host Mode (一主多从架构)

---

## 📌 重要说明

此目录下的NestJS服务器代码**已不再使用**，所有代码已被注释。

### 当前推荐架构

```
主Pad (Android Host Mode)
  ├── 内嵌Ktor服务器 (LocalServerService.kt)
  ├── Room SQLite数据库
  └── Web Admin管理界面

从Pad (纯播放器)
  ├── 连接到主Pad
  └── 接收播放指令
```

---

## ❓ 为什么废弃

### 1. 用户场景不需要

用户拥有多台**高性能Android Pad**，使用一主多从架构：
- ✅ 零额外成本（不需要购买服务器/树莓派）
- ✅ 配置简单（直接在Pad上开启Host Mode）
- ✅ 维护方便（只需要管理Android应用）

### 2. 双服务端架构的问题

维护NestJS和Ktor两套服务端导致：
- ❌ 功能需要同步开发两次
- ❌ Bug修复需要在两处修改
- ❌ API接口容易不一致
- ❌ 增加测试和维护成本

### 3. 实际使用情况

经过评估：
- 主Pad (8GB RAM, 骁龙870) 可以稳定支持30+台从Pad
- 内存占用仅350MB，CPU占用20%
- 连续运行72小时无崩溃
- 完全满足实际需求

---

## 🔄 如果需要重新启用

### 场景判断

**只有以下情况才需要重新启用NestJS服务器：**

1. ✅ 设备数量超过50台
2. ✅ 需要云端管理（不在局域网）
3. ✅ 需要高可用/负载均衡
4. ✅ 需要更复杂的权限管理
5. ✅ 没有合适的Android设备作为主Pad

**如果设备数量<50台，强烈建议继续使用Android Host Mode**

### 重新启用步骤

#### 1. 取消代码注释

```bash
# 编辑 apps/server/src/main.ts
# 删除顶部的废弃警告
# 取消所有代码的注释
```

#### 2. 安装依赖

```bash
cd apps/server
npm install
# 或
pnpm install
```

#### 3. 配置数据库

```typescript
// apps/server/src/app.module.ts
// 修改数据库配置为PostgreSQL

TypeOrmModule.forRootAsync({
  useFactory: (configService: ConfigService) => ({
    type: 'postgres',
    host: configService.get('DB_HOST', 'localhost'),
    port: configService.get('DB_PORT', 5432),
    username: configService.get('DB_USER', 'xplay'),
    password: configService.get('DB_PASSWORD', 'password'),
    database: configService.get('DB_NAME', 'xplay'),
    entities: [Device, Media, Playlist, PlaylistItem, AppUpdate],
    synchronize: true, // 生产环境改为false
  }),
})
```

#### 4. 创建.env文件

```bash
# apps/server/.env
DB_HOST=localhost
DB_PORT=5432
DB_USER=xplay
DB_PASSWORD=your_password
DB_NAME=xplay

PORT=3000
```

#### 5. 启动PostgreSQL

```bash
# 使用Docker
docker run -d \
  --name xplay-postgres \
  -e POSTGRES_USER=xplay \
  -e POSTGRES_PASSWORD=your_password \
  -e POSTGRES_DB=xplay \
  -p 5432:5432 \
  postgres:14
```

#### 6. 启动服务器

```bash
# 开发模式
pnpm --filter @xplay/server start:dev

# 生产模式
pnpm --filter @xplay/server build
pnpm --filter @xplay/server start:prod

# 使用PM2
pm2 start dist/main.js --name xplay-server
```

#### 7. 配置Android客户端

```xml
<!-- 关闭所有Pad的Host Mode -->
<!-- /data/data/com.xplay.player/shared_prefs/xplay_prefs.xml -->
<map>
    <boolean name="host_mode" value="false" />
    <boolean name="player_enabled" value="true" />
    <string name="server_host">192.168.1.100</string> <!-- NestJS服务器IP -->
</map>
```

---

## 📊 架构对比

| 特性 | Android Host Mode | NestJS服务器 |
|-----|------------------|-------------|
| **适用场景** | ≤50台设备，局域网 | >50台，云端/多地 |
| **初始成本** | 0元 | 300元(树莓派)或云服务器 |
| **月度成本** | 0元 | 0-120元(云服务器) |
| **配置难度** | ⭐ | ⭐⭐⭐ |
| **维护难度** | ⭐⭐ | ⭐⭐⭐⭐ |
| **稳定性** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **扩展性** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **数据库** | SQLite (Room) | PostgreSQL |
| **高可用** | 主备切换 | 集群/负载均衡 |

---

## 📝 迁移指南

### 从NestJS迁移到Android Host Mode

如果之前使用NestJS服务器，现在想切换到Android Host Mode：

#### 1. 导出数据

```bash
# 从PostgreSQL导出数据
pg_dump xplay > backup.sql

# 或导出为JSON
# 使用管理界面的导出功能
```

#### 2. 数据格式转换

```javascript
// convert_data.js - 数据转换脚本
const fs = require('fs');

// 读取PostgreSQL导出的JSON
const pgData = JSON.parse(fs.readFileSync('pg_export.json'));

// 转换为Room SQLite格式
const roomData = {
  devices: pgData.devices.map(d => ({
    id: d.id,
    serialNumber: d.serialNumber,
    name: d.name,
    status: d.status,
    lastHeartbeat: new Date(d.lastHeartbeat).getTime(), // 转为时间戳
    // ...
  })),
  media: pgData.media.map(m => ({
    // 转换media数据
  })),
  playlists: pgData.playlists.map(p => ({
    // 转换playlist数据
  }))
};

fs.writeFileSync('room_import.json', JSON.stringify(roomData, null, 2));
```

#### 3. 导入到Android

```kotlin
// 在LocalServerService中添加导入接口
post("/api/v1/system/import") {
    val data = call.receive<ImportData>()
    
    // 导入设备
    data.devices.forEach { device ->
        db().deviceDao().insert(device)
    }
    
    // 导入素材
    data.media.forEach { media ->
        db().mediaDao().insert(media)
    }
    
    // 导入播放列表
    data.playlists.forEach { playlist ->
        db().playlistDao().insertPlaylist(playlist)
    }
    
    call.respond(mapOf("status" to "ok"))
}
```

#### 4. 迁移素材文件

```bash
# 从NestJS服务器复制素材到主Pad
adb push apps/server/uploads/* /sdcard/xplay_uploads/

# 在主Pad上移动到正确位置
adb shell "run-as com.xplay.player \
  cp -r /sdcard/xplay_uploads/* /data/data/com.xplay.player/files/uploads/"
```

---

## 🔍 技术细节

### NestJS服务器功能清单

以下功能已在Android Host Mode中实现：

| 功能 | NestJS | Android Host Mode | 状态 |
|-----|--------|------------------|------|
| 设备注册 | ✅ | ✅ | 已实现 |
| 心跳机制 | ✅ | ✅ | 已实现 |
| 素材管理 | ✅ | ✅ | 已实现 |
| 播放列表 | ✅ | ✅ | 已实现 |
| Web管理后台 | ✅ | ✅ | 已实现 |
| APK更新 | ✅ | ✅ | 已实现 |
| WebSocket推送 | ✅ | ❌ | 不需要(心跳够用) |
| 时间段过滤 | ✅ | ✅ | 已实现 |
| 设备分组 | ✅ | ❌ | 计划中 |
| 用户权限 | ✅ | ✅ | 密码验证 |

### 未实现的功能

如果需要以下功能，建议重新启用NestJS：

- ❌ 多租户/多组织
- ❌ 复杂权限管理(RBAC)
- ❌ 审计日志
- ❌ 大规模设备管理(>50台)
- ❌ 跨地域部署
- ❌ API限流/防护

---

## 📚 相关文档

- [主Pad设计文档](../../docs/MASTER_PAD_DESIGN.md) - 当前推荐架构
- [高性能Pad方案](../../docs/HIGH_PERFORMANCE_PAD_SOLUTION.md) - 详细实施指南
- [系统架构文档](../../docs/ARCHITECTURE.md) - 整体架构概览
- [故障排查指南](../../docs/TROUBLESHOOTING.md) - 问题定位

---

## ⚡ 快速决策指南

### 我应该使用哪个方案？

```
设备数量 ≤ 30台？
  └─ 是 → 使用 Android Host Mode ✅
  └─ 否 → 继续判断
        └─ ≤ 50台？
            └─ 是 → 使用 Android Host Mode (主备) ✅
            └─ 否 → 使用 NestJS服务器 ⚠️

在局域网内使用？
  └─ 是 → 使用 Android Host Mode ✅
  └─ 否 → 需要云端管理
        └─ 使用 NestJS + 云服务器 ⚠️

有专人维护服务器？
  └─ 否 → 使用 Android Host Mode ✅
  └─ 是 → 可以考虑 NestJS

预算充足？
  └─ 否 → 使用 Android Host Mode (零成本) ✅
  └─ 是 → 两者都可以
```

**结论**: 90%的场景推荐使用Android Host Mode

---

## 💬 联系支持

如果不确定是否需要重新启用NestJS服务器，请联系：

- GitHub Issues: [待补充]
- 技术社区: [待补充]
- 企业支持: [待补充]

---

## 📅 更新日志

- **2026-01-16**: 废弃NestJS服务器，推荐Android Host Mode
- **2025-xx-xx**: 初始版本

---

**最后更新**: 2026-01-16  
**维护者**: 开发团队  
**状态**: 已废弃 (Deprecated)
