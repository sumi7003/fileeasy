# NSD 服务发现故障排查

> 针对某些设备 NSD 功能异常的专项指南

---

## 🚨 问题症状

设备一直显示"正在寻找主机..."但从未找到，而其他设备能正常连接。

### 典型日志特征

```
✅ NSD discovery started successfully
❌ (没有 "Service found" 日志)
❌ (没有 "NSD resolved host" 日志)
❌ Waiting for NSD discovery, no IP found yet (重复10次)
❌ Failed to register after 10 attempts
```

---

## 🔍 问题原因

### 已知有 NSD 问题的设备品牌

| 品牌/型号 | Android版本 | 问题描述 | 解决方案 |
|----------|------------|---------|---------|
| **CUBOT MAX_5** | 13 | NSD 无法发现服务 | 手动配置IP |
| 部分华为设备 | 10+ | 鸿蒙系统 mDNS 受限 | 手动配置IP |
| 部分小米设备 | 11+ | MIUI 限制多播 | 需在安全中心授权 |
| 部分OPPO设备 | 12+ | ColorOS 后台限制 | 手动配置IP |

### 技术原因

1. **厂商 ROM 限制**
   - 部分厂商为省电禁用了 mDNS 多播
   - 或在后台限制了网络服务发现

2. **系统 Bug**
   - 某些 Android 版本的 NSD 实现有缺陷
   - mDNS 端口(5353)被阻塞

3. **网络配置**
   - 设备防火墙拦截多播
   - Wi-Fi 省电模式影响

---

## ✅ 解决方案

### 方案A：手动配置IP（推荐）

#### 步骤1：获取主机IP

**方法1 - 在主机设备上查看**：
- 打开 Xplay → 设置
- 查看"本机IP"或"局域网地址"
- 记下IP（例如：192.168.1.100）

**方法2 - 从其他正常连接的设备查看**：
```bash
# 连接其他能正常工作的设备
adb devices
adb logcat -d | grep "NSD resolved host"

# 输出示例：
# NSD resolved host: 192.168.1.100
```

**方法3 - 在路由器管理页面查看**：
- 浏览器访问路由器管理地址（通常是 192.168.1.1）
- 查看已连接设备列表
- 找到运行主机模式的设备

#### 步骤2：在问题设备上配置

1. 打开 Xplay
2. 进入设置
3. 找到"服务器地址"
4. 将 `xplay.local` 改为主机的实际IP
5. 保存设置
6. 启动播放器

**正确格式**：
```
✅ 正确：192.168.1.100
❌ 错误：http://192.168.1.100
❌ 错误：192.168.1.100:3000
❌ 错误：xplay.local
```

---

### 方案B：设置静态IP（高级）

如果多台设备都有 NSD 问题，可以给主机设置静态IP：

#### 在路由器中绑定IP

1. 登录路由器管理页面
2. 找到"DHCP 设置"或"IP与MAC绑定"
3. 找到主机设备的 MAC 地址
4. 绑定一个固定IP（如 192.168.1.100）
5. 重启主机设备

#### 在所有客户端配置该IP

- 所有设备统一使用 `192.168.1.100`
- 不再依赖 NSD 自动发现

---

### 方案C：诊断和修复 NSD（不推荐，成功率低）

#### 步骤1：检查权限

```bash
# 检查应用权限
adb shell dumpsys package com.xplay.player | grep permission

# 需要的权限：
# - INTERNET
# - ACCESS_WIFI_STATE  
# - ACCESS_NETWORK_STATE
# - CHANGE_WIFI_MULTICAST_STATE (如果有)
```

#### 步骤2：检查 Wi-Fi 设置

1. 设置 → Wi-Fi → 高级设置
2. 关闭"Wi-Fi 休眠"
3. 关闭"扫描始终可用"
4. 尝试切换 2.4G/5G 频段

#### 步骤3：清除应用数据

```bash
# 清除 Xplay 数据
adb shell pm clear com.xplay.player

# 重新打开应用配置
```

#### 步骤4：重启网络服务

```bash
# 重启设备的网络服务
adb shell svc wifi disable
adb shell svc wifi enable
```

---

## 🧪 测试验证

### 测试 mDNS 是否工作

