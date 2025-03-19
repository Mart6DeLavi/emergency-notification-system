package com.sensa.templateservice.controller;

import com.sensa.templateservice.dto.TemplateRequest;
import com.sensa.templateservice.dto.TemplateResponse;
import com.sensa.templateservice.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @Operation(summary = "Create template", description = "Creates a new template and returns data about it")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Template created successfully"),
            @ApiResponse(responseCode = "400", description = "Incorrect template data"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping("/create")
    public ResponseEntity<TemplateResponse> createTemplate(
            @RequestHeader String username,
            @RequestBody @Valid TemplateRequest templateRequest
    ) {
        return ResponseEntity.status(CREATED).body(templateService.createTemplate(username, templateRequest));
    }

    @Operation(summary = "Find a template", description = "Returns data about a template by name")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Template found"),
            @ApiResponse(responseCode = "404", description = "Template not found")
    })
    @GetMapping("/find/{title}")
    public ResponseEntity<TemplateResponse> findTemplate(
            @RequestHeader String username,
            @PathVariable("title") @Valid String title
    ) {
        return ResponseEntity.status(OK).body(templateService.findTemplate(username, title));
    }

    @Operation(summary = "Update template", description = "Updates existing template by name")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Template successfully updated"),
            @ApiResponse(responseCode = "400", description = "Incorrect template data"),
            @ApiResponse(responseCode = "404", description = "Template not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PatchMapping("/update/{title}")
    public ResponseEntity<TemplateResponse> updateTemplate(
            @RequestHeader String username,
            @PathVariable("title") String title,
            @RequestBody @Valid TemplateRequest templateRequest
    ) {
        TemplateResponse response = templateService.updateTemplate(username, title, templateRequest);
        return ResponseEntity.status(OK).body(response);
    }

    @Operation(summary = "Delete template", description = "Deletes template by name")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Template successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Template not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @DeleteMapping("/delete/{title}")
    public ResponseEntity<Boolean> deleteTemplate(
            @RequestHeader String username,
            @PathVariable("title") @Valid String title
    ) {
        return ResponseEntity.status(OK).body(templateService.deleteTemplate(username, title));
    }
}
