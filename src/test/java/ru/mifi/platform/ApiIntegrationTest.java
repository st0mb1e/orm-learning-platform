package ru.mifi.platform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import ru.mifi.platform.dto.Views.QuizView;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApiIntegrationTest extends IntegrationTestSupport {
    @Test
    @DisplayName("REST создаёт пользователя и возвращает 201, невалидные данные дают 400")
    void validatesUserRequest() throws Exception {
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Анна","email":"anna@example.org","role":"STUDENT"}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Анна"));
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"","email":"wrong","role":"STUDENT"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("REST возвращает 404 для неизвестного курса и 400 для ошибочного запроса")
    void returnsUsefulErrors() throws Exception {
        mvc.perform(get("/api/courses/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Курс отсутствует"));
        mvc.perform(get("/api/courses/not-a-number")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/courses").param("size", "0")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{bad json"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Анна","email":"anna@example.org","role":"STUDENT","unknown":1}
                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Дублирующий email даёт 409 и понятное сообщение")
    void returnsConflict() throws Exception {
        fixture();
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Анна","email":"student@example.org","role":"STUDENT"}
                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Пользователь с таким email уже существует"));
    }

    @Test
    @DisplayName("REST отдаёт структуру курса при выключенном Open Session in View")
    void courseJsonContainsNestedContent() throws Exception {
        Fixture data = fixture();
        mvc.perform(get("/api/courses/{id}", data.courseId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.course.teacherName").value("Преподаватель"))
                .andExpect(jsonPath("$.modules[0].lessons[0].assignments[0].maxScore").value(100));
        mvc.perform(get("/api/courses").param("search", "hibernate").param("categoryId", data.categoryId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/courses").param("search", "%"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Через API можно записаться, увидеть студентов и отписаться")
    void enrollmentEndpointsWork() throws Exception {
        Fixture data = fixture();
        String body = "{\"studentId\":" + data.studentId() + "}";
        mvc.perform(post("/api/courses/{id}/enrollments", data.courseId()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/courses/{id}/students", data.courseId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(data.studentId()));
        mvc.perform(delete("/api/courses/{id}/students/{studentId}", data.courseId(), data.studentId()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/users/{id}/enrollments", data.studentId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("В JSON теста нет признака правильности вариантов")
    void quizDoesNotExposeCorrectAnswers() throws Exception {
        Fixture data = fixture();
        QuizView quiz = quizzes.create(data.moduleId(), quizData());
        mvc.perform(get("/api/quizzes/{id}", quiz.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].options[0].text").value("Many"))
                .andExpect(jsonPath("$.questions[0].options[0].correct").doesNotExist())
                .andExpect(jsonPath("$.questions[0].options[0].isCorrect").doesNotExist());
        enrollments.enroll(data.courseId(), data.studentId());
        mvc.perform(post("/api/quizzes/{id}/submissions", quiz.id()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":" + data.studentId() + ",\"answers\":[]}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.score").value(0));
    }

    @Test
    @DisplayName("Вложенная валидация теста проверяет вопросы и варианты")
    void validatesQuizOptions() throws Exception {
        Fixture data = fixture();
        mvc.perform(post("/api/modules/{id}/quiz", data.moduleId()).contentType(MediaType.APPLICATION_JSON).content("""
                {"title":"Тест","questions":[{"text":"Вопрос","type":"SINGLE_CHOICE","options":[{"text":"","correct":true}]}]}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isNotEmpty());
    }

    @Test
    @DisplayName("Дедлайн со смещением часового пояса сохраняется как момент UTC")
    void preservesDeadlineAcrossTimeZones() throws Exception {
        Fixture data = fixture();
        mvc.perform(post("/api/lessons/{id}/assignments", data.lessonId())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"title":"Работа","description":"Условие","dueDate":"2026-12-01T23:59:00+03:00","maxScore":100}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dueDate").value("2026-12-01T20:59:00Z"));
    }

    @Test
    @DisplayName("Главная страница и локальные ресурсы интерфейса доступны")
    void servesUserInterface() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html lang=\"ru\">")));
        mvc.perform(get("/app.js")).andExpect(status().isOk());
        mvc.perform(get("/styles.css")).andExpect(status().isOk());
    }
}
