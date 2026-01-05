package com.backend.config;

import com.backend.filter.PasetoAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class SecurityConfig {
    
    private final PasetoAuthenticationFilter pasetoAuthenticationFilter;
    
    public SecurityConfig(PasetoAuthenticationFilter pasetoAuthenticationFilter) {
        this.pasetoAuthenticationFilter = pasetoAuthenticationFilter;
    }
    
    /**
     * Register PASETO authentication filter
     */
    @Bean
    public FilterRegistrationBean<PasetoAuthenticationFilter> pasetoFilter() {
        FilterRegistrationBean<PasetoAuthenticationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(pasetoAuthenticationFilter);
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
    
    /**
     * Configure CORS
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(0);
        return bean;
    }
}
