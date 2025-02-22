package com.sensa.templateservice.controller;

import com.sensa.templateservice.dto.TemplateRequest;
import com.sensa.templateservice.dto.TemplateResponse;
import com.sensa.templateservice.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @PostMapping("/create")
    public ResponseEntity<TemplateResponse> createTemplate(
            @RequestHeader String username,
            @RequestBody @Valid TemplateRequest templateRequest
    ) {
        return ResponseEntity.status(CREATED).body(templateService.createTemplate(username, templateRequest));
    }

    @GetMapping("/find/{title}")
    public ResponseEntity<TemplateResponse> findTemplate(
            @RequestHeader String username,
            @PathVariable("title") @Valid String title
    ) {
        return ResponseEntity.status(OK).body(templateService.findTemplate(username, title));
    }

    @PatchMapping("/update/{title}")
    public ResponseEntity<TemplateResponse> updateTemplate(
            @RequestHeader String username,
            @PathVariable("title") String title,
            @RequestBody @Valid TemplateRequest templateRequest
    ) {
        TemplateResponse response = templateService.updateTemplate(username, title, templateRequest);
        return ResponseEntity.status(OK).body(response);
    }

    @DeleteMapping("/delete/{title}")
    public ResponseEntity<Boolean> deleteTemplate(
            @RequestHeader String username,
            @PathVariable("title") @Valid String title
    ) {
        return ResponseEntity.status(OK).body(templateService.deleteTemplate(username, title));
    }
}
