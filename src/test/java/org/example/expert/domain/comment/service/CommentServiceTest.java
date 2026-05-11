package org.example.expert.domain.comment.service;

import org.example.expert.domain.comment.dto.request.CommentSaveRequest;
import org.example.expert.domain.comment.dto.response.CommentSaveResponse;
import org.example.expert.domain.comment.entity.Comment;
import org.example.expert.domain.comment.repository.CommentRepository;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private TodoRepository todoRepository;
    @InjectMocks
    private CommentService commentService;

    @Test
    public void comment_등록_중_할일을_찾지_못해_에러가_발생한다() {
        // given
        long todoId = 1;
        CommentSaveRequest request = new CommentSaveRequest("contents");
        AuthUser authUser = new AuthUser(1L, "email", UserRole.USER);

        given(todoRepository.findById(anyLong())).willReturn(Optional.empty());

        // when
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
            commentService.saveComment(authUser, todoId, request);
        });

        // then
        assertEquals("Todo not found", exception.getMessage());
    }

    @Test
    public void comment를_정상적으로_등록한다() {
        // given
        long todoId = 1;
        CommentSaveRequest request = new CommentSaveRequest("contents");
        AuthUser authUser = new AuthUser(1L, "email", UserRole.USER);
        User user = User.fromAuthUser(authUser);
        Todo todo = new Todo("title", "title", "contents", user);
        Comment comment = new Comment(request.getContents(), user, todo);
        ReflectionTestUtils.setField(comment, "id", 10L);

        given(todoRepository.findById(anyLong())).willReturn(Optional.of(todo));
        given(commentRepository.save(any())).willReturn(comment);

        // when
        CommentSaveResponse result = commentService.saveComment(authUser, todoId, request);

        // then
        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("contents", result.getContents());
        assertEquals(authUser.getId(), result.getUser().getId());
        assertEquals(authUser.getEmail(), result.getUser().getEmail());
    }

    @Test
    void comment_목록_조회에_성공한다() {
        // given
        long todoId = 1L;
        User user = new User("user@example.com", "password", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", 10L);
        Todo todo = new Todo("title", "contents", "Sunny", user);
        Comment comment = new Comment("contents", user, todo);
        ReflectionTestUtils.setField(comment, "id", 20L);
        given(commentRepository.findByTodoIdWithUser(todoId)).willReturn(List.of(comment));

        // when
        var responses = commentService.getComments(todoId);

        // then
        assertEquals(1, responses.size());
        assertEquals(20L, responses.get(0).getId());
        assertEquals("contents", responses.get(0).getContents());
        assertEquals(10L, responses.get(0).getUser().getId());
        assertEquals("user@example.com", responses.get(0).getUser().getEmail());
    }

    @Test
    void comment_목록이_없으면_빈_목록을_반환한다() {
        // given
        long todoId = 1L;
        given(commentRepository.findByTodoIdWithUser(todoId)).willReturn(List.of());

        // when
        var responses = commentService.getComments(todoId);

        // then
        assertTrue(responses.isEmpty());
        then(commentRepository).should().findByTodoIdWithUser(todoId);
    }
}
