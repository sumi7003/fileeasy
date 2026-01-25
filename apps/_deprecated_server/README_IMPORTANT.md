# ⚠️ 此目录已废弃

## 重要提示

**此目录包含已废弃的NestJS服务器代码**

- ❌ **不推荐使用** - 除非设备数量 > 50台
- 📅 **废弃日期**: 2026-01-16
- 🔄 **当前推荐**: [Android Host Mode](../../docs/MASTER_PAD_DESIGN.md)

---

## 为什么废弃？

1. **用户场景不需要**: 拥有高性能Android Pad，无需额外服务器
2. **维护成本高**: 双服务端架构导致重复开发和Bug修复困难
3. **性能已足够**: Android Host Mode可稳定支持30+台设备

详细原因: [废弃说明文档](../../docs/DEPRECATED_NESTJS_SERVER.md)

---

## 如何使用当前推荐方案？

### 快速开始

1. 配置主Pad (开启Host Mode)
2. 配置从Pad (连接主Pad)
3. 开始使用

详细步骤: [主Pad设计文档](../../docs/MASTER_PAD_DESIGN.md)

---

## 如果确实需要重启NestJS服务器

### 评估清单

只有满足以下条件才建议重启：

- [ ] 设备数量 > 50台
- [ ] 需要跨地域/云端管理
- [ ] 需要高级功能(多租户、RBAC)
- [ ] 没有合适的Android设备

### 重启指南

完整步骤: [完整重启指南](./README_DEPRECATED.md)

---

## 目录结构

```
apps/_deprecated_server/
├── README_IMPORTANT.md       ← 你正在读这个
├── README_DEPRECATED.md      ← 完整重启指南
├── src/                      ← 服务端源码
│   ├── main.ts              ← 入口 (已注释)
│   ├── app.module.ts
│   ├── device/              ← 设备管理
│   ├── media/               ← 素材管理
│   ├── playlist/            ← 播放列表
│   └── update/              ← APK更新
├── package.json
└── tsconfig.json
```

---

## 快速决策

### 我应该使用哪个方案？

```
设备数量 ≤ 30台？
  └─ 是 → Android Host Mode ✅

在局域网使用？
  └─ 是 → Android Host Mode ✅

有专人运维服务器？
  └─ 否 → Android Host Mode ✅

需要云端管理？
  └─ 是 → 考虑NestJS ⚠️
```

**90%的场景推荐使用Android Host Mode！**

---

## 相关文档

### 必读
- [主Pad设计文档](../../docs/MASTER_PAD_DESIGN.md) ⭐
- [高性能Pad方案](../../docs/HIGH_PERFORMANCE_PAD_SOLUTION.md) ⭐

### 参考
- [NestJS废弃说明](../../docs/DEPRECATED_NESTJS_SERVER.md)
- [完整重启指南](./README_DEPRECATED.md)
- [架构变更通知](../../ARCHITECTURE_CHANGE_NOTICE.md)

---

**最后更新**: 2026-01-16  
**维护状态**: 已停止维护  
**建议操作**: 切换到Android Host Mode
