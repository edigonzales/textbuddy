package app.textbuddy.web.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "textbuddy.auth.enabled=true",
        "spring.security.oauth2.client.registration.test.client-id=textbuddy-client",
        "spring.security.oauth2.client.registration.test.client-secret=textbuddy-secret",
        "spring.security.oauth2.client.registration.test.client-name=Textbuddy Login",
        "spring.security.oauth2.client.registration.test.scope=openid,profile,email",
        "spring.security.oauth2.client.registration.test.authorization-grant-type=authorization_code",
        "spring.security.oauth2.client.registration.test.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}",
        "spring.security.oauth2.client.provider.test.authorization-uri=https://issuer.example.test/oauth2/authorize",
        "spring.security.oauth2.client.provider.test.token-uri=https://issuer.example.test/oauth2/token",
        "spring.security.oauth2.client.provider.test.user-info-uri=https://issuer.example.test/userinfo",
        "spring.security.oauth2.client.provider.test.user-name-attribute=sub",
        "spring.security.oauth2.client.provider.test.jwk-set-uri=https://issuer.example.test/oauth2/jwks"
})
@AutoConfigureMockMvc
class AuthEnabledMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void homeShowsLoginPromptForAnonymousUsers() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-auth-enabled=\"true\"")))
                .andExpect(content().string(containsString("data-authenticated=\"false\"")))
                .andExpect(content().string(containsString("data-testid=\"auth-login-link\"")))
                .andExpect(content().string(containsString("href=\"/oauth2/authorization/test\"")));
    }

    @Test
    void protectedApiReturnsProblemJsonWhenAnonymous() throws Exception {
        mockMvc.perform(post("/api/text-correction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "This is teh text.",
                                  "language": "en-US"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.detail").value("Anmeldung erforderlich."))
                .andExpect(jsonPath("$.path").value("/api/text-correction"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void authenticatedUsersSeeAccountStateAndCanCallApis() throws Exception {
        mockMvc.perform(get("/")
                        .with(oauth2Login().attributes(attributes -> {
                            attributes.put("sub", "demo-user");
                            attributes.put("email", "demo@example.org");
                        })))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-authenticated=\"true\"")))
                .andExpect(content().string(containsString("data-testid=\"auth-user\">demo@example.org</strong>")))
                .andExpect(content().string(containsString("data-testid=\"auth-logout\"")));

        mockMvc.perform(post("/api/text-correction")
                        .with(oauth2Login())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "text": "This is teh text.",
                                  "language": "en-US"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocks[0].shortMessage").value("Spelling"));
    }
}
