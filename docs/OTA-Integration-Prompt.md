# OTA 升级融合提示词

这份提示词用于把 demo 项目里的 OTA 升级能力，从 0 到 1 融合到其他 Android APK 项目中。使用时请把目标项目的包名、flavor、channel、版本号和服务端地址替换成对应值。

## 可直接复用的提示词

```text
你是一个 Android 资深工程师。现在需要把 demo 项目中的 OTA 升级功能，从 0-1 融合到当前 APK 项目中。请先阅读 demo 项目，不要凭空实现。

一、参考项目和入口

参考 demo 项目中的以下逻辑：

1. LauncherActivity
   重点查看：
   AppHttpUpdateController.checkVersion2(this) { ... }

2. OTA 相关类
   查找并理解：
   - AppHttpUpdateController
   - NetUtil / OtaHttpClient / HTTP 请求封装
   - 下载 APK 的逻辑
   - 安装 APK 的逻辑
   - 版本响应数据结构
   - 签名方法 signAfterMd5 / MD5 规则
   - BuildConfig.CHANNEL 的来源

3. 先用 demo 对照一次真实请求
   如果 demo APK 能正常 OTA，请抓取 demo 的 OTA 请求日志或代码参数，作为目标项目的对照基准。目标项目的 channel、client_type、version、header、sign 规则必须和 demo 协议一致。

二、需要迁移/新建的核心文件

请在目标项目中补齐类似这些文件，命名可按项目规范调整：

1. AppHttpUpdateController.kt
   职责：
   - 对外提供 checkVersion2(activity, callback)
   - 启动时检查版本
   - 没有新版本或检查失败时继续进入主流程
   - 有新版本时弹出升级确认框
   - 下载完成后调起系统安装器
   - 全流程打印关键日志

2. OtaHttpClient.kt / NetUtil.kt
   职责：
   - 拼接 OTA 检查接口
   - 生成请求参数
   - 生成 sign
   - 发起 HTTP GET/POST，必须和 demo 一致
   - 打印完整请求参数、URL、header、响应 code、响应 body

3. OtaDownloader.kt
   职责：
   - 下载 APK
   - 打印下载 URL、保存路径、进度、文件大小、异常

4. OtaVersionData.kt
   职责：
   - 定义服务端返回的版本信息字段
   - 字段必须和服务端 JSON 对齐

5. ApkInstaller.kt 或复用项目已有安装逻辑
   职责：
   - 使用 FileProvider 暴露 APK
   - Intent ACTION_VIEW 调起安装器
   - 适配 Android 8+ 未知来源安装权限
   - Manifest 中补齐 REQUEST_INSTALL_PACKAGES 和 FileProvider

三、Gradle / BuildConfig 配置

目标项目必须配置 OTA 所需字段：

1. OTA_BASE_URL
   例如：
   http://elike.1daobo.com

2. CHANNEL / OTA_CHANNEL
   必须和服务端要求一致。
   参考 demo 中 channel 配置：

   ylk -> 1001
   xinwu -> 1002
   yilaidun -> 1003
   g1200TestKey -> 1016
   g1200ReleaseKey -> 1016

   如果当前 APK 使用 g1200ReleaseKey，则：
   CHANNEL = 1016

3. applicationId / 包名
   必须和服务端 client_type 一致。
   例如：
   com.xplay.fileeasy

4. versionCode
   OTA 判断通常依赖 versionCode。
   测试时本地安装旧版本，例如：
   versionCode = 11

   服务端配置新版本，例如：
   versionCode = 12

5. versionName
   只用于展示，但也要确认 flavor 是否覆盖。
   例如：
   默认：1.1.3-Build11 -> 1.1.4-Build12
   fileeasy flavor：V1.0 -> V1.1

6. release 签名
   OTA 新包和设备上已安装的旧包必须使用同一个签名证书，否则系统安装器会拒绝覆盖安装。请明确 release keystore、alias、storePassword、keyPassword 是否和服务端 APK 一致。

四、请求参数必须和 demo 对齐

请重点检查 OTA 请求是否包含 demo 中的这些参数：

- channel_num = BuildConfig.CHANNEL 或 BuildConfig.OTA_CHANNEL
- version = 当前 APK versionCode
- client_type = 当前 applicationId / packageName
- incremental = Build.VERSION.INCREMENTAL
- release = Build.VERSION.RELEASE
- security_patch = Build.VERSION.SECURITY_PATCH
- user = Build.USER
- fingerprint = Build.FINGERPRINT
- model = Build.MODEL
- build_id = Build.ID
- host = Build.HOST
- manufacturer = Build.MANUFACTURER
- product = Build.PRODUCT
- rk_version = ro.product.version，如果有值
- serialno = ro.serialno，如果有值
- did = 设备唯一 ID，如果 demo 或服务端白名单要求，比如 s2.chip.id

请求 header 也要和 demo 一致：

- XX-Device-Type: screenaide

sign 生成规则必须和 demo 完全一致：
- 参数 map 先按 demo 规则排序/拼接
- 使用 demo 的 key
- MD5 后转大写/小写要和 demo 一致
- sign 参数加入请求

五、启动时接入方式

在目标项目首页 Activity 的 onCreate 中接入：

AppHttpUpdateController.checkVersion2(this) {
    // 没有升级、升级失败、用户取消时，继续进入 App 主流程
    launchMainContent()
}

注意：
- 不要因为 OTA 失败阻塞主界面
- 如果有升级弹窗，可以等用户处理
- callback 需要保证只执行一次
- Activity 销毁时不要继续弹窗

六、Manifest 配置

检查是否包含：

1. 网络权限
   android.permission.INTERNET
   android.permission.ACCESS_NETWORK_STATE

2. 安装权限
   android.permission.REQUEST_INSTALL_PACKAGES

3. FileProvider
   authority 建议使用：
   ${applicationId}.fileprovider

4. provider_paths
   允许访问下载 APK 所在目录，例如 cache/files/downloads。

七、依赖库

检查目标项目是否已有：

- OkHttp，发起 HTTP 请求
- Gson 或 Moshi，解析 JSON
- Kotlin coroutines，异步检查版本和下载
- AndroidX AppCompat/Activity/Fragment，如弹窗依赖
- FileProvider，安装 APK

不要重复引入冲突版本，优先复用项目已有依赖。

八、必须增加的关键日志

请在 OTA 全流程打印日志，tag 建议：

- AppHttpUpdateController
- OtaHttpClient
- OtaDownloader
- ApkInstaller

日志必须包含：

1. 启动检查
   - 当前 packageName
   - versionCode
   - versionName
   - channel
   - baseUrl

2. 请求前
   - 完整接口 URL
   - 所有请求参数
   - sign 前参数
   - sign 结果
   - header

3. 响应后
   - HTTP code
   - response body
   - 解析后的 versionCode / versionName / apkUrl / forceUpdate / changelog

4. 判断升级
   - 当前版本
   - 服务端版本
   - 是否需要升级
   - 不升级原因

5. 下载过程
   - 下载 URL
   - 保存路径
   - contentLength
   - progress
   - 下载完成文件大小
   - 下载失败异常

6. 安装过程
   - APK 文件路径
   - FileProvider URI
   - 是否有未知来源安装权限
   - 是否跳转权限页
   - 是否调起系统安装器

九、编译和安装测试流程

1. 先确认本地配置
   - packageName 是否等于服务端 client_type
   - channel 是否等于服务端 channel_num
   - versionCode 是否低于服务端新版本
   - release 签名是否和 OTA 新包一致

2. 编译旧版本 release 包
   示例：
   ./gradlew --no-daemon assembleFileeasyRelease

3. 安装旧版本到设备
   示例：
   adb install -r app-release.apk

4. 确认设备当前版本
   adb shell dumpsys package com.xxx.xxx | grep -E "versionCode|versionName|lastUpdateTime"

5. 清空日志
   adb logcat -c

6. 启动 App
   adb shell am start -n 包名/启动 Activity

7. 抓取 OTA 日志
   adb logcat -d -v time | grep -E "Ota|OTA|AppHttpUpdateController|OtaHttpClient|OtaDownloader|PackageInstaller|client_upgrade|upgrade|update"

8. 如果弹出未知来源安装权限
   - 手动授权
   - 返回安装器继续安装

9. 安装完成后再次确认版本
   adb shell dumpsys package com.xxx.xxx | grep -E "versionCode|versionName|lastUpdateTime"

十、成功标准

OTA 融合成功必须同时满足：

1. 旧版本 APK 可以正常启动
2. 启动后发起 OTA 请求
3. 请求参数中：
   - channel_num 正确
   - version 正确
   - client_type 正确
   - sign 正确
   - header 正确
4. 服务端返回新版本
5. App 能解析新版本
6. App 能下载 APK
7. App 能调起系统安装器
8. 安装完成后版本真的变化，例如：
   versionCode 11 -> 12
   versionName V1.0 -> V1.1
9. 升级后再次启动 App 不崩溃
10. 没有新版本时能正常进入主界面

十一、常见问题定位

1. 没有弹升级
   优先检查：
   - channel_num 是否错
   - client_type 是否错
   - versionCode 是否已经等于或高于服务端版本
   - sign 是否和 demo 不一致
   - 服务端白名单 did / ip 是否匹配
   - response body 是否返回空版本

2. HTTP 500
   打印完整请求参数和 response body。
   对比 demo 请求，看是否缺少 version、channel_num、client_type、did、serialno 等字段。

3. 能下载但不能安装
   检查：
   - REQUEST_INSTALL_PACKAGES 权限
   - FileProvider authority
   - provider_paths
   - Android 8+ 未知来源授权
   - APK 签名是否和已安装包一致
   - 是否降级安装

4. adb install 失败
   如果是 versionCode 回退，需要：
   - 卸载旧包后安装，或
   - 使用 adb install -d，前提设备允许降级

5. release 和 debug 表现不同
   检查：
   - flavor 是否覆盖 versionName / applicationId
   - release BuildConfig 是否真的生成正确
   - 混淆是否影响 JSON 字段
   - release 签名是否和 OTA 包签名一致

十二、最终输出

完成后请输出：

1. 修改了哪些文件
2. 新增了哪些依赖
3. 当前 OTA 配置：
   - packageName
   - channel
   - versionCode
   - versionName
   - baseUrl
4. 编译命令和结果
5. APK 路径
6. 设备安装前后版本
7. 关键 log 摘要
8. 是否确认 OTA 从旧版本升级到新版本成功
```

## 本项目关键路径

- 目标项目：`/Users/jainaluo/易思态/code/Xplay/apps/android-player`
- demo 参考项目：`/Users/jainaluo/易思态/code/Xplay/apps/参考ota代码项目/omniviewproject`
- 当前 OTA 入口：`/Users/jainaluo/易思态/code/Xplay/apps/android-player/src/main/java/com/xplay/player/MainActivity.kt`
- 当前 OTA 代码目录：`/Users/jainaluo/易思态/code/Xplay/apps/android-player/src/main/java/com/xplay/player/update`
- 当前 Gradle 配置：`/Users/jainaluo/易思态/code/Xplay/apps/android-player/build.gradle.kts`
- 当前 release APK：`/Users/jainaluo/易思态/code/Xplay/apps/android-player/build/outputs/apk/fileeasy/release/XplayPlayer-fileeasy-release.apk`

## 当前项目已验证配置

- packageName：`com.xplay.fileeasy`
- channel：`1016`
- 旧版本：`versionCode=11`，`versionName=V1.0`
- 服务端新版本：`versionCode=12`，`versionName=V1.1`
- 验证结果：设备已从 `11 / V1.0` 通过 OTA 升级到 `12 / V1.1`
