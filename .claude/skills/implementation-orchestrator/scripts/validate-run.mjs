#!/usr/bin/env node
// Validate a run ledger against the schema shipped beside this script.
//
//   node scripts/validate-run.mjs context/implementation-runs/<run-id>/run.json
//
// Exits 0 when the ledger satisfies the contract and 1 with one finding per
// line when it does not. No dependencies, so it runs anywhere Node runs.
//
// The schema deliberately uses only the keywords handled here: type, enum,
// required, properties and items. Keep it that way.

import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const DEFAULT_SCHEMA_PATH = path.join(
  path.dirname(fileURLToPath(import.meta.url)),
  "..",
  "schemas",
  "run-schema.json"
);

function matchesType(type, value) {
  if (type === "null") return value === null;
  if (type === "array") return Array.isArray(value);
  if (type === "object") return isPlainObject(value);
  if (type === "integer") return Number.isInteger(value);
  return typeof value === type;
}

function isPlainObject(value) {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

export function validateRun(schema, value, location = "$") {
  const findings = [];

  if (schema.enum && !schema.enum.includes(value)) {
    return [`${location}: ${JSON.stringify(value)} is not one of ${schema.enum.join(", ")}`];
  }

  const expectedTypes = schema.type ? [schema.type].flat() : null;

  if (expectedTypes && !expectedTypes.some((type) => matchesType(type, value))) {
    return [`${location}: expected ${expectedTypes.join(" or ")}`];
  }

  if (schema.properties && isPlainObject(value)) {
    for (const key of schema.required ?? []) {
      if (!(key in value)) {
        findings.push(`${location}: missing required property "${key}"`);
      }
    }

    for (const [key, propertySchema] of Object.entries(schema.properties)) {
      if (key in value) {
        findings.push(...validateRun(propertySchema, value[key], `${location}.${key}`));
      }
    }
  }

  if (schema.items && Array.isArray(value)) {
    value.forEach((item, index) => {
      findings.push(...validateRun(schema.items, item, `${location}[${index}]`));
    });
  }

  return findings;
}

export async function validateRunFile(runPath, schemaPath = DEFAULT_SCHEMA_PATH) {
  const [schema, ledger] = await Promise.all([
    readFile(schemaPath, "utf8").then(JSON.parse),
    readFile(runPath, "utf8").then(JSON.parse)
  ]);

  return validateRun(schema, ledger);
}

async function main(argv) {
  const runPath = argv.find((argument) => !argument.startsWith("--"));

  if (!runPath) {
    console.error("usage: validate-run.mjs <run.json> [--schema <schema.json>]");
    return 2;
  }

  const schemaFlag = argv.indexOf("--schema");
  const schemaPath = schemaFlag === -1 ? DEFAULT_SCHEMA_PATH : argv[schemaFlag + 1];
  const findings = await validateRunFile(runPath, schemaPath);

  if (findings.length === 0) {
    console.log(`${runPath} satisfies the run ledger contract`);
    return 0;
  }

  for (const finding of findings) {
    console.error(finding);
  }

  return 1;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  process.exit(await main(process.argv.slice(2)));
}
