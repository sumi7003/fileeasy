你负责 FileEasy 的一个验证任务。

## 任务身份

- Task ID: `FE-V101`
- Task name: `Environment and build verification`
- Stage: `validation`

## 任务背景

FileEasy 当前已经完成第一批任务级实现与 PM 验收：

- `FE-T001` 已通过
- `FE-T002` 已通过
- `FE-T003` 已通过
- `FE-T004` 已通过
- `FE-T005` 已通过

现在项目进入下一阶段，不是继续拆功能，而是进入：

- 整体验收
- 环境确认
- 端到端联调准备

当前已知问题是：

- Android 本地编译验证曾被 `JAVA_HOME` / Java Runtime 环境阻塞
- Web 构建是可用的，但需要重新确认它是否稳定到足以支撑下一阶段联调

所以本任务的目标不是写功能，而是：
**确认 FileEasy 当前是否具备进入整体验收的环境条件。**

## 任务目标

验证 FileEasy 当前是否具备可用的本地构建环境，为下一步整体验收做准备。

本次任务的重点不是实现新功能，而是区分：

1. 哪些是环境问题
2. 哪些是产品代码问题
3. 哪些已经可以进入端到端联调

## 非目标

你这次不要做下面这些事：

- 不要扩展产品功能
- 不要修改 PRD 或设计文档
- 不要顺手重构无关代码
- 不要把环境问题伪装成代码问题
- 不要主动修改业务代码，除非 PM 后续明确批准一个单独 blocker fix 任务

如果你认为必须改代码才能完成任务，先停下来，在返回结果里说明原因，不要自行越界改动。

## 必读文档

1. `docs/ai-execution-framework/projects/fileeasy/reports/FileEasy-v1-Overall-Acceptance-Report.md`
2. `docs/ai-execution-framework/projects/fileeasy/reports/FileEasy-v1-Next-Phase-Plan.md`
3. `docs/ai-execution-framework/projects/fileeasy/reports/FileEasy-v1-Integration-Checklist.md`
4. `docs/ai-execution-framework/projects/fileeasy/task-packs/FE-V101-environment-build-verification.json`

## 允许修改范围

本任务默认是只读验证任务。
如确有必要，你只允许在下面路径里写验证性文档，不允许改业务代码：

- `docs/ai-execution-framework/projects/fileeasy/reports`

## 禁止修改范围

不要修改这些内容：

- `apps/android-player/src`
- `apps/web-admin/src`
- `docs/FileEasy-PRD-v1.0.md`
- `docs/FileEasy-Design-v1.0.md`

## 你需要完成的工作

### 1. Android 构建环境检查

验证以下内容：

- 是否存在可用 Java 运行时
- `JAVA_HOME` 是否有效
- FileEasy Android 编译命令是否能执行
- 如果失败，失败是环境原因还是代码原因

重点命令：

```bash
GRADLE_USER_HOME=.gradle-home ./gradlew compileFileeasyDebugKotlin
```

你需要明确写出：
- 命令是否成功
- 如果失败，错误是什么
- 这个失败是环境问题还是代码问题

### 2. Web 构建检查

验证以下内容：

- `apps/web-admin` 构建是否可用
- 构建结果是否足够支持下一步联调

重点命令：

```bash
npm --prefix apps/web-admin run build
```

### 3. 环境与代码问题拆分

最终必须明确区分：

#### 环境问题
例如：
- `JAVA_HOME` 无效
- 本机没有 JDK/JRE
- Android 编译依赖环境缺失

#### 代码问题
例如：
- 真正的编译报错
- 真正的构建报错
- 与产品代码实现有关的问题

不要混写。

### 4. 给 PM 的下一步建议

任务结束时，你必须给出一个明确判断：

#### 选项 A
- 可以进入 `FE-V102` 端到端联调

#### 选项 B
- 还不能进入 `FE-V102`
- 先需要用户或 PM 处理这些环境问题：`...`

## 结果判断标准

本次任务至少要回答清楚下面这些问题：

1. Android 编译是否可运行
2. 如果不可运行，具体卡在什么环境问题
3. Web 构建是否通过
4. 当前是否可以进入 `FE-V102` 端到端联调
5. 哪些问题需要用户手动处理

## 必须执行的命令

至少执行并报告这些命令：

```bash
GRADLE_USER_HOME=.gradle-home ./gradlew compileFileeasyDebugKotlin
```

```bash
npm --prefix apps/web-admin run build
```

如果其中任一命令失败，必须写出：
- 原始失败原因
- 你判断的责任归属（环境 / 代码）
- 下一步应由谁处理

## 交付时必须按这个格式返回

1. Environment summary
- Android 环境状态
- Web 环境状态

2. Commands executed
- 实际跑了哪些命令
- 哪些通过
- 哪些失败

3. Blockers
- 明确列出环境阻塞项

4. Product-code concerns
- 只有真正属于代码的问题才写这里

5. PM handoff
- 是否建议直接进入 `FE-V102`
- 如果不建议，用户或 PM 下一步需要做什么
