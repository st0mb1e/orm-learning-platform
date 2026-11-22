package ru.mifi.platform.repository;

import ru.mifi.platform.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import ru.mifi.platform.model.EnrollmentStatus;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);
    boolean existsByStudentIdAndCourseIdAndStatus(Long studentId, Long courseId, EnrollmentStatus status);

    @EntityGraph(attributePaths = {"course", "student"})
    List<Enrollment> findByStudentIdOrderById(Long studentId);

    @EntityGraph(attributePaths = "student")
    List<Enrollment> findByCourseIdAndStatusOrderById(Long courseId, EnrollmentStatus status);
}
