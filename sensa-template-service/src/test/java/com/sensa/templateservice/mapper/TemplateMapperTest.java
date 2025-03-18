package com.sensa.templateservice.mapper;

import com.sensa.templateservice.dto.TemplateRequest;
import com.sensa.templateservice.dto.TemplateResponse;
import com.sensa.templateservice.entity.Template;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemplateMapperTest {

    private final TemplateMapper templateMapper = new TemplateMapper();

    @Test
    void testMapToEntity() {
        TemplateRequest request = TemplateRequest.builder()
                .clientUsername("user1")
                .title("Template1")
                .content("Content1")
                .build();

        Template entity = templateMapper.mapToEntity(request);
        assertEquals(request.clientUsername(), entity.getClientUsername());
        assertEquals(request.title(), entity.getTitle());
        assertEquals(request.content(), entity.getContent());
    }

    @Test
    void testMapToResponse() {
        Template entity = Template.builder()
                .clientUsername("user1")
                .title("Template1")
                .content("Content1")
                .build();

        TemplateResponse response = templateMapper.mapToResponse(entity);
        assertEquals(entity.getClientUsername(), response.clientUsername());
        assertEquals(entity.getTitle(), response.title());
        assertEquals(entity.getContent(), response.content());
    }
}
