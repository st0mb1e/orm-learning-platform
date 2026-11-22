package ru.mifi.platform.repository;

import ru.mifi.platform.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    Optional<Submission> findByStudentIdAndAssignmentId(Long studentId, Long assignmentId);

    @EntityGraph(attributePaths = {"student", "assignment"})
    List<Submission> findByAssignmentIdOrderById(Long assignmentId);

    @EntityGraph(attributePaths = {"student", "assignment"})
    List<Submission> findByStudentIdOrderBySubmittedAtDesc(Long studentId);
}
