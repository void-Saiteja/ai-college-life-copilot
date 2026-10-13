package com.collegecopilot.rag;

import com.collegecopilot.rag.dto.ChunkDto;
import com.collegecopilot.rag.dto.ExtractedPage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class PdfExtractionService {

    private static final Logger log = LoggerFactory.getLogger(PdfExtractionService.class);

    public List<ExtractedPage> extractPagesFromPDF(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Invalid PDF file buffer provided");
        }

        List<ExtractedPage> pages = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = document.getNumberOfPages();

            for (int p = 1; p <= totalPages; p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                String pageText = stripper.getText(document);
                String clean = cleanTextContent(pageText);
                if (!clean.isBlank()) {
                    pages.add(new ExtractedPage(p, clean));
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse PDF document: {}", e.getMessage());
            throw new IOException("Failed to parse PDF document: " + e.getMessage(), e);
        }

        pages.sort(Comparator.comparingInt(ExtractedPage::getPageNumber));
        return pages;
    }

    public String cleanTextContent(String text) {
        if (text == null) return "";
        return text
                .replace("\r\n", "\n")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    public List<ChunkDto> generateOverlappingChunks(List<ExtractedPage> pages, String docId, String docTitle) {
        return generateOverlappingChunks(pages, docId, docTitle, 650, 120);
    }

    public List<ChunkDto> generateOverlappingChunks(List<ExtractedPage> pages, String docId, String docTitle,
                                                   int targetChunkSize, int overlapSize) {
        List<ChunkDto> chunks = new ArrayList<>();
        int globalChunkIndex = 0;
        long timestamp = System.currentTimeMillis();

        if (pages == null) return chunks;

        for (ExtractedPage pageObj : pages) {
            int pageNumber = pageObj.getPageNumber();
            String text = pageObj.getText();

            if (text == null || text.trim().isEmpty()) continue;

            // Split on sentence terminators (. ? !) or line breaks
            String[] rawSentences = text.split("(?<=[.?!])\\s+|\\n+");
            List<String> sentences = new ArrayList<>();
            for (String s : rawSentences) {
                if (s != null && !s.trim().isEmpty()) {
                    sentences.add(s.trim());
                }
            }

            String currentChunk = "";

            for (String sentence : sentences) {
                if (currentChunk.isEmpty()) {
                    currentChunk = sentence;
                } else if (currentChunk.length() + sentence.length() + 1 <= targetChunkSize) {
                    currentChunk += " " + sentence;
                } else {
                    // Push completed chunk with exact page number metadata
                    chunks.add(new ChunkDto(
                            "chk-" + timestamp + "-" + globalChunkIndex,
                            docId,
                            docTitle,
                            pageNumber,
                            globalChunkIndex,
                            currentChunk.trim(),
                            null,
                            "gemini-embedding-2",
                            768,
                            "v1",
                            0.0
                    ));
                    globalChunkIndex++;

                    // Calculate overlap from tail of currentChunk
                    String[] words = currentChunk.split(" ");
                    StringBuilder overlapText = new StringBuilder();
                    for (int j = words.length - 1; j >= 0; j--) {
                        if ((words[j] + " " + overlapText).length() <= overlapSize) {
                            if (overlapText.length() > 0) {
                                overlapText.insert(0, words[j] + " ");
                            } else {
                                overlapText.append(words[j]);
                            }
                        } else {
                            break;
                        }
                    }

                    currentChunk = (overlapText.toString().trim() + " " + sentence).trim();
                }
            }

            // Push final chunk for the page
            if (!currentChunk.trim().isEmpty()) {
                chunks.add(new ChunkDto(
                        "chk-" + timestamp + "-" + globalChunkIndex,
                        docId,
                        docTitle,
                        pageNumber,
                        globalChunkIndex,
                        currentChunk.trim(),
                        null,
                        "gemini-embedding-2",
                        768,
                        "v1",
                        0.0
                ));
                globalChunkIndex++;
            }
        }

        return chunks;
    }
}
