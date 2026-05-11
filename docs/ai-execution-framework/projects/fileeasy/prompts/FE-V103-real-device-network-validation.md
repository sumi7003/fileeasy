你负责 FileEasy 的一个真实设备验证任务。

## 任务身份

- Task ID: `FE-V103`
- Task name: `Real-device and network-scene validation`
- Stage: `validation`

## 任务背景

FileEasy 当前已经完成：

- `FE-T001` 到 `FE-T005` 任务级验收
- `FE-V101` 环境与构建确认
- `FE-V102` 端到端联调验证准备

现在需要补上最后一类关键验证：

- 真实设备
- 真实局域网
- 真实网络切换
- 真实重启恢复

`FE-V103` 的目标不是再看代码结构，而是确认：

1. FileEasy 在真实设备和真实局域网场景里是否稳定
2. Wi-Fi / 热点 / 断网恢复 / 重启恢复这些关键场景是否成立
3. 哪些问题会阻塞 FileEasy v1 进入发布收口

## 任务目标

验证以下真实场景：

1. 同一 Wi-Fi 局域网访问
2. 热点局域网访问
3. 网络切换后的地址与二维码刷新
4. 上传中断后的恢复
5. 设备重启后的服务恢复

最终你要输出的不是“测试过程描述”，而是：

- 通过的设备/网络场景
- 未通过的设备/网络场景
- 发布阻塞项
- 非阻塞后续项
- 给 PM 的 release go/no-go 参考

## 非目标

这次不要做下面这些事：

- 不要实现新功能
- 不要修改产品代码
- 不要修改 PRD 或设计文档
- 不要把设备验证任务变成 bugfix 任务

如果发现问题，只记录，不要直接修。

## 必读文档

1. `docs/ai-execution-framework/projects/fileeasy/reports/FileEasy-v1-Overall-Acceptance-Report.md`
2. `docs/ai-execution-framework/projects/fileeasy/reports/FileEasy-v1-Next-Phase-Plan.md`
3. `docs/ai-execution-framework/projects/fileeasy/reports/FileEasy-v1-Integration-Checklist.md`
4. `docs/ai-execution-framework/projects/fileeasy/task-packs/FE-V103-real-device-network-validation.json`

## 允许修改范围

本任务默认是只读验证任务。
如确有必要，你只允许在下面路径里写验证结果类文档：

- `docs/ai-execution-framework/projects/fileeasy/reports`

## 禁止修改范围

不要修改这些内容：

- `apps/android-player/src`
- `apps/web-admin/src`
- `docs/FileEasy-PRD-v1.0.md`
- `docs/FileEasy-Design-v1.0.md`

## 你需要完成的工作

### 1. 逐项验证真实设备与网络场景

重点验证下面这些场景：

#### Wi-Fi 场景
- 其他设备在同一 Wi-Fi 下能否访问上传页
- `/admin` 是否可访问
- 上传与管理动作是否可完成

#### 热点场景
- FileEasy 设备开热点后，其他设备能否访问上传页
- `/admin` 是否可访问
- 上传与管理动作是否可完成

#### 地址与二维码刷新
- 网络切换后首页地址是否更新
- 二维码是否更新
- 无可用地址时是否不展示误导性二维码

#### 上传恢复
- 上传中断后是否可继续
- 恢复网络后是否可继续
- 重新进入页面后是否仍能继续

#### 重启恢复
- 设备重启后服务是否恢复
- 重启后首页状态是否正确
- 重启后地址和二维码是否正确

### 2. 记录通过项与失败项

你必须明确输出：

#### 通过项
- 哪些设备/网络场景已经通过

#### 失败项
- 哪些设备/网络场景失败
- 失败现象是什么
- 是否是稳定复现

### 3. 区分“发布阻塞”与“非阻塞问题”

#### 发布阻塞
例如：

- 热点下无法访问上传页
- 地址刷新错误导致扫码入口不可用
- 重启后服务不恢复
- 断点续传在真实网络场景下失效

#### 非阻塞问题
例如：

- 某些状态文案不够准确
- 某些设备下 UI 展示不够理想
- 次要交互反馈问题

不要混写。

### 4. 给 PM 一个明确判断

你必须在最后给出下面二选一建议：

#### 选项 A
- 真实设备与网络场景已基本通过，可以进入发布阻塞项收口阶段

#### 选项 B
- 当前还不能进入发布收口
- 先需要修这些真实场景阻塞项：`...`

## 推荐验证方式

优先基于真实设备和真实网络执行验证。

如果某些场景只能由用户手动执行，请明确记录：

- 哪个场景需要人工操作
- 当前未能自动验证的原因
- 需要用户补测什么

不能把未验证场景默认为通过。

## 最低验证要求

至少覆盖下面这些判断：

1. Wi-Fi 场景可访问上传页和管理页
2. 热点场景可访问上传页和管理页
3. 网络切换后地址与二维码刷新正确
4. 上传中断后恢复可用
5. 设备重启后服务恢复可用

## 交付时必须按这个格式返回

1. Validation scope
- 本次实际验证覆盖了哪些真实设备/网络场景

2. Passed scenarios
- 已通过的设备/网络场景

3. Failed scenarios
- 未通过的设备/网络场景
- 失败现象是什么

4. Release blockers
- 发布前必须解决的真实设备/网络问题

5. Non-blocking follow-ups
- 可以留到后续版本的问题

6. PM handoff
- 是否建议进入发布阻塞项收口阶段
- 如果不建议，下一步需要什么
