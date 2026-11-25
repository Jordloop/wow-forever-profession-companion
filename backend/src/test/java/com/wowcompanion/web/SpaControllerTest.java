package com.wowcompanion.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class SpaControllerTest {
    @Test
    void forwardsEveryWorkspaceRouteToTheAngularApplicationShell() throws Exception {
        MockMvc mockMvc = standaloneSetup(new SpaController()).build();

        for (String route : new String[] {
                "/overview",
                "/professions",
                "/market",
                "/opportunities",
                "/inventory",
                "/characters",
                "/reference-data",
                "/settings"
        }) {
            mockMvc.perform(get(route))
                    .andExpect(status().isOk())
                    .andExpect(forwardedUrl("/index.html"));
        }
    }
}
