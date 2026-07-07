package com.campustrade;

import com.campustrade.common.Result;
import com.campustrade.common.ResultCode;
import com.campustrade.controller.AuthController;
import com.campustrade.dto.LoginDTO;
import com.campustrade.dto.RegisterDTO;
import com.campustrade.service.AuthService;
import com.campustrade.vo.TokenVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CampusTradeApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
    }

    @Test
    void testRegister() throws Exception {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("testuser_" + System.currentTimeMillis());
        dto.setPassword("test123456");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void testLoginFail() throws Exception {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("notexist");
        dto.setPassword("wrong");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1004));
    }

    @Test
    void testGoodsListPublic() throws Exception {
        mockMvc.perform(get("/api/goods")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void testHotGoodsPublic() throws Exception {
        mockMvc.perform(get("/api/goods/hot"))
                .andExpect(status().isOk());
    }

    @Test
    void testRecommendGoodsPublic() throws Exception {
        mockMvc.perform(get("/api/goods/recommend"))
                .andExpect(status().isOk());
    }

    @Test
    void testCategoryListPublic() throws Exception {
        mockMvc.perform(get("/api/goods-category"))
                .andExpect(status().isOk());
    }

    @Test
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(post("/api/goods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}