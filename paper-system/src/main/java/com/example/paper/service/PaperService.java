package com.example.paper.service;

import com.example.paper.dto.PaperDTO;
import com.example.paper.dto.PaperUpdateRequest;
import com.example.paper.dto.UploadResponse;
import com.example.paper.entity.Category;
import com.example.paper.entity.Paper;
import com.example.paper.entity.PaperCategory;
import com.example.paper.exception.ResourceNotFoundException;
import com.example.paper.repository.CategoryRepository;
import com.example.paper.repository.PaperRepository;
import com.example.paper.util.CategoryConstants;
import com.example.paper.util.PdfParserUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.example.paper.exception.ApiException;

@Service
public class PaperService {

    private final PaperRepository paperRepository;
    private final CategoryRepository categoryRepository;
    private final FileService fileService;
    private final PdfParserUtil pdfParserUtil;
    private final LLMService llmService;

    public PaperService(PaperRepository paperRepository,
                         CategoryRepository categoryRepository,
                         FileService fileService,
                         PdfParserUtil pdfParserUtil,
                         LLMService llmService) {
        this.paperRepository = paperRepository;
        this.categoryRepository = categoryRepository;
        this.fileService = fileService;
        this.pdfParserUtil = pdfParserUtil;
        this.llmService = llmService;
    }

    @Transactional
    public UploadResponse uploadPaper(MultipartFile file, String codeUrl) throws IOException {
        String title = deriveTitle(file.getOriginalFilename());
        byte[] pdfBytes = file.getBytes();
        String text = pdfParserUtil.extractAbstractAndIntroduction(pdfBytes);
        List<String> categories = llmService.classify(text);
        categories = normalizeCategories(categories);

        String storedFilePath = fileService.uploadBytes(pdfBytes, file.getOriginalFilename());

        Paper paper = new Paper();
        paper.setTitle(title);
        paper.setFilePath(storedFilePath);
        paper.setCodeUrl(emptyToNull(codeUrl));
        paperRepository.save(paper); // 先拿到 paper.id

        attachCategories(paper, categories);
        paperRepository.save(paper);

        return new UploadResponse(paper.getId(), categories);
    }

    @Transactional(readOnly = true)
    public List<PaperDTO> listPapers(String keyword, String category) {
        List<Paper> papers = paperRepository.search(
                emptyToNull(keyword),
                emptyToNull(category)
        );
        List<PaperDTO> out = new ArrayList<>();
        for (Paper paper : papers) {
            out.add(toDto(paper));
        }
        out.sort(Comparator.comparing(PaperDTO::getCreatedAt).reversed());
        return out;
    }

    @Transactional
    public PaperDTO updatePaper(Long id, PaperUpdateRequest req) {
        Paper paper = paperRepository.findByIdWithCategories(id)
                .orElseThrow(() -> new ResourceNotFoundException("paper not found: " + id));

        paper.setCodeUrl(req.getCodeUrl());

        List<String> categories = normalizeCategories(req.getCategories());
        paper.getPaperCategories().clear();
        attachCategories(paper, categories);

        Paper saved = paperRepository.save(paper);
        return toDto(saved);
    }

    @Transactional
    public void deletePaper(Long id) {
        Paper paper = paperRepository.findByIdWithCategories(id)
                .orElseThrow(() -> new ResourceNotFoundException("paper not found: " + id));

        String filePath = paper.getFilePath();
        paperRepository.delete(paper); // orphanRemoval + cascade 会删除 paper_category
        fileService.delete(filePath);
    }

    private void attachCategories(Paper paper, List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return;
        }

        for (String categoryName : categories) {
            Category category = categoryRepository.findByName(categoryName)
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "unknown category: " + categoryName));
            PaperCategory pc = new PaperCategory();
            pc.setPaper(paper);
            pc.setCategory(category);
            pc.getId().setPaperId(paper.getId());
            pc.getId().setCategoryId(category.getId());
            paper.getPaperCategories().add(pc);
        }
    }

    private PaperDTO toDto(Paper paper) {
        List<String> categories = new ArrayList<>();
        if (paper.getPaperCategories() != null) {
            for (PaperCategory pc : paper.getPaperCategories()) {
                if (pc.getCategory() != null && pc.getCategory().getName() != null) {
                    categories.add(pc.getCategory().getName());
                }
            }
        }
        categories = new ArrayList<>(new HashSet<>(categories));

        PaperDTO dto = new PaperDTO();
        dto.setId(paper.getId());
        dto.setTitle(paper.getTitle());
        dto.setFileUrl(fileService.buildPublicUrl(paper.getFilePath()));
        dto.setCodeUrl(paper.getCodeUrl());
        dto.setCreatedAt(paper.getCreatedAt());
        dto.setCategories(categories);
        return dto;
    }

    private String deriveTitle(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "paper";
        }
        String name = originalFilename;
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        return name.trim();
    }

    private String emptyToNull(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<String> normalizeCategories(List<String> categories) {
        if (categories == null) {
            return List.of();
        }
        Set<String> allowed = new HashSet<>(CategoryConstants.DEFAULT_CATEGORIES);
        List<String> out = new ArrayList<>();
        for (String c : categories) {
            if (c == null) continue;
            String t = c.trim();
            if (allowed.contains(t)) {
                out.add(t);
            }
        }
        // 保证至少有一个类别（便于前端渲染）
        if (out.isEmpty()) {
            out.add("LLM Based");
        }
        return out;
    }
}

