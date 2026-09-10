package com.swp391.g1;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * MockMvc integration tests for the v1.0 authentication foundation.
 *
 * <p>The full Spring context (with the real Spring Security filter chain, CSRF
 * enabled) is booted, so these tests assert real HTTP-level behavior. The
 * {@code test} profile excludes the JPA/DataSource auto-configurations, so no
 * SQL Server is required. Credentials come from the configuration-backed v1.0
 * development account.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationFlowTests {

    /** Development placeholder password matching the BCrypt hash in application.properties. */
    private static final String DEV_PASSWORD = "admin123";

    @Autowired
    private MockMvc mockMvc;

    @Value("${app.auth.dev-account.username}")
    private String devUsername;

    @Test
    void loginPageIsPublicAndRenders() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sign in")));
    }

    @Test
    void homeWithoutAuthenticationRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/home"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void loginWithConfiguredDevAccountSucceeds() throws Exception {
        mockMvc.perform(formLogin().user(this.devUsername).password(DEV_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    void loginWithInvalidPasswordRedirectsToLoginError() throws Exception {
        mockMvc.perform(formLogin().user(this.devUsername).password("wrong-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void authenticatedUserCanAccessHome() throws Exception {
        MockHttpSession session = logIn();

        mockMvc.perform(get("/home").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("You are authenticated.")));
    }

    @Test
    void logoutInvalidatesSessionAndRedirectsToLogin() throws Exception {
        MockHttpSession session = logIn();

        mockMvc.perform(post("/logout").with(csrf()).session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));

        // Session is invalidated by the logout handler; a fresh request is
        // unauthenticated again and /home redirects to /login.
        Assertions.assertTrue(session.isInvalid());
        mockMvc.perform(get("/home"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void logoutWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().isForbidden());
    }

    /**
     * Logs in with the configuration-backed development account and returns the
     * authenticated session.
     */
    private MockHttpSession logIn() throws Exception {
        MvcResult result = mockMvc.perform(formLogin().user(this.devUsername).password(DEV_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession();
    }
}