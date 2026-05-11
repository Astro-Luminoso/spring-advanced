package org.example.expert.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class FilterConfigTest {

    @Test
    void jwtFilter는_모든_URL에_등록된다() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", "dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGVzdHRlc3Q=");
        jwtUtil.init();
        FilterConfig filterConfig = new FilterConfig(jwtUtil, new ObjectMapper());

        FilterRegistrationBean<JwtFilter> registrationBean = filterConfig.jwtFilter();

        assertInstanceOf(JwtFilter.class, registrationBean.getFilter());
        assertEquals(1, registrationBean.getUrlPatterns().size());
        assertEquals("/*", registrationBean.getUrlPatterns().iterator().next());
    }
}
