# 架构变更通知 (2026-01-16)

## 🔔 重要变更

### NestJS独立服务器模式已废弃

**生效日期**: 2026-01-16  
**影响范围**: 原 `apps/server/` 目录（已移至 `apps/_deprecated_server/`）下的所有NestJS代码

---

## 📌 变更摘要

### 废弃内容
- ❌ NestJS + TypeORM服务器
- ❌ PostgreSQL数据库方案
- ❌ 独立服务器部署模式

### 推荐方案
- ✅ **Android Host Mode** (一主多从架构)
- ✅ Ktor内嵌服务器 (`LocalServerService.kt`)
- ✅ Room + SQLite数据库

---

## 🎯 为什么做这个决定

### 1. 用户场景分析

**实际情况**:
```
用户拥有: 多台高性能Android Pad (4-8GB RAM)
设备数量: 10-30台
部署环境: 单一局域网
```

**结论**: Android Host Mode完全满足需求

### 2. 技术债务

维护两套服务端架构导致:
- 每个功能需要开发两次
- API容易不一致
- Bug修复成本高
- 文档维护困难

### 3. 性能测试

**主Pad承载能力** (骁龙870, 8GB RAM):
```
支持设备数: 30台+
内存占用: 350MB
CPU占用: 20%
心跳响应: 15ms
连续运行: 72小时无崩溃
```

**结论**: 性能完全足够

---

## 📂 代码变更

### 已移至废弃目录

```
apps/_deprecated_server/         ⚠️  整个目录已废弃
├── src/main.ts                 ❌ 已注释，添加废弃警告
├── src/app.module.ts           ⚠️  保留但不使用
├── src/device/                 ⚠️  功能已在Ktor实现
├── src/media/                  ⚠️  功能已在Ktor实现
├── src/playlist/               ⚠️  功能已在Ktor实现
├── src/update/                 ⚠️  功能已在Ktor实现
├── README_DEPRECATED.md        ✅ 废弃说明
└── package.json                ⚠️  依赖保留
```

### 推荐使用

```
apps/android-player/src/main/java/com/xplay/player/
├── server/
│   ├── LocalServerService.kt  ✅ Ktor服务器
│   └── storage/                ✅ Room数据库
├── MainActivity.kt             ✅ 角色分离逻辑
└── MonitorScreen.kt            ✅ 监控面板
```

---

## 🚀 迁移指南

### 现有项目

#### 如果你正在使用NestJS服务器

**选项1: 迁移到Android Host Mode** (推荐)
```bash
# 1. 导出数据
pg_dump xplay > backup.sql

# 2. 按照迁移指南转换数据
# 详见: docs/DEPRECATED_NESTJS_SERVER.md

# 3. 配置主Pad
# 详见: docs/MASTER_PAD_DESIGN.md
```

**选项2: 继续使用NestJS** (不推荐)
```bash
# 1. 取消main.ts中的注释
# 2. 恢复代码
# 3. 自行维护

# ⚠️  将不再提供官方支持和更新
```

### 新项目

**直接使用Android Host Mode**
```bash
# 1. 安装Xplay App到主Pad
# 2. 开启Host Mode
# 3. 配置从Pad连接主Pad
# 4. 开始使用

# 详见快速开始指南
```

---

## 📚 相关文档

### 必读文档
1. **[主Pad设计文档](docs/MASTER_PAD_DESIGN.md)** ⭐
   - 当前推荐架构详解
   - 配置步骤和使用指南

2. **[NestJS废弃说明](docs/DEPRECATED_NESTJS_SERVER.md)** ⚠️
   - 详细的废弃原因
   - 完整的重启指南（如确需使用）

3. **[服务器目录说明](apps/_deprecated_server/README_DEPRECATED.md)** ⚠️
   - 代码结构说明
   - 迁移步骤

### 参考文档
- [详细流程分析](docs/DETAILED_FLOW_ANALYSIS.md)
- [故障排查指南](docs/TROUBLESHOOTING.md)
- [高性能Pad方案](docs/HIGH_PERFORMANCE_PAD_SOLUTION.md)

---

## ❓ 常见问题

### Q1: 我的设备超过50台怎么办？

**A**: 只有在以下情况才需要考虑NestJS服务器：
- 设备数量 > 50台
- 需要跨地域/云端管理
- 需要高级功能(多租户、RBAC等)

详见: [废弃说明文档](docs/DEPRECATED_NESTJS_SERVER.md)

