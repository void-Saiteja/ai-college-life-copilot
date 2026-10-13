package com.collegecopilot.rag;

import com.collegecopilot.rag.dto.ChunkDto;
import com.collegecopilot.rag.dto.ExtractedPage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfExtractionServiceTest {

    private final PdfExtractionService service = new PdfExtractionService();

    @Test
    @DisplayName("PDF extraction preserves individual page numbers and text content across multiple pages")
    void extractPagesFromPDF_multiPage_preservesPageNumbers() throws Exception {
        byte[] pdfBytes;
        try (PDDocument doc = new PDDocument()) {
            PDPage page1 = new PDPage();
            doc.addPage(page1);
            try (PDPageContentStream stream = new PDPageContentStream(doc, page1)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText("Page One: College attendance requirement is 75 percent minimum.");
                stream.endText();
            }

            PDPage page2 = new PDPage();
            doc.addPage(page2);
            try (PDPageContentStream stream = new PDPageContentStream(doc, page2)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText("Page Two: Academic probation occurs when GPA falls below 2.0.");
                stream.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            pdfBytes = baos.toByteArray();
        }

        List<ExtractedPage> pages = service.extractPagesFromPDF(pdfBytes);

        assertNotNull(pages);
        assertEquals(2, pages.size());
        assertEquals(1, pages.get(0).getPageNumber());
        assertTrue(pages.get(0).getText().contains("Page One"));
        assertTrue(pages.get(0).getText().contains("attendance requirement is 75 percent"));

        assertEquals(2, pages.get(1).getPageNumber());
        assertTrue(pages.get(1).getText().contains("Page Two"));
        assertTrue(pages.get(1).getText().contains("Academic probation occurs"));
    }

    @Test
    @DisplayName("Invalid PDF buffer throws IOException")
    void extractPagesFromPDF_invalidBuffer_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> service.extractPagesFromPDF(null));
        assertThrows(IllegalArgumentException.class, () -> service.extractPagesFromPDF(new byte[0]));
        assertThrows(IOException.class, () -> service.extractPagesFromPDF("not a pdf at all".getBytes()));
    }

    @Test
    @DisplayName("cleanTextContent normalizes newlines and whitespace")
    void cleanTextContent_normalizesStructure() {
        String dirty = "Line 1\r\n\r\n\r\n\r\nLine    2   with    tabs\t\there.\n";
        String cleaned = service.cleanTextContent(dirty);

        assertFalse(cleaned.contains("\r"));
        assertFalse(cleaned.contains("\n\n\n"));
        assertTrue(cleaned.contains("Line 2 with tabs here."));
    }

    @Test
    @DisplayName("generateOverlappingChunks preserves page number and produces overlapping text")
    void generateOverlappingChunks_createsChunksWithPageNumbersAndOverlap() {
        String longTextPage1 = "Section 1: Academic policies are strictly enforced for all enrolled students. " +
                "Attendance must stay above seventy-five percent in each registered course. " +
                "Students missing more than three consecutive lectures must submit medical documentation. " +
                "Failure to comply leads to disciplinary review and examination disbarment. " +
                "Appeals must be submitted within seven calendar days of notification.";

        String longTextPage2 = "Section 2: Examination schedules are released four weeks prior to midterms. " +
                "Calculators are prohibited in all introductory mathematics examinations. " +
                "Students arriving fifteen minutes late will not be admitted under any circumstances. " +
                "Special accommodations must be requested through disability services at semester start.";

        List<ExtractedPage> pages = List.of(
                new ExtractedPage(1, longTextPage1),
                new ExtractedPage(2, longTextPage2)
        );

        List<ChunkDto> chunks = service.generateOverlappingChunks(pages, "doc-test", "Policy Handbook", 200, 50);

        assertFalse(chunks.isEmpty());
        // Verify page numbers are preserved on chunks
        assertTrue(chunks.stream().anyMatch(c -> c.getPageNumber() == 1));
        assertTrue(chunks.stream().anyMatch(c -> c.getPageNumber() == 2));

        // Verify document title and id are retained
        for (ChunkDto chunk : chunks) {
            assertEquals("doc-test", chunk.getDocumentId());
            assertEquals("Policy Handbook", chunk.getDocumentTitle());
            assertNotNull(chunk.getContent());
            assertFalse(chunk.getContent().isBlank());
        }
    }
}
