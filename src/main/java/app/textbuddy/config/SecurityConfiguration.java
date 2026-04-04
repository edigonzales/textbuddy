package app.textbuddy.config;

import app.textbuddy.web.error.ApiErrorResponseFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfiguration.class);
    private static final String API_AUTH_REQUIRED_MESSAGE = "Anmeldung erforderlich.";
    private static final String API_ACCESS_DENIED_MESSAGE = "Zugriff verweigert.";
    private static final PathPatternRequestMatcher API_REQUEST_MATCHER =
            PathPatternRequestMatcher.withDefaults().matcher("/api/**");

    @Bean
    RequestTracingFilter requestTracingFilter() {
        return new RequestTracingFilter();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            TextbuddyProperties properties,
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider,
            RequestTracingFilter requestTracingFilter,
            ApiErrorResponseFactory errorResponseFactory,
            ObjectMapper objectMapper
    ) throws Exception {
        boolean authEnabled = properties.getAuth().isEnabled();
        ClientRegistrationRepository clientRegistrationRepository = clientRegistrationRepositoryProvider.getIfAvailable();

        if (authEnabled && clientRegistrationRepository == null) {
            throw new IllegalStateException(
                    "OIDC ist aktiviert, aber es wurde keine spring.security.oauth2.client.registration.* Konfiguration gefunden."
            );
        }

        log.info("Security mode: {}", authEnabled ? "OIDC protected APIs" : "open local mode");

        http.csrf(AbstractHttpConfigurer::disable);
        http.addFilterBefore(requestTracingFilter, SecurityContextHolderFilter.class);
        http.authorizeHttpRequests(authorize -> {
            authorize.requestMatchers(
                    "/",
                    "/error",
                    "/favicon.ico",
                    "/login",
                    "/oauth2/**",
                    "/styles/**",
                    "/editor/**",
                    "/webjars/**",
                    "/actuator/health",
                    "/actuator/info"
            ).permitAll();

            if (authEnabled) {
                authorize.requestMatchers("/api/**", "/logout").authenticated();
            }

            authorize.anyRequest().permitAll();
        });
        http.exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(
                        (request, response, exception) -> writeApiProblem(
                                request,
                                response,
                                HttpStatus.UNAUTHORIZED,
                                API_AUTH_REQUIRED_MESSAGE,
                                errorResponseFactory,
                                objectMapper
                        ),
                        API_REQUEST_MATCHER
                )
                .defaultAccessDeniedHandlerFor(
                        (request, response, exception) -> writeApiProblem(
                                request,
                                response,
                                HttpStatus.FORBIDDEN,
                                API_ACCESS_DENIED_MESSAGE,
                                errorResponseFactory,
                                objectMapper
                        ),
                        API_REQUEST_MATCHER
                )
        );

        if (authEnabled) {
            http.oauth2Login(Customizer.withDefaults());
            http.logout(logout -> logout.logoutSuccessUrl("/"));
        } else {
            http.logout(logout -> logout.disable());
        }

        return http.build();
    }

    private void writeApiProblem(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String detail,
            ApiErrorResponseFactory errorResponseFactory,
            ObjectMapper objectMapper
    ) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), errorResponseFactory.create(status, detail, request));
    }
}
