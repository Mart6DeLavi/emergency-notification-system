package com.sensa.notificationservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailTemplateRendererTest {

    private EmailTemplateRenderer renderer;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        renderer = new EmailTemplateRenderer(engine);
        ReflectionTestUtils.setField(renderer, "panelUrl", "https://panel.sensanotify.com");
    }

    @Test
    void rendersEveryAlertTypeWithSharedData() {
        for (String type : new String[]{"FIRE", "ACCIDENT", "WEATHER", "EVACUATION", "OTHER"}) {
            String html = renderer.render(
                    type,
                    "Test alert",
                    "Line 1\nLine 2",
                    "Opole",
                    "Main Street 1",
                    2.5);

            assertTrue(html.contains("Test alert"), type);
            assertTrue(html.contains("Line 1<br>Line 2"), type);
            assertTrue(html.contains("Opole, Main Street 1"), type);
            assertTrue(html.contains("Radius: 2.5 km"), type);
            assertTrue(html.contains("SENSA"), type);
            assertTrue(html.contains("https://panel.sensanotify.com"), type);
        }
    }

    @Test
    void escapesMessageBeforeInjectingItIntoHtml() {
        String html = renderer.render("FIRE", "Title", "<b>alert</b>\nnext", "Opole", null, null);

        assertTrue(html.contains("&lt;b&gt;alert&lt;/b&gt;"));
        assertTrue(html.contains("&lt;b&gt;alert&lt;/b&gt;<br>next"));
        assertFalse(html.contains("<b>alert</b>"));
    }

    @Test
    void fallsBackToAllUsersWhenCityIsMissing() {
        String html = renderer.render("OTHER", "Title", "Message", null, null, null);

        assertTrue(html.contains("All users"));
        assertFalse(html.contains("Radius:"));
    }

    @Test
    void detectsAlertTypesFromOperatorValues() {
        assertEquals("fire", renderer.detectType("FIRE"));
        assertEquals("fire", renderer.detectType("Pożar"));
        assertEquals("accident", renderer.detectType("WYPADEK"));
        assertEquals("weather", renderer.detectType("Burza"));
        assertEquals("evacuation", renderer.detectType("EWAKUACJA"));
        assertEquals("other", renderer.detectType("OTHER"));
    }
}