```bash
# 在设备上测试（需要 root）
adb shell

# 检查多播路由
ip route show table all | grep 224

# 监听 mDNS 端口（需要 tcpdump）
tcpdump -i wlan0 port 5353

# 应该看到 mDNS 查询和响应
```

### 测试直接IP连接

```bash
# 在问题设备的浏览器中访问
http://主机IP:3000/api/v1/ping

# 应该返回：pong
```

如果浏览器能访问，说明：
- ✅ 网络连接正常
- ✅ 主机服务正常
- ❌ 只是 NSD 发现有问题
- ✅ 使用方案A手动配置IP即可解决

---

## 📊 NSD vs 手动配置对比

| 特性 | NSD 自动发现 | 手动配置IP |
|------|------------|-----------|
| 易用性 | ⭐⭐⭐⭐⭐ 自动 | ⭐⭐⭐ 需手动输入 |
| 可靠性 | ⭐⭐⭐ 部分设备不支持 | ⭐⭐⭐⭐⭐ 稳定 |
| 兼容性 | ⭐⭐⭐ 依赖设备 | ⭐⭐⭐⭐⭐ 通用 |
| 维护成本 | ⭐⭐⭐⭐⭐ 无需维护 | ⭐⭐ 主机IP变化需更新 |
| 适用场景 | 小规模、同品牌设备 | 大规模、混合品牌 |

---

## 🎯 最佳实践

### 小规模部署（< 5台）

**推荐方案**：
1. 优先尝试 NSD 自动发现
2. 不支持的设备手动配置IP
3. 定期检查主机IP是否变化

### 中大规模部署（≥ 5台）

**推荐方案**：
1. 在路由器中为主机绑定固定IP
2. 所有设备统一手动配置该IP
3. 不依赖 NSD，避免个别设备问题

### 生产环境

**推荐方案**：
1. 使用独立服务器（而非 Host Mode）
2. 服务器配置静态IP
3. 所有客户端手动配置IP
4. 配置备用服务器（高可用）

---

## 🔧 代码层面的改进建议

### 当前问题

当 NSD 失败时，用户无法快速切换到手动模式，必须：
1. 停止播放器
2. 进入设置
3. 更改服务器地址
4. 重新启动

这个流程太长。

### 改进方案

#### 方案1：自动降级提示

```kotlin
// DeviceRepository.kt
private suspend fun registerDevice(): Boolean {
    while (retryCount < MAX_RETRIES) {
        val baseUrl = buildBaseUrl()
        if (baseUrl == null) {
            // 如果NSD尝试多次仍失败，建议手动配置
            if (retryCount >= 3 && _serverHost.value == "xplay.local") {
                _status.value = """
                    未找到主机 (${retryCount + 1}/$MAX_RETRIES)
                    建议：设置 → 输入主机IP地址
                    或从其他设备查看主机IP
                """.trimIndent()
            }
            // ...
        }
    }
}
```

#### 方案2：快速切换按钮

在连接失败界面添加：
- "手动输入IP"快捷按钮
- "扫描二维码"按钮（主机显示IP二维码）

#### 方案3：IP缓存

```kotlin
// 记录上次成功连接的IP
private val _lastSuccessfulIp = MutableStateFlow<String?>(null)

// NSD失败时，尝试使用缓存的IP
if (_discoveredIp.value == null && _lastSuccessfulIp.value != null) {
    return "http://${_lastSuccessfulIp.value}:3000/api/v1/"
}
```

---

## 📞 报告 NSD 问题

如果发现新的有 NSD 问题的设备，请提供：

```bash
# 1. 设备信息
adb shell getprop ro.product.model
adb shell getprop ro.product.brand  
adb shell getprop ro.build.version.release

# 2. NSD 日志
adb logcat -d | grep -i nsd > nsd_issue.log

# 3. 网络配置
adb shell dumpsys connectivity > connectivity.log

# 4. Wi-Fi 信息
adb shell dumpsys wifi > wifi.log
```

---

## 总结

- **NSD 不是必需的**，只是为了方便
- **手动配置IP 更稳定可靠**
- **生产环境建议固定IP**，不依赖 NSD
- **某些设备的 NSD 就是不工作**，这是厂商问题，不是应用bug

---

更新时间：2026-01-19  
贡献者：AI Assistant
