package ru.mifi.platform.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.mifi.platform.dto.Requests.*;
import ru.mifi.platform.dto.Views.*;
import ru.mifi.platform.service.QuizService;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class QuizController {
    private final QuizService quizzes;

    @PostMapping("/modules/{id}/quiz")
    @ResponseStatus(HttpStatus.CREATED)
    public QuizView create(@PathVariable Long id, @RequestBody @Valid QuizData data) {
        return quizzes.create(id, data);
    }

    @GetMapping("/quizzes/{id}")
    public QuizView get(@PathVariable Long id) {
        return quizzes.get(id);
    }

    @PutMapping("/quizzes/{id}")
    public QuizView update(@PathVariable Long id, @RequestBody @Valid QuizData data) {
        return quizzes.update(id, data);
    }

    @DeleteMapping("/quizzes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        quizzes.delete(id);
    }

    @PostMapping("/quizzes/{id}/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    public QuizResult take(@PathVariable Long id, @RequestBody @Valid QuizAttempt data) {
        return quizzes.take(id, data);
    }

    @GetMapping("/quizzes/{id}/submissions")
    public List<QuizResult> results(@PathVariable Long id, @RequestParam(required = false) Long studentId) {
        return quizzes.quizResults(id, studentId);
    }
}
