package com.neueda.instrumentservice.controllers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import com.neueda.instrumentservice.config.SecurityConfig;
import com.neueda.instrumentservice.config.TestJwtIssuer;
import com.neueda.instrumentservice.dtos.ai.AssistantAnswer;
import com.neueda.instrumentservice.dtos.ai.AssistantSource;
import com.neueda.instrumentservice.dtos.responses.AssistantChatResponse;
import com.neueda.instrumentservice.exceptions.AssistantRateLimitException;
import com.neueda.instrumentservice.services.assistant.AssistantRateLimiter;
import com.neueda.instrumentservice.services.assistant.AssistantService;

@WebMvcTest(AssistantController.class)
@Import(SecurityConfig.class)
class AssistantControllerTest {

    private static final String CHAT = "/assistant/chat";
    private static final String QUESTION = "{\"message\":\"Why is Microsoft down today?\"}";

    @DynamicPropertySource
    static void auth(DynamicPropertyRegistry registry) {
        TestJwtIssuer.registerProperties(registry);
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AssistantService assistantService;

    @MockBean
    private AssistantRateLimiter rateLimiter;

    private static MockHttpServletRequestBuilder chat(String token, String body) {
        MockHttpServletRequestBuilder request = post(CHAT).contentType(MediaType.APPLICATION_JSON).content(body);
        return token == null ? request : request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    @Test
    void answersASignedInUserWithTheReplySourcesAndDisclaimer() throws Exception {
        when(assistantService.respond("Why is Microsoft down today?")).thenReturn(new AssistantAnswer(
                "MSFT is down 2.06%.", true, true, false, "openai/gpt-oss-120b", List.of(),
                List.of(new AssistantSource(AssistantSource.Type.NEWS, "MSFT", "Azure growth slows",
                        "https://news.example.com/azure", Instant.parse("2026-10-19T13:05:00Z"))),
                List.of()));

        mockMvc.perform(chat(TestJwtIssuer.userToken(), QUESTION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("MSFT is down 2.06%."))
                .andExpect(jsonPath("$.aiSummary").value(true))
                .andExpect(jsonPath("$.checksPassed").value(true))
                .andExpect(jsonPath("$.model").value("openai/gpt-oss-120b"))
                .andExpect(jsonPath("$.sources[0].type").value("NEWS"))
                .andExpect(jsonPath("$.sources[0].url").value("https://news.example.com/azure"))
                .andExpect(jsonPath("$.disclaimer").value(AssistantChatResponse.DISCLAIMER))
                .andExpect(jsonPath("$.problems").doesNotExist())
                .andExpect(jsonPath("$.toolResults").doesNotExist());

        verify(rateLimiter).check("alice");
    }

    @Test
    void returnsTheRawDataAnswerWithAiSummaryOff() throws Exception {
        when(assistantService.respond(anyString())).thenReturn(new AssistantAnswer(
                "The AI assistant is unavailable right now.", false, false, false, null,
                List.of("down"), List.of(), List.of()));

        mockMvc.perform(chat(TestJwtIssuer.userToken(), QUESTION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiSummary").value(false))
                .andExpect(jsonPath("$.checksPassed").value(false))
                .andExpect(jsonPath("$.disclaimer").value(AssistantChatResponse.DISCLAIMER));
    }

    @Test
    void rejectsARequestWithoutAToken() throws Exception {
        mockMvc.perform(chat(null, QUESTION)).andExpect(status().isUnauthorized());

        verifyNoInteractions(assistantService, rateLimiter);
    }

    @Test
    void rejectsServiceTokensBecauseOnlyPeopleUseTheAssistant() throws Exception {
        mockMvc.perform(chat(TestJwtIssuer.serviceToken(), QUESTION)).andExpect(status().isForbidden());

        verifyNoInteractions(assistantService, rateLimiter);
    }

    @Test
    void rejectsABlankQuestion() throws Exception {
        mockMvc.perform(chat(TestJwtIssuer.userToken(), "{\"message\":\"   \"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VAL-422"));

        verifyNoInteractions(assistantService);
    }

    @Test
    void rejectsAQuestionThatIsTooLong() throws Exception {
        String longQuestion = "a".repeat(501);

        mockMvc.perform(chat(TestJwtIssuer.userToken(), "{\"message\":\"" + longQuestion + "\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("message must be at most 500 characters"));

        verifyNoInteractions(assistantService);
    }

    @Test
    void returns429WhenTheUserAsksTooOften() throws Exception {
        doThrow(new AssistantRateLimitException("You can ask up to 5 questions a minute. Please wait a moment."))
                .when(rateLimiter).check("alice");

        mockMvc.perform(chat(TestJwtIssuer.userToken(), QUESTION))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("AI-429"));

        verifyNoInteractions(assistantService);
    }
}
