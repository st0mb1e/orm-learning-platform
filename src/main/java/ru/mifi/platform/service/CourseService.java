package ru.mifi.platform.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.mifi.platform.dto.Requests.CourseData;
import ru.mifi.platform.dto.Views.*;
import ru.mifi.platform.model.*;
import ru.mifi.platform.repository.*;

import java.util.List;
import java.util.Locale;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {
    private final CourseRepository courses;
    private final UserRepository users;
    private final CategoryRepository categories;
    private final TagRepository tags;

    public PageView<CourseSummary> list(String search, Long categoryId, Long teacherId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw BusinessException.invalid("Номер страницы должен быть от 0, размер — от 1 до 100");
        }
        Specification<Course> filter = (root, query, cb) -> cb.conjunction();
        if (search != null && !search.isBlank()) {
            String escaped = search.trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            filter = filter.and((root, query, cb) -> cb.like(cb.lower(root.get("title")), "%" + escaped + "%", '\\'));
        }
        if (categoryId != null) {
            filter = filter.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }
        if (teacherId != null) {
            filter = filter.and((root, query, cb) -> cb.equal(root.get("teacher").get("id"), teacherId));
        }
        Page<CourseSummary> result = courses.findAll(filter, PageRequest.of(page, size, Sort.by("id")))
                .map(ViewMapper::course);
        return new PageView<>(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    public CourseDetail get(Long id) {
        Course course = courses.findDetailedById(id).orElseThrow(() -> BusinessException.notFound("Курс"));
        // DTO собирается до закрытия транзакции: в контроллер ленивые коллекции не попадают.
        return ViewMapper.courseDetail(course);
    }

    @Transactional
    public CourseSummary create(@Valid CourseData data) {
        Course course = new Course();
        fill(course, data);
        return ViewMapper.course(courses.save(course));
    }

    @Transactional
    public CourseSummary update(Long id, @Valid CourseData data) {
        Course course = courses.findById(id).orElseThrow(() -> BusinessException.notFound("Курс"));
        fill(course, data);
        return ViewMapper.course(course);
    }

    @Transactional
    public void delete(Long id) {
        Course course = courses.findById(id).orElseThrow(() -> BusinessException.notFound("Курс"));
        courses.delete(course);
    }

    private void fill(Course course, CourseData data) {
        User teacher = users.findById(data.teacherId()).orElseThrow(() -> BusinessException.notFound("Преподаватель"));
        if (teacher.getRole() != Role.TEACHER) {
            throw BusinessException.invalid("Вести курс может только пользователь с ролью TEACHER");
        }
        Category category = categories.findById(data.categoryId())
                .orElseThrow(() -> BusinessException.notFound("Категория"));
        List<Tag> selectedTags = tags.findAllById(data.tagIds());
        if (selectedTags.size() != data.tagIds().size()) {
            throw BusinessException.notFound("Один из тегов");
        }
        course.setTitle(data.title().trim());
        course.setDescription(data.description().trim());
        course.setTeacher(teacher);
        course.setCategory(category);
        course.getTags().clear();
        course.getTags().addAll(selectedTags);
    }
}
