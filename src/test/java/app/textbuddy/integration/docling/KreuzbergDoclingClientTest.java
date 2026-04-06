package app.textbuddy.integration.docling;

import app.textbuddy.document.DocumentUpload;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class KreuzbergDoclingClientTest {

    private final KreuzbergDoclingClient client = new KreuzbergDoclingClient();

    @ParameterizedTest
    @MethodSource("supportedUploads")
    void convertsSupportedFormatsToHtml(DocumentUpload upload, String expectedFragment) {
        assertThat(client.convertToHtml(upload)).contains(expectedFragment);
    }

    private static List<Object[]> supportedUploads() throws IOException {
        return List.of(
                new Object[]{
                        new DocumentUpload(
                                "import.pdf",
                                "application/pdf",
                                createPdf("PDF Import Titel")
                        ),
                        "PDF Import Titel"
                },
                new Object[]{
                        new DocumentUpload(
                                "import.docx",
                                "application/octet-stream",
                                createDocx("DOCX Import Titel", "Zweite Zeile")
                        ),
                        "DOCX Import Titel"
                },
                new Object[]{
                        new DocumentUpload(
                                "import.md",
                                "text/markdown",
                                "# Import Titel\n\nErste Zeile".getBytes(StandardCharsets.UTF_8)
                        ),
                        "Import Titel"
                },
                new Object[]{
                        new DocumentUpload(
                                "import.txt",
                                "text/plain",
                                "Erste Zeile\nZweite Zeile".getBytes(StandardCharsets.UTF_8)
                        ),
                        "Erste Zeile"
                }
        );
    }

    private static byte[] createDocx(String firstParagraph, String secondParagraph) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            writeZipEntry(zipOutputStream, "[Content_Types].xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="xml" ContentType="application/xml"/>
                      <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                    </Types>
                    """);
            writeZipEntry(zipOutputStream, "_rels/.rels", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                    </Relationships>
                    """);
            writeZipEntry(zipOutputStream, "word/document.xml", """
                    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                      <w:body>
                        <w:p><w:r><w:t>%s</w:t></w:r></w:p>
                        <w:p><w:r><w:t>%s</w:t></w:r></w:p>
                      </w:body>
                    </w:document>
                    """.formatted(escapeXml(firstParagraph), escapeXml(secondParagraph)));
        }

        return outputStream.toByteArray();
    }

    private static void writeZipEntry(ZipOutputStream zipOutputStream, String path, String content) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(path));
        zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();
    }

    private static byte[] createPdf(String text) {
        String stream = """
                BT
                /F1 18 Tf
                72 720 Td
                (%s) Tj
                ET
                """.formatted(escapePdf(text)).strip();

        List<String> objects = List.of(
                "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n",
                "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n",
                "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n",
                "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n",
                "5 0 obj\n<< /Length %d >>\nstream\n%s\nendstream\nendobj\n".formatted(
                        stream.getBytes(StandardCharsets.UTF_8).length,
                        stream
                )
        );

        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        int[] offsets = new int[objects.size()];

        for (int index = 0; index < objects.size(); index += 1) {
            offsets[index] = pdf.toString().getBytes(StandardCharsets.UTF_8).length;
            pdf.append(objects.get(index));
        }

        int xrefOffset = pdf.toString().getBytes(StandardCharsets.UTF_8).length;
        pdf.append("xref\n0 ").append(objects.size() + 1).append("\n");
        pdf.append("0000000000 65535 f \n");

        for (int offset : offsets) {
            pdf.append(String.format("%010d 00000 n %n", offset));
        }

        pdf.append("""
                trailer
                << /Size %d /Root 1 0 R >>
                startxref
                %d
                %%EOF
                """.formatted(objects.size() + 1, xrefOffset));

        return pdf.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String escapePdf(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private static String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
