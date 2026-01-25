# 连接修复测试指南

> 如何验证自动连接修复是否生效

---

## 🚀 快速测试

### 1. 编译并安装新版本

```bash
cd /Users/jainaluo/易思态/code/Xplay

# 编译 Android 应用
cd apps/android-player
./gradlew assembleDebug

# 安装到设备
adb install -r build/outputs/apk/debug/XplayPlayer-debug.apk
```

### 2. 准备测试环境

**启动服务器（Host Mode 或 NestJS）**

```bash
# 方式1: 在另一台设备上启用 Host Mode
# - 安装应用
# - 在设置中开启"主机模式"
# - 记下该设备的局域网 IP

# 方式2: 启动 NestJS 服务器（如果还在使用）
cd apps/_deprecated_server
pnpm start:dev
```

### 3. 测试场景

#### ✅ 场景1: xplay.local 自动发现（推荐）

**步骤：**
1. 确保主机设备已启用 Host Mode
2. 客户端设备保持默认配置（xplay.local）
3. 启动客户端播放器

**预期结果：**
- 日志显示 `✅ NSD resolved host: xxx.xxx.xxx.xxx`
- 状态从 "正在寻找主机..." 变为 "已连接"
- 连接时间 < 10秒

**查看日志：**
```bash
adb logcat -c  # 清空日志
adb logcat | grep -E "DeviceRepository|NsdHelper"
```

**预期日志：**
```
I DeviceRepository: === DeviceRepository initialized ===
D DeviceRepository: Host mode: false
D DeviceRepository: Server host: xplay.local
D NsdHelper: NSD discovery initiated
I NsdHelper: ✅ NSD discovery started successfully
D NsdHelper: Service found: xplay, type: _xplay._tcp.
I NsdHelper: ✅ NSD resolved host: 192.168.1.100
I DeviceRepository: ✅ Discovered Xplay host via NSD: 192.168.1.100
D DeviceRepository: Built base URL: http://192.168.1.100:3000/api/v1/
I DeviceRepository: Successfully registered device: Pad-100
```

---

#### ✅ 场景2: 手动输入 IP

**步骤：**
1. 在客户端设置中输入主机 IP（如：192.168.1.100）
2. 启动播放器

**预期结果：**
- 直接连接，不需要 NSD 发现
- 状态从 "正在连接主机..." 变为 "已连接"
- 连接时间 < 5秒

**预期日志：**
```
D DeviceRepository: Attempting to register, baseUrl: http://192.168.1.100:3000/api/v1/, retry: 0
I DeviceRepository: Successfully registered device
```

---

#### ✅ 场景3: NSD 发现失败（降级处理）

**步骤：**
1. 使用不支持 mDNS 的路由器
2. 或在主机未启动时测试

**预期结果：**
- NSD 自动重试5次
- 每次间隔3秒
- 状态提示 "正在寻找主机... (1/10)"
- 失败后可手动输入 IP

**预期日志：**
```
I NsdHelper: ✅ NSD discovery started successfully
E NsdHelper: ❌ NSD start discovery failed: errorCode=...
D NsdHelper: Scheduling NSD retry 1/5 in 3000ms
D NsdHelper: Retrying NSD discovery (attempt 1)
...
E NsdHelper: Max NSD retry attempts reached (5), giving up
```

---

#### ✅ 场景4: 服务器未启动

**步骤：**
1. 确保服务器未运行
2. 输入正确的 IP 地址
3. 启动播放器

**预期结果：**
- 显示 "无法连接到主机 (1/10)"
- 自动重试，每次延迟递增
- 重试10次后显示 "连接失败，请检查网络设置"

**预期日志：**
```
D DeviceRepository: Attempting to register, baseUrl: http://192.168.1.100:3000/api/v1/, retry: 0
E DeviceRepository: Connection refused: Connection refused
D DeviceRepository: Attempting to register, retry: 1
...
E DeviceRepository: Failed to register after 10 attempts
```

---

#### ✅ 场景5: 错误的 IP 地址

**步骤：**
1. 输入不存在的 IP（如：192.168.1.200）
2. 启动播放器

**预期结果：**
- 显示 "连接超时 (1/10)"
- 10秒后超时
- 自动重试

**预期日志：**
```
E DeviceRepository: Connection timeout to: http://192.168.1.200:3000/api/v1/
```

---

#### ✅ 场景6: DNS 解析失败

**步骤：**
1. 输入无效域名（如：invalid.local）
2. 启动播放器

**预期结果：**
- 显示 "无法解析主机地址 (invalid.local)"
- 不会长时间挂起

**预期日志：**
```
E DeviceRepository: DNS resolution failed for host: invalid.local
java.net.UnknownHostException: Unable to resolve host "invalid.local"
```

---

#### ✅ 场景7: 网络断开后恢复

**步骤：**
1. 正常连接后
2. 断开 Wi-Fi
3. 重新连接 Wi-Fi

