import fs from "node:fs";
import path from "node:path";

const TASK_STATUSES = new Set([
  "draft",
  "approved",
  "in_progress",
  "delivered",
  "accepted",
  "blocked"
]);
const ACCEPTANCE_TYPES = new Set(["scenario", "command", "manual"]);
const PUBLISH_MODES = new Set(["manual", "ai-assisted", "automatic"]);

function fail(message) {
  console.error(`ERROR: ${message}`);
  process.exitCode = 1;
}

function ok(message) {
  console.log(message);
}

function requireString(value, fieldName) {
  if (typeof value !== "string" || value.trim() === "") {
    fail(`${fieldName} must be a non-empty string`);
    return false;
  }

  return true;
}

function requireArray(value, fieldName) {
  if (!Array.isArray(value) || value.length === 0) {
    fail(`${fieldName} must be a non-empty array`);
    return false;
  }

  return true;
}

function resolveRepoPath(relativePath) {
  return path.resolve(process.cwd(), relativePath);
}

function normalizeRepoPath(inputPath) {
  return inputPath.replaceAll("\\", "/").replace(/\/+$/, "");
}

function pathOverlaps(left, right) {
  return left === right || left.startsWith(`${right}/`) || right.startsWith(`${left}/`);
}

const taskArg = process.argv[2];

if (!taskArg) {
  console.error(
    "Usage: node scripts/ai-framework/validate-task-pack.mjs <task-pack.json>"
  );
  process.exit(1);
}

const taskPath = resolveRepoPath(taskArg);

if (!fs.existsSync(taskPath)) {
  console.error(`ERROR: task pack not found: ${taskArg}`);
  process.exit(1);
}

let task;

try {
  task = JSON.parse(fs.readFileSync(taskPath, "utf8"));
} catch (error) {
  console.error(`ERROR: failed to parse JSON from ${taskArg}`);
  console.error(error instanceof Error ? error.message : String(error));
  process.exit(1);
}

requireString(task.taskId, "taskId");
requireString(task.taskName, "taskName");
requireString(task.projectId, "projectId");
requireString(task.projectBundlePath, "projectBundlePath");
requireString(task.stage, "stage");
requireString(task.status, "status");
requireString(task.ownerRole, "ownerRole");
requireString(task.goal, "goal");
requireArray(task.nonGoals, "nonGoals");
requireArray(task.documentRefs, "documentRefs");
requireArray(task.allowedPaths, "allowedPaths");
requireArray(task.deliverables, "deliverables");
requireArray(task.acceptanceChecks, "acceptanceChecks");
requireArray(task.verificationCommands, "verificationCommands");

if (task.controlPaths && !Array.isArray(task.controlPaths)) {
  fail("controlPaths must be an array when provided");
}

if (task.forbiddenPaths && !Array.isArray(task.forbiddenPaths)) {
  fail("forbiddenPaths must be an array when provided");
}

if (task.handoffQuestions && !Array.isArray(task.handoffQuestions)) {
  fail("handoffQuestions must be an array when provided");
}

if (task.publishPolicy && typeof task.publishPolicy !== "object") {
  fail("publishPolicy must be an object when provided");
}

if (typeof task.status === "string" && !TASK_STATUSES.has(task.status)) {
  fail(`status must be one of ${Array.from(TASK_STATUSES).join(", ")}`);
}

for (const fieldName of ["projectBundlePath"]) {
  const value = task[fieldName];
  if (typeof value === "string" && path.isAbsolute(value)) {
    fail(`${fieldName} must be repo-relative, got absolute path`);
  }
}

for (const fieldName of ["allowedPaths", "controlPaths", "forbiddenPaths"]) {
  const values = task[fieldName] || [];
  for (const [index, value] of values.entries()) {
    if (!requireString(value, `${fieldName}[${index}]`)) {
      continue;
    }

    if (path.isAbsolute(value)) {
      fail(`${fieldName}[${index}] must be repo-relative, got absolute path`);
    }
  }
}

