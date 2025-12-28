package com.fintech.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Service for rendering notification templates.
 * Uses Thymeleaf for HTML email templates.
 */
@Service
@Slf4j
public class TemplateService {

    private final TemplateEngine templateEngine;

    public TemplateService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * Render a template with given variables.
     */
    public String render(String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            return templateEngine.process(templateName, context);
        } catch (Exception e) {
            log.error("Failed to render template {}: {}", templateName, e.getMessage());
            // Return a fallback message
            return buildFallbackContent(templateName, variables);
        }
    }

    /**
     * Fallback when template rendering fails.
     */
    private String buildFallbackContent(String templateName, Map<String, Object> variables) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h2>Digital Wallet Notification</h2>");
        sb.append("<p>Template: ").append(templateName).append("</p>");

        if (variables != null && !variables.isEmpty()) {
            sb.append("<ul>");
            variables.forEach((key, value) ->
                    sb.append("<li><strong>").append(key).append(":</strong> ")
                            .append(value).append("</li>"));
            sb.append("</ul>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }
}

