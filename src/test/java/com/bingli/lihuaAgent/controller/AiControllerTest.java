package com.bingli.lihuaAgent.controller;

import com.bingli.lihuaAgent.module.ai.api.AiChatService;
import com.bingli.lihuaAgent.module.ai.api.AiStreamChatService;
import com.bingli.lihuaAgent.module.ai.api.model.AiChatResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiControllerTest {

    private MockMvc mockMvc;

    private AiChatService aiChatService;

    private AiStreamChatService aiStreamChatService;

    @BeforeEach
    void setUp() {
        aiChatService = mock(AiChatService.class);
        aiStreamChatService = mock(AiStreamChatService.class);
        AiController aiController = new AiController(aiChatService, aiStreamChatService);
        mockMvc = MockMvcBuilders.standaloneSetup(aiController).build();
    }

    @Test
    void shouldMapModuleResultToHttpResponse() throws Exception {
        given(aiChatService.chat(anyString())).willReturn(AiChatResult.builder()
                .content("模块响应")
                .modelAlias("primary")
                .modelName("gpt-4o-mini")
                .fallbackUsed(false)
                .costMs(12L)
                .totalTokens(9)
                .build());

        mockMvc.perform(post("/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"你好\",\"systemPrompt\":\"你是助手\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").value("模块响应"))
                .andExpect(jsonPath("$.data.modelAlias").value("primary"))
                .andExpect(jsonPath("$.data.fallbackUsed").value(false));
    }

    @Test
    void shouldExposeSseStreamingEndpoint() throws Exception {
        given(aiStreamChatService.streamChat(anyString())).willReturn(new SseEmitter());

        MvcResult result = mockMvc.perform(post("/ai/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"你好\"}"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        assertEquals("no", result.getResponse().getHeader("X-Accel-Buffering"));
        assertEquals("no-cache", result.getResponse().getHeader("Cache-Control"));
    }
}
