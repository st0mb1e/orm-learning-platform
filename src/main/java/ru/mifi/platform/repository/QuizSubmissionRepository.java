package ru.mifi.platform.repository;

import ru.mifi.platform.model.QuizSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;

public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Long> {

    @EntityGraph(attributePaths = {"quiz", "student"})
    List<QuizSubmission> findByQuizIdAndStudentIdOrderByTakenAtDesc(Long quizId, Long studentId);

    @EntityGraph(attributePaths = {"quiz", "student"})
    List<QuizSubmission> findByQuizIdOrderByTakenAtDesc(Long quizId);

    @EntityGraph(attributePaths = {"quiz", "student"})
    List<QuizSubmission> findByStudentIdOrderByTakenAtDesc(Long studentId);
}
