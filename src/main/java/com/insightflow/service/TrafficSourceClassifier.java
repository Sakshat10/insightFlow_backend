package com.insightflow.service;

import com.insightflow.dto.TrafficSourceType;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.regex.Pattern;

@Component
public class TrafficSourceClassifier {

    public static class ClassificationResult {
        private final String source;
        private final TrafficSourceType sourceType;

        public ClassificationResult(String source, TrafficSourceType sourceType) {
            this.source = source;
            this.sourceType = sourceType;
        }

        public String getSource() {
            return source;
        }

        public TrafficSourceType getSourceType() {
            return sourceType;
        }
    }

    public ClassificationResult classify(String entryReferrer, String projectDomain) {
        if (entryReferrer == null || entryReferrer.isBlank()) {
            return new ClassificationResult("Direct", TrafficSourceType.DIRECT);
        }

        String host;
        try {
            // Attempt to parse referrer URL safely
            URL url = new URL(entryReferrer.trim());
            host = url.getHost();
        } catch (Exception e) {
            return new ClassificationResult("Unknown", TrafficSourceType.UNKNOWN);
        }

        if (host == null || host.isBlank()) {
            return new ClassificationResult("Unknown", TrafficSourceType.UNKNOWN);
        }

        host = host.toLowerCase();

        // Referrer belongs to the project's own domain
        String normalizedProjectDomain = projectDomain != null ? projectDomain.trim().toLowerCase() : "";
        if (!normalizedProjectDomain.isEmpty()) {
            if (host.equals(normalizedProjectDomain) || host.endsWith("." + normalizedProjectDomain)) {
                return new ClassificationResult("Direct", TrafficSourceType.DIRECT);
            }
        }

        // Google domains
        if (isDomainOrSubdomain(host, "google")) {
            return new ClassificationResult("Google", TrafficSourceType.ORGANIC_SEARCH);
        }

        // Bing domains
        if (isDomainOrSubdomain(host, "bing")) {
            return new ClassificationResult("Bing", TrafficSourceType.ORGANIC_SEARCH);
        }

        // Yahoo domains
        if (isDomainOrSubdomain(host, "yahoo")) {
            return new ClassificationResult("Yahoo", TrafficSourceType.ORGANIC_SEARCH);
        }

        // DuckDuckGo
        if (isDomainOrSubdomain(host, "duckduckgo")) {
            return new ClassificationResult("DuckDuckGo", TrafficSourceType.ORGANIC_SEARCH);
        }

        // Facebook domains
        if (isDomainOrSubdomain(host, "facebook")) {
            return new ClassificationResult("Facebook", TrafficSourceType.SOCIAL);
        }

        // Instagram domains
        if (isDomainOrSubdomain(host, "instagram")) {
            return new ClassificationResult("Instagram", TrafficSourceType.SOCIAL);
        }

        // LinkedIn domains
        if (isDomainOrSubdomain(host, "linkedin")) {
            return new ClassificationResult("LinkedIn", TrafficSourceType.SOCIAL);
        }

        // Twitter/X domains
        if (isDomainOrSubdomain(host, "twitter") || isDomainOrSubdomain(host, "x") || host.equals("t.co") || host.endsWith(".t.co")) {
            return new ClassificationResult("X", TrafficSourceType.SOCIAL);
        }

        // YouTube
        if (isDomainOrSubdomain(host, "youtube") || host.equals("youtu.be") || host.endsWith(".youtu.be")) {
            return new ClassificationResult("YouTube", TrafficSourceType.SOCIAL);
        }

        // GitHub
        if (isDomainOrSubdomain(host, "github")) {
            return new ClassificationResult("GitHub", TrafficSourceType.REFERRAL);
        }

        // Any other external referrer
        String normalizedHost = host;
        if (normalizedHost.startsWith("www.")) {
            normalizedHost = normalizedHost.substring(4);
        }
        return new ClassificationResult(normalizedHost, TrafficSourceType.REFERRAL);
    }

    private boolean isDomainOrSubdomain(String host, String domainName) {
        String pattern = "^(.*\\.)?" + Pattern.quote(domainName) + "\\.[a-z.]{2,}$";
        return host.matches(pattern);
    }
}
