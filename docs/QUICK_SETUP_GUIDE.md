# Xplay 快速设置指南

> 解决"找不到主机"问题的快速指南

---

## 🚨 症状：一直显示"正在寻找主机..."

如果你看到这个提示，说明 NSD 自动发现没有找到任何 Xplay 主机。

```
正在寻找主机... (1/10)
请确保主机已启动或在设置中手动输入IP
```

---

## 🎯 3种解决方案（任选其一）

### 方案1：启动主机设备（推荐 ⭐）

**适用场景**：你有2台或更多设备，想用其中一台做主机

**步骤**：

1. **在主机设备上**：
   - 安装 Xplay APK
   - 打开应用
   - 点击右上角设置图标 ⚙️
   - **开启"主机模式"（Host Mode）**
   - 确保主机连接到 Wi-Fi

2. **在客户端设备上**：
   - 保持默认配置（xplay.local）
   - 启动播放器
   - 应该会自动发现主机并连接

**预期日志**：
```
✅ NSD discovery started successfully, searching for xplay services...
🎯 Found Xplay service: xplay, attempting to resolve...
✅ NSD resolved host: 192.168.1.100
Successfully registered device
```

---

### 方案2：手动输入主机IP（简单直接 ⭐⭐）

**适用场景**：你知道主机的IP地址，或者 NSD 自动发现失败

**步骤**：

1. **找到主机IP地址**：
   
   方法A - 在主机设备上查看：
   - 打开 Xplay → 设置 → 查看"本机IP"
   - 或在设置 → Wi-Fi → 查看当前网络详情
   
   方法B - 在路由器管理页面查看：
   - 浏览器打开路由器管理页面（通常是 192.168.1.1 或 192.168.0.1）
   - 查看已连接设备列表

2. **在客户端设置IP**：
   - 打开 Xplay
   - 进入设置
   - 找到"服务器地址"设置
   - 输入主机IP（例如：`192.168.1.100`）
   - **不要**加 `http://` 或端口号，只输入IP
   - 保存并返回
   - 启动播放器

**示例**：
```
✅ 正确：192.168.1.100
❌ 错误：http://192.168.1.100
❌ 错误：192.168.1.100:3000
```

---

### 方案3：单机模式（开发/测试 ⭐⭐⭐）

**适用场景**：只有一台设备，用于开发或测试

**步骤**：

1. **启用主机模式**：
   - 打开 Xplay
   - 进入设置
   - **开启"主机模式"（Host Mode）**
   - 这时设备既是服务器也是客户端

2. **启动播放器**：
   - 返回主界面
   - 点击"启动播放器"
   - 会自动连接到本地服务器（127.0.0.1）

**注意**：单机模式下无法远程管理，只能在本设备上操作。

---

## 🔍 故障排查

### Q1: 为什么 NSD 自动发现失败？

**可能原因**：

1. **主机未启动或未开启主机模式**
   - 解决：检查主机设备，确保 Host Mode 已开启

2. **路由器不支持 mDNS**
   - 部分企业路由器或老旧路由器不支持 mDNS 协议
   - 解决：使用方案2手动输入IP

3. **设备不在同一网络**
   - 确保所有设备连接到同一个 Wi-Fi
   - 检查是否连接到 5G 和 2.4G 不同频段（有些路由器会隔离）

4. **防火墙拦截**
   - 部分设备防火墙会拦截 mDNS 流量
   - 解决：使用方案2手动输入IP

### Q2: 输入IP后还是连不上？

**检查清单**：

```bash
# 1. 确认主机服务运行中
# 在主机设备上，应该看到"Host Mode 运行中"

# 2. 测试网络连通性
# 在客户端设备浏览器中访问：
http://主机IP:3000/api/v1/ping
# 应该返回：pong

# 3. 检查防火墙
# 确保主机防火墙开放 3000 端口
```

### Q3: 如何查看详细日志？

```bash
# 连接设备到电脑
adb devices

# 查看连接日志
adb logcat | grep -E "DeviceRepository|NsdHelper"

# 只看重要日志
adb logcat *:I | grep -E "DeviceRepository|NsdHelper"

# 查看错误
adb logcat *:E
```

**正常连接日志示例**：
```
I DeviceRepository: === DeviceRepository initialized ===
D DeviceRepository: Server host: 192.168.1.100
D DeviceRepository: Attempting to register, retry: 0
I DeviceRepository: Successfully registered device: Pad-105
```

**NSD 发现日志示例**：
```
I NsdHelper: ✅ NSD discovery started successfully
D NsdHelper: Service found: xplay, type: _xplay._tcp.
I NsdHelper: 🎯 Found Xplay service: xplay
I NsdHelper: ✅ NSD resolved host: 192.168.1.100
```

### Q4: 连接成功但没有内容播放？

这是另一个问题，请参考 [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) 第1.2节。

---

## 📱 典型部署场景

### 场景A：主Pad + 多个从Pad

```
主Pad (192.168.1.100)
├─ 开启 Host Mode
└─ 运行 Web Admin 管理界面

从Pad 1 (自动发现)
├─ 模式：xplay.local
└─ 自动发现主Pad IP

从Pad 2 (手动配置)
├─ 模式：192.168.1.100
└─ 手动输入主Pad IP

从Pad 3 (自动发现)
├─ 模式：xplay.local
└─ 自动发现主Pad IP
```

**优点**：
- 主Pad 故障，其他 Pad 不受影响
- 集中管理所有内容和设备
- 方便扩展

### 场景B：NestJS 服务器 + 多个Pad

```
服务器 (192.168.1.50)
├─ Docker 运行 NestJS
└─ 端口: 3000

所有Pad
├─ 模式：192.168.1.50
└─ 统一连接到服务器
```

**优点**：
- 性能更强
- 更稳定
- 适合大规模部署

### 场景C：单机开发测试

```
开发设备
├─ 开启 Host Mode
└─ 本机播放和管理
```

**优点**：
- 不需要其他设备
- 方便开发调试

---

## 🎨 用户界面指引

### 设置页面导航

```
主界面
└─ 点击右上角 ⚙️ 设置按钮
    ├─ 主机模式开关
    ├─ 服务器地址输入框
    │   ├─ xplay.local (默认，自动发现)
    │   └─ 192.168.1.100 (手动输入)
    ├─ 设备名称
    └─ 启动/停止播放器按钮
```

### 状态指示说明

| 状态文本 | 含义 | 操作建议 |
|---------|------|----------|
| 初始化中... | 应用刚启动 | 等待 |
| 正在寻找主机... | NSD 自动发现中 | 等待或手动输入IP |
| 正在连接主机... | 正在注册 | 等待 |
| 已连接 | 连接成功 ✅ | 正常使用 |
| 连接超时 | 网络问题 | 检查网络和主机 |
| 无法解析主机地址 | DNS 失败 | 检查IP是否正确 |
| 无法连接到主机 | 主机未启动 | 启动主机 |
| 连接失败，请检查网络设置 | 多次重试失败 | 参考本指南 |

---

## 💡 最佳实践

### 推荐配置

1. **主Pad**：
   - 开启 Host Mode
   - 设置固定IP（在路由器中绑定MAC地址）
   - 保持充电状态
   - 禁用休眠

2. **从Pad**：
   - 首选自动发现（xplay.local）
   - 备选手动IP
   - 启用自动启动

3. **网络**：
   - 使用稳定的 Wi-Fi
   - 避免使用访客网络（通常有隔离）
   - 路由器支持 mDNS 更佳

### 性能优化

- **小规模部署（< 5台）**：推荐使用 Host Mode
- **中规模部署（5-20台）**：考虑使用独立服务器
- **大规模部署（> 20台）**：必须使用独立服务器

---

## 📞 获取帮助

如果以上方案都无法解决问题，请收集以下信息：

```bash
# 1. 应用版本
# 在设置中查看

# 2. 设备信息
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release

# 3. 网络信息
adb shell dumpsys connectivity | grep "NetworkInfo"

# 4. 连接日志
adb logcat -d > xplay_debug.log

# 5. 主机状态
# 截图设置页面
```

然后查看：
- [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) - 详细故障排查
- [CONNECTION_FIX_SUMMARY.md](./CONNECTION_FIX_SUMMARY.md) - 连接修复说明
- GitHub Issues（如有）

---

**祝你使用愉快！** 🎉

更新时间：2026-01-19
