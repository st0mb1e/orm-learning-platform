package ru.mifi.platform.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.mifi.platform.dto.Requests.*;
import ru.mifi.platform.dto.Views.*;
import ru.mifi.platform.model.*;
import ru.mifi.platform.repository.*;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {
    private final QuizRepository quizzes;
    private final CourseModuleRepository modules;
    private final QuizSubmissionRepository results;
    private final UserRepository users;
    private final EnrollmentService enrollments;
    private final QuizGrader grader;

    public QuizView get(Long id) {
        return ViewMapper.quiz(requireQuiz(id));
    }

    @Transactional
    public QuizView create(Long moduleId, @Valid QuizData data) {
        CourseModule module = modules.findById(moduleId).orElseThrow(() -> BusinessException.notFound("Модуль"));
        if (module.getQuiz() != null) {
            throw BusinessException.conflict("У модуля уже есть тест");
        }
        Quiz quiz = new Quiz();
        quiz.setModule(module);
        fill(quiz, data);
        quizzes.saveAndFlush(quiz);
        return ViewMapper.quiz(quiz);
    }

    @Transactional
    public QuizView update(Long id, @Valid QuizData data) {
        Quiz quiz = requireQuiz(id);
        if (!quiz.getSubmissions().isEmpty()) {
            throw BusinessException.conflict("Нельзя менять вопросы теста, по которому уже есть результаты");
        }
        fill(quiz, data);
        quizzes.flush();
        return ViewMapper.quiz(quiz);
    }

    @Transactional
    public void delete(Long id) {
        quizzes.delete(requireQuiz(id));
    }

    @Transactional
    public QuizResult take(Long id, @Valid QuizAttempt data) {
        Quiz quiz = requireQuiz(id);
        User student = enrollments.requireActiveStudent(data.studentId(), quiz.getModule().getCourse().getId());
        List<QuizGrader.QuestionSpec> questions = quiz.getQuestions().stream().map(question ->
                new QuizGrader.QuestionSpec(question.getId(), question.getType(),
                        question.getOptions().stream().map(AnswerOption::getId).collect(Collectors.toSet()),
                        question.getOptions().stream().filter(AnswerOption::isCorrect)
                                .map(AnswerOption::getId).collect(Collectors.toSet()))).toList();
        int score = grader.grade(questions, data.answers());
        QuizSubmission submission = new QuizSubmission();
        submission.setQuiz(quiz);
        submission.setStudent(student);
        submission.setScore(score);
        return ViewMapper.quizResult(results.save(submission));
    }

    public List<QuizResult> quizResults(Long quizId, Long studentId) {
        requireQuiz(quizId);
        if (studentId == null) {
            return results.findByQuizIdOrderByTakenAtDesc(quizId).stream().map(ViewMapper::quizResult).toList();
        }
        return results.findByQuizIdAndStudentIdOrderByTakenAtDesc(quizId, studentId)
                .stream().map(ViewMapper::quizResult).toList();
    }

    public List<QuizResult> studentResults(Long studentId) {
        if (!users.existsById(studentId)) {
            throw BusinessException.notFound("Пользователь");
        }
        return results.findByStudentIdOrderByTakenAtDesc(studentId).stream().map(ViewMapper::quizResult).toList();
    }

    private Quiz requireQuiz(Long id) {
        return quizzes.findById(id).orElseThrow(() -> BusinessException.notFound("Тест"));
    }

    private void fill(Quiz quiz, QuizData data) {
        for (QuestionData question : data.questions()) {
            long correctCount = question.options().stream().filter(OptionData::correct).count();
            if (correctCount == 0 || question.type() == QuestionType.SINGLE_CHOICE && correctCount != 1) {
                throw BusinessException.invalid("У одиночного выбора нужен один верный вариант, у множественного — хотя бы один");
            }
        }
        quiz.setTitle(data.title().trim());
        quiz.getQuestions().clear();
        for (QuestionData questionData : data.questions()) {
            Question question = new Question();
            question.setQuiz(quiz);
            question.setText(questionData.text().trim());
            question.setType(questionData.type());
            for (OptionData optionData : questionData.options()) {
                AnswerOption option = new AnswerOption();
                option.setQuestion(question);
                option.setText(optionData.text().trim());
                option.setCorrect(optionData.correct());
                question.getOptions().add(option);
            }
            quiz.getQuestions().add(question);
        }
    }
}
