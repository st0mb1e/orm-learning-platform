package ru.mifi.platform.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.mifi.platform.dto.Requests.*;
import ru.mifi.platform.dto.Views.*;
import ru.mifi.platform.service.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService users;
    private final EnrollmentService enrollments;
    private final SubmissionService submissions;
    private final QuizService quizzes;

    @GetMapping
    public List<UserView> list() {
        return users.list();
    }

    @GetMapping("/{id}")
    public UserView get(@PathVariable Long id) {
        return users.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserView create(@RequestBody @Valid UserData data) {
        return users.create(data);
    }

    @PutMapping("/{id}")
    public UserView update(@PathVariable Long id, @RequestBody @Valid UserData data) {
        return users.update(id, data);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        users.delete(id);
    }

    @GetMapping("/{id}/profile")
    public ProfileView getProfile(@PathVariable Long id) {
        return users.getProfile(id);
    }

    @PutMapping("/{id}/profile")
    public ProfileView saveProfile(@PathVariable Long id, @RequestBody @Valid ProfileData data) {
        return users.saveProfile(id, data);
    }

    @DeleteMapping("/{id}/profile")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProfile(@PathVariable Long id) {
        users.deleteProfile(id);
    }

    @GetMapping("/{id}/enrollments")
    public List<EnrollmentView> courses(@PathVariable Long id) {
        return enrollments.studentCourses(id);
    }

    @GetMapping("/{id}/submissions")
    public List<SubmissionView> submissions(@PathVariable Long id) {
        return submissions.studentSubmissions(id);
    }

    @GetMapping("/{id}/quiz-results")
    public List<QuizResult> quizResults(@PathVariable Long id) {
        return quizzes.studentResults(id);
    }
}
