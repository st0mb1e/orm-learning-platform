package ru.mifi.platform.repository;

import ru.mifi.platform.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    @Override
    @EntityGraph(attributePaths = {"teacher", "teacher.profile", "category"})
    Page<Course> findAll(Specification<Course> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"teacher", "category", "modules"})
    Optional<Course> findDetailedById(Long id);

    boolean existsByTeacherId(Long teacherId);
    boolean existsByCategoryId(Long categoryId);
}
