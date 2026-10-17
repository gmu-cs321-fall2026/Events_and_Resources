package edu.gmu.cs321.events_resources.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebConfig: tells Spring "run AuthInterceptor before requests to these URLs".
 *
 * Without this class, AuthInterceptor would exist but never run.
 * The interceptor itself decides which of these URLs are public and which need a login.
 */
@Configuration // "this class holds settings for Spring", read once at startup
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) { // Spring passes in the @Component AuthInterceptor
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                // ** = "anything after this", e.g. /events/5/register
                .addPathPatterns("/events", "/events/**", "/users/**");
        // URLs not listed here (like /hello) skip the auth check entirely.
    }
}
