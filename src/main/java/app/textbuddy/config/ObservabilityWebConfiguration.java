package app.textbuddy.config;

import app.textbuddy.observability.ApiUsageLoggingInterceptor;
import app.textbuddy.observability.UsagePseudonymizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class ObservabilityWebConfiguration {

    @Bean
    ApiUsageLoggingInterceptor apiUsageLoggingInterceptor(
            TextbuddyProperties textbuddyProperties,
            UsagePseudonymizer usagePseudonymizer
    ) {
        return new ApiUsageLoggingInterceptor(textbuddyProperties, usagePseudonymizer);
    }

    @Bean
    WebMvcConfigurer observabilityWebMvcConfigurer(ApiUsageLoggingInterceptor apiUsageLoggingInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(apiUsageLoggingInterceptor)
                        .addPathPatterns("/api/**");
            }
        };
    }
}
