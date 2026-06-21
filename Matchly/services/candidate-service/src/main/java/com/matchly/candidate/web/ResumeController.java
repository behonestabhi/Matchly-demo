package com.matchly.candidate.web;

import com.matchly.candidate.dto.ResumeDetailResponse;
import com.matchly.candidate.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Resume read API. Paths under {@code /api/v1/resumes}. */
@RestController
@RequestMapping("/api/v1/resumes")
@Tag(name = "Resumes", description = "Resume parse status + structured data")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @Operation(summary = "Get a resume: parse status, plus structured data once PARSED")
    @GetMapping("/{id}")
    public ResumeDetailResponse getResume(@PathVariable("id") UUID id) {
        return resumeService.getDetail(id);
    }
}
