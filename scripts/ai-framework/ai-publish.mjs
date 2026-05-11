import fs from "node:fs";
import path from "node:path";
import { execFileSync, execSync } from "node:child_process";

function fail(message) {
  console.error(`ERROR: ${message}`);
  process.exitCode = 1;
}

function info(message) {
  console.log(message);
}

function summarizeFiles(files, limit = 12) {
  if (files.length <= limit) {
    return files.join(", ");
  }

  return `${files.slice(0, limit).join(", ")} ... (+${files.length - limit} more)`;
}

function usage() {
  console.error(
    "Usage: node scripts/ai-framework/ai-publish.mjs <task-pack.json> [--dry-run] [--skip-pr]"
  );
  process.exit(1);
}

function resolveRepoPath(relativePath) {
  return path.resolve(process.cwd(), relativePath);
}

function normalizeRepoPath(inputPath) {
  return inputPath.replaceAll("\\", "/").replace(/\/+$/, "");
}

function isInside(filePath, scopePath) {
  return filePath === scopePath || filePath.startsWith(`${scopePath}/`);
}

function run(command, args = [], options = {}) {
  if (options.dryRun) {
    info(`DRY RUN: ${[command, ...args].join(" ")}`);
    return "";
  }

  return execFileSync(command, args, {
    cwd: process.cwd(),
    encoding: "utf8",
    stdio: options.captureOutput ? ["ignore", "pipe", "pipe"] : "pipe"
  }).trim();
}

function runShell(command) {
  return execSync(command, {
    cwd: process.cwd(),
    encoding: "utf8"
  }).trim();
}

function runNodeScript(scriptPath, args = []) {
  execFileSync("node", [scriptPath, ...args], {
    cwd: process.cwd(),
    encoding: "utf8",
    stdio: "inherit"
  });
}

function getChangedFiles() {
  const output = runShell("git status --short --untracked-files=all");
  if (!output) {
    return [];
  }

  return output
    .split("\n")
    .map((line) => line.trimEnd())
    .filter(Boolean)
    .map((line) => {
      const content = line.slice(3);
      const renameArrow = content.lastIndexOf(" -> ");
      return renameArrow >= 0 ? content.slice(renameArrow + 4) : content;
    })
    .map(normalizeRepoPath);
}

function loadTaskPack(taskArg) {
  const taskPath = resolveRepoPath(taskArg);
  if (!fs.existsSync(taskPath)) {
    throw new Error(`task pack not found: ${taskArg}`);
  }

  return JSON.parse(fs.readFileSync(taskPath, "utf8"));
}

function ensurePublishReady(task, taskArg) {
  if (!task.publishPolicy) {
    fail("task pack does not define publishPolicy");
    return null;
  }

  const publish = task.publishPolicy;
  const currentBranch = runShell("git branch --show-current");
  const remotes = runShell("git remote").split("\n").filter(Boolean);

  if (publish.enabled === false) {
    fail("publishPolicy disables remote publishing for this task");
    return null;
  }

  if (publish.mode === "manual") {
    fail("publishPolicy.mode=manual does not allow automated ai publish");
  }

  if (!currentBranch) {
    fail("current git branch is empty or detached");
  }

  if (!remotes.includes(publish.remoteName)) {
    fail(`git remote not found: ${publish.remoteName}`);
  }

  if ((publish.protectedBranches || []).includes(currentBranch)) {
    fail(`current branch is protected: ${currentBranch}`);
  }

  if (publish.branchName !== currentBranch) {
    fail(`current branch "${currentBranch}" does not match publishPolicy.branchName`);
  }

  if (publish.requireAcceptedStatus && task.status !== "accepted") {
    fail(`task status must be accepted before publish, got "${task.status}"`);
  }

  const allowedScopes = [
    ...(task.allowedPaths || []),
    ...(task.controlPaths || []),
    task.projectBundlePath,
    taskArg
  ]
    .filter(Boolean)
    .map(normalizeRepoPath);
  const forbiddenScopes = (task.forbiddenPaths || []).map(normalizeRepoPath);
  const changedFiles = getChangedFiles();
  const inScopeFiles = [];
  const outOfScopeFiles = [];

  for (const file of changedFiles) {
    const forbidden = forbiddenScopes.find((scope) => isInside(file, scope));
    if (forbidden) {
      outOfScopeFiles.push(file);
      continue;
    }

    const allowed = allowedScopes.find((scope) => isInside(file, scope));
    if (allowed) {
      inScopeFiles.push(file);
    } else {
      outOfScopeFiles.push(file);
    }
  }

  if (outOfScopeFiles.length > 0 && publish.blockOnOutOfScopeChanges) {
    fail(
      `out-of-scope changes detected (${outOfScopeFiles.length}): ${summarizeFiles(outOfScopeFiles)}`
    );
  }

  return {
    publish,
    currentBranch,
    changedFiles,
    inScopeFiles,
    outOfScopeFiles
  };
}

