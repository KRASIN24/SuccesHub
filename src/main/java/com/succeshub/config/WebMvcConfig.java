package com.succeshub.config;

import com.succeshub.coreinfra.auth.DeactivatedAccountInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers MVC interceptors (deactivated-account API gate).
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final DeactivatedAccountInterceptor deactivatedAccountInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(deactivatedAccountInterceptor)
                .addPathPatterns("/api/**");
    }
}
