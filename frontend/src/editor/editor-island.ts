import { Editor } from "@tiptap/core";
import StarterKit from "@tiptap/starter-kit";

import { TextCorrectionDecorationExtension } from "./correction-mark-extension";
import {
  findCorrectionElements,
  findDocumentImportElements,
  findEditorElements,
  findQuickActionElements,
  findRewriteBubbleElements,
} from "./dom";
import { mountDocumentImport } from "./document-import";
import { dispatchSelectionChanged, dispatchTextChanged } from "./events";
import { countWords, getPlainText, plainTextToHtml } from "./plain-text";
import { mountQuickActionStream } from "./quick-action-stream";
import { mountRewriteBubble } from "./rewrite-bubble";
import { mountTextCorrectionBridge } from "./text-correction";
import type { EditorElements } from "./types";

function syncUndoRedoState(elements: EditorElements, editor: Editor): void {
  elements.undoButton.disabled = !editor.can().chain().focus().undo().run();
  elements.redoButton.disabled = !editor.can().chain().focus().redo().run();
}

function syncTextState(elements: EditorElements, editor: Editor): void {
  const text = getPlainText(editor);
  const wordCount = countWords(text);

  elements.mirror.value = text;
  elements.characterCount.textContent = String(text.length);
  elements.wordCount.textContent = String(wordCount);
  elements.surface.dataset.editorEmpty = text.length === 0 ? "true" : "false";

  dispatchTextChanged(elements.root, {
    text,
    characters: text.length,
    words: wordCount,
  });
}

function syncSelectionState(elements: EditorElements, editor: Editor): void {
  const { empty, from, to } = editor.state.selection;

  dispatchSelectionChanged(elements.root, {
    from,
    to,
    empty,
  });
}

export function mountEditorIsland(): void {
  const elements = findEditorElements();
  const correctionElements = findCorrectionElements();

  if (!elements) {
    return;
  }

  const documentImportElements = findDocumentImportElements(elements.root);
  const rewriteBubbleElements = findRewriteBubbleElements(elements.root);
  const quickActionElements = findQuickActionElements(elements.root);

  const editor = new Editor({
    element: elements.surface,
    extensions: [
      StarterKit.configure({
        code: false,
        codeBlock: false,
        dropcursor: false,
        gapcursor: false,
        link: false,
        underline: false,
      }),
      TextCorrectionDecorationExtension,
    ],
    content: plainTextToHtml(elements.mirror.value),
    editorProps: {
      attributes: {
        "aria-label": "Textbuddy Editor",
        "data-testid": "editor-input",
        class: "editor-input",
        spellcheck: "false",
      },
    },
    onCreate: ({ editor: activeEditor }) => {
      syncTextState(elements, activeEditor);
      syncSelectionState(elements, activeEditor);
      syncUndoRedoState(elements, activeEditor);
    },
    onUpdate: ({ editor: activeEditor }) => {
      syncTextState(elements, activeEditor);
      syncUndoRedoState(elements, activeEditor);
    },
    onSelectionUpdate: ({ editor: activeEditor }) => {
      syncSelectionState(elements, activeEditor);
      syncUndoRedoState(elements, activeEditor);
    },
    onTransaction: ({ editor: activeEditor }) => {
      syncUndoRedoState(elements, activeEditor);
    },
  });

  elements.undoButton.addEventListener("click", () => {
    editor.chain().focus().undo().run();
  });

  elements.redoButton.addEventListener("click", () => {
    editor.chain().focus().redo().run();
  });

  if (documentImportElements) {
    mountDocumentImport(editor, elements.root, documentImportElements);
  }

  if (correctionElements) {
    mountTextCorrectionBridge(editor, elements.root, correctionElements);
  }

  if (rewriteBubbleElements) {
    mountRewriteBubble(editor, elements.root, elements, rewriteBubbleElements);
  }

  if (quickActionElements) {
    mountQuickActionStream(editor, elements.root, quickActionElements);
  }
}
