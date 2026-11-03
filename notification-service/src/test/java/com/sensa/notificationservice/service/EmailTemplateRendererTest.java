package com.sensa.notificationservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.List;

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
    void rendersEveryAlertTypeWithDifferentData() {
        record TemplateCase(
                String type,
                String label,
                String title,
                String message,
                String city,
                String street,
                String radius,
                String location
        ) {
        }

        List<TemplateCase> cases = List.of(
                new TemplateCase("FIRE", "FIRE ALERT",
                        "Fire near the market",
                        "Smoke and flames are visible near the market square.\nStay away from the area.",
                        "Opole", "Mikołajczyka 14", "2.5", "Opole, Mikołajczyka 14"),
                new TemplateCase("ACCIDENT", "ACCIDENT ALERT",
                        "Car accident on Wrocławska",
                        "Two vehicles collided at the junction.\nEmergency services are on the scene.",
                        "Opole", "Wrocławska 12", "1.0", "Opole, Wrocławska 12"),
                new TemplateCase("WEATHER", "WEATHER ALERT",
                        "Severe storm warning",
                        "Strong wind and heavy rain are expected.\nAvoid open areas and stay indoors.",
                        "Opole", "1 Maja 5", "5.0", "Opole, 1 Maja 5"),
                new TemplateCase("EVACUATION", "EVACUATION ORDER",
                        "Evacuation of Primary School No. 2",
                        "Residents near the school must leave the area.\nFollow the instructions of emergency services.",
                        "Opole", "Ozimska 8", "0.8", "Opole, Ozimska 8"),
                new TemplateCase("OTHER", "EMERGENCY ALERT",
                        "Water supply failure",
                        "Boil water before drinking until further notice.\nRepairs are in progress.",
                        "Opole", "Krakowska 3", "3.0", "Opole, Krakowska 3")
        );

        for (TemplateCase testCase : cases) {
            String html = renderer.render(
                    testCase.type(),
                    testCase.title(),
                    testCase.message(),
                    testCase.city(),
                    testCase.street(),
                    Double.parseDouble(testCase.radius()));

            assertTrue(html.contains(testCase.label()), testCase.type());
            assertTrue(html.contains(testCase.title()), testCase.type());
            assertTrue(html.contains(testCase.message().replace("\n", "<br>")), testCase.type());
            assertTrue(html.contains("Radius: " + testCase.radius() + " km"), testCase.type());
            assertTrue(html.contains(testCase.location()), testCase.type());
            assertTrue(html.contains("SENSA"), testCase.type());
            assertTrue(html.contains("https://panel.sensanotify.com"), testCase.type());
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
