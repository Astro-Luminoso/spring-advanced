package org.example.expert.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import org.example.expert.config.interceptor.CheckAdminInterceptor;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.todo.controller.TodoController;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.service.TodoService;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TodoController.class)
@Import({
        JwtUtil.class,
        GlobalExceptionHandler.class,
        WebConfig.class,
        AuthUserArgumentResolver.class,
        CheckAdminInterceptor.class,
        AuthUserMvcArgumentResolutionTest.TestFilterConfig.class
})
@TestPropertySource(properties = "jwt.secret.key=dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGVzdHRlc3Q=")
class AuthUserMvcArgumentResolutionTest {

    private static final long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private TodoService todoService;

    @Test
    void 인증_토큰이_있으면_JWT_filter와_Auth_argument_resolver를_거쳐_controller가_서비스를_호출한다() throws Exception {
        TodoSaveRequest request = new TodoSaveRequest("title", "contents");
        given(todoService.saveTodo(any(AuthUser.class), any(TodoSaveRequest.class)))
                .willReturn(new TodoSaveResponse(10L, "title", "contents", "Sunny",
                        new UserResponse(USER_ID, "user@example.com")));

        mockMvc.perform(post("/todos")
                        .header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));

        ArgumentCaptor<AuthUser> authUserCaptor = ArgumentCaptor.forClass(AuthUser.class);
        ArgumentCaptor<TodoSaveRequest> requestCaptor = ArgumentCaptor.forClass(TodoSaveRequest.class);
        then(todoService).should().saveTodo(authUserCaptor.capture(), requestCaptor.capture());
        assertEquals(USER_ID, authUserCaptor.getValue().getId());
        assertEquals("user@example.com", authUserCaptor.getValue().getEmail());
        assertEquals(UserRole.USER, authUserCaptor.getValue().getUserRole());
        assertEquals("title", requestCaptor.getValue().getTitle());
        assertEquals("contents", requestCaptor.getValue().getContents());
    }

    @Test
    void 인증_헤더_없이_보호된_API_요청시_401을_반환하고_controller를_호출하지_않는다() throws Exception {
        TodoSaveRequest request = new TodoSaveRequest("title", "contents");

        mockMvc.perform(post("/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));

        then(todoService).should(never()).saveTodo(any(AuthUser.class), any(TodoSaveRequest.class));
    }

    @Test
    void 잘못된_JWT로_보호된_API_요청시_400을_반환하고_controller를_호출하지_않는다() throws Exception {
        TodoSaveRequest request = new TodoSaveRequest("title", "contents");

        mockMvc.perform(post("/todos")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));

        then(todoService).should(never()).saveTodo(any(AuthUser.class), any(TodoSaveRequest.class));
    }

    @Test
    void 인증된_요청의_validation_실패는_400_응답으로_변환하고_service를_호출하지_않는다() throws Exception {
        TodoSaveRequest request = new TodoSaveRequest("", "contents");

        mockMvc.perform(post("/todos")
                        .header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.code").value(400));

        then(todoService).should(never()).saveTodo(any(AuthUser.class), any(TodoSaveRequest.class));
    }

    private String userToken() {
        return jwtUtil.createToken(USER_ID, "user@example.com", UserRole.USER);
    }

    @TestConfiguration
    static class TestFilterConfig {

        @Bean
        Filter jwtFilter(JwtUtil jwtUtil, ObjectMapper objectMapper) {
            return new JwtFilter(jwtUtil, objectMapper);
        }
    }
}
