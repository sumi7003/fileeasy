import fs from "node:fs";
import path from "node:path";

const READY_STATUSES = new Set(["final", "approved"]);
const DOCUMENT_STATUSES = new Set([
  "draft",
  "candidate",
  "final",
  "approved",
  "archived"
]);

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

const rawArgs = process.argv.slice(2);
const readyMode = rawArgs.includes("--ready");
const bundleArg = rawArgs.find((arg) => arg !== "--ready");

if (!bundleArg) {
  console.error(
    "Usage: node scripts/ai-framework/validate-project-bundle.mjs [--ready] <project-bundle.json>"
  );
  process.exit(1);
}

const bundlePath = resolveRepoPath(bundleArg);

if (!fs.existsSync(bundlePath)) {
  console.error(`ERROR: bundle file not found: ${bundleArg}`);
  process.exit(1);
}

let bundle;

try {
  bundle = JSON.parse(fs.readFileSync(bundlePath, "utf8"));
} catch (error) {
  console.error(`ERROR: failed to parse JSON from ${bundleArg}`);
  console.error(error instanceof Error ? error.message : String(error));
  process.exit(1);
}

requireString(bundle.projectId, "projectId");
requireString(bundle.projectName, "projectName");
requireString(bundle.bundleVersion, "bundleVersion");
requireString(bundle.bundleStatus, "bundleStatus");
requireArray(bundle.requiredDocumentKinds, "requiredDocumentKinds");
requireArray(bundle.documents, "documents");

const documentIds = new Set();
const documentsByKind = new Map();

for (const [index, document] of (bundle.documents || []).entries()) {
  const prefix = `documents[${index}]`;
  const ok =
    requireString(document?.id, `${prefix}.id`) &&
    requireString(document?.kind, `${prefix}.kind`) &&
    requireString(document?.version, `${prefix}.version`) &&
    requireString(document?.status, `${prefix}.status`) &&
    requireString(document?.path, `${prefix}.path`);

  if (!ok) {
    continue;
  }

  if (path.isAbsolute(document.path)) {
    fail(`${prefix}.path must be repo-relative, got absolute path`);
  }

  if (!DOCUMENT_STATUSES.has(document.status)) {
    fail(
      `${prefix}.status must be one of ${Array.from(DOCUMENT_STATUSES).join(", ")}`
    );
  }

  if (documentIds.has(document.id)) {
    fail(`duplicate document id found: ${document.id}`);
  } else {
    documentIds.add(document.id);
  }

  if (!fs.existsSync(resolveRepoPath(document.path))) {
    fail(`${prefix}.path does not exist: ${document.path}`);
  }

  const byKind = documentsByKind.get(document.kind) || [];
  byKind.push(document);
  documentsByKind.set(document.kind, byKind);
}

for (const [index, kind] of (bundle.requiredDocumentKinds || []).entries()) {
  if (!requireString(kind, `requiredDocumentKinds[${index}]`)) {
    continue;
  }

  const matches = documentsByKind.get(kind) || [];

  if (matches.length === 0) {
    fail(`required document kind is missing: ${kind}`);
    continue;
  }

  if (readyMode) {
    const readyDocument = matches.find((document) => READY_STATUSES.has(document.status));
    if (!readyDocument) {
      fail(
        `ready gate failed for kind "${kind}": no document is in ${Array.from(READY_STATUSES).join(" or ")} status`
      );
    }
  }
}

if (process.exitCode) {
  process.exit(process.exitCode);
}

info(
  readyMode
    ? `READY OK: ${bundle.projectName} bundle passed strict readiness validation`
    : `OK: ${bundle.projectName} bundle passed structural validation`
);
