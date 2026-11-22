package ru.mifi.platform.service;

import org.springframework.stereotype.Component;
import ru.mifi.platform.dto.Requests.AnswerData;
import ru.mifi.platform.model.QuestionType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class QuizGrader {
    public record QuestionSpec(Long id, QuestionType type, Set<Long> optionIds, Set<Long> correctIds) {
    }

    public int grade(List<QuestionSpec> questions, List<AnswerData> answers) {
        if (questions.isEmpty()) {
            throw BusinessException.conflict("В тесте нет вопросов");
        }
        Map<Long, QuestionSpec> questionById = new HashMap<>();
        questions.forEach(question -> questionById.put(question.id(), question));
        Map<Long, Set<Long>> selected = new HashMap<>();
        for (AnswerData answer : answers) {
            QuestionSpec question = questionById.get(answer.questionId());
            if (question == null) {
                throw BusinessException.invalid("Вопрос не относится к этому тесту");
            }
            if (selected.putIfAbsent(answer.questionId(), answer.optionIds()) != null) {
                throw BusinessException.invalid("Ответ на один вопрос передан несколько раз");
            }
            if (!question.optionIds().containsAll(answer.optionIds())) {
                throw BusinessException.invalid("Вариант ответа не относится к указанному вопросу");
            }
            if (question.type() == QuestionType.SINGLE_CHOICE && answer.optionIds().size() > 1) {
                throw BusinessException.invalid("У вопроса с одиночным выбором может быть только один ответ");
            }
        }
        long correct = questions.stream()
                .filter(question -> question.correctIds().equals(selected.getOrDefault(question.id(), Set.of())))
                .count();
        // Для множественного выбора балл даётся только за полный набор верных вариантов.
        return (int) Math.round(100.0 * correct / questions.size());
    }
}
