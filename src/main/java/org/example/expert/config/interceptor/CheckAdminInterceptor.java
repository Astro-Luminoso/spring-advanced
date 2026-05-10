package org.example.expert.config.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.expert.domain.common.exception.InvalidAuthorizationException;
import org.example.expert.domain.user.enums.UserRole;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class CheckAdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userRole = (String) request.getAttribute("userRole");
        if(!userRole.equals(UserRole.ADMIN.name())) {
            log.warn("권한 부족: userId={}, role={}, URI={}", request.getAttribute("userId"), userRole, request.getRequestURI());
            throw new InvalidAuthorizationException("Invalid Authorization. Need Admin role to access");
        }
        log.info("접근 승인: userId={}, role={}, URI={}", request.getAttribute("userId"), userRole, request.getRequestURI());
        return true;

    }

}
