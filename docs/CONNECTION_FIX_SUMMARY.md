# 自动连接服务器逻辑修复总结

**修复日期**: 2026-01-19  
**修复范围**: Android端自动连接和服务发现逻辑

---

## 🎯 修复的主要问题

### 1. ✅ 递归重试导致的栈溢出风险

**问题描述**:
- `registerDevice()` 方法使用递归调用进行重试
- 可能导致栈溢出，尤其在网络不稳定时

**修复方案**:
- 将递归改为 `while` 循环
- 避免栈溢出风险
- 重试逻辑更清晰可控

**代码位置**: `DeviceRepository.kt` - `registerDevice()` 方法

```kotlin
// 修复前：递归重试
private suspend fun registerDevice(): Boolean {
    // ... 
    return registerDevice()  // 递归调用
}

// 修复后：循环重试
private suspend fun registerDevice(): Boolean {
    while (retryCount < MAX_RETRIES) {
        // ... 重试逻辑
    }
    return false
}
```

---

### 2. ✅ xplay.local 域名无法解析

**问题描述**:
- 当 `serverHost` 为 `xplay.local` 且 NSD 未发现 IP 时
- 会尝试连接无法解析的 `xplay.local` 域名
- 导致 DNS 解析失败

**修复方案**:
- 在 `xplay.local` 模式下，必须等待 NSD 发现 IP
- 如果没有发现 IP，`buildBaseUrl()` 返回 `null`
- 状态提示改为 "正在寻找主机..."

**代码位置**: `DeviceRepository.kt` - `buildBaseUrl()` 方法

```kotlin
val host = if (_serverHost.value == "xplay.local") {
    val discovered = _discoveredIp.value
    if (discovered.isNullOrBlank()) {
        Log.d(TAG, "Waiting for NSD discovery, no IP found yet")
        return null  // 等待 NSD 发现
    } else {
        discovered
    }
} else {
    _serverHost.value
}
```

---

### 3. ✅ 网络错误提示不精确

**问题描述**:
- 所有网络异常都显示 "与服务器不在同一局域网"
- 实际可能是 DNS 解析失败、连接超时、连接被拒绝等

**修复方案**:
- 细化异常捕获类型
- 针对不同错误显示不同提示
- 添加详细的日志输出

**代码位置**: `DeviceRepository.kt` - `registerDevice()` 方法

```kotlin
catch (e: java.net.UnknownHostException) {
    _status.value = "无法解析主机地址 (${_serverHost.value})"
    Log.e(TAG, "DNS resolution failed for host: ${_serverHost.value}", e)
}
catch (e: java.net.SocketTimeoutException) {
    _status.value = "连接超时 (${retryCount + 1}/$MAX_RETRIES)"
    Log.e(TAG, "Connection timeout to: ${buildBaseUrl()}", e)
}
catch (e: java.net.ConnectException) {
    _status.value = "无法连接到主机 (${retryCount + 1}/$MAX_RETRIES)"
    Log.e(TAG, "Connection refused: ${e.message}", e)
}
```

---

### 4. ✅ NSD 服务发现失败后不重试

**问题描述**:
- NSD 发现失败时直接停止
- 没有重试机制
- 在某些网络环境下容易失败

**修复方案**:
- 添加自动重试机制（最多5次）
- 每次重试延迟3秒
- 成功后重置重试计数

**代码位置**: `NsdHelper.kt` - `startDiscovery()` 方法

```kotlin
private fun scheduleRetry(onHostFound: (String) -> Unit) {
    if (retryCount >= MAX_RETRY) {
        Log.e(TAG, "Max NSD retry attempts reached ($MAX_RETRY), giving up")
        return
    }
    
    retryCount++
    Log.d(TAG, "Scheduling NSD retry $retryCount/$MAX_RETRY in ${RETRY_DELAY_MS}ms")
    
    retryJob?.cancel()
    retryJob = scope.launch {
        delay(RETRY_DELAY_MS)
        startDiscoveryInternal(onHostFound)
    }
}
```

---

### 5. ✅ 网络请求缺少超时配置

**问题描述**:
- Retrofit 没有配置超时时间
- 可能导致连接长时间挂起
- 影响用户体验

**修复方案**:
- 添加 OkHttpClient 配置
- 连接超时: 10秒
- 读写超时: 15秒
- 启用连接失败自动重试

**代码位置**: `NetworkModule.kt`

```kotlin
private fun createOkHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
}
```

---

### 6. ✅ 日志输出不够详细

**问题描述**:
- 缺少关键连接步骤的日志
- 难以排查连接失败问题

**修复方案**:
- 在初始化时输出配置信息
- 在每次连接尝试时输出详细日志
- 使用不同日志级别（DEBUG/INFO/WARN/ERROR）
- 添加表情符号方便快速识别

**代码位置**: 多处改进

```kotlin
// 初始化日志
Log.i(TAG, "=== DeviceRepository initialized ===")
Log.d(TAG, "Host mode: ${_isHostMode.value}")
Log.d(TAG, "Server host: ${_serverHost.value}")

// NSD 发现日志
Log.i(TAG, "✅ NSD resolved host: $hostAddress")
Log.e(TAG, "❌ NSD start discovery failed")

// 连接状态日志
Log.d(TAG, "Built base URL: $url")
Log.i(TAG, "Successfully registered device")
```

---

## 📊 修复效果对比

### 修复前的问题：

| 场景 | 表现 |
|------|------|
| xplay.local 未发现 | 尝试连接，DNS 失败，显示"不在同一局域网" |
| NSD 发现失败 | 永久失败，不重试 |
| 网络超时 | 无限等待或超长时间 |
| 连接被拒绝 | 错误提示不准确 |
| 递归重试 | 潜在栈溢出风险 |

