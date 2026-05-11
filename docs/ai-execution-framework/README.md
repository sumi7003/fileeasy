# AI Execution Framework

## 1. Goal

This framework turns AI development from a prompt habit into a gated delivery system.

It is designed to prevent the most common AI delivery failures:

- starting implementation before upstream documents are ready
- changing code outside the approved scope
- treating vague prompts as executable tasks
- delivering code without verification, risks, or unfinished items
- letting PRD, design, and implementation drift apart

## 2. What Lives Here

- `templates/`
  - reusable templates for project bundles, task packs, prompts, and acceptance records
- `examples/`
  - onboarding examples that show how a real project plugs into the framework
- `scripts/ai-framework/`
  - machine gates that validate readiness, task completeness, and change scope

## 3. Execution Model

The framework assumes four layers:

1. `Project bundle`
   - registers the current PRD, design, workflow, and their readiness status
2. `Task pack`
   - defines a single AI task with clear inputs, allowed paths, forbidden paths, and acceptance checks
3. `Prompt`
   - dispatches one approved task pack to one execution AI
4. `Delivery + acceptance`
   - records what changed, what was verified, what is still risky, and whether the task passed

## 4. Mandatory Gates

### Gate A: document readiness

Before an execution task can start:

- the project bundle must exist
- required document kinds must be registered
- the ready gate must pass for the current project stage

Command:

```bash
npm run ai:validate:bundle -- docs/ai-execution-framework/examples/fileeasy/project-bundle.json
```

Strict ready gate:

```bash
npm run ai:validate:bundle:ready -- docs/ai-execution-framework/examples/fileeasy/project-bundle.json
```

### Gate B: task completeness

Before an execution AI receives work:

- the task pack must pass structural validation
- document references must resolve into the project bundle
- allowed and forbidden paths must be explicit
- acceptance checks must exist

Command:

```bash
npm run ai:validate:task -- path/to/task-pack.json
```

### Gate C: scope compliance

Before delivery is accepted:

- changed files must stay inside `allowedPaths` or `controlPaths`
- changed files must not enter `forbiddenPaths`

Command:

```bash
npm run ai:check-scope -- path/to/task-pack.json
```

You can also pass explicit files:

```bash
npm run ai:check-scope -- path/to/task-pack.json apps/android-player/src/fileeasy/res/values/strings.xml
```

### Gate D: git publish readiness

The framework now treats remote collaboration as an explicit gate.

It exists to prevent the opposite failure mode:

- work is accepted locally but never reaches the remote repository
- AI pushes from an unsafe branch
- AI pushes before acceptance or before a publish policy is declared

Command:

```bash
npm run ai:validate:publish -- path/to/task-pack.json
```

What this gate checks:

- the task pack contains a `publishPolicy`
- remote name, branch rules, and protection rules are explicit
- the current git branch is eligible for publishing
- the configured remote exists locally

Important rule:

- automatic push should only be enabled when the task pack explicitly allows it
- default-safe behavior is `prepare for publish` rather than `push unconditionally`

### Gate E: AI publish execution

After all prior gates pass, the framework can execute a controlled publish flow:

1. stage only in-scope files
2. create a commit
3. push the working branch
4. open or reuse a pull request

Command:

```bash
npm run ai:publish -- path/to/task-pack.json --dry-run
```

Real execution:

```bash
npm run ai:publish -- path/to/task-pack.json
```

This command still obeys `publishPolicy`. It does not publish when:

- publishing is disabled
- the branch is protected
- the current branch does not match the declared publish branch
- the task status does not satisfy the required publish status
- out-of-scope changes exist and the policy says to block

## 5. Recommended Workflow

1. Register one project bundle.
2. Run the bundle validator.
3. Create one task pack per atomic delivery.
4. Run the task validator.
5. Generate the execution prompt from the approved task pack.
6. Let the execution AI work only inside the declared scope.
7. Run the scope checker before acceptance.
8. Record delivery and acceptance using the templates.
9. Validate publish readiness.
10. Only then commit, push, and open the remote review flow if the task pack allows it.

## 6. FileEasy Example

`examples/fileeasy/project-bundle.json` is intentionally limited to project onboarding.

It proves that:

- the framework is generic
- FileEasy can plug into it immediately
- business task content should only appear after the bundle is ready

At the moment, the FileEasy example is useful as a preflight check, not as an implementation-ready task source.
