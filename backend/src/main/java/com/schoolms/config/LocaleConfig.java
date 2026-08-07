package com.schoolms.config;

import com.schoolms.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.LocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * Resolves the request locale, preferring the authenticated user's stored
 * preference (from the JWT) over the Accept-Language header.
 */
@Configuration
public class LocaleConfig {

    @Bean
    public LocaleResolver localeResolver() {
        return new LocaleResolver() {
            @Override
            public Locale resolveLocale(HttpServletRequest request) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal
                        && principal.locale() != null && !principal.locale().isBlank()) {
                    return Locale.forLanguageTag(principal.locale());
                }
                String acceptLanguage = request.getHeader("Accept-Language");
                if (acceptLanguage != null && !acceptLanguage.isBlank()) {
                    List<Locale.LanguageRange> ranges = Locale.LanguageRange.parse(acceptLanguage);
                    if (!ranges.isEmpty()) {
                        return Locale.forLanguageTag(ranges.get(0).getRange());
                    }
                }
                return Locale.ENGLISH;
            }

            @Override
            public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
                // Locale is bound to the authenticated user's profile instead.
            }
        };
    }
}
