import { execFileSync } from 'node:child_process';
import { existsSync, readdirSync, readFileSync, statSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import ts from 'typescript';

const OUTPUT_ROOT = 'src/app/api';
const clients = JSON.parse(readFileSync('api-clients.json', 'utf8'));

function stripComments(fileName, text) {
  const source = ts.createSourceFile(fileName, text, ts.ScriptTarget.Latest, true);
  const ranges = new Map();
  const collect = (pos) => {
    for (const range of [...(ts.getLeadingCommentRanges(text, pos) ?? []), ...(ts.getTrailingCommentRanges(text, pos) ?? [])]) {
      ranges.set(range.pos, range);
    }
  };
  const visit = (node) => {
    collect(node.getFullStart());
    collect(node.getEnd());
    node.getChildren(source).forEach(visit);
  };
  visit(source);

  let output = '';
  let last = 0;
  for (const range of [...ranges.values()].sort((a, b) => a.pos - b.pos)) {
    if (range.pos < last || text.slice(range.pos, range.end).includes('@ts-')) {
      continue;
    }
    output += text.slice(last, range.pos);
    last = range.end;
  }
  output += text.slice(last);
  return output.replace(/[ \t]+$/gm, '').replace(/\n{3,}/g, '\n\n').replace(/^\n+/, '');
}

function stripCommentsIn(dir) {
  for (const entry of readdirSync(dir)) {
    const path = join(dir, entry);
    if (statSync(path).isDirectory()) {
      stripCommentsIn(path);
    } else if (path.endsWith('.ts')) {
      writeFileSync(path, stripComments(path, readFileSync(path, 'utf8')));
    }
  }
}

for (const { name, spec } of clients) {
  if (!existsSync(spec)) {
    console.error(`Spec for ${name} not found: ${spec}`);
    process.exit(1);
  }
  const output = `${OUTPUT_ROOT}/${name}`;
  execFileSync(
    'npx',
    [
      'ng-openapi-gen',
      '--input', spec,
      '--output', output,
      '--removeStaleFiles', 'true',
      '--indexFile', 'true',
      '--services', 'true',
      '--skipJsonSuffix', 'true',
      '--enumStyle', 'alias',
      '--promises', 'false',
      '--silent', 'true',
    ],
    { stdio: 'inherit' },
  );
  stripCommentsIn(output);
  console.log(`Generated ${name} client from ${spec}`);
}
