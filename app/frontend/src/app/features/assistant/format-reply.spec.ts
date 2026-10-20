import { formatReply, inline } from './format-reply';

describe('formatReply', () => {
  it('splits paragraphs on blank lines and keeps line breaks inside a paragraph', () => {
    expect(formatReply('First line\nsecond line\n\nNew paragraph')).toEqual([
      {
        kind: 'paragraph',
        lines: [[{ text: 'First line', bold: false }], [{ text: 'second line', bold: false }]],
      },
      { kind: 'paragraph', lines: [[{ text: 'New paragraph', bold: false }]] },
    ]);
  });

  it('turns dash, star and dot lines into a bullet list', () => {
    expect(formatReply('Highlights:\n- one\n* two\n• three')).toEqual([
      { kind: 'paragraph', lines: [[{ text: 'Highlights:', bold: false }]] },
      {
        kind: 'bullets',
        items: [
          [{ text: 'one', bold: false }],
          [{ text: 'two', bold: false }],
          [{ text: 'three', bold: false }],
        ],
      },
    ]);
  });

  it('turns numbered lines into a numbered list', () => {
    expect(formatReply('1. first\n2) second')).toEqual([
      {
        kind: 'numbered',
        items: [[{ text: 'first', bold: false }], [{ text: 'second', bold: false }]],
      },
    ]);
  });

  it('reads headings', () => {
    expect(formatReply('### MSFT today')).toEqual([
      { kind: 'heading', content: [{ text: 'MSFT today', bold: false }] },
    ]);
  });

  it('reads a table and skips its divider row', () => {
    const blocks = formatReply(
      '| Symbol | Change |\n|---|:---:|\n| AAPL | **-0.30%** |\n| MSFT | -2.06% |',
    );

    expect(blocks).toEqual([
      {
        kind: 'table',
        header: [[{ text: 'Symbol', bold: false }], [{ text: 'Change', bold: false }]],
        rows: [
          [[{ text: 'AAPL', bold: false }], [{ text: '-0.30%', bold: true }]],
          [[{ text: 'MSFT', bold: false }], [{ text: '-2.06%', bold: false }]],
        ],
      },
    ]);
  });

  it('copes with Windows line endings and empty input', () => {
    expect(formatReply('a\r\n\r\nb')).toHaveLength(2);
    expect(formatReply('')).toEqual([]);
  });
});

describe('inline', () => {
  it('marks bold text and drops other markdown symbols', () => {
    expect(inline('MSFT is **down 2.06%** today')).toEqual([
      { text: 'MSFT is ', bold: false },
      { text: 'down 2.06%', bold: true },
      { text: ' today', bold: false },
    ]);
    expect(inline('see `P/E` and [the article](https://example.com)')).toEqual([
      { text: 'see P/E and the article', bold: false },
    ]);
  });

  it('keeps HTML as plain text', () => {
    expect(inline('<img src=x onerror="alert(1)">')).toEqual([
      { text: '<img src=x onerror="alert(1)">', bold: false },
    ]);
  });
});
