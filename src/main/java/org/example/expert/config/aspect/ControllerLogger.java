package org.example.expert.config.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class ControllerLogger {

    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Around("execution(* *..controller..*(..))")
    public Object logAdminApiRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = getHttpServletRequest();

        if (request == null || !request.getRequestURI().startsWith("/admin")) {
            return joinPoint.proceed();
        }

        Long userId = (Long) request.getAttribute("userId");
        String requestTime = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        String requestUrl = request.getRequestURI();
        String requestBody = extractRequestBody(joinPoint);

        log.info("=== Admin API Request ===");
        log.info("ADMIN REQUEST: id={}, time={}, url={}, requestBody={}", userId, requestTime, requestUrl, requestBody);
        try {
            Object result = joinPoint.proceed();

            String responseBody = serializeResponseBody(result);
            String responseTime = LocalDateTime.now().format(DATE_TIME_FORMATTER);
            log.info("ADMIN RESPONSE: id={}, time={}, url={}, responseBody={}", userId, responseTime, requestUrl, responseBody);


            return result;
        } catch (Exception e) {
            log.error("=== Admin API Error ===");
            log.error("오류: ", e);
            throw e;
        }
    }

    private HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest();
            }
        } catch (Exception e) {
            log.debug("HttpServletRequest 추출 실패", e);
        }
        return null;
    }

    private String extractRequestBody(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length == 0) {
            return "{}";
        }

        // 첫 번째 인자가 요청 본문으로 간주 (일반적으로 RequestBody가 기본 인자)
        return serializeResponseBody(args[0]);
    }

    private String serializeResponseBody(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            log.debug("직렬화 실패: {}", object.getClass().getSimpleName(), e);
            return object.toString();
        }
    }
}
