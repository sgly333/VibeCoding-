package com.example.paper.controller;

import com.example.paper.dto.PaperDTO;
import com.example.paper.dto.PaperUpdateRequest;
import com.example.paper.dto.UploadResponse;
import com.example.paper.service.PaperService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class PaperController {

    private final PaperService paperService;

    public PaperController(PaperService paperService) {
        this.paperService = paperService;
    }

    @PostMapping(value = "/papers/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse upload(@RequestParam("file") MultipartFile file) throws IOException {
        return paperService.uploadPaper(file);
    }

    @GetMapping("/papers")
    public List<PaperDTO> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category
    ) {
        return paperService.listPapers(keyword, category);
    }

    @PutMapping("/papers/{id}")
    public PaperDTO update(
            @PathVariable("id") Long id,
            @Valid @RequestBody PaperUpdateRequest request
    ) {
        return paperService.updatePaper(id, request);
    }

    @DeleteMapping("/papers/{id}")
    public void delete(@PathVariable("id") Long id) {
        paperService.deletePaper(id);
    }
}

