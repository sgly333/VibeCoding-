package com.example.paper.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Component
public class PdfParserUtil {

    /**
     * 从 PDF 文本中“尽力”截取 Abstract + Introduction。
     * 不同论文排版差异较大，这里做的是经验性规则兜底。
     */
    public String extractAbstractAndIntroduction(File pdfFile) throws IOException {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return extractAbstractAndIntroductionFromText(text);
        }
    }

    public String extractAbstractAndIntroduction(byte[] pdfBytes) throws IOException {
        if (pdfBytes == null || pdfBytes.length == 0) {
            return "";
        }
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return extractAbstractAndIntroductionFromText(text);
        }
    }

    private String extractAbstractAndIntroductionFromText(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        String text = rawText.replace('\u0000', ' ');
        String lower = text.toLowerCase();

        int absStart = lower.indexOf("abstract");
        int introStart = lower.indexOf("introduction");

        // 取段落的结束位置：尽量碰到下一个常见标题
        int absEnd = firstIndexOfAny(lower, new String[]{"introduction", "keywords", "index terms"});
        if (absStart >= 0 && absEnd <= absStart) absEnd = -1;

        int introEnd = firstIndexOfAny(lower, new String[]{"conclusion", "related work", "references"});
        if (introStart >= 0 && introEnd <= introStart) introEnd = -1;

        String abs = "";
        if (absStart >= 0) {
            int end = (absEnd > absStart) ? absEnd : Math.min(text.length(), absStart + 3000);
            abs = text.substring(absStart, end).trim();
        }

        String intro = "";
        if (introStart >= 0) {
            int end = (introEnd > introStart) ? introEnd : Math.min(text.length(), introStart + 5000);
            intro = text.substring(introStart, end).trim();
        }

        // 如果找不到标题，退化为开头内容
        if (abs.isBlank() && intro.isBlank()) {
            return text.trim().substring(0, Math.min(text.trim().length(), 4000));
        }

        return abs + "\n\n" + intro;
    }

    private int firstIndexOfAny(String lowerText, String[] candidates) {
        int best = -1;
        for (String c : candidates) {
            int idx = lowerText.indexOf(c);
            if (idx >= 0 && (best < 0 || idx < best)) {
                best = idx;
            }
        }
        return best;
    }
}

