package ru.mifi.platform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mifi.platform.dto.Views.EnrollmentView;
import ru.mifi.platform.dto.Views.UserView;
import ru.mifi.platform.model.*;
import ru.mifi.platform.repository.CourseRepository;
import ru.mifi.platform.repository.EnrollmentRepository;
import ru.mifi.platform.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentService {
    private final EnrollmentRepository enrollments;
    private final CourseRepository courses;
    private final UserRepository users;

    @Transactional
    public EnrollmentView enroll(Long courseId, Long studentId) {
        Course course = courses.findById(courseId).orElseThrow(() -> BusinessException.notFound("Курс"));
        User student = requireStudent(studentId);
        Enrollment enrollment = enrollments.findByStudentIdAndCourseId(studentId, courseId).orElse(null);
        if (enrollment != null) {
            if (enrollment.getStatus() != EnrollmentStatus.CANCELLED) {
                throw BusinessException.conflict("Студент уже записан на курс");
            }
            enrollment.setStatus(EnrollmentStatus.ACTIVE);
            return ViewMapper.enrollment(enrollment);
        }
        enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setStudent(student);
        return ViewMapper.enrollment(enrollments.save(enrollment));
    }

    public List<EnrollmentView> studentCourses(Long studentId) {
        requireStudent(studentId);
        return enrollments.findByStudentIdOrderById(studentId).stream().map(ViewMapper::enrollment).toList();
    }

    public List<UserView> courseStudents(Long courseId) {
        if (!courses.existsById(courseId)) {
            throw BusinessException.notFound("Курс");
        }
        return enrollments.findByCourseIdAndStatusOrderById(courseId, EnrollmentStatus.ACTIVE)
                .stream().map(enrollment -> ViewMapper.user(enrollment.getStudent())).toList();
    }

    @Transactional
    public EnrollmentView changeStatus(Long id, EnrollmentStatus status) {
        if (status == null) {
            throw BusinessException.invalid("Укажите статус записи");
        }
        Enrollment enrollment = enrollments.findById(id).orElseThrow(() -> BusinessException.notFound("Запись на курс"));
        enrollment.setStatus(status);
        return ViewMapper.enrollment(enrollment);
    }

    @Transactional
    public void cancel(Long courseId, Long studentId) {
        Enrollment enrollment = enrollments.findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> BusinessException.notFound("Запись на курс"));
        enrollments.delete(enrollment);
    }

    public User requireActiveStudent(Long studentId, Long courseId) {
        User student = requireStudent(studentId);
        if (!enrollments.existsByStudentIdAndCourseIdAndStatus(studentId, courseId, EnrollmentStatus.ACTIVE)) {
            throw BusinessException.conflict("Для этого действия нужна активная запись на курс");
        }
        return student;
    }

    private User requireStudent(Long id) {
        User student = users.findById(id).orElseThrow(() -> BusinessException.notFound("Студент"));
        if (student.getRole() != Role.STUDENT) {
            throw BusinessException.invalid("Действие доступно только студенту");
        }
        return student;
    }
}
