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

export const textCorrectionPluginKey = new PluginKey<DecorationSet>(
  "textbuddyTextCorrections",
);

function buildBoundaryIndex(doc: ProseMirrorNode): number[] {
  const boundaries: number[] = [];
  doc.descendants((node, pos) => {
    if (!node.isTextblock) return true;
    // Text and hard breaks each occupy the same number of units in both models.
    for (let offset = 0; offset <= node.content.size; offset += 1) {
      boundaries.push(pos + 1 + offset);
    }
    return false;
  });
  return boundaries;
}

function documentRange(
  boundaries: readonly number[],
  offset: number,
  length: number,
): DocumentRange | null {
  const from = boundaries[offset];
  const to = boundaries[offset + length];
  return length >= 0 && from !== undefined && to !== undefined ? { from, to } : null;
}

export function plainTextRangeToDocumentRange(
  doc: ProseMirrorNode,
  offset: number,
  length: number,
): DocumentRange | null {
  return documentRange(buildBoundaryIndex(doc), offset, length);
}

function buildDecorationSet(
  doc: ProseMirrorNode,
  corrections: readonly CorrectionRange[],
): DecorationSet {
  const boundaries = buildBoundaryIndex(doc);
  const decorations = corrections
    .map((correction, index) => {
      const range = documentRange(boundaries, correction.offset, correction.length);

      if (!range) {
        return null;
      }

      if (range.from === range.to) {
        return Decoration.widget(range.from, () => {
          const marker = document.createElement("span");
          marker.className = "textbuddy-correction-insertion";
          marker.dataset.testid = TEXT_CORRECTION_TEST_ID;
          marker.dataset.correctionIndex = String(index);
          return marker;
        });
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