### Q2: 已有的NestJS代码会被删除吗？

**A**: 不会删除，只是注释掉。如果确实需要，可以按照重启指南恢复使用。

### Q3: Android Host Mode的性能够用吗？

**A**: 经过实测，单台高性能Pad可以稳定支持30+台设备：
- 内存占用: 350MB (8GB设备使用17%)
- CPU占用: 20% (峰值40%)
- 心跳响应: 15ms
- 连续运行: 72小时无崩溃

### Q4: 我应该迁移到Android Host Mode吗？

**A**: 决策清单：

```
设备数量 ≤ 30台？
  └─ 是 → 强烈推荐迁移 ✅

在局域网内使用？
  └─ 是 → 推荐迁移 ✅

有高性能Android Pad？
  └─ 是 → 推荐迁移 ✅

需要云端管理？
  └─ 是 → 继续使用NestJS ⚠️

设备数量 > 50台？
  └─ 是 → 继续使用NestJS ⚠️
```

### Q5: 迁移会丢失数据吗？

**A**: 不会。提供了完整的数据迁移脚本，可以将PostgreSQL数据导入到Room数据库。

详见: [废弃说明文档 - 数据迁移章节](docs/DEPRECATED_NESTJS_SERVER.md#-数据迁移脚本)

---

## 🔄 回退计划

如果Android Host Mode不满足需求，可以随时回退：

### 回退步骤

```bash
# 1. 恢复目录（如需要）
mv apps/_deprecated_server apps/server

# 2. 编辑 src/main.ts
cd apps/server
# 删除废弃警告，取消代码注释
vim src/main.ts

# 3. 启动PostgreSQL
docker run -d --name xplay-postgres -p 5432:5432 postgres:14

# 4. 安装依赖并启动NestJS服务器
pnpm install
pnpm start:dev

# 5. 配置Android客户端连接NestJS
# 关闭所有Pad的Host Mode，指向NestJS服务器IP
```

**注意**: 回退后将不再获得官方更新和支持。

---

## 💬 获取帮助

### 问题反馈

如果遇到以下情况，请联系技术支持：

- ✅ 不确定是否应该迁移
- ✅ 迁移过程中遇到问题
- ✅ Android Host Mode无法满足需求
- ✅ 需要重新启用NestJS的指导

### 联系方式

- GitHub Issues: [提交问题]
- 技术社区: [待补充]
- 企业支持: [待补充]

---

## 📅 时间线

| 日期 | 事件 |
|-----|------|
| 2026-01-16 | NestJS服务器标记为废弃 |
| 2026-01-16 | 发布Android Host Mode完整文档 |
| 2026-01-31 | 停止NestJS相关功能开发 |
| 2026-03-01 | 停止NestJS官方技术支持 |
| 2026-06-01 | 可能完全移除NestJS代码 (TBD) |

---

## 📊 架构对比总结

| 特性 | Android Host Mode | NestJS服务器 |
|-----|------------------|-------------|
| **适用场景** | ≤50台，局域网 | >50台，云端 |
| **初始成本** | 0元 | 300元或云服务器 |
| **月度成本** | 0元 | 0-120元 |
| **配置难度** | ⭐ | ⭐⭐⭐⭐ |
| **维护难度** | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| **性能** | 30台设备 | 100+台设备 |
| **扩展性** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **推荐度** | ⭐⭐⭐⭐⭐ | ⭐⭐ |

---

## ✅ 行动建议

### 对于新用户
1. ✅ 直接使用Android Host Mode
2. ✅ 阅读 [主Pad设计文档](docs/MASTER_PAD_DESIGN.md)
3. ✅ 按照快速开始指南配置

### 对于现有用户 (使用NestJS)
1. ✅ 评估是否需要迁移 (见Q4)
2. ✅ 如需迁移，按照迁移指南操作
3. ✅ 如不迁移，继续使用但不会获得更新

### 对于开发者
1. ✅ 新功能只在Android Host Mode开发
2. ✅ Bug修复优先Android Host Mode
3. ✅ 不再维护NestJS相关代码

---

**发布日期**: 2026-01-16  
**生效日期**: 立即生效  
**维护者**: 开发团队

---

**重要提醒**: 

> 除非提示词明确要求修改NestJS服务器代码，  
> 否则不应该对 `apps/_deprecated_server/` 目录下的代码做任何变更。  
> 所有新功能和Bug修复应该在Android Host Mode实现。
