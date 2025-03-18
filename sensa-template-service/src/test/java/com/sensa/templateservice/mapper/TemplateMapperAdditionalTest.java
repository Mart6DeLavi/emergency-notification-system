package com.sensa.templateservice.mapper;

import com.sensa.templateservice.dto.TemplateRequest;
import com.sensa.templateservice.entity.Template;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TemplateMapperAdditionalTest {

    private final TemplateMapper templateMapper = new TemplateMapper();

    @Test
    void testMapToEntityWithNullContent() {
        TemplateRequest request = TemplateRequest.builder()
                .clientUsername("user1")
                .title("Template1")
                .content(null)
                .build();

        Template entity = templateMapper.mapToEntity(request);
        assertEquals("user1", entity.getClientUsername());
        assertEquals("Template1", entity.getTitle());
        assertNull(entity.getContent());
    }

    @Test
    void testUpdateMethod() {
        TemplateRequest request = TemplateRequest.builder()
                .clientUsername("user2")
                .title("NewTitle")
                .content("NewContent")
                .build();

        Template template = Template.builder()
                .clientUsername("user1")
                .title("OldTitle")
                .content("OldContent")
                .build();

        Template updated = templateMapper.update(request, template);
        assertEquals("user2", updated.getClientUsername());
        assertEquals("OldTitle", updated.getTitle());
        assertEquals("OldContent", updated.getContent());
    }
}