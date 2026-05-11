package org.example.expert.config;

import org.example.expert.domain.common.exception.ServerException;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", "dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGVzdHRlc3Q=");
        jwtUtil.init();
    }

    @Test
    void createToken은_Bearer_prefix와_claims를_포함한_토큰을_생성한다() {
        String bearerToken = jwtUtil.createToken(1L, "admin@example.com", UserRole.ADMIN);

        assertTrue(bearerToken.startsWith("Bearer "));
        Object claims = ReflectionTestUtils.invokeMethod(jwtUtil, "extractClaims", jwtUtil.substringToken(bearerToken));
        assertEquals("1", ReflectionTestUtils.invokeMethod(claims, "getSubject"));
        assertEquals("admin@example.com", ((Map<?, ?>) claims).get("email"));
        assertEquals("ADMIN", ((Map<?, ?>) claims).get("userRole"));
    }

    @Test
    void substringToken은_Bearer_prefix가_있으면_토큰만_반환한다() {
        assertEquals("abc.def.ghi", jwtUtil.substringToken("Bearer abc.def.ghi"));
    }

    @Test
    void substringToken은_토큰이_없거나_prefix가_다르면_ServerException을_던진다() {
        assertThrows(ServerException.class, () -> jwtUtil.substringToken(null));
        assertThrows(ServerException.class, () -> jwtUtil.substringToken("abc.def.ghi"));
    }
}
