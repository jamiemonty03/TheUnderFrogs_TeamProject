export interface Inline {
  text: string;
  bold: boolean;
}

export type Block =
  | { kind: 'heading'; content: Inline[] }
  | { kind: 'paragraph'; lines: Inline[][] }
  | { kind: 'bullets'; items: Inline[][] }
  | { kind: 'numbered'; items: Inline[][] }
  | { kind: 'table'; header: Inline[][]; rows: Inline[][][] };

const HEADING = /^#{1,6}\s+(.*)$/;
const BULLET = /^\s*[-*•]\s+(.*)$/;
const NUMBERED = /^\s*\d+[.)]\s+(.*)$/;
const TABLE_ROW = /^\s*\|.*\|\s*$/;
const TABLE_DIVIDER = /^\s*\|?\s*:?-{2,}:?\s*(\|\s*:?-{2,}:?\s*)*\|?\s*$/;

export function formatReply(text: string): Block[] {
  const blocks: Block[] = [];
  const lines = (text ?? '').replace(/\r\n?/g, '\n').split('\n');

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    if (!line.trim()) {
      continue;
    }

    if (TABLE_ROW.test(line)) {
      const rows: string[] = [];
      while (i < lines.length && TABLE_ROW.test(lines[i])) {
        if (!TABLE_DIVIDER.test(lines[i])) {
          rows.push(lines[i]);
        }
        i++;
      }
      i--;
      const [header, ...body] = rows.map(cells);
      blocks.push({ kind: 'table', header: header ?? [], rows: body });
      continue;
    }

    const heading = HEADING.exec(line);
    if (heading) {
      blocks.push({ kind: 'heading', content: inline(heading[1]) });
      continue;
    }

    const bullet = BULLET.exec(line);
    const numbered = bullet ? null : NUMBERED.exec(line);
    if (bullet || numbered) {
      const kind = bullet ? 'bullets' : 'numbered';
      const pattern = bullet ? BULLET : NUMBERED;
      const items: Inline[][] = [];
      while (i < lines.length && pattern.test(lines[i])) {
        items.push(inline(pattern.exec(lines[i])![1]));
        i++;
      }
      i--;
      blocks.push({ kind, items });
      continue;
    }

    const last = blocks.at(-1);
    if (last?.kind === 'paragraph' && lines[i - 1]?.trim()) {
      last.lines.push(inline(line.trim()));
    } else {
      blocks.push({ kind: 'paragraph', lines: [inline(line.trim())] });
    }
  }
  return blocks;
}

function cells(row: string): Inline[][] {
  return row
    .trim()
    .replace(/^\|/, '')
    .replace(/\|$/, '')
    .split('|')
    .map((cell) => inline(cell.trim()));
}

export function inline(text: string): Inline[] {
  const plain = text
    .replace(/\[([^\]]+)\]\((?:[^)]+)\)/g, '$1')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/<br\s*\/?>/gi, ' ');
  const parts: Inline[] = [];
  const bold = /\*\*(.+?)\*\*|__(.+?)__/g;
  let from = 0;
  for (let match = bold.exec(plain); match; match = bold.exec(plain)) {
    if (match.index > from) {
      parts.push({ text: plain.slice(from, match.index), bold: false });
    }
    parts.push({ text: match[1] ?? match[2], bold: true });
    from = match.index + match[0].length;
  }
  if (from < plain.length) {
    parts.push({ text: plain.slice(from), bold: false });
  }
  return parts;
}
