package app.textbuddy.integration.docling;

import app.textbuddy.document.DocumentUpload;
import dev.kreuzberg.Kreuzberg;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;

class KreuzbergDoclingClientTest {

    private final KreuzbergDoclingClient client = new KreuzbergDoclingClient(45);

    @ParameterizedTest
    @MethodSource("supportedUploads")
    void convertsSupportedFormatsToHtml(DocumentUpload upload, String expectedFragment) {
        assertThat(client.convertToHtml(upload, "de")).contains(expectedFragment);
    }

    @Test
    void convertsScannedImageWithLocalOcrWhenRuntimeProvidesOcrBackend() throws IOException {
        Assumptions.assumeTrue(isOcrRuntimeAvailable(), "Keine lokale OCR-Engine verfügbar.");
        Assumptions.assumeTrue(isOcrRuntimeReady("en"), "Lokale OCR ist vorhanden, aber nicht betriebsbereit.");

        byte[] image = createScannedPng("HELLO OCR");
        String html = client.convertToHtml(new DocumentUpload("scan.png", "image/png", image), "en");
        String normalized = normalizeLetters(html);

        assertThat(normalized).contains("hello");
    }

    @Test
    void convertsScannedPdfWithLocalOcrWhenRuntimeProvidesOcrBackend() throws IOException {
        Assumptions.assumeTrue(isOcrRuntimeAvailable(), "Keine lokale OCR-Engine verfügbar.");
        Assumptions.assumeTrue(isOcrRuntimeReady("de"), "Lokale OCR ist vorhanden, aber nicht betriebsbereit.");

        byte[] scannedPdf = createScannedPdf("OCR PDF");
        String html = client.convertToHtml(new DocumentUpload("scan.pdf", "application/pdf", scannedPdf), "de");
        String normalized = normalizeLetters(html);

        assertThat(normalized).contains("ocr");
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

    private static boolean isOcrRuntimeAvailable() {
        try {
            return !Kreuzberg.listOCRBackends().isEmpty();
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean isOcrRuntimeReady(String language) {
        try {
            byte[] image = createScannedPng("OCR READY");
            String html = client.convertToHtml(
                    new DocumentUpload("ocr-health.png", "image/png", image),
                    language
            );
            return !normalizeLetters(html).isBlank();
        } catch (Exception exception) {
            return false;
        }
    }

    private static byte[] createScannedPng(String text) throws IOException {
        BufferedImage image = new BufferedImage(720, 180, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 72));
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.drawString(text, 24, 120);
        graphics.dispose();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static byte[] createScannedPdf(String text) throws IOException {
        BufferedImage image = new BufferedImage(900, 220, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 84));
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.drawString(text, 40, 150);
        graphics.dispose();

        byte[] rgb = toRgbBytes(image);
        byte[] compressed = compress(rgb);
        String imageStream = "q\n900 0 0 220 0 0 cm\n/Im0 Do\nQ\n";

        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        pdf.write(bytes("%PDF-1.4\n"));
        int[] offsets = new int[5];

        offsets[0] = pdf.size();
        pdf.write(bytes("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n"));

        offsets[1] = pdf.size();
        pdf.write(bytes("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n"));

        offsets[2] = pdf.size();
        pdf.write(bytes("""
                3 0 obj
                << /Type /Page /Parent 2 0 R /MediaBox [0 0 900 220]
                   /Resources << /XObject << /Im0 4 0 R >> >>
                   /Contents 5 0 R >>
                endobj
                """));

        offsets[3] = pdf.size();
        pdf.write(bytes("""
                4 0 obj
                << /Type /XObject /Subtype /Image /Width 900 /Height 220
                   /ColorSpace /DeviceRGB /BitsPerComponent 8
                   /Filter /FlateDecode /Length %d >>
                stream
                """.formatted(compressed.length)));
        pdf.write(compressed);
        pdf.write(bytes("\nendstream\nendobj\n"));

        offsets[4] = pdf.size();
        pdf.write(bytes("""
                5 0 obj
                << /Length %d >>
                stream
                %s
                endstream
                endobj
                """.formatted(
                imageStream.getBytes(StandardCharsets.UTF_8).length,
                imageStream
        )));

        int xrefOffset = pdf.size();
        pdf.write(bytes("xref\n0 6\n"));
        pdf.write(bytes("0000000000 65535 f \n"));

        for (int offset : offsets) {
            pdf.write(bytes(String.format("%010d 00000 n %n", offset)));
        }

        pdf.write(bytes("""
                trailer
                << /Size 6 /Root 1 0 R >>
                startxref
                %d
                %%EOF
                """.formatted(xrefOffset)));

        return pdf.toByteArray();
    }

    private static byte[] toRgbBytes(BufferedImage image) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(image.getWidth() * image.getHeight() * 3);

        for (int y = 0; y < image.getHeight(); y += 1) {
            for (int x = 0; x < image.getWidth(); x += 1) {
                int rgb = image.getRGB(x, y);
                output.write((rgb >> 16) & 0xFF);
                output.write((rgb >> 8) & 0xFF);
                output.write(rgb & 0xFF);
            }
        }

        return output.toByteArray();
    }

    private static byte[] compress(byte[] value) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (DeflaterOutputStream deflater = new DeflaterOutputStream(output)) {
            deflater.write(value);
        }

        return output.toByteArray();
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String normalizeLetters(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
    }
}