**预期结果：**
- 心跳失败时有日志记录
- 网络恢复后自动恢复连接
- 播放不中断（如果有缓存）

**预期日志：**
```
E DeviceRepository: Heartbeat error: SocketTimeoutException - timeout
D DeviceRepository: Heartbeat successful. Playlists: [...]
```

---

## 📊 性能指标

| 指标 | 目标值 | 测量方法 |
|------|--------|----------|
| NSD 发现时间 | < 5秒 | 从启动到发现 IP |
| 首次连接时间 | < 10秒 | 从启动到注册成功 |
| 重连时间 | < 5秒 | 网络恢复后重连 |
| 超时时间 | 10秒 | 连接失败时的等待时间 |
| 心跳间隔 | 30秒 | 心跳请求间隔 |
| CPU 占用 | < 5% | 空闲时 CPU 使用率 |
| 内存占用 | < 100MB | 运行2小时后内存 |

---

## 🔍 调试命令速查

### 实时查看连接过程

```bash
# 查看所有相关日志
adb logcat -c && adb logcat | grep -E "DeviceRepository|NsdHelper|NetworkModule"

# 只看重要日志（INFO 及以上）
adb logcat -c && adb logcat *:I | grep -E "DeviceRepository|NsdHelper"

# 只看错误
adb logcat -c && adb logcat *:E

# 保存日志到文件
adb logcat -c
adb logcat > connection_test_$(date +%Y%m%d_%H%M%S).log
# 按 Ctrl+C 停止，然后分析日志文件
```

### 查看应用状态

```bash
# 查看应用是否运行
adb shell ps | grep com.xplay.player

# 查看应用内存
adb shell dumpsys meminfo com.xplay.player

# 查看网络状态
adb shell dumpsys connectivity
```

### 模拟网络问题

```bash
# 查看设备 IP
adb shell ip addr show wlan0 | grep inet

# 测试服务器连通性（在设备上）
adb shell ping -c 3 192.168.1.100

# 测试端口（需要 telnet）
adb shell telnet 192.168.1.100 3000
```

---

## ✅ 测试检查清单

### 基础功能

- [ ] xplay.local 模式正常工作
- [ ] 手动输入 IP 正常工作
- [ ] Host 模式正常工作
- [ ] 设备名称正确显示
- [ ] 心跳正常（30秒一次）

### 错误处理

- [ ] 服务器未启动时正确提示
- [ ] 错误 IP 时正确提示
- [ ] DNS 解析失败时正确提示
- [ ] 网络断开时正确处理
- [ ] 达到重试上限后停止

### 性能表现

- [ ] 连接时间符合预期
- [ ] 没有内存泄漏
- [ ] 没有 ANR
- [ ] CPU 占用正常
- [ ] 日志输出清晰

### 用户体验

- [ ] 状态提示友好
- [ ] 重试进度可见
- [ ] 错误信息有帮助
- [ ] 手动重连可用
- [ ] 配置保存正确

---

## 🐛 常见问题

### Q1: 一直显示"正在寻找主机..."

**可能原因：**
- 路由器不支持 mDNS
- 主机未启动或未注册 NSD

**解决方法：**
1. 检查主机 Host Mode 是否启用
2. 查看 NSD 日志确认发现状态
3. 切换到手动输入 IP 模式

### Q2: 连接成功但无内容播放

**可能原因：**
- 未分配播放列表
- 播放列表为空

**解决方法：**
1. 在 Web Admin 中分配播放列表
2. 检查播放列表是否有内容
3. 查看心跳日志确认播放列表 ID

### Q3: 日志显示"Connection refused"

**可能原因：**
- 服务器未启动
- 端口被占用
- 防火墙拦截

**解决方法：**
```bash
# 检查服务器
curl http://192.168.1.100:3000/api/v1/ping

# 检查端口
netstat -an | grep 3000

# 检查防火墙（Mac）
sudo pfctl -s rules | grep 3000
```

### Q4: 内存持续增长

**可能原因：**
- 日志过多
- 缓存未清理

**解决方法：**
1. 减少 DEBUG 日志
2. 定期重启应用
3. 检查 Coil 缓存配置

---

## 📈 回归测试

修改任何网络相关代码后，务必运行完整测试：

```bash
# 1. 清理并重新编译
cd apps/android-player
./gradlew clean
./gradlew assembleDebug

# 2. 卸载旧版本
adb uninstall com.xplay.player

# 3. 安装新版本
adb install build/outputs/apk/debug/XplayPlayer-debug.apk

# 4. 清空日志并启动
adb logcat -c
adb shell am start -n com.xplay.player/.MainActivity

# 5. 观察日志
adb logcat | grep -E "DeviceRepository|NsdHelper"
```

---

**祝测试顺利！** 🎉

如有问题请查看 [CONNECTION_FIX_SUMMARY.md](./CONNECTION_FIX_SUMMARY.md)
