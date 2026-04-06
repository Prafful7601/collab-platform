package com.collab.apigateway.security;

import org.springframework.stereotype.Component;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

@Component
public class HtmlSanitizer {

    private final PolicyFactory policy;

    public HtmlSanitizer() {
        // Allow common HTML tags for rich text editing
        this.policy = new HtmlPolicyBuilder()
                .allowElements("b", "strong", "i", "em", "u", "br", "p", "div", "span")
                .allowAttributes("style").onElements("span") // Allow inline styles for formatting
                .allowAttributes("class").onElements("span", "div", "p") // Allow CSS classes
                .toFactory();
    }

    public String sanitize(String html) {
        if (html == null) {
            return null;
        }
        return policy.sanitize(html);
    }

    public boolean isSafe(String html) {
        if (html == null) {
            return true;
        }
        String sanitized = policy.sanitize(html);
        return sanitized.equals(html);
    }
}