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

function usage() {
  console.error(
    "Usage: node scripts/ai-framework/check-change-scope.mjs <task-pack.json> [changed-file ...]"
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

function loadChangedFiles(explicitFiles) {
  if (explicitFiles.length > 0) {
    return explicitFiles.map(normalizeRepoPath);
  }

  const output = execSync("git status --short --untracked-files=all", {
    cwd: process.cwd(),
    encoding: "utf8"
  });

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

const [taskArg, ...explicitFiles] = process.argv.slice(2);

if (!taskArg) {
  usage();
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

const changedFiles = loadChangedFiles(explicitFiles);

if (changedFiles.length === 0) {
  info("OK: no changed files detected");
  process.exit(0);
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

for (const file of changedFiles) {
  const inForbidden = forbiddenScopes.find((scope) => isInside(file, scope));
  if (inForbidden) {
    fail(`file is inside forbidden scope: ${file} -> ${inForbidden}`);
    continue;
  }

  const inAllowed = allowedScopes.find((scope) => isInside(file, scope));
  if (!inAllowed) {
    fail(`file is outside allowed scope: ${file}`);
  }
}

if (process.exitCode) {
  process.exit(process.exitCode);
}

info(`OK: ${changedFiles.length} changed file(s) stayed within the declared scope`);
