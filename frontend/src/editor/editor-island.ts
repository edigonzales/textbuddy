import { Editor } from "@tiptap/core";
import StarterKit from "@tiptap/starter-kit";

import { TextCorrectionDecorationExtension } from "./correction-mark-extension";
import {
  findCorrectionElements,
  findDocumentImportElements,
  findEditorElements,
} from "./dom";
import { mountDocumentImport } from "./document-import";
import { dispatchTextChanged } from "./events";
import { countWords, getPlainText, plainTextToHtml } from "./plain-text";
import { mountQuickActions } from "./quick-actions";
import { mountTextCorrectionBridge } from "./text-correction";
import type { EditorElements, WorkspaceBusyChangedDetail } from "./types";
import { t } from "./ui-i18n";

// Temporary local demo content for manual UI testing. Remove this block when it is no longer needed.
const TEMPORARY_START_TEXT = `Um die steuerliche Wettbewerbsfähigkeit des Kantons zu verbessern, das Steuersubstrat gezielt zu stärken und die Finanzierung der staatlichen Aufgaben langfristig zu sichern, hat der Regierungsrat des Kantons Solothurn am 19. Mai 2026 die Steuerstrategie 2030 verabschiedet (RRB Nr. 2026/942 vom 19. Mai 2026). Grund dafür ist, dass der Kanton Solothurn bei der Besteuerung natürlicher Personen im interkantonalen Vergleich seit Jahren eine deutlich überdurchschnittliche Belastung aufweist, insbesondere bei mittleren und höheren Einkommen, und damit deutlich schlechter positioniert ist als der Schweizer Durchschnitt und die relevanten Vergleichskantone (AG, BL, LU). Im Gegensatz dazu besteuert der Kanton Solothurn die Vermögen vergleichsweise sehr tief; dieser Vorteil vermag die Nachteile bei der Einkommensbesteuerung jedoch nicht zu kompensieren. Damit besteht im Kanton Solothurn ein Ungleichgewicht zwischen der Besteuerung von Vermögen und der Besteuerung von Erwerbseinkommen – eine Fehlentwicklung, die sich während Jahrzehnten verstärkt hat und die der Regierungsrat nun korrigieren will. Eine Steuerpolitik, die Erwerbseinkommen übermässig stark belastet, Vermögen und Liegenschaften dagegen vergleichsweise zu tief erfasst, ist weder effizient noch standortstrategisch überzeugend. Gleichzeitig ist der finanzpolitische Handlungsspielraum des Kantons begrenzt.\nVor diesem Hintergrund sieht die Steuerstrategie eine gezielte Systemkorrektur bei der Besteuerung natürlicher Personen vor. Kernstück ist eine substanzielle Senkung des Einkommenssteuertarifs, ergänzt durch die Abschaffung der Personalsteuer. Ermöglicht wird diese Entlastung bei der Einkommenssteuer durch Gegenfinanzierungsmassnahmen. Diese umfassen insbesondere die sachgerechte Revision der Katasterschätzung, eine moderate Erhöhung der Vermögensbesteuerung sowie eine angepasste Ausgestaltung der Grundstückgewinnsteuer. Bei den juristischen Personen ist der Kanton Solothurn im interkantonalen Vergleich bereits wettbewerbsfähig, weshalb dort keine tarifliche Entlastung vorgesehen ist.\nDie zur Umsetzung empfohlenen Massnahmen der Steuerstrategie 2030 bilden ein austariertes Gesamtpaket: Ihre Ziele lassen sich nur erreichen, wenn die vorgesehenen Massnahmen in ihrer Gesamtheit umgesetzt werden. Die Steuerstrategie dient als Grundlage für die politische Diskussion sowie für die nachfolgenden Entscheidungen und die entsprechenden konkreten Gesetzesrevisionen und wird dem Kantonsrat daher hiermit zur Kenntnis gebracht.`;

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

  dispatchTextChanged(elements.root, {
    text,
    characters: text.length,
    words: wordCount,
  });
}

export function mountEditorIsland(): void {
  const elements = findEditorElements();
  const correctionElements = findCorrectionElements();

  if (!elements) {
    return;
  }

  const documentImportElements = findDocumentImportElements(elements.root);
  let workspaceBusy = false;
  const initialText = elements.mirror.value.trim() ? elements.mirror.value : TEMPORARY_START_TEXT;

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
    content: plainTextToHtml(initialText),
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

  mountQuickActions(editor, elements.root);
}
