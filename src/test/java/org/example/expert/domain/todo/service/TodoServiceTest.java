package org.example.expert.domain.todo.service;

import org.example.expert.client.WeatherClient;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @Mock
    private TodoRepository todoRepository;
    @Mock
    private WeatherClient weatherClient;
    @InjectMocks
    private TodoService todoService;

    @Test
    void saveTodo는_오늘_날씨와_인증_유저로_Todo를_저장하고_응답한다() {
        AuthUser authUser = new AuthUser(1L, "user@example.com", UserRole.USER);
        TodoSaveRequest request = new TodoSaveRequest("title", "contents");
        given(weatherClient.getTodayWeather()).willReturn("Sunny");
        given(todoRepository.save(any(Todo.class))).willAnswer(invocation -> {
            Todo todo = invocation.getArgument(0);
            ReflectionTestUtils.setField(todo, "id", 100L);
            return todo;
        });

        TodoSaveResponse response = todoService.saveTodo(authUser, request);

        assertEquals(100L, response.getId());
        assertEquals("title", response.getTitle());
        assertEquals("contents", response.getContents());
        assertEquals("Sunny", response.getWeather());
        assertEquals(authUser.getId(), response.getUser().getId());
        assertEquals(authUser.getEmail(), response.getUser().getEmail());

        ArgumentCaptor<Todo> todoCaptor = ArgumentCaptor.forClass(Todo.class);
        then(todoRepository).should().save(todoCaptor.capture());
        assertEquals("title", todoCaptor.getValue().getTitle());
        assertEquals("contents", todoCaptor.getValue().getContents());
        assertEquals("Sunny", todoCaptor.getValue().getWeather());
        assertEquals(authUser.getId(), todoCaptor.getValue().getUser().getId());
    }

    @Test
    void getTodos는_1부터_시작하는_page를_0부터_시작하는_PageRequest로_변환하고_응답을_매핑한다() {
        User user = new User("user@example.com", "password", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        Todo todo = new Todo("title", "contents", "Cloudy", user);
        ReflectionTestUtils.setField(todo, "id", 10L);

        given(todoRepository.findAllByOrderByModifiedAtDesc(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(todo)));

        Page<TodoResponse> response = todoService.getTodos(2, 5);

        assertEquals(1, response.getTotalElements());
        assertEquals(10L, response.getContent().get(0).getId());
        assertEquals("title", response.getContent().get(0).getTitle());
        assertEquals("contents", response.getContent().get(0).getContents());
        assertEquals("Cloudy", response.getContent().get(0).getWeather());
        assertEquals(user.getEmail(), response.getContent().get(0).getUser().getEmail());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(todoRepository).should().findAllByOrderByModifiedAtDesc(pageableCaptor.capture());
        assertEquals(1, pageableCaptor.getValue().getPageNumber());
        assertEquals(5, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void getTodo는_Todo가_없으면_InvalidRequestException을_던진다() {
        given(todoRepository.findByIdWithUser(10L)).willReturn(Optional.empty());

        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> todoService.getTodo(10L));

        assertEquals("Todo not found", exception.getMessage());
    }

    @Test
    void getTodo는_Todo와_작성자_정보를_응답으로_매핑한다() {
        User user = new User("user@example.com", "password", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        Todo todo = new Todo("title", "contents", "Rainy", user);
        ReflectionTestUtils.setField(todo, "id", 10L);
        given(todoRepository.findByIdWithUser(10L)).willReturn(Optional.of(todo));

        TodoResponse response = todoService.getTodo(10L);

        assertEquals(10L, response.getId());
        assertEquals("title", response.getTitle());
        assertEquals("contents", response.getContents());
        assertEquals("Rainy", response.getWeather());
        assertEquals(user.getId(), response.getUser().getId());
        assertEquals(user.getEmail(), response.getUser().getEmail());
    }
}
