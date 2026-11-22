package ru.mifi.platform.repository;

import ru.mifi.platform.model.CourseModule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseModuleRepository extends JpaRepository<CourseModule, Long> {
}
