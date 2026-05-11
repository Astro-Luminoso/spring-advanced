package org.example.expert.domain.common;

import org.example.expert.client.dto.WeatherDto;
import org.example.expert.domain.auth.dto.response.SigninResponse;
import org.example.expert.domain.auth.dto.response.SignupResponse;
import org.example.expert.domain.comment.dto.response.CommentResponse;
import org.example.expert.domain.comment.dto.response.CommentSaveResponse;
import org.example.expert.domain.comment.entity.Comment;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.manager.dto.response.ManagerResponse;
import org.example.expert.domain.manager.dto.response.ManagerSaveResponse;
import org.example.expert.domain.manager.entity.Manager;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.dto.response.UserSaveResponse;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomainObjectTest {

    @Test
    void User는_AuthUser에서_식별자_이메일_role을_복사하고_비밀번호와_role을_변경한다() {
        AuthUser authUser = new AuthUser(1L, "user@example.com", UserRole.USER);

        User user = User.fromAuthUser(authUser);
        user.changePassword("newPassword");
        user.updateRole(UserRole.ADMIN);

        assertEquals(1L, user.getId());
        assertEquals("user@example.com", user.getEmail());
        assertEquals("newPassword", user.getPassword());
        assertEquals(UserRole.ADMIN, user.getUserRole());
    }

    @Test
    void Todo_생성자는_작성자를_담당자_목록에_추가하고_update는_title과_contents를_바꾼다() {
        User user = new User("user@example.com", "password", UserRole.USER);

        Todo todo = new Todo("title", "contents", "Sunny", user);
        todo.update("updated title", "updated contents");

        assertEquals("updated title", todo.getTitle());
        assertEquals("updated contents", todo.getContents());
        assertEquals("Sunny", todo.getWeather());
        assertEquals(1, todo.getManagers().size());
        assertSame(user, todo.getManagers().get(0).getUser());
        assertSame(todo, todo.getManagers().get(0).getTodo());
    }

    @Test
    void Comment는_생성자와_update로_contents_user_todo를_관리한다() {
        User user = new User("user@example.com", "password", UserRole.USER);
        Todo todo = new Todo("title", "contents", "Sunny", user);

        Comment comment = new Comment("contents", user, todo);
        comment.update("updated contents");

        assertEquals("updated contents", comment.getContents());
        assertSame(user, comment.getUser());
        assertSame(todo, comment.getTodo());
    }

    @Test
    void Manager는_생성자에서_user와_todo를_보관한다() {
        User user = new User("user@example.com", "password", UserRole.USER);
        Todo todo = new Todo("title", "contents", "Sunny", user);

        Manager manager = new Manager(user, todo);

        assertSame(user, manager.getUser());
        assertSame(todo, manager.getTodo());
    }

    @Test
    void UserRole_of는_대소문자를_무시하고_매핑하며_유효하지_않으면_예외를_던진다() {
        assertEquals(UserRole.ADMIN, UserRole.of("admin"));
        assertEquals(UserRole.USER, UserRole.of("USER"));

        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> UserRole.of("manager"));
        assertEquals("유효하지 않은 UerRole", exception.getMessage());
    }

    @Test
    void response_DTO들은_생성자_값을_getter로_노출한다() {
        LocalDateTime now = LocalDateTime.now();
        UserResponse user = new UserResponse(1L, "user@example.com");

        assertEquals(1L, user.getId());
        assertEquals("user@example.com", user.getEmail());
        assertEquals("Bearer signup", new SignupResponse("Bearer signup").getBearerToken());
        assertEquals("Bearer signin", new SigninResponse("Bearer signin").getBearerToken());
        assertEquals("Bearer saved", new UserSaveResponse("Bearer saved").getBearerToken());

        CommentSaveResponse commentSave = new CommentSaveResponse(10L, "saved", user);
        assertEquals(10L, commentSave.getId());
        assertEquals("saved", commentSave.getContents());
        assertSame(user, commentSave.getUser());

        CommentResponse comment = new CommentResponse(11L, "read", user);
        assertEquals(11L, comment.getId());
        assertEquals("read", comment.getContents());
        assertSame(user, comment.getUser());

        ManagerSaveResponse managerSave = new ManagerSaveResponse(20L, user);
        assertEquals(20L, managerSave.getId());
        assertSame(user, managerSave.getUser());

        ManagerResponse manager = new ManagerResponse(21L, user);
        assertEquals(21L, manager.getId());
        assertSame(user, manager.getUser());

        TodoSaveResponse todoSave = new TodoSaveResponse(30L, "title", "contents", "Sunny", user);
        assertEquals(30L, todoSave.getId());
        assertEquals("title", todoSave.getTitle());
        assertEquals("contents", todoSave.getContents());
        assertEquals("Sunny", todoSave.getWeather());
        assertSame(user, todoSave.getUser());

        TodoResponse todo = new TodoResponse(31L, "read title", "read contents", "Rainy", user, now, now);
        assertEquals(31L, todo.getId());
        assertEquals("read title", todo.getTitle());
        assertEquals("read contents", todo.getContents());
        assertEquals("Rainy", todo.getWeather());
        assertSame(user, todo.getUser());
        assertEquals(now, todo.getCreatedAt());
        assertEquals(now, todo.getModifiedAt());
    }

    @Test
    void WeatherDto는_날짜와_날씨를_노출한다() {
        WeatherDto dto = new WeatherDto("05-11", "Sunny");

        assertEquals("05-11", dto.getDate());
        assertEquals("Sunny", dto.getWeather());
    }

    @Test
    void 기본_생성자가_필요한_JPA_엔티티도_생성할_수_있다() {
        User user = new User();
        Todo todo = new Todo();
        Comment comment = new Comment();
        Manager manager = new Manager();

        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(todo, "id", 2L);
        ReflectionTestUtils.setField(comment, "id", 3L);
        ReflectionTestUtils.setField(manager, "id", 4L);

        assertEquals(1L, user.getId());
        assertEquals(2L, todo.getId());
        assertEquals(3L, comment.getId());
        assertEquals(4L, manager.getId());
    }
}
