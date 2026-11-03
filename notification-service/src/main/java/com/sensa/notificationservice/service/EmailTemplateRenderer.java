package com.sensa.notificationservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailTemplateRenderer {

    private static final DateTimeFormatter SENT_AT_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH)
                    .withZone(ZoneId.of("Europe/Warsaw"));

    private final TemplateEngine templateEngine;

    @Value("${app.panel-url:https://panel.sensanotify.com}")
    private String panelUrl;

    public String render(String alertType, String title, String message,
                         String city, String street, Double radiusKm) {
        String templateName = resolveTemplate(alertType);
        String location = formatLocation(city, street);
        String safeTitle = title == null ? "" : title;

        Context context = new Context(Locale.ENGLISH);
        context.setVariable("title", safeTitle);
        context.setVariable("messageHtml", toHtml(message));
        context.setVariable("location", location);
        context.setVariable("radius", formatRadius(radiusKm));
        context.setVariable("sentAt", SENT_AT_FORMAT.format(ZonedDateTime.now()));
        context.setVariable("panelUrl", panelUrl);
        context.setVariable("preheader", safeTitle + " — " + location);

        return templateEngine.process("email/" + templateName, context);
    }

    public String detectType(String value) {
        String text = value == null ? "" : value.toUpperCase(Locale.ROOT);
        if (text.contains("FIRE") || text.contains("POŻAR") || text.contains("POZAR")
                || text.contains("ПОЖ")) {
            return "fire";
        }
        if (text.contains("ACCIDENT") || text.contains("WYPADEK") || text.contains("АВАР")) {
            return "accident";
        }
        if (text.contains("WEATHER") || text.contains("POGODA") || text.contains("STORM")
                || text.contains("BURZA") || text.contains("ПОГОД")) {
            return "weather";
        }
        if (text.contains("EVAC") || text.contains("EWAKU") || text.contains("ЭВАК")) {
            return "evacuation";
        }
        return "other";
    }

    private String resolveTemplate(String alertType) {
        return switch (detectType(alertType)) {
            case "fire" -> "fire";
            case "accident" -> "accident";
            case "weather" -> "weather";
            case "evacuation" -> "evacuation";
            default -> "other";
        };
    }

    private static String toHtml(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return HtmlUtils.htmlEscape(value)
                .replace("\r\n", "\n")
                .replace("\n", "<br>");
    }

    private static String formatLocation(String city, String street) {
        if (city == null || city.isBlank()) {
            return "All users";
        }
        StringBuilder location = new StringBuilder(city.trim());
        if (street != null && !street.isBlank()) {
            location.append(", ").append(street.trim());
        }
        return location.toString();
    }

    private static String formatRadius(Double radiusKm) {
        return radiusKm == null ? "" : String.format(Locale.US, "%.1f km", radiusKm);
    }
}