for (const [index, check] of (task.acceptanceChecks || []).entries()) {
  const prefix = `acceptanceChecks[${index}]`;
  const valid =
    requireString(check?.id, `${prefix}.id`) &&
    requireString(check?.type, `${prefix}.type`) &&
    requireString(check?.text, `${prefix}.text`);

  if (!valid) {
    continue;
  }

  if (!ACCEPTANCE_TYPES.has(check.type)) {
    fail(`${prefix}.type must be one of ${Array.from(ACCEPTANCE_TYPES).join(", ")}`);
  }
}

if (task.publishPolicy) {
  const publish = task.publishPolicy;
  requireString(publish.mode, "publishPolicy.mode");
  requireString(publish.remoteName, "publishPolicy.remoteName");
  requireString(publish.branchName, "publishPolicy.branchName");
  requireString(publish.targetBranch, "publishPolicy.targetBranch");

  if (typeof publish.enabled !== "boolean") {
    fail("publishPolicy.enabled must be a boolean");
  }

  if (typeof publish.autoPushAfterAcceptance !== "boolean") {
    fail("publishPolicy.autoPushAfterAcceptance must be a boolean");
  }

  if (typeof publish.blockOnOutOfScopeChanges !== "boolean") {
    fail("publishPolicy.blockOnOutOfScopeChanges must be a boolean");
  }

  if (typeof publish.requireAcceptedStatus !== "boolean") {
    fail("publishPolicy.requireAcceptedStatus must be a boolean");
  }

  if (typeof publish.requirePullRequest !== "boolean") {
    fail("publishPolicy.requirePullRequest must be a boolean");
  }

  if (typeof publish.draftPullRequest !== "boolean") {
    fail("publishPolicy.draftPullRequest must be a boolean");
  }

  if (!Array.isArray(publish.protectedBranches) || publish.protectedBranches.length === 0) {
    fail("publishPolicy.protectedBranches must be a non-empty array");
  }

  if (typeof publish.mode === "string" && !PUBLISH_MODES.has(publish.mode)) {
    fail(`publishPolicy.mode must be one of ${Array.from(PUBLISH_MODES).join(", ")}`);
  }

  for (const [index, branch] of (publish.protectedBranches || []).entries()) {
    requireString(branch, `publishPolicy.protectedBranches[${index}]`);
  }

  if (publish.commitMessage !== undefined) {
    requireString(publish.commitMessage, "publishPolicy.commitMessage");
  }

  if (publish.prTitle !== undefined) {
    requireString(publish.prTitle, "publishPolicy.prTitle");
  }

  if (publish.prBody !== undefined) {
    requireString(publish.prBody, "publishPolicy.prBody");
  }

  if (publish.prBodyPath !== undefined) {
    requireString(publish.prBodyPath, "publishPolicy.prBodyPath");
  }
}

const projectBundlePath = resolveRepoPath(task.projectBundlePath || "");

if (!fs.existsSync(projectBundlePath)) {
  fail(`projectBundlePath does not exist: ${task.projectBundlePath}`);
} else {
  const bundle = JSON.parse(fs.readFileSync(projectBundlePath, "utf8"));
  const bundleDocumentIds = new Set((bundle.documents || []).map((document) => document.id));

  if (bundle.projectId && task.projectId && bundle.projectId !== task.projectId) {
    fail(
      `projectId mismatch: task pack uses "${task.projectId}" but bundle uses "${bundle.projectId}"`
    );
  }

  for (const [index, ref] of (task.documentRefs || []).entries()) {
    if (!requireString(ref, `documentRefs[${index}]`)) {
      continue;
    }

    if (!bundleDocumentIds.has(ref)) {
      fail(`documentRefs[${index}] does not exist in bundle: ${ref}`);
    }
  }
}

const allowed = (task.allowedPaths || []).map(normalizeRepoPath);
const forbidden = (task.forbiddenPaths || []).map(normalizeRepoPath);

for (const left of allowed) {
  for (const right of forbidden) {
    if (pathOverlaps(left, right)) {
      fail(`allowedPaths and forbiddenPaths overlap: "${left}" vs "${right}"`);
    }
  }
}

if (process.exitCode) {
  process.exit(process.exitCode);
}

ok(`OK: ${task.taskId} passed task-pack validation`);