function buildPrBody(task, publish) {
  if (publish.prBody) {
    return publish.prBody;
  }

  if (publish.prBodyPath) {
    const bodyPath = resolveRepoPath(publish.prBodyPath);
    if (fs.existsSync(bodyPath)) {
      return fs.readFileSync(bodyPath, "utf8");
    }
  }

  return [
    `Task ID: ${task.taskId}`,
    `Task name: ${task.taskName}`,
    "",
    "Automated by ai-publish.",
    "",
    "Verification commands:",
    ...(task.verificationCommands || []).map((command) => `- ${command}`)
  ].join("\n");
}

function findExistingPr(branchName, targetBranch) {
  try {
    const output = execFileSync(
      "gh",
      [
        "pr",
        "list",
        "--head",
        branchName,
        "--base",
        targetBranch,
        "--json",
        "url",
        "--limit",
        "1"
      ],
      {
        cwd: process.cwd(),
        encoding: "utf8"
      }
    );

    const data = JSON.parse(output);
    return data[0]?.url || "";
  } catch {
    return "";
  }
}

const rawArgs = process.argv.slice(2);
const taskArg = rawArgs.find((arg) => !arg.startsWith("--"));
const dryRun = rawArgs.includes("--dry-run");
const skipPr = rawArgs.includes("--skip-pr");

if (!taskArg) {
  usage();
}

let task;

try {
  task = loadTaskPack(taskArg);
} catch (error) {
  console.error(error instanceof Error ? `ERROR: ${error.message}` : String(error));
  process.exit(1);
}

runNodeScript("scripts/ai-framework/validate-task-pack.mjs", [taskArg]);
runNodeScript("scripts/ai-framework/validate-publish-gate.mjs", [taskArg]);

const state = ensurePublishReady(task, taskArg);

if (process.exitCode) {
  process.exit(process.exitCode);
}

const { publish, currentBranch, inScopeFiles, outOfScopeFiles } = state;

info(`Publish branch: ${currentBranch}`);
info(`In-scope changed files: ${inScopeFiles.length}`);

if (outOfScopeFiles.length > 0 && !publish.blockOnOutOfScopeChanges) {
  info(
    `Ignoring out-of-scope changes per policy (${outOfScopeFiles.length}): ${summarizeFiles(outOfScopeFiles)}`
  );
}

if (inScopeFiles.length > 0) {
  run("git", ["add", "--", ...inScopeFiles], { dryRun });
  const commitMessage = publish.commitMessage || `${task.taskId}: ${task.taskName}`;
  run("git", ["commit", "-m", commitMessage], { dryRun });
} else {
  info("No in-scope local changes to commit");
}

run("git", ["push", publish.remoteName, currentBranch], { dryRun });

let prUrl = "";

if (publish.requirePullRequest && !skipPr) {
  if (!dryRun) {
    run("gh", ["auth", "status"], { captureOutput: true });
  }

  if (dryRun) {
    prUrl = "DRY-RUN-PR";
    info("DRY RUN: would create or reuse pull request");
  } else {
    prUrl = findExistingPr(currentBranch, publish.targetBranch);
  }

  if (prUrl && !dryRun) {
    info(`Pull request already exists: ${prUrl}`);
  } else if (!prUrl) {
    const prTitle = publish.prTitle || publish.commitMessage || `${task.taskId}: ${task.taskName}`;
    const prBody = buildPrBody(task, publish);
    const args = [
      "pr",
      "create",
      "--base",
      publish.targetBranch,
      "--head",
      currentBranch,
      "--title",
      prTitle,
      "--body",
      prBody
    ];

    if (publish.draftPullRequest) {
      args.push("--draft");
    }

    const output = run("gh", args, { dryRun, captureOutput: true });
    prUrl = dryRun ? "DRY-RUN-PR" : output;
  }
}

if (process.exitCode) {
  process.exit(process.exitCode);
}

info(
  dryRun
    ? "DRY RUN OK: ai publish flow validated without remote writes"
    : `OK: ai publish completed${prUrl ? ` (${prUrl})` : ""}`
);
