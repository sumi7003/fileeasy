# Xplay 技术文档中心

> 完整的系统架构、流程分析和问题排查指南

---

## 📚 文档目录

### 🎯 快速开始
1. **[系统架构文档](./ARCHITECTURE.md)** - 总体架构概览
2. **[需求文档](../doc/requid.md)** - 项目需求和功能说明
3. **[快速开始指南](../README.md)** - 5分钟启动系统

### 🔍 深度解析（新增）
4. **[详细流程分析](./DETAILED_FLOW_ANALYSIS.md)** ⭐ **重点文档**
   - 服务端完整流程（启动、注册、心跳、素材、播放列表、更新）
   - Android客户端完整流程（启动、播放引擎、Host Mode）
   - 系统交互点分析（网络、数据库、文件存储）
   - **⚠️ 易出问题的地方详细标注**
   - 架构痛点和优化建议

5. **[流程图集](./FLOW_DIAGRAMS.md)** ⭐ **可视化流程**
   - 10个核心Mermaid流程图
   - 包含：整体交互、设备生命周期、内容下发、心跳机制、播放器状态机等
   - 可直接在IDE或在线工具中预览

6. **[故障排查指南](./TROUBLESHOOTING.md)** ⭐ **问题定位神器**
   - 紧急问题快速定位（5分钟检查清单）
   - 常见Bug及解决方案
   - 系统监控指标和SQL
   - 诊断工具箱和脚本
   - 问题优先级矩阵

---

## 🎬 核心流程速览

### 服务端流程
```
启动 → 初始化数据库 → 监听3000端口 → 
接收设备注册 → 心跳轮询 → 
素材管理 → 播放列表分发 → APK更新
```

**关键代码文件:**

**Android Host Mode** (推荐) ✅:
- `apps/android-player/.../server/LocalServerService.kt` - 服务端入口
- `apps/android-player/.../MainActivity.kt` - 客户端入口

**NestJS服务器** (已废弃) ⚠️:
- `apps/_deprecated_server/src/main.ts` - 入口
- `apps/_deprecated_server/src/device/device.service.ts` - 设备管理核心
- `apps/_deprecated_server/src/playlist/playlist.gateway.ts` - WebSocket推送

### Android客户端流程
```
启动 → 服务发现/配置IP → 注册设备 → 
启动心跳定时器(30秒) → 
接收播放列表 → 播放引擎循环播放 → 
检测更新并安装
```

**关键代码文件:**
- `apps/android-player/src/main/java/com/xplay/player/MainActivity.kt` - 入口
- `apps/android-player/src/main/java/com/xplay/player/DeviceRepository.kt` - 网络和状态管理
- `apps/android-player/src/main/java/com/xplay/player/PlayerScreen.kt` - 播放引擎

---

## ⚠️ 高频问题快速索引

