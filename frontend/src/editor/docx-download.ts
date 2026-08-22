export async function createDocxBlob(text: string): Promise<Blob> {
  const { convertMarkdownToDocx } = await import("@mohtasham/md-to-docx");
  return convertMarkdownToDocx(text);
}

export function textbuddyDocxFilename(date = new Date()): string {
  return `textbuddy-${date.toISOString().slice(0, 10)}.docx`;
}
