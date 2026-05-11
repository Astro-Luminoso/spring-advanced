package org.example.expert.domain.auth.service;

import org.example.expert.config.JwtUtil;
import org.example.expert.config.PasswordEncoder;
import org.example.expert.domain.auth.dto.request.SigninRequest;
import org.example.expert.domain.auth.dto.request.SignupRequest;
import org.example.expert.domain.auth.dto.response.SigninResponse;
import org.example.expert.domain.auth.dto.response.SignupResponse;
import org.example.expert.domain.auth.exception.AuthException;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @InjectMocks
    private AuthService authService;

    @Test
    void signup은_이미_존재하는_이메일이면_InvalidRequestException을_던진다() {
        SignupRequest request = new SignupRequest("user@example.com", "password", "USER");
        given(userRepository.existsByEmail(request.getEmail())).willReturn(true);

        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> authService.signup(request));

        assertEquals("이미 존재하는 이메일입니다.", exception.getMessage());
        then(passwordEncoder).should(never()).encode(any());
        then(userRepository).should(never()).save(any());
    }

    @Test
    void signup은_비밀번호를_인코딩하고_토큰을_반환한다() {
        SignupRequest request = new SignupRequest("user@example.com", "rawPassword", "user");
        User savedUser = new User(request.getEmail(), "encodedPassword", UserRole.USER);
        ReflectionTestUtils.setField(savedUser, "id", 10L);

        given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(passwordEncoder.encode(request.getPassword())).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(jwtUtil.createToken(10L, request.getEmail(), UserRole.USER)).willReturn("Bearer signup-token");

        SignupResponse response = authService.signup(request);

        assertEquals("Bearer signup-token", response.getBearerToken());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        then(userRepository).should().save(userCaptor.capture());
        assertEquals(request.getEmail(), userCaptor.getValue().getEmail());
        assertEquals("encodedPassword", userCaptor.getValue().getPassword());
        assertEquals(UserRole.USER, userCaptor.getValue().getUserRole());
    }

    @Test
    void signup은_유효하지_않은_role이면_저장하지_않고_InvalidRequestException을_던진다() {
        SignupRequest request = new SignupRequest("user@example.com", "rawPassword", "INVALID");
        given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(passwordEncoder.encode(request.getPassword())).willReturn("encodedPassword");

        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> authService.signup(request));

        assertEquals("유효하지 않은 UerRole", exception.getMessage());
        then(userRepository).should(never()).save(any());
        then(jwtUtil).should(never()).createToken(any(), any(), any());
    }

    @Test
    void signin은_가입되지_않은_유저이면_InvalidRequestException을_던진다() {
        SigninRequest request = new SigninRequest("missing@example.com", "password");
        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.empty());

        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> authService.signin(request));

        assertEquals("가입되지 않은 유저입니다.", exception.getMessage());
        then(passwordEncoder).should(never()).matches(any(), any());
    }

    @Test
    void signin은_비밀번호가_틀리면_AuthException을_던진다() {
        SigninRequest request = new SigninRequest("user@example.com", "wrongPassword");
        User user = new User(request.getEmail(), "encodedPassword", UserRole.USER);
        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.getPassword(), user.getPassword())).willReturn(false);

        AuthException exception = assertThrows(AuthException.class, () -> authService.signin(request));

        assertEquals("잘못된 비밀번호입니다.", exception.getMessage());
        then(jwtUtil).should(never()).createToken(any(), any(), any());
    }

    @Test
    void signin은_비밀번호가_맞으면_토큰을_반환한다() {
        SigninRequest request = new SigninRequest("admin@example.com", "password");
        User user = new User(request.getEmail(), "encodedPassword", UserRole.ADMIN);
        ReflectionTestUtils.setField(user, "id", 20L);

        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.getPassword(), user.getPassword())).willReturn(true);
        given(jwtUtil.createToken(20L, request.getEmail(), UserRole.ADMIN)).willReturn("Bearer signin-token");

        SigninResponse response = authService.signin(request);

        assertEquals("Bearer signin-token", response.getBearerToken());
    }
}