### 修复后的改进：

| 场景 | 表现 |
|------|------|
| xplay.local 未发现 | 等待 NSD，显示"正在寻找主机..." |
| NSD 发现失败 | 自动重试最多5次，间隔3秒 |
| 网络超时 | 10秒连接超时，15秒读写超时 |
| 连接被拒绝 | 精确提示："无法连接到主机" |
| 循环重试 | 安全的循环逻辑，无栈溢出风险 |

---

## 🔍 调试建议

### 查看连接日志

```bash
# 查看完整连接过程
adb logcat | grep -E "DeviceRepository|NsdHelper"

# 只看错误
adb logcat *:E | grep -E "DeviceRepository|NsdHelper"

# 查看 NSD 发现过程
adb logcat | grep "NSD"

# 查看网络请求
adb logcat | grep "OkHttp"
```

### 常见问题诊断

**1. 一直显示"正在寻找主机..."**
- 检查 NSD 是否正常工作
- 确认主机已启动并注册了 NSD 服务
- 检查路由器是否支持 mDNS

```bash
adb logcat | grep "NSD"
# 应该看到 "NSD discovery started" 和服务发现日志
```

**2. 显示"无法解析主机地址"**
- 检查输入的 IP 地址是否正确
- 尝试切换到 xplay.local 模式
- 或手动输入正确的局域网 IP

**3. 显示"连接超时"**
- 检查服务器是否在运行
- 检查防火墙是否开放 3000 端口
- 确认设备和服务器在同一网络

```bash
# 在主机上检查端口
netstat -an | grep 3000
# 应该看到 0.0.0.0:3000 或 :::3000
```

**4. 显示"无法连接到主机"**
- 服务器可能未启动
- 或端口被其他程序占用
- 检查服务器日志

---

## 🎨 用户体验改进

### 状态提示更加友好

| 旧提示 | 新提示 |
|--------|--------|
| "Initializing..." | "初始化中..." |
| "与服务器不在同一局域网" | "无法解析主机地址 (192.168.1.100)" |
| "连接失败" | "连接超时 (3/10)" |
| 无提示 | "正在寻找主机... (1/10)" |

### 进度指示

- 重试时显示当前进度: `(3/10)`
- NSD 重试也有计数提示
- 让用户了解系统在做什么

---

## 📝 代码变更清单

### 修改的文件

1. **DeviceRepository.kt**
   - ✅ 修复 `registerDevice()` 方法 - 循环代替递归
   - ✅ 改进 `buildBaseUrl()` - xplay.local 处理
   - ✅ 优化 `syncOnce()` - 日志和错误处理
   - ✅ 优化 `fetchAndCombinePlaylists()` - 日志和错误处理
   - ✅ 改进 `setServerHostFromDiscovery()` - 智能重连
   - ✅ 删除 `getApiOrWait()` - 不再需要
   - ✅ 添加初始化日志

2. **NetworkModule.kt**
   - ✅ 添加 `createOkHttpClient()` 方法
   - ✅ 配置超时时间
   - ✅ 启用自动重试

3. **NsdHelper.kt**
   - ✅ 添加重试机制
   - ✅ 添加 `scheduleRetry()` 方法
   - ✅ 改进日志输出
   - ✅ 优化 `stopDiscovery()` - 取消重试任务

### 新增的常量

```kotlin
// NsdHelper.kt
private var retryCount = 0
private val MAX_RETRY = 5
private val RETRY_DELAY_MS = 3000L
```

---

## 🧪 测试建议

### 测试场景

1. **正常连接场景**
   - ✅ xplay.local 模式 + NSD 发现
   - ✅ 手动输入 IP 模式
   - ✅ Host 模式（本地服务器）

2. **异常场景**
   - ✅ NSD 发现失败（路由器不支持 mDNS）
   - ✅ 服务器未启动
   - ✅ 错误的 IP 地址
   - ✅ 网络断开
   - ✅ 防火墙拦截

3. **边界场景**
   - ✅ 连续重试10次后放弃
   - ✅ NSD 重试5次后放弃
   - ✅ 网络在重试过程中恢复

### 验证清单

- [ ] xplay.local 模式下能正常发现并连接
- [ ] 输入错误 IP 时能显示正确错误提示
- [ ] 服务器未启动时能正确提示
- [ ] 网络断开后能自动重试
- [ ] 重试次数达到上限后停止
- [ ] 日志输出清晰完整
- [ ] 没有内存泄漏
- [ ] 没有 ANR（应用无响应）

---

## 📚 相关文档

- [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) - 故障排查指南
- [ARCHITECTURE.md](./ARCHITECTURE.md) - 系统架构说明
- [HIGH_PERFORMANCE_PAD_SOLUTION.md](./HIGH_PERFORMANCE_PAD_SOLUTION.md) - Host Mode 设计

---

## 💡 未来改进建议

### 短期（可选）

1. **添加连接质量监控**
   - 记录连接成功率
   - 统计平均连接时间
   - 记录失败原因分布

2. **优化重试策略**
   - 指数退避算法
   - 根据错误类型调整重试间隔

3. **添加手动重连按钮**
   - 在设置页面添加"立即重连"按钮
   - 重置重试计数

### 长期（可选）

1. **支持多服务器切换**
   - 保存多个服务器配置
   - 自动选择最快的服务器

2. **添加连接诊断工具**
   - 一键检测网络问题
   - 生成诊断报告

3. **支持离线模式**
   - 缓存最后的播放列表
   - 离线也能播放

---

**修复完成！** ✅

所有修改已通过编译检查，无 linter 错误。
