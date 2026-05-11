import fs from "node:fs";
import path from "node:path";
import { execSync } from "node:child_process";

function fail(message) {
  console.error(`ERROR: ${message}`);
  process.exitCode = 1;
}

function info(message) {
  console.log(message);
}

function requireString(value, fieldName) {
  if (typeof value !== "string" || value.trim() === "") {
    fail(`${fieldName} must be a non-empty string`);
    return false;
  }

  return true;
}

function runGit(command) {
  return execSync(command, {
    cwd: process.cwd(),
    encoding: "utf8"
  }).trim();
}

const taskArg = process.argv[2];

if (!taskArg) {
  console.error(
    "Usage: node scripts/ai-framework/validate-publish-gate.mjs <task-pack.json>"
  );
  process.exit(1);
}

const taskPath = path.resolve(process.cwd(), taskArg);

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

if (!task.publishPolicy) {
  console.error("ERROR: task pack does not define publishPolicy");
  process.exit(1);
}

const publish = task.publishPolicy;
requireString(publish.remoteName, "publishPolicy.remoteName");
requireString(publish.branchName, "publishPolicy.branchName");
requireString(publish.targetBranch, "publishPolicy.targetBranch");

if (typeof publish.enabled !== "boolean") {
  fail("publishPolicy.enabled must be a boolean");
}

if (!Array.isArray(publish.protectedBranches) || publish.protectedBranches.length === 0) {
  fail("publishPolicy.protectedBranches must be a non-empty array");
}

let currentBranch = "";
let remotes = [];

try {
  currentBranch = runGit("git branch --show-current");
  remotes = runGit("git remote").split("\n").filter(Boolean);
} catch (error) {
  console.error("ERROR: failed to read git state");
  console.error(error instanceof Error ? error.message : String(error));
  process.exit(1);
}

if (!currentBranch) {
  fail("current git branch is empty or detached");
}

if (!remotes.includes(publish.remoteName)) {
  fail(`git remote not found: ${publish.remoteName}`);
}

if (publish.enabled === false) {
  info("OK: publishPolicy explicitly disables remote publishing for this task");
  process.exit(0);
}

if (publish.mode === "manual") {
  fail("publishPolicy.mode=manual does not allow automated ai publish");
}

if (publish.protectedBranches.includes(currentBranch)) {
  fail(`current branch is protected and cannot be used for AI publishing: ${currentBranch}`);
}

if (currentBranch !== publish.branchName) {
  fail(
    `current branch mismatch: expected "${publish.branchName}" but found "${currentBranch}"`
  );
}

if (publish.requireAcceptedStatus && task.status !== "accepted") {
  fail(
    `publish requires accepted task status, but current status is "${task.status}"`
  );
}

if (process.exitCode) {
  process.exit(process.exitCode);
}

info(
  `OK: publish gate passed for branch ${currentBranch} -> ${publish.remoteName}/${publish.targetBranch}`
);
