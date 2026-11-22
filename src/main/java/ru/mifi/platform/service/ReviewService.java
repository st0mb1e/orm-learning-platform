package ru.mifi.platform.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.mifi.platform.dto.Requests.ReviewData;
import ru.mifi.platform.dto.Views.ReviewView;
import ru.mifi.platform.model.CourseReview;
import ru.mifi.platform.model.Enrollment;
import ru.mifi.platform.model.EnrollmentStatus;
import ru.mifi.platform.repository.CourseRepository;
import ru.mifi.platform.repository.CourseReviewRepository;
import ru.mifi.platform.repository.EnrollmentRepository;

import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {
    private final CourseReviewRepository reviews;
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;

    public List<ReviewView> list(Long courseId) {
        if (!courses.existsById(courseId)) {
            throw BusinessException.notFound("Курс");
        }
        return reviews.findByCourseIdOrderByCreatedAtDesc(courseId).stream().map(ViewMapper::review).toList();
    }

    @Transactional
    public ReviewView create(Long courseId, @Valid ReviewData data) {
        Enrollment enrollment = enrollments.findByStudentIdAndCourseId(data.studentId(), courseId)
                .filter(item -> item.getStatus() != EnrollmentStatus.CANCELLED)
                .orElseThrow(() -> BusinessException.conflict("Оставить отзыв может студент этого курса"));
        if (reviews.findByStudentIdAndCourseId(data.studentId(), courseId).isPresent()) {
            throw BusinessException.conflict("Отзыв уже оставлен");
        }
        CourseReview review = new CourseReview();
        review.setCourse(enrollment.getCourse());
        review.setStudent(enrollment.getStudent());
        review.setRating(data.rating());
        review.setComment(data.comment().trim());
        return ViewMapper.review(reviews.save(review));
    }

    @Transactional
    public ReviewView update(Long id, @Valid ReviewData data) {
        CourseReview review = requireReview(id);
        if (!review.getStudent().getId().equals(data.studentId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Изменять отзыв может его автор");
        }
        review.setRating(data.rating());
        review.setComment(data.comment().trim());
        return ViewMapper.review(review);
    }

    @Transactional
    public void delete(Long id) {
        reviews.delete(requireReview(id));
    }

    private CourseReview requireReview(Long id) {
        return reviews.findById(id).orElseThrow(() -> BusinessException.notFound("Отзыв"));
    }
}
