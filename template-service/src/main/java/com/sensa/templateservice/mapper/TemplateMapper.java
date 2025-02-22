package com.sensa.templateservice.mapper;

import com.sensa.templateservice.dto.TemplateRequest;
import com.sensa.templateservice.dto.TemplateResponse;
import com.sensa.templateservice.entity.Template;
import org.springframework.stereotype.Component;

@Component
public class TemplateMapper {
    public Template mapToEntity(TemplateRequest templateRequest) {
        return Template.builder()
                .clientUsername(templateRequest.clientUsername())
                .title(templateRequest.title())
                .content(templateRequest.content())
                .build();
    }

    public TemplateResponse mapToResponse(Template template) {
        return TemplateResponse.builder()
                .clientUsername(template.getClientUsername())
                .title(template.getTitle())
                .content(template.getContent())
                .build();
    }

    public Template update(TemplateRequest templateRequest, Template template) {
        template.setClientUsername(templateRequest.clientUsername());
        template.setTitle(template.getTitle());
        template.setContent(template.getContent());
        return template;
    }
}
