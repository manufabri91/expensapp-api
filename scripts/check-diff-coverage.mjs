#!/usr/bin/env node
// Enforces the same 80% rule Codecov's "patch" check applies in CI (see ../codecov.yml), but
// locally and pre-push: only lines added/changed under src/main/java since the branch diverged
// from origin/develop need to be covered - untested pre-existing code is never penalized. Run
// manually with `node scripts/check-diff-coverage.mjs`; wired into .githooks/pre-push, activated
// once per clone via `git config core.hooksPath .githooks` (see README).

import { execFileSync } from 'node:child_process';
import { existsSync, readFileSync } from 'node:fs';

const THRESHOLD_PERCENT = 80;
const BASE_REF = process.env.DIFF_BASE_REF || 'origin/develop';
const JACOCO_XML_PATH = 'target/site/jacoco/jacoco.xml';

const run = (command, args) => execFileSync(command, args, { encoding: 'utf8' });

// Maven's Windows launcher is mvn.cmd, which Node can't spawn directly without a shell. Route it
// through cmd.exe explicitly (rather than execFileSync's generic shell:true, which silently
// disables argument escaping) - safe here since these args are always static, known-safe flags.
const runMaven = (args) =>
  process.platform === 'win32'
    ? execFileSync('cmd.exe', ['/d', '/s', '/c', 'mvn.cmd', ...args], { encoding: 'utf8' })
    : execFileSync('mvn', args, { encoding: 'utf8' });

const fail = (message) => {
  console.error(`\n✖ ${message}\n`);
  process.exit(1);
};

const findMergeBase = () => {
  const [remote, branch] = BASE_REF.includes('/') ? BASE_REF.split('/') : [null, BASE_REF];
  if (remote) {
    try {
      run('git', ['fetch', '--quiet', remote, branch]);
    } catch {
      console.warn(`Could not fetch ${BASE_REF}; diffing against whatever is already fetched locally.`);
    }
  }
  try {
    return run('git', ['merge-base', 'HEAD', BASE_REF]).trim();
  } catch {
    console.warn(`Could not find a merge base with ${BASE_REF}; skipping diff-coverage check.`);
    process.exit(0);
  }
};

// Parses a unified diff (-U0) into { file -> [added line numbers in the new version] }.
const collectAddedLines = (mergeBase, files) => {
  const addedLinesByFile = new Map();
  for (const file of files) {
    const diffOutput = run('git', ['diff', '-U0', mergeBase, 'HEAD', '--', file]);
    const addedLines = [];
    let newLineCursor = null;
    for (const line of diffOutput.split('\n')) {
      const hunkMatch = line.match(/^@@ -\d+(?:,\d+)? \+(\d+)/);
      if (hunkMatch) {
        newLineCursor = Number(hunkMatch[1]);
        continue;
      }
      if (newLineCursor === null) continue;
      if (line.startsWith('+++') || line.startsWith('---')) continue;
      if (line.startsWith('+')) {
        addedLines.push(newLineCursor);
        newLineCursor += 1;
      } else if (!line.startsWith('-')) {
        newLineCursor += 1;
      }
    }
    if (addedLines.length > 0) addedLinesByFile.set(file, addedLines);
  }
  return addedLinesByFile;
};

// Builds { "package/path/File.java" -> Map<lineNr, {mi, ci}> } from JaCoCo's XML report.
const parseJacocoLineCoverage = (xml) => {
  const lineCoverageByFile = new Map();
  const packageRegex = /<package name="([^"]+)">([\s\S]*?)<\/package>/g;
  let packageMatch;
  while ((packageMatch = packageRegex.exec(xml))) {
    const [, packageName, packageBody] = packageMatch;
    const sourcefileRegex = /<sourcefile name="([^"]+)">([\s\S]*?)<\/sourcefile>/g;
    let sourcefileMatch;
    while ((sourcefileMatch = sourcefileRegex.exec(packageBody))) {
      const [, sourcefileName, sourcefileBody] = sourcefileMatch;
      const lines = new Map();
      const lineRegex = /<line nr="(\d+)" mi="(\d+)" ci="(\d+)"/g;
      let lineMatch;
      while ((lineMatch = lineRegex.exec(sourcefileBody))) {
        const [, nr, mi, ci] = lineMatch;
        lines.set(Number(nr), { missedInstructions: Number(mi), coveredInstructions: Number(ci) });
      }
      lineCoverageByFile.set(`${packageName}/${sourcefileName}`, lines);
    }
  }
  return lineCoverageByFile;
};

const mergeBase = findMergeBase();

const changedFiles = run('git', ['diff', '--name-only', '--diff-filter=ACMR', mergeBase, 'HEAD', '--', 'src/main/java'])
  .split('\n')
  .map((line) => line.trim())
  .filter((line) => line.endsWith('.java'));

if (changedFiles.length === 0) {
  console.log('No changed src/main/java files - skipping diff-coverage check.');
  process.exit(0);
}

const addedLinesByFile = collectAddedLines(mergeBase, changedFiles);
if (addedLinesByFile.size === 0) {
  console.log('No added lines in changed Java files - skipping diff-coverage check.');
  process.exit(0);
}

console.log('Running `mvn test` to produce a fresh coverage report...');
try {
  runMaven(['-B', '-q', 'test']);
} catch {
  fail('Tests failed - fix them before pushing.');
}

if (!existsSync(JACOCO_XML_PATH)) {
  fail(`Expected a JaCoCo report at ${JACOCO_XML_PATH} but none was found.`);
}

const lineCoverageByFile = parseJacocoLineCoverage(readFileSync(JACOCO_XML_PATH, 'utf8'));

let coveredCount = 0;
let coverableCount = 0;
const uncoveredByFile = new Map();

for (const [file, addedLines] of addedLinesByFile) {
  const key = file.replace(/^src\/main\/java\//, '');
  const coverage = lineCoverageByFile.get(key);
  if (!coverage) continue; // Not instrumented (e.g. an interface with no method bodies).

  for (const lineNr of addedLines) {
    const entry = coverage.get(lineNr);
    if (!entry) continue; // Not a coverable line (blank, comment, brace, import, package decl.).
    if (entry.missedInstructions === 0 && entry.coveredInstructions === 0) continue;

    coverableCount += 1;
    if (entry.coveredInstructions > 0) {
      coveredCount += 1;
    } else {
      const list = uncoveredByFile.get(file) ?? [];
      list.push(lineNr);
      uncoveredByFile.set(file, list);
    }
  }
}

if (coverableCount === 0) {
  console.log('No coverable added lines (only comments/imports/braces changed) - skipping diff-coverage check.');
  process.exit(0);
}

const percentage = (coveredCount / coverableCount) * 100;
console.log(`\nDiff coverage: ${coveredCount}/${coverableCount} lines covered (${percentage.toFixed(1)}%)\n`);

if (percentage + 1e-9 < THRESHOLD_PERCENT) {
  console.error(`Uncovered added lines (need ${THRESHOLD_PERCENT}% diff coverage):`);
  for (const [file, lineNrs] of uncoveredByFile) {
    console.error(`  ${file}: ${lineNrs.join(', ')}`);
  }
  fail(`Diff coverage ${percentage.toFixed(1)}% is below the ${THRESHOLD_PERCENT}% threshold.`);
}

console.log('Diff coverage check passed.');
