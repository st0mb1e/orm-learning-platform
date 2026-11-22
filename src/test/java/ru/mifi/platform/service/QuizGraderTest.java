package ru.mifi.platform.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mifi.platform.dto.Requests.AnswerData;
import ru.mifi.platform.model.QuestionType;
import ru.mifi.platform.service.QuizGrader.QuestionSpec;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class QuizGraderTest {
    private final QuizGrader grader = new QuizGrader();
    private final QuestionSpec single = new QuestionSpec(1L, QuestionType.SINGLE_CHOICE, Set.of(10L, 11L), Set.of(10L));
    private final QuestionSpec multiple = new QuestionSpec(2L, QuestionType.MULTIPLE_CHOICE, Set.of(20L, 21L, 22L), Set.of(20L, 21L));

    @Test
    @DisplayName("Все правильные ответы дают 100 процентов")
    void allCorrect() {
        int score = grader.grade(List.of(single, multiple), List.of(
                new AnswerData(1L, Set.of(10L)), new AnswerData(2L, Set.of(20L, 21L))));
        assertThat(score).isEqualTo(100);
    }

    @Test
    @DisplayName("Один верный ответ из двух даёт 50 процентов")
    void halfCorrect() {
        int score = grader.grade(List.of(single, multiple), List.of(
                new AnswerData(1L, Set.of(11L)), new AnswerData(2L, Set.of(20L, 21L))));
        assertThat(score).isEqualTo(50);
    }

    @Test
    @DisplayName("Частичный ответ на множественный выбор не засчитывается")
    void incompleteMultipleChoice() {
        assertThat(grader.grade(List.of(multiple), List.of(new AnswerData(2L, Set.of(20L))))).isZero();
    }

    @Test
    @DisplayName("Лишний неверный вариант не засчитывается")
    void extraWrongOption() {
        assertThat(grader.grade(List.of(multiple), List.of(new AnswerData(2L, Set.of(20L, 21L, 22L))))).isZero();
    }

    @Test
    @DisplayName("Пропущенный вопрос считается неверным")
    void unansweredQuestion() {
        assertThat(grader.grade(List.of(single), List.of())).isZero();
    }

    @Test
    @DisplayName("Неизвестный вопрос отклоняется")
    void rejectsUnknownQuestion() {
        assertThatThrownBy(() -> grader.grade(List.of(single), List.of(new AnswerData(99L, Set.of(10L)))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Вопрос");
    }

    @Test
    @DisplayName("Вариант из другого вопроса отклоняется")
    void rejectsForeignOption() {
        assertThatThrownBy(() -> grader.grade(List.of(single), List.of(new AnswerData(1L, Set.of(20L)))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Вариант");
    }

    @Test
    @DisplayName("Повторный ответ на один вопрос отклоняется")
    void rejectsDuplicateAnswer() {
        assertThatThrownBy(() -> grader.grade(List.of(single), List.of(new AnswerData(1L, Set.of(10L)), new AnswerData(1L, Set.of(11L)))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("несколько раз");
    }

    @Test
    @DisplayName("Одиночный выбор не принимает два варианта")
    void rejectsMultipleOptionsForSingleChoice() {
        assertThatThrownBy(() -> grader.grade(List.of(single), List.of(new AnswerData(1L, Set.of(10L, 11L)))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("один ответ");
    }

    @Test
    @DisplayName("Пустой тест нельзя оценить")
    void rejectsEmptyQuiz() {
        assertThatThrownBy(() -> grader.grade(List.of(), List.of()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("нет вопросов");
    }
}
