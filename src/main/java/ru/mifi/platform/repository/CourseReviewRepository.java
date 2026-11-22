package ru.mifi.platform.repository;

import ru.mifi.platform.model.CourseReview;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    Optional<CourseReview> findByStudentIdAndCourseId(Long studentId, Long courseId);

    @EntityGraph(attributePaths = "student")
    List<CourseReview> findByCourseIdOrderByCreatedAtDesc(Long courseId);
}
