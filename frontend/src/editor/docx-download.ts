export async function createDocxBlob(text: string): Promise<Blob> {
  const { convertMarkdownToDocx } = await import("@mohtasham/md-to-docx");
  return convertMarkdownToDocx(literalTextToMarkdown(text));
}

export function textbuddyDocxFilename(date = new Date()): string {
  const year = String(date.getFullYear()).padStart(4, "0");
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `textbuddy-${year}-${month}-${day}.docx`;
}

export function literalTextToMarkdown(text: string): string {
  return text
    .replace(/\\/g, "\\\\")
    .replace(/([`*_[\]{}<>#+\-.!|()])/g, "\\$1")
    .split(/\r?\n/u)
    .map((line, index, lines) => index < lines.length - 1 ? `${line}  ` : line)
    .join("\n");
}
