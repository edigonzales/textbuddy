import type { Editor } from "@tiptap/core";

import { plainTextToHtml } from "./plain-text";

interface SetEditorContentOptions {
  emitUpdate?: boolean;
  addToHistory?: boolean;
}

function applyEditorContent(
  editor: Editor,
  content: string,
  options: SetEditorContentOptions = {},
): void {
  const chain = editor.chain();

  if (options.addToHistory === false) {
    chain.setMeta("addToHistory", false);
  }

  chain.setContent(content, {
    emitUpdate: options.emitUpdate ?? true,
  }).run();
}

function sanitizeImportedHtml(html: string): Document {
  const parsedDocument = new DOMParser().parseFromString(html, "text/html");

  parsedDocument
    .querySelectorAll("script,style,iframe,object,embed,link,meta")
    .forEach((element) => element.remove());

  parsedDocument.querySelectorAll<HTMLElement>("*").forEach((element) => {
    Array.from(element.attributes).forEach((attribute) => {
      const name = attribute.name.toLowerCase();
      const value = attribute.value.trim().toLowerCase();

      if (name.startsWith("on") || name === "srcdoc") {
        element.removeAttribute(attribute.name);
        return;
      }

      if ((name === "href" || name === "src") && value.startsWith("javascript:")) {
        element.removeAttribute(attribute.name);
      }
    });
  });

  return parsedDocument;
}

export function setEditorPlainText(
  editor: Editor,
  text: string,
  options: SetEditorContentOptions = {},
): void {
  applyEditorContent(editor, plainTextToHtml(text), options);
}

export function importedHtmlToPlainText(html: string): string {
  const parsedDocument = sanitizeImportedHtml(html);
  const blocks = parsedDocument.body.querySelectorAll(
    "address,article,aside,blockquote,div,dl,dt,dd,figcaption,figure,footer,form,h1,h2,h3,h4,h5,h6,header,hr,li,main,nav,ol,p,pre,section,table,tr,ul",
  );

  parsedDocument.body.querySelectorAll("br").forEach((element) => element.replaceWith("\n"));
  blocks.forEach((element) => element.append("\n"));

  return (parsedDocument.body.textContent ?? "")
    .replace(/\u00a0/g, " ")
    .replace(/[ \t]+\n/g, "\n")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}
