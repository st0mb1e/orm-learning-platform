package ru.mifi.platform.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.mifi.platform.dto.Requests.*;
import ru.mifi.platform.dto.Views.*;
import ru.mifi.platform.service.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService courses;
    private final ContentService content;
    private final EnrollmentService enrollments;
    private final ReviewService reviews;

    @GetMapping("/courses")
    public PageView<CourseSummary> list(@RequestParam(defaultValue = "") String search,
                                        @RequestParam(required = false) Long categoryId,
                                        @RequestParam(required = false) Long teacherId,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        return courses.list(search, categoryId, teacherId, page, size);
    }

    @GetMapping("/courses/{id}")
    public CourseDetail get(@PathVariable Long id) {
        return courses.get(id);
    }

    @PostMapping("/courses")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseSummary create(@RequestBody @Valid CourseData data) {
        return courses.create(data);
    }

    @PutMapping("/courses/{id}")
    public CourseSummary update(@PathVariable Long id, @RequestBody @Valid CourseData data) {
        return courses.update(id, data);
    }

    @DeleteMapping("/courses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        courses.delete(id);
    }

    @PostMapping("/courses/{id}/modules")
    @ResponseStatus(HttpStatus.CREATED)
    public ModuleView addModule(@PathVariable Long id, @RequestBody @Valid ModuleData data) {
        return content.createModule(id, data);
    }

    @PostMapping("/courses/{id}/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentView enroll(@PathVariable Long id, @RequestBody @Valid EnrollmentData data) {
        return enrollments.enroll(id, data.studentId());
    }

    @GetMapping("/courses/{id}/students")
    public List<UserView> students(@PathVariable Long id) {
        return enrollments.courseStudents(id);
    }

    @PatchMapping("/enrollments/{id}")
    public EnrollmentView changeStatus(@PathVariable Long id, @RequestBody @Valid EnrollmentState data) {
        return enrollments.changeStatus(id, data.status());
    }

    @DeleteMapping("/courses/{id}/students/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id, @PathVariable Long studentId) {
        enrollments.cancel(id, studentId);
    }

    @GetMapping("/courses/{id}/reviews")
    public List<ReviewView> reviews(@PathVariable Long id) {
        return reviews.list(id);
    }

    @PostMapping("/courses/{id}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewView createReview(@PathVariable Long id, @RequestBody @Valid ReviewData data) {
        return reviews.create(id, data);
    }

    @PutMapping("/reviews/{id}")
    public ReviewView updateReview(@PathVariable Long id, @RequestBody @Valid ReviewData data) {
        return reviews.update(id, data);
    }

    @DeleteMapping("/reviews/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(@PathVariable Long id) {
        reviews.delete(id);
    }
}
