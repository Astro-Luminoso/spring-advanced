package org.example.expert.domain.user.service;

import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.dto.request.UserRoleChangeRequest;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private UserAdminService userAdminService;

    @Test
    void changeUserRole은_유저가_없으면_InvalidRequestException을_던진다() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> userAdminService.changeUserRole(1L, new UserRoleChangeRequest("ADMIN")));

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void changeUserRole은_유효하지_않은_role이면_InvalidRequestException을_던진다() {
        User user = new User("user@example.com", "password", UserRole.USER);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> userAdminService.changeUserRole(1L, new UserRoleChangeRequest("INVALID")));

        assertEquals("유효하지 않은 UerRole", exception.getMessage());
        assertEquals(UserRole.USER, user.getUserRole());
    }

    @Test
    void changeUserRole은_대소문자와_무관하게_role을_변경한다() {
        User user = new User("user@example.com", "password", UserRole.USER);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        userAdminService.changeUserRole(1L, new UserRoleChangeRequest("admin"));

        assertEquals(UserRole.ADMIN, user.getUserRole());
    }
}
