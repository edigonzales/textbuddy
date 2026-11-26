import { Editor } from "@tiptap/core";
import StarterKit from "@tiptap/starter-kit";

import { mountAdvisor } from "./advisor";
import { TextCorrectionDecorationExtension } from "./correction-mark-extension";
import {
  findCorrectionElements,
  findDocumentImportElements,
  findEditorElements,
} from "./dom";
import { mountDocumentImport } from "./document-import";
import { dispatchTextChanged } from "./events";
import { countWords, getPlainText, plainTextDocument } from "./plain-text";
import { mountQuickActions } from "./quick-actions";
import { createReviewController } from "./review-controller";
import { mountTextCorrectionBridge } from "./text-correction";
import type { EditorElements, WorkspaceBusyChangedDetail } from "./types";
import { t } from "./ui-i18n";

function syncUndoRedoState(elements: EditorElements, editor: Editor, busy = false): void {
  elements.undoButton.disabled = busy || !editor.can().chain().focus().undo().run();
  elements.redoButton.disabled = busy || !editor.can().chain().focus().redo().run();
}

function syncTextState(elements: EditorElements, editor: Editor): void {
  const text = getPlainText(editor);
  const wordCount = countWords(text);

  elements.mirror.value = text;
  elements.characterCount.textContent = String(text.length);
  elements.wordCount.textContent = String(wordCount);
  elements.surface.dataset.editorEmpty = text.length === 0 ? "true" : "false";

  dispatchTextChanged(elements.root, { text });
}

export function mountEditorIsland(): void {
  const elements = findEditorElements();
  const correctionElements = findCorrectionElements();

  if (!elements) {
    return;
  }

  const documentImportElements = findDocumentImportElements(elements.root);
  let workspaceBusy = false;

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
    content: plainTextDocument(elements.mirror.value),
    editorProps: {
      attributes: {
        "aria-label": t("editor.ariaLabel"),
        role: "textbox",
        "aria-multiline": "true",
        "data-testid": "editor-input",
        class: "editor-input",
        spellcheck: "false",
      },
    },
    onCreate: ({ editor: activeEditor }) => {
      syncTextState(elements, activeEditor);
      syncUndoRedoState(elements, activeEditor, workspaceBusy);
    },
    onUpdate: ({ editor: activeEditor }) => {
      syncTextState(elements, activeEditor);
      syncUndoRedoState(elements, activeEditor, workspaceBusy);
    },
    onTransaction: ({ editor: activeEditor }) => {
      syncUndoRedoState(elements, activeEditor, workspaceBusy);
    },
  });

  elements.root.addEventListener("workspace:busy-changed", (event) => {
    workspaceBusy = (event as CustomEvent<WorkspaceBusyChangedDetail>).detail.busy;
    editor.setEditable(!workspaceBusy);
    syncUndoRedoState(elements, editor, workspaceBusy);
  });

  elements.undoButton.addEventListener("click", () => {
    if (workspaceBusy) {
      return;
    }
    editor.chain().focus().undo().run();
  });

  elements.redoButton.addEventListener("click", () => {
    if (workspaceBusy) {
      return;
    }
    editor.chain().focus().redo().run();
  });

  if (documentImportElements) {
    mountDocumentImport(editor, elements.root, documentImportElements);
  }

  if (correctionElements) {
    mountTextCorrectionBridge(editor, elements.root, correctionElements);
  }

  const reviewController = createReviewController(editor, elements.root);
  mountQuickActions(editor, elements.root, reviewController);
  mountAdvisor(editor, elements.root, reviewController);
}
