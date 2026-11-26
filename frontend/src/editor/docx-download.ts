export async function createDocxBlob(text: string): Promise<Blob> {
  const { Document, Packer, Paragraph, TextRun } = await import("docx");
  const paragraphs = text.split(/\r\n?|\n/).map((line) =>
    new Paragraph({ children: line ? [new TextRun(line)] : [] }),
  );
  return Packer.toBlob(new Document({ sections: [{ children: paragraphs }] }));
}

export function textbuddyDocxFilename(date = new Date()): string {
  const year = String(date.getFullYear()).padStart(4, "0");
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `textbuddy-${year}-${month}-${day}.docx`;
}
