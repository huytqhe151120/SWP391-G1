package com.swp391.g1;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the 404 error handling: unknown URLs return 404 and the
 * user-friendly page renders through Spring Boot's normal error handling.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErrorHandlingTests {

    // Standard servlet attribute that carries the error status code.
    private static final String SERVLET_ERROR_STATUS_CODE_ATTRIBUTE = "jakarta.servlet.error.status_code";

    private static final String DEV_PASSWORD = "admin123";

    @Autowired
    private MockMvc mockMvc;

    @Value("${app.auth.dev-account.username}")
    private String devUsername;

    @Test
    void unknownUrlReturns404StatusCodeForAuthenticatedUser() throws Exception {
        MockHttpSession session = logIn();

        mockMvc.perform(get("/does-not-exist").session(session))
                .andExpect(status().isNotFound());
    }

    @Test
    void errorPageRenders404TemplateForNotFoundStatus() throws Exception {
        mockMvc.perform(get("/error")
                        .accept(MediaType.TEXT_HTML)
                        .requestAttr(SERVLET_ERROR_STATUS_CODE_ATTRIBUTE, 404))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Page not found")));
    }

    private MockHttpSession logIn() throws Exception {
        return (MockHttpSession) mockMvc
                .perform(formLogin().user(this.devUsername).password(DEV_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andReturn()
                .getRequest()
                .getSession();
    }
}