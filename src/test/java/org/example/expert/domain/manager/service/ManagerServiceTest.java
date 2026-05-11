package org.example.expert.domain.manager.service;

import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.manager.dto.request.ManagerSaveRequest;
import org.example.expert.domain.manager.dto.response.ManagerResponse;
import org.example.expert.domain.manager.dto.response.ManagerSaveResponse;
import org.example.expert.domain.manager.entity.Manager;
import org.example.expert.domain.manager.repository.ManagerRepository;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ManagerServiceTest {

    @Mock
    private ManagerRepository managerRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TodoRepository todoRepository;
    @InjectMocks
    private ManagerService managerService;

    @Test
    public void manager_목록_조회_시_Todo가_없다면_InvalidRequestException을_던진다() {
        // given
        long todoId = 1L;
        given(todoRepository.findById(todoId)).willReturn(Optional.empty());

        // when & then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> managerService.getManagers(todoId));
        assertEquals("Todo not found", exception.getMessage());
    }

    @Test
    void todo의_user가_null인_경우_예외가_발생한다() {
        // given
        AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
        long todoId = 1L;
        long managerUserId = 2L;

        Todo todo = new Todo();
        ReflectionTestUtils.setField(todo, "user", null);

        ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId);

        given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));

        // when & then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
            managerService.saveManager(authUser, todoId, managerSaveRequest)
        );

        assertEquals("일정을 생성한 유저만 담당자를 지정할 수 있습니다.", exception.getMessage());
    }

    @Test // 테스트코드 샘플
    public void manager_목록_조회에_성공한다() {
        // given
        long todoId = 1L;
        User user = new User("user1@example.com", "password", UserRole.USER);
        Todo todo = new Todo("Title", "Contents", "Sunny", user);
        ReflectionTestUtils.setField(todo, "id", todoId);

        Manager mockManager = new Manager(todo.getUser(), todo);
        ReflectionTestUtils.setField(mockManager, "id", 10L);
        List<Manager> managerList = List.of(mockManager);

        given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
        given(managerRepository.findByTodoIdWithUser(todoId)).willReturn(managerList);

        // when
        List<ManagerResponse> managerResponses = managerService.getManagers(todoId);

        // then
        assertEquals(1, managerResponses.size());
        assertEquals(10L, managerResponses.get(0).getId());
        assertEquals(mockManager.getUser().getEmail(), managerResponses.get(0).getUser().getEmail());
    }

    @Test // 테스트코드 샘플
    void todo가_정상적으로_등록된다() {
        // given
        AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
        User user = User.fromAuthUser(authUser);  // 일정을 만든 유저

        long todoId = 1L;
        Todo todo = new Todo("Test Title", "Test Contents", "Sunny", user);

        long managerUserId = 2L;
        User managerUser = new User("b@b.com", "password", UserRole.USER);  // 매니저로 등록할 유저
        ReflectionTestUtils.setField(managerUser, "id", managerUserId);

        ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId); // request dto 생성

        given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
        given(userRepository.findById(managerUserId)).willReturn(Optional.of(managerUser));
        given(managerRepository.save(any(Manager.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ManagerSaveResponse response = managerService.saveManager(authUser, todoId, managerSaveRequest);

        // then
        assertNotNull(response);
        assertEquals(managerUser.getId(), response.getUser().getId());
        assertEquals(managerUser.getEmail(), response.getUser().getEmail());
    }

    @Test
    void manager_등록_시_Todo가_없다면_InvalidRequestException을_던진다() {
        // given
        AuthUser authUser = new AuthUser(1L, "owner@example.com", UserRole.USER);
        ManagerSaveRequest request = new ManagerSaveRequest(2L);
        given(todoRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> managerService.saveManager(authUser, 1L, request));
        assertEquals("Todo not found", exception.getMessage());
        then(userRepository).should(never()).findById(any());
        then(managerRepository).should(never()).save(any());
    }

    @Test
    void manager_등록_시_일정_작성자가_아니면_InvalidRequestException을_던진다() {
        // given
        AuthUser authUser = new AuthUser(1L, "requester@example.com", UserRole.USER);
        User owner = new User("owner@example.com", "password", UserRole.USER);
        ReflectionTestUtils.setField(owner, "id", 2L);
        Todo todo = new Todo("title", "contents", "Sunny", owner);
        given(todoRepository.findById(1L)).willReturn(Optional.of(todo));

        // when & then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> managerService.saveManager(authUser, 1L, new ManagerSaveRequest(3L)));
        assertEquals("일정을 생성한 유저만 담당자를 지정할 수 있습니다.", exception.getMessage());
        then(userRepository).should(never()).findById(any());
        then(managerRepository).should(never()).save(any());
    }

    @Test
    void manager_등록_시_담당자_유저가_없다면_InvalidRequestException을_던진다() {
        // given
        AuthUser authUser = new AuthUser(1L, "owner@example.com", UserRole.USER);
        User owner = User.fromAuthUser(authUser);
        Todo todo = new Todo("title", "contents", "Sunny", owner);
        given(todoRepository.findById(1L)).willReturn(Optional.of(todo));
        given(userRepository.findById(2L)).willReturn(Optional.empty());

        // when & then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> managerService.saveManager(authUser, 1L, new ManagerSaveRequest(2L)));
        assertEquals("등록하려고 하는 담당자 유저가 존재하지 않습니다.", exception.getMessage());
        then(managerRepository).should(never()).save(any());
    }

    @Test
    void manager_등록_시_작성자_본인을_담당자로_등록하면_InvalidRequestException을_던진다() {
        // given
        AuthUser authUser = new AuthUser(1L, "owner@example.com", UserRole.USER);
        User owner = User.fromAuthUser(authUser);
        Todo todo = new Todo("title", "contents", "Sunny", owner);
        User managerUser = new User("owner@example.com", "password", UserRole.USER);
        ReflectionTestUtils.setField(managerUser, "id", 1L);

        given(todoRepository.findById(1L)).willReturn(Optional.of(todo));
        given(userRepository.findById(1L)).willReturn(Optional.of(managerUser));

        // when & then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> managerService.saveManager(authUser, 1L, new ManagerSaveRequest(1L)));
        assertEquals("일정 작성자는 본인을 담당자로 등록할 수 없습니다.", exception.getMessage());
        then(managerRepository).should(never()).save(any());
    }

    @Test
    void manager_삭제_시_User가_없다면_InvalidRequestException을_던진다() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> managerService.deleteManager(1L, 10L, 100L));
        assertEquals("User not found", exception.getMessage());
        then(todoRepository).should(never()).findById(any());
    }

    @Test
    void manager_삭제_시_Todo가_없다면_InvalidRequestException을_던진다() {
        // given
        User user = new User("owner@example.com", "password", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(todoRepository.findById(10L)).willReturn(Optional.empty());

        // when & then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> managerService.deleteManager(1L, 10L, 100L));
        assertEquals("Todo not found", exception.getMessage());
        then(managerRepository).should(never()).findById(any());
    }

    @Test
    void manager_삭제_시_Todo의_user가_null이면_InvalidRequestException을_던진다() {
        // given
        User user = new User("owner@example.com", "password", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        Todo todo = new Todo();
        ReflectionTestUtils.setField(todo, "user", null);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(todoRepository.findById(10L)).willReturn(Optional.of(todo));

        // when & then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> managerService.deleteManager(1L, 10L, 100L));
        assertEquals("해당 일정을 만든 유저가 유효하지 않습니다.", exception.getMessage());
        then(managerRepository).should(never()).findById(any());
    }

    @Test
    void manager_삭제_시_일정_작성자가_아니면_InvalidRequestException을_던진다() {
        // given
        User requester = new User("requester@example.com", "password", UserRole.USER);
        ReflectionTestUtils.setField(requester, "id", 1L);
        User owner = new User("owner@example.com", "password", UserRole.USER);
        ReflectionTestUtils.setField(owner, "id", 2L);
        Todo todo = new Todo("title", "contents", "Sunny", owner);

        given(userRepository.findById(1L)).willReturn(Optional.of(requester));
        given(todoRepository.findById(10L)).willReturn(Optional.of(todo));

        // when & then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> managerService.deleteManager(1L, 10L, 100L));
        assertEquals("해당 일정을 만든 유저가 유효하지 않습니다.", exception.getMessage());
        then(managerRepository).should(never()).findById(any());
    }

    @Test
    void manager_삭제_시_Manager가_없다면_InvalidRequestException을_던진다() {
        // given
        User owner = new User("owner@example.com", "password", UserRole.USER);
        ReflectionTestUtils.setField(owner, "id", 1L);
        Todo todo = new Todo("title", "contents", "Sunny", owner);

        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(todoRepository.findById(10L)).willReturn(Optional.of(todo));
        given(managerRepository.findById(100L)).willReturn(Optional.empty());

        // when & then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> managerService.deleteManager(1L, 10L, 100L));
        assertEquals("Manager not found", exception.getMessage());
    }

    @Test
    void manager_삭제_시_해당_일정의_담당자가_아니면_InvalidRequestException을_던진다() {
        // given
        User owner = new User("owner@example.com", "password", UserRole.USER);
        ReflectionTestUtils.setField(owner, "id", 1L);
        Todo todo = new Todo("title", "contents", "Sunny", owner);
        ReflectionTestUtils.setField(todo, "id", 10L);
        Todo otherTodo = new Todo("other", "contents", "Sunny", owner);
        ReflectionTestUtils.setField(otherTodo, "id", 20L);
        Manager manager = new Manager(new User("manager@example.com", "password", UserRole.USER), otherTodo);

        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(todoRepository.findById(10L)).willReturn(Optional.of(todo));
        given(managerRepository.findById(100L)).willReturn(Optional.of(manager));

        // when & then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> managerService.deleteManager(1L, 10L, 100L));
        assertEquals("해당 일정에 등록된 담당자가 아닙니다.", exception.getMessage());
        then(managerRepository).should(never()).delete(any());
    }

    @Test
    void manager_삭제에_성공한다() {
        // given
        User owner = new User("owner@example.com", "password", UserRole.USER);
        ReflectionTestUtils.setField(owner, "id", 1L);
        User managerUser = new User("manager@example.com", "password", UserRole.USER);
        ReflectionTestUtils.setField(managerUser, "id", 2L);
        Todo todo = new Todo("title", "contents", "Sunny", owner);
        ReflectionTestUtils.setField(todo, "id", 10L);
        Manager manager = new Manager(managerUser, todo);
        ReflectionTestUtils.setField(manager, "id", 100L);

        given(userRepository.findById(1L)).willReturn(Optional.of(owner));
        given(todoRepository.findById(10L)).willReturn(Optional.of(todo));
        given(managerRepository.findById(100L)).willReturn(Optional.of(manager));

        // when
        managerService.deleteManager(1L, 10L, 100L);

        // then
        then(managerRepository).should().delete(manager);
    }
}
