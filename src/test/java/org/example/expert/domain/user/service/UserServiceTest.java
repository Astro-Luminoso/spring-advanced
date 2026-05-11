package org.example.expert.domain.user.service;

import org.example.expert.config.PasswordEncoder;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.dto.request.UserChangePasswordRequest;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserService userService;

    @Test
    void getUser는_유저가_없으면_InvalidRequestException을_던진다() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> userService.getUser(1L));

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void getUser는_유저_응답을_반환한다() {
        User user = new User("user@example.com", "encoded", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UserResponse response = userService.getUser(1L);

        assertEquals(1L, response.getId());
        assertEquals("user@example.com", response.getEmail());
    }

    @Test
    void changePassword는_유저가_없으면_InvalidRequestException을_던진다() {
        UserChangePasswordRequest request = new UserChangePasswordRequest("oldPassword", "NewPass1");
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> userService.changePassword(1L, request));

        assertEquals("User not found", exception.getMessage());
        then(passwordEncoder).should(never()).matches(any(), any());
    }

    @Test
    void changePassword는_새_비밀번호가_기존_비밀번호와_같으면_InvalidRequestException을_던진다() {
        User user = new User("user@example.com", "encodedOldPassword", UserRole.USER);
        UserChangePasswordRequest request = new UserChangePasswordRequest("oldPassword", "NewPass1");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.getNewPassword(), user.getPassword())).willReturn(true);

        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> userService.changePassword(1L, request));

        assertEquals("새 비밀번호는 기존 비밀번호와 같을 수 없습니다.", exception.getMessage());
        then(passwordEncoder).should(never()).encode(any());
    }

    @Test
    void changePassword는_기존_비밀번호가_틀리면_InvalidRequestException을_던진다() {
        User user = new User("user@example.com", "encodedOldPassword", UserRole.USER);
        UserChangePasswordRequest request = new UserChangePasswordRequest("wrongPassword", "NewPass1");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.getNewPassword(), user.getPassword())).willReturn(false);
        given(passwordEncoder.matches(request.getOldPassword(), user.getPassword())).willReturn(false);

        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> userService.changePassword(1L, request));

        assertEquals("잘못된 비밀번호입니다.", exception.getMessage());
        then(passwordEncoder).should(never()).encode(any());
    }

    @Test
    void changePassword는_검증을_통과하면_인코딩된_새_비밀번호로_변경한다() {
        User user = new User("user@example.com", "encodedOldPassword", UserRole.USER);
        UserChangePasswordRequest request = new UserChangePasswordRequest("oldPassword", "NewPass1");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.getNewPassword(), user.getPassword())).willReturn(false);
        given(passwordEncoder.matches(request.getOldPassword(), user.getPassword())).willReturn(true);
        given(passwordEncoder.encode(request.getNewPassword())).willReturn("encodedNewPassword");

        userService.changePassword(1L, request);

        assertEquals("encodedNewPassword", user.getPassword());
    }
}
