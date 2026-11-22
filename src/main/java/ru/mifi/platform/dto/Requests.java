package ru.mifi.platform.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import ru.mifi.platform.model.EnrollmentStatus;
import ru.mifi.platform.model.QuestionType;
import ru.mifi.platform.model.Role;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public final class Requests {
    private Requests() {
    }

    public record UserData(@NotBlank @Size(max = 120) String name,
                           @NotBlank @Email @Size(max = 254) String email,
                           @NotNull Role role) {
    }

    public record ProfileData(@Size(max = 2000) String bio, @Size(max = 1000) String avatarUrl) {
    }

    public record NameData(@NotBlank @Size(max = 60) String name) {
    }

    public record CourseData(@NotBlank @Size(max = 200) String title,
                             @NotBlank @Size(max = 5000) String description,
                             @NotNull @Positive Long teacherId,
                             @NotNull @Positive Long categoryId,
                             @NotNull Set<@NotNull @Positive Long> tagIds) {
    }

    public record ModuleData(@NotBlank @Size(max = 200) String title, @Min(1) int orderIndex) {
    }

    public record LessonData(@NotBlank @Size(max = 200) String title,
                             @NotBlank @Size(max = 10000) String content,
                             @Size(max = 1000) String videoUrl, @Min(1) int orderIndex) {
    }

    public record AssignmentData(@NotBlank @Size(max = 200) String title,
                                 @NotBlank @Size(max = 5000) String description,
                                 Instant dueDate, @Min(1) @Max(1000) int maxScore) {
    }

    public record EnrollmentData(@NotNull @Positive Long studentId) {
    }

    public record EnrollmentState(@NotNull EnrollmentStatus status) {
    }

    public record SubmissionData(@NotNull @Positive Long studentId,
                                 @NotBlank @Size(max = 10000) String content) {
    }

    public record GradeData(@NotNull @Positive Long teacherId, @NotNull @Min(0) Integer score,
                            @Size(max = 3000) String feedback) {
    }

    public record OptionData(@NotBlank @Size(max = 2000) String text, boolean correct) {
    }

    public record QuestionData(@NotBlank @Size(max = 2000) String text,
                               @NotNull QuestionType type,
                               @NotNull @Size(min = 2, max = 10) List<@NotNull @Valid OptionData> options) {
    }

    public record QuizData(@NotBlank @Size(max = 200) String title,
                           @NotNull @Size(min = 1, max = 100) List<@NotNull @Valid QuestionData> questions) {
    }

    public record AnswerData(@NotNull @Positive Long questionId,
                             @NotNull Set<@NotNull @Positive Long> optionIds) {
    }

    public record QuizAttempt(@NotNull @Positive Long studentId,
                              @NotNull @Size(max = 100) List<@NotNull @Valid AnswerData> answers) {
    }

    public record ReviewData(@NotNull @Positive Long studentId, @Min(1) @Max(5) int rating,
                             @NotBlank @Size(max = 3000) String comment) {
    }
}
