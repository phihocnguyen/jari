package com.example.jari.reference.controller;

import com.example.jari.reference.dto.ReferenceItemResponse;
import com.example.jari.reference.service.ReferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReferenceControllerTest {

    @Mock
    private ReferenceService referenceService;

    @InjectMocks
    private ReferenceController referenceController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(referenceController).build();
    }

    @Test
    void getIssueTypes_returnsWrappedApiResponse() throws Exception {
        UUID id = UUID.randomUUID();
        when(referenceService.getIssueTypes()).thenReturn(List.of(
            ReferenceItemResponse.builder().id(id).name("Bug").description("Defect").build()
        ));

        mockMvc.perform(get("/api/v1/ref/issue-types"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Success"))
            .andExpect(jsonPath("$.data[0].id").value(id.toString()))
            .andExpect(jsonPath("$.data[0].name").value("Bug"));
    }

    @Test
    void getStatuses_returnsOk() throws Exception {
        when(referenceService.getStatuses()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/ref/statuses"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
    }
}
