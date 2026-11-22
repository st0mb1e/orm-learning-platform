package ru.mifi.platform.service;

import ru.mifi.platform.dto.Views.*;
import ru.mifi.platform.model.*;

public final class ViewMapper {
    private ViewMapper() {
    }

    public static UserView user(User user) {
        return new UserView(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    public static ProfileView profile(Profile profile) {
        return new ProfileView(profile.getId(), profile.getUser().getId(), profile.getBio(), profile.getAvatarUrl());
    }

    public static CourseSummary course(Course course) {
        return new CourseSummary(course.getId(), course.getTitle(), course.getDescription(),
                course.getTeacher().getId(), course.getTeacher().getName(),
                course.getCategory().getId(), course.getCategory().getName());
    }

    public static CourseDetail courseDetail(Course course) {
        return new CourseDetail(course(course),
                course.getTags().stream().map(tag -> new NameView(tag.getId(), tag.getName())).toList(),
                course.getModules().stream().map(ViewMapper::module).toList());
    }

    public static ModuleView module(CourseModule module) {
        return new ModuleView(module.getId(), module.getTitle(), module.getOrderIndex(),
                module.getQuiz() == null ? null : module.getQuiz().getId(),
                module.getLessons().stream().map(ViewMapper::lesson).toList());
    }

    public static LessonView lesson(Lesson lesson) {
        return new LessonView(lesson.getId(), lesson.getTitle(), lesson.getContent(), lesson.getVideoUrl(),
                lesson.getOrderIndex(), lesson.getAssignments().stream().map(ViewMapper::assignment).toList());
    }

    public static AssignmentView assignment(Assignment assignment) {
        return new AssignmentView(assignment.getId(), assignment.getTitle(), assignment.getDescription(),
                assignment.getDueDate(), assignment.getMaxScore());
    }

    public static EnrollmentView enrollment(Enrollment enrollment) {
        return new EnrollmentView(enrollment.getId(), enrollment.getStudent().getId(), enrollment.getCourse().getId(),
                enrollment.getCourse().getTitle(), enrollment.getEnrolledAt(), enrollment.getStatus());
    }

    public static SubmissionView submission(Submission submission) {
        return new SubmissionView(submission.getId(), submission.getAssignment().getId(),
                submission.getStudent().getId(), submission.getStudent().getName(), submission.getSubmittedAt(),
                submission.getContent(), submission.getScore(), submission.getFeedback());
    }

    public static QuizView quiz(Quiz quiz) {
        return new QuizView(quiz.getId(), quiz.getTitle(), quiz.getModule().getId(),
                quiz.getQuestions().stream().map(question -> new QuestionView(question.getId(), question.getText(),
                        question.getType(), question.getOptions().stream()
                        .map(option -> new OptionView(option.getId(), option.getText())).toList())).toList());
    }

    public static QuizResult quizResult(QuizSubmission submission) {
        return new QuizResult(submission.getId(), submission.getQuiz().getId(), submission.getStudent().getId(),
                submission.getScore(), submission.getTakenAt());
    }

    public static ReviewView review(CourseReview review) {
        return new ReviewView(review.getId(), review.getCourse().getId(), review.getStudent().getId(),
                review.getStudent().getName(), review.getRating(), review.getComment(), review.getCreatedAt());
    }
}
