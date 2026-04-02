import { Extension } from "@tiptap/core";
import type { Editor } from "@tiptap/core";
import type { Node as ProseMirrorNode } from "@tiptap/pm/model";
import { Plugin, PluginKey } from "@tiptap/pm/state";
import { Decoration, DecorationSet } from "@tiptap/pm/view";

import type { CorrectionRange } from "./types";

const TEXT_CORRECTION_TEST_ID = "correction-mark";

interface DocumentRange {
  from: number;
  to: number;
}

interface TextBlockBoundary {
  start: number;
  textLength: number;
}

export const textCorrectionPluginKey = new PluginKey<DecorationSet>(
  "textbuddyTextCorrections",
);

function collectTextBlocks(doc: ProseMirrorNode): TextBlockBoundary[] {
  const blocks: TextBlockBoundary[] = [];

  doc.descendants((node, pos) => {
    if (!node.isTextblock) {
      return true;
    }

    blocks.push({
      start: pos + 1,
      textLength: node.textContent.length,
    });

    return false;
  });

  return blocks;
}

function buildBoundaryIndex(doc: ProseMirrorNode): number[] {
  const blocks = collectTextBlocks(doc);

  if (blocks.length === 0) {
    return [0];
  }

  const plainTextLength = blocks.reduce(
    (sum, block, index) => sum + block.textLength + (index < blocks.length - 1 ? 1 : 0),
    0,
  );
  const boundaries = new Array<number>(plainTextLength + 1);
  let plainOffset = 0;

  blocks.forEach((block, index) => {
    for (let offset = 0; offset <= block.textLength; offset += 1) {
      boundaries[plainOffset + offset] = block.start + offset;
    }

    plainOffset += block.textLength;

    if (index < blocks.length - 1) {
      plainOffset += 1;
    }
  });

  return boundaries;
}

export function plainTextRangeToDocumentRange(
  doc: ProseMirrorNode,
  offset: number,
  length: number,
): DocumentRange | null {
  if (offset < 0 || length <= 0) {
    return null;
  }

  const boundaries = buildBoundaryIndex(doc);
  const endOffset = offset + length;

  if (endOffset >= boundaries.length) {
    return null;
  }

  const from = boundaries[offset];
  const to = boundaries[endOffset];

  if (typeof from !== "number" || typeof to !== "number" || from === to) {
    return null;
  }

  return { from, to };
}

function buildDecorationSet(
  doc: ProseMirrorNode,
  corrections: readonly CorrectionRange[],
): DecorationSet {
  const decorations = corrections
    .map((correction, index) => {
      const range = plainTextRangeToDocumentRange(doc, correction.offset, correction.length);

      if (!range) {
        return null;
      }

      return Decoration.inline(range.from, range.to, {
        class: "textbuddy-correction-mark",
        "data-testid": TEXT_CORRECTION_TEST_ID,
        "data-correction-index": String(index),
      });
    })
    .filter((decoration): decoration is Decoration => decoration !== null);

  return DecorationSet.create(doc, decorations);
}

export const TextCorrectionDecorationExtension = Extension.create({
  name: "textCorrectionDecoration",

  addProseMirrorPlugins() {
    return [
      new Plugin({
        key: textCorrectionPluginKey,
        state: {
          init: (_, state) => DecorationSet.create(state.doc, []),
          apply: (transaction, decorationSet) => {
            const nextDecorationSet = decorationSet.map(transaction.mapping, transaction.doc);
            const corrections =
              transaction.getMeta(textCorrectionPluginKey) as CorrectionRange[] | undefined;

            if (!corrections) {
              return nextDecorationSet;
            }

            if (corrections.length === 0) {
              return DecorationSet.create(transaction.doc, []);
            }

            return buildDecorationSet(transaction.doc, corrections);
          },
        },
        props: {
          decorations(state) {
            return textCorrectionPluginKey.getState(state) ?? null;
          },
        },
      }),
    ];
  },
});

export function setTextCorrections(editor: Editor, corrections: readonly CorrectionRange[]): void {
  editor.view.dispatch(editor.state.tr.setMeta(textCorrectionPluginKey, [...corrections]));
}