| 问题 | 查看章节 | 预计解决时间 |
|-----|---------|------------|
| 设备无法连接服务器 | [TROUBLESHOOTING.md §1.1](./TROUBLESHOOTING.md#11-设备完全无法连接服务器) | 5分钟 |
| 设备在线但不播放 | [TROUBLESHOOTING.md §1.2](./TROUBLESHOOTING.md#12-设备在线但不播放内容) | 10分钟 |
| 视频播放黑屏 | [TROUBLESHOOTING.md §1.3](./TROUBLESHOOTING.md#13-视频播放黑屏绿屏) | 15分钟 |
| 素材上传失败 | [TROUBLESHOOTING.md §1.4](./TROUBLESHOOTING.md#14-素材上传失败) | 10分钟 |
| Host Mode无法访问 | [DETAILED_FLOW_ANALYSIS.md §3.3](./DETAILED_FLOW_ANALYSIS.md#33-host-mode内嵌服务器流程) | 20分钟 |
| 内存泄漏/卡顿 | [TROUBLESHOOTING.md §3.1](./TROUBLESHOOTING.md#31-内存泄漏问题) | 30分钟 |
| 数据库锁死 | [TROUBLESHOOTING.md §3.2](./TROUBLESHOOTING.md#32-数据库锁死) | 1小时 |

---

## 🔧 技术栈汇总

### 服务端 (NestJS)
| 技术 | 版本 | 用途 |
|-----|------|------|
| NestJS | 10+ | Web框架 |
| TypeORM | 0.3+ | ORM |
| Socket.io | 4+ | WebSocket |
| SQLite/PostgreSQL | - | 数据库 |
| Multer | 1+ | 文件上传 |

### Android客户端
| 技术 | 版本 | 用途 |
|-----|------|------|
| Kotlin | 1.9+ | 编程语言 |
| Jetpack Compose | 1.5+ | UI框架 |
| ExoPlayer | 2.19+ | 视频播放 |
| Coil | 2.4+ | 图片加载 |
| Retrofit | 2.9+ | 网络请求 |
| Room | 2.5+ | 本地数据库 |
| Ktor | 2.3+ | 内嵌服务器 |

### Web管理后台
| 技术 | 版本 | 用途 |
|-----|------|------|
| React | 18+ | 前端框架 |
| Ant Design | 5+ | UI组件库 |
| Vite | 4+ | 构建工具 |

---

## 🚨 架构痛点与优化方向

### 当前问题
1. ❌ **心跳轮询效率低** - 30秒延迟导致播放列表分配不及时
2. ❌ **没有离线缓存** - 断网后立即无法播放
3. ❌ **日志系统缺失** - 问题定位困难
4. ❌ **性能监控缺失** - 不知道设备播放是否流畅
5. ❌ **双数据库维护** - NestJS TypeORM 和 Ktor Room 需要同步修改

### 优化建议
✅ **立即可做:**
1. 实现WebSocket双向通信替代轮询
2. 添加Sentry/Bugly错误上报
3. 配置日志采集系统

✅ **中期规划:**
1. 实现素材本地缓存和预加载
2. 引入Redis做消息队列
3. 对象存储(MinIO/S3)分离静态文件

✅ **长期架构:**
1. 完整的监控体系 (ELK + Prometheus + Grafana)
2. 负载均衡和高可用
3. 废弃Host Mode或改为纯代理模式

详见：[DETAILED_FLOW_ANALYSIS.md §7](./DETAILED_FLOW_ANALYSIS.md#-七架构优化建议)

---

## 📊 系统监控指南

### 关键指标
```sql
-- 设备在线率
SELECT COUNT(*) FILTER (WHERE status='online') * 100.0 / COUNT(*) 
FROM devices;

-- 素材使用率
SELECT m.originalName, COUNT(pi.id) as used_count
FROM media m
LEFT JOIN playlist_items pi ON m.id = pi.mediaId
GROUP BY m.id;

-- 最近心跳统计
SELECT name, lastHeartbeat
FROM devices
WHERE lastHeartbeat >= datetime('now', '-5 minutes');
```

### 日志查看
```bash
# 服务端
tail -f server.log | grep "ERROR\|Heartbeat\|Register"

# Android客户端
adb logcat | grep "DeviceRepository\|PlayerScreen\|ExoPlayer"

# Host Mode
adb logcat | grep "LocalServerService\|Ktor"
```

完整脚本见：[TROUBLESHOOTING.md §5](./TROUBLESHOOTING.md#-五诊断工具箱)

---

## 🛠 调试工具

### 一键诊断脚本
```bash
# 服务端诊断
bash docs/scripts/xplay_diagnose.sh

# Android调试
bash docs/scripts/android_debug.sh

# 健康检查
bash docs/scripts/xplay_health_check.sh
```

### Chrome DevTools
- Network: 查看API请求和响应
- Console: 查看Web Admin错误
- Application: 查看LocalStorage和Cookie

### Android Studio Profiler
- Memory: 检测内存泄漏
- Network: 查看网络请求
- CPU: 分析性能瓶颈

---

## 📞 寻求帮助

### 自助排查流程
1. 查看 [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) 快速定位
2. 运行诊断脚本收集信息
3. 查看对应章节的详细分析
4. 尝试文档中的解决方案

### 提交Issue
如果文档未能解决问题，请提供：
- [ ] 问题描述和复现步骤
- [ ] 诊断脚本输出
- [ ] 相关日志（服务端和Android）
- [ ] 环境信息（OS、版本、配置）

### 联系方式
- GitHub Issues: [待补充]
- 技术社区: [待补充]
- 企业支持: [待补充]

---

## 📝 文档贡献

欢迎完善文档：
1. Fork本项目
2. 创建新分支: `git checkout -b docs/improve-xxx`
3. 提交修改: `git commit -m "docs: improve xxx section"`
4. 推送分支: `git push origin docs/improve-xxx`
5. 提交Pull Request

**文档规范:**
- 使用Markdown格式
- Mermaid图表放在独立的代码块
- 代码示例要完整可运行
- 添加必要的注释和说明

---

## 📅 更新日志

### 2026-01-16
- ✅ 新增《详细流程分析》文档
- ✅ 新增《流程图集》（10个核心流程图）
- ✅ 新增《故障排查指南》
- ✅ 创建文档中心索引

### 历史版本
- 2025-xx-xx: 初始版本（ARCHITECTURE.md）

---

**文档维护者**: AI Assistant  
**最后更新**: 2026-01-16  
**文档版本**: v2.0  

---

## 🎓 学习路径推荐

### 新手入门（1小时）⭐ 推荐
1. 阅读 [主Pad设计文档](./MASTER_PAD_DESIGN.md) - 了解当前推荐架构
2. 按照 [快速开始指南](../README.md#-快速开始-android-host-mode) 配置主从Pad
3. 查看监控面板，体验管理后台

### 进阶开发（1周）
1. 精读 [DETAILED_FLOW_ANALYSIS.md](./DETAILED_FLOW_ANALYSIS.md) 理解每个流程细节
2. 使用 [FLOW_DIAGRAMS.md](./FLOW_DIAGRAMS.md) 中的流程图加深理解
3. 尝试修改代码添加新功能

### 生产部署（1天）
1. 配置主Pad静态IP
2. 批量配置所有从Pad
3. 准备 [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) 作为运维手册
4. 建立定期维护流程

### 特殊场景（可选）
1. 如果需要>50台设备: 阅读 [NestJS服务器重启指南](./DEPRECATED_NESTJS_SERVER.md)
2. 如果需要高可用: 配置主备切换
3. 如果需要跨地域: 考虑多主Pad分区部署

---

## ⚠️ 架构变更说明

### v2.0 (2026-01-16) - 重大变更

**废弃**: NestJS独立服务器模式  
**推荐**: Android Host Mode (一主多从架构)

**原因**:
- 用户已有多台高性能Android Pad
- 双服务端架构维护成本高
- 实测Android Host Mode性能足够

**影响**:
- `apps/server/` 已移至 `apps/_deprecated_server/`
- 代码已注释，目录已排除在工作区外
- 新项目直接使用Android Host Mode
- 旧项目可选择迁移或继续使用NestJS

详见: [废弃说明文档](./DEPRECATED_NESTJS_SERVER.md)

---

**Happy Coding! 🚀**
