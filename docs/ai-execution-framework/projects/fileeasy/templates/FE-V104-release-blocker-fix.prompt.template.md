你负责 FileEasy 的一个发布阻塞修复任务。

## 任务身份

- Task ID: `FE-V104-<blocker-slug>`
- Task name: `Release blocker fix - <short-blocker-name>`
- Stage: `blocker-fix`

## 任务背景

这个任务不是常规功能开发，而是为了解决一个已经在 `FE-V102` 或 `FE-V103` 中确认过的发布阻塞项。

当前阻塞项是：

- `<在这里写清楚阻塞项名称>`
- 来源：`<FE-V102 或 FE-V103 报告中的具体条目>`
- 当前影响：`<为什么它会阻塞 FileEasy v1 进入发布收口>`

## 任务目标

你只需要完成一个目标：

- 把上面这个阻塞项从 `fail/blocking` 修到 `pass/non-blocking`

本任务的重点是：

1. 精准修复当前阻塞项
2. 不扩大范围
3. 不顺手做无关清理
4. 给 PM 一个清晰的复验结果

## 非目标

你这次不要做下面这些事：

- 不要扩展产品功能
- 不要顺手修 unrelated bug
- 不要改 PRD 或设计文档
- 不要把多个阻塞项混成一个任务

如果你发现当前阻塞项无法在既定范围内完成，请停止扩展，直接在交付说明里写明原因。

## 必读文档

1. `docs/ai-execution-framework/projects/fileeasy/reports/FileEasy-v1-Overall-Acceptance-Report.md`
2. `docs/ai-execution-framework/projects/fileeasy/reports/FileEasy-v1-Next-Phase-Plan.md`
3. `docs/ai-execution-framework/projects/fileeasy/reports/FileEasy-v1-Integration-Checklist.md`
4. `<对应的 FE-V102 或 FE-V103 报告>`
5. `docs/ai-execution-framework/projects/fileeasy/task-packs/FE-V104-<blocker-slug>.json`

## 允许修改范围

你只能修改 task pack 里列出的 `allowedPaths`。

如果你认为必须改 allowedPaths 之外的文件，停止实现，不要越界，直接把原因写进交付说明。

## 你需要完成的工作

### 1. 修复当前阻塞项

目标场景：

- `<把失败场景写清楚>`

修复完成后，应满足：

- `<把修复成功后的产品行为写清楚>`

### 2. 保持范围边界

修复时必须同时保证：

- `<不该被影响的行为 A>`
- `<不该被影响的行为 B>`

### 3. 输出复验依据

你必须给出：

- 改动摘要
- 为什么这次改动能关闭当前 blocker
- 做了哪些验证
- 是否还有残留 blocker 风险

## 最低验证要求

至少执行并报告：

```bash
<替换成 blocker 对应的最小验证命令>
```

如果这个 blocker 需要页面或设备场景验证，也请明确补充：

- 你实际验证了什么
- 哪些仍需用户或 PM 补测

## 交付时必须按这个格式返回

1. Completed work
- 本次完成了什么

2. Changed files
- 改了哪些文件

3. Verification performed
- 做了哪些验证
- 哪些通过
- 哪些没法验证

4. Blocker status
- 当前阻塞项是否已关闭
- 依据是什么

5. Known risks
- 还剩哪些风险

6. PM handoff
- 是否建议 PM 重新复验这个 blocker
- 是否还有后续 blocker 需要单独开任务
