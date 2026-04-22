package app.textbuddy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.time.Duration;
import java.util.Locale;

@Configuration(proxyBeanMethods = false)
public class WebI18nConfiguration {

    public static final Locale DEFAULT_UI_LOCALE = Locale.GERMAN;
    public static final String UI_LOCALE_COOKIE = "textbuddy-ui-locale";
    public static final String UI_LOCALE_PARAMETER = "lang";

    @Bean
    LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver(UI_LOCALE_COOKIE);
        resolver.setDefaultLocale(DEFAULT_UI_LOCALE);
        resolver.setCookieHttpOnly(true);
        resolver.setCookiePath("/");
        resolver.setCookieMaxAge(Duration.ofDays(365));
        return resolver;
    }

    @Bean
    LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName(UI_LOCALE_PARAMETER);
        return interceptor;
    }

    @Bean
    WebMvcConfigurer i18nWebMvcConfigurer(LocaleChangeInterceptor localeChangeInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(localeChangeInterceptor);
            }
        };
    }
}
