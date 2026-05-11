package org.example.expert.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import org.example.expert.config.interceptor.CheckAdminInterceptor;
import org.example.expert.domain.comment.controller.CommentAdminController;
import org.example.expert.domain.comment.service.CommentAdminService;
import org.example.expert.domain.user.controller.UserAdminController;
import org.example.expert.domain.user.dto.request.UserRoleChangeRequest;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.service.UserAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        UserAdminController.class,
        CommentAdminController.class
})
@Import({
        JwtUtil.class,
        GlobalExceptionHandler.class,
        WebConfig.class,
        AuthUserArgumentResolver.class,
        CheckAdminInterceptor.class,
        AdminAuthorizationWebMvcTest.TestFilterConfig.class
})
@TestPropertySource(properties = "jwt.secret.key=dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGVzdHRlc3Q=")
class AdminAuthorizationWebMvcTest {

    private static final long USER_ID = 1L;
    private static final long ADMIN_ID = 2L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private UserAdminService userAdminService;

    @MockBean
    private CommentAdminService commentAdminService;

    @Test
    void USER_토큰으로_admin_user_역할_변경_요청시_WebConfig_interceptor가_403을_반환한다() throws Exception {
        UserRoleChangeRequest request = new UserRoleChangeRequest("ADMIN");

        mockMvc.perform(patch("/admin/users/{userId}", 10L)
                        .header("Authorization", userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Invalid Authorization. Need Admin role to access"));

        then(userAdminService).should(never()).changeUserRole(anyLong(), any(UserRoleChangeRequest.class));
    }

    @Test
    void USER_토큰으로_admin_comment_삭제_요청시_WebConfig_interceptor가_403을_반환한다() throws Exception {
        mockMvc.perform(delete("/admin/comments/{commentId}", 20L)
                        .header("Authorization", userToken()))
                .andExpect(status().isForbidden());

        then(commentAdminService).should(never()).deleteComment(anyLong());
    }

    @Test
    void ADMIN_토큰으로_admin_user_역할_변경_요청시_interceptor를_통과하고_service를_호출한다() throws Exception {
        UserRoleChangeRequest request = new UserRoleChangeRequest("USER");

        mockMvc.perform(patch("/admin/users/{userId}", 10L)
                        .header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        then(userAdminService).should().changeUserRole(eq(10L), any(UserRoleChangeRequest.class));
    }

    @Test
    void ADMIN_토큰으로_admin_comment_삭제_요청시_interceptor를_통과하고_service를_호출한다() throws Exception {
        mockMvc.perform(delete("/admin/comments/{commentId}", 20L)
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk());

        then(commentAdminService).should().deleteComment(20L);
    }

    @Test
    void 인증_헤더_없이_admin_api_요청시_401을_반환한다() throws Exception {
        UserRoleChangeRequest request = new UserRoleChangeRequest("ADMIN");

        mockMvc.perform(patch("/admin/users/{userId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        then(userAdminService).should(never()).changeUserRole(anyLong(), any(UserRoleChangeRequest.class));
    }

    private String userToken() {
        return jwtUtil.createToken(USER_ID, "user@example.com", UserRole.USER);
    }

    private String adminToken() {
        return jwtUtil.createToken(ADMIN_ID, "admin@example.com", UserRole.ADMIN);
    }

    @TestConfiguration
    static class TestFilterConfig {

        @Bean
        Filter jwtFilter(JwtUtil jwtUtil, ObjectMapper objectMapper) {
            return new JwtFilter(jwtUtil, objectMapper);
        }
    }
}
