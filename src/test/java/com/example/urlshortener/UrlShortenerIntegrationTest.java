package com.example.urlshortener;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UrlShortenerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    private String create(String json) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.shortCode");
    }

    @Test
    void shouldCreateRedirectAndCountClicks() throws Exception {
        String code = create("{\"url\":\"https://example.com/docs\"}");

        mockMvc.perform(get("/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/docs"));

        mockMvc.perform(get("/api/v1/urls/" + code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/docs"))
                .andExpect(jsonPath("$.clickCount").value(1));
    }

    @Test
    void shouldReturn404ForUnknownCode() throws Exception {
        mockMvc.perform(get("/nope123")).andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400ForInvalidUrl() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"not-a-url\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn409ForDuplicateAlias() throws Exception {
        String body = "{\"url\":\"https://example.com\",\"customAlias\":\"my-alias\"}";
        create(body);
        mockMvc.perform(post("/api/v1/urls").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldDeleteShortUrl() throws Exception {
        String code = create("{\"url\":\"https://example.com/delete-me\"}");
        mockMvc.perform(delete("/api/v1/urls/" + code)).andExpect(status().isNoContent());
        mockMvc.perform(get("/" + code)).andExpect(status().isNotFound());
    }

    @Test
    void shouldExposePrometheusMetrics() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("urlshortener_urls_shortened_total")))
                .andExpect(content().string(containsString("urlshortener_redirects_total")));
    }
}
