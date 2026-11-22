package ru.mifi.platform.dto;

import ru.mifi.platform.model.EnrollmentStatus;
import ru.mifi.platform.model.QuestionType;
import ru.mifi.platform.model.Role;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

public final class Views {
    private Views() {
    }

    public record UserView(Long id, String name, String email, Role role) {
    }

    public record ProfileView(Long id, Long userId, String bio, String avatarUrl) {
    }

    public record NameView(Long id, String name) {
    }

    public record CourseSummary(Long id, String title, String description,
                                Long teacherId, String teacherName, Long categoryId, String categoryName) {
    }

    public record CourseDetail(CourseSummary course, List<NameView> tags, List<ModuleView> modules) {
    }

    public record ModuleView(Long id, String title, int orderIndex, Long quizId, List<LessonView> lessons) {
    }

    public record LessonView(Long id, String title, String content, String videoUrl,
                             int orderIndex, List<AssignmentView> assignments) {
    }

    public record AssignmentView(Long id, String title, String description, Instant dueDate, int maxScore) {
    }

    public record EnrollmentView(Long id, Long studentId, Long courseId, String courseTitle,
                                 LocalDate enrolledAt, EnrollmentStatus status) {
    }

    public record SubmissionView(Long id, Long assignmentId, Long studentId, String studentName,
                                 Instant submittedAt, String content, Integer score, String feedback) {
    }

    public record OptionView(Long id, String text) {
    }

    public record QuestionView(Long id, String text, QuestionType type, List<OptionView> options) {
    }

    public record QuizView(Long id, String title, Long moduleId, List<QuestionView> questions) {
    }

    public record QuizResult(Long id, Long quizId, Long studentId, int score, Instant takenAt) {
    }

    public record ReviewView(Long id, Long courseId, Long studentId, String studentName,
                             int rating, String comment, Instant createdAt) {
    }

    public record PageView<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    }
}
