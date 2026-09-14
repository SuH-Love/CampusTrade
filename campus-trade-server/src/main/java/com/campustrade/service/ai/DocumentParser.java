package com.campustrade.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class DocumentParser {

    public static class ParsedPage {
        public String text;
        public int pageNum;
        public List<String> imageUrls;

        public ParsedPage(String text, int pageNum) {
            this.text = text;
            this.pageNum = pageNum;
            this.imageUrls = new ArrayList<>();
        }
    }

    public static class ParsedDocument {
        public List<ParsedPage> pages = new ArrayList<>();
        public String title;
        public String fileType;
    }

    public ParsedDocument parse(byte[] content, String fileName) {
        ParsedDocument result = new ParsedDocument();
        String lowerName = fileName.toLowerCase();
        result.title = fileName;

        if (lowerName.endsWith(".pdf")) {
            result.fileType = "pdf";
            parsePdf(content, result);
        } else if (lowerName.endsWith(".docx")) {
            result.fileType = "word";
            parseDocx(content, result);
        } else if (lowerName.endsWith(".doc")) {
            result.fileType = "word";
            log.warn("Legacy .doc format not supported, please convert to .docx: {}", fileName);
            result.pages.add(new ParsedPage("[不支持的文档格式，请转换为.docx]", 1));
        } else if (lowerName.endsWith(".txt") || lowerName.endsWith(".md")) {
            result.fileType = "text";
            parseText(content, result);
        } else if (isImageFile(lowerName)) {
            result.fileType = "image";
            result.pages.add(new ParsedPage("[图片文件，需通过多模态模型识别]", 1));
        } else {
            result.fileType = "text";
            parseText(content, result);
        }
        return result;
    }

    private void parsePdf(byte[] content, ParsedDocument result) {
        try (PDDocument document = PDDocument.load(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            int pageCount = document.getNumberOfPages();
            for (int i = 1; i <= pageCount; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String text = stripper.getText(document);
                if (text != null && !text.trim().isEmpty()) {
                    result.pages.add(new ParsedPage(text.trim(), i));
                }
            }
            log.info("Parsed PDF: {} pages, {} text pages", pageCount, result.pages.size());
        } catch (Exception e) {
            log.error("Failed to parse PDF: {}", e.getMessage());
            result.pages.add(new ParsedPage("[PDF解析失败: " + e.getMessage() + "]", 1));
        }
    }

    private void parseDocx(byte[] content, ParsedDocument result) {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(content))) {
            XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
            String text = extractor.getText();
            if (text != null && !text.trim().isEmpty()) {
                result.pages.add(new ParsedPage(text.trim(), 1));
            }
            log.info("Parsed DOCX: {} chars", text != null ? text.length() : 0);
        } catch (Exception e) {
            log.error("Failed to parse DOCX: {}", e.getMessage());
            result.pages.add(new ParsedPage("[Word解析失败: " + e.getMessage() + "]", 1));
        }
    }

    private void parseText(byte[] content, ParsedDocument result) {
        try {
            String text = new String(content, "UTF-8");
            result.pages.add(new ParsedPage(text.trim(), 1));
        } catch (Exception e) {
            log.error("Failed to parse text: {}", e.getMessage());
            result.pages.add(new ParsedPage("[文本解析失败]", 1));
        }
    }

    private boolean isImageFile(String fileName) {
        return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ||
               fileName.endsWith(".png") || fileName.endsWith(".gif") ||
               fileName.endsWith(".bmp") || fileName.endsWith(".webp");
    }

    public boolean supportsFileType(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".pdf") || lower.endsWith(".docx") ||
               lower.endsWith(".txt") || lower.endsWith(".md") ||
               isImageFile(lower);
    }
}