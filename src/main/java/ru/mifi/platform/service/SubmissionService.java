package ru.mifi.platform.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.mifi.platform.dto.Requests.GradeData;
import ru.mifi.platform.dto.Requests.SubmissionData;
import ru.mifi.platform.dto.Views.SubmissionView;
import ru.mifi.platform.model.Assignment;
import ru.mifi.platform.model.Submission;
import ru.mifi.platform.model.User;
import ru.mifi.platform.repository.AssignmentRepository;
import ru.mifi.platform.repository.SubmissionRepository;
import ru.mifi.platform.repository.UserRepository;

import java.time.Instant;
import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionService {
    private final AssignmentRepository assignments;
    private final SubmissionRepository submissions;
    private final EnrollmentService enrollments;
    private final UserRepository users;

    @Transactional
    public SubmissionView submit(Long assignmentId, @Valid SubmissionData data) {
        Assignment assignment = requireAssignment(assignmentId);
        Long courseId = assignment.getLesson().getModule().getCourse().getId();
        User student = enrollments.requireActiveStudent(data.studentId(), courseId);
        if (assignment.getDueDate() != null && Instant.now().isAfter(assignment.getDueDate())) {
            throw BusinessException.conflict("Срок сдачи задания истёк");
        }
        if (submissions.findByStudentIdAndAssignmentId(data.studentId(), assignmentId).isPresent()) {
            throw BusinessException.conflict("Решение уже отправлено: повторная сдача не предусмотрена");
        }
        Submission submission = new Submission();
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setContent(data.content().trim());
        return ViewMapper.submission(submissions.save(submission));
    }

    public SubmissionView get(Long id) {
        return ViewMapper.submission(requireSubmission(id));
    }

    public List<SubmissionView> list(Long assignmentId) {
        requireAssignment(assignmentId);
        return submissions.findByAssignmentIdOrderById(assignmentId).stream().map(ViewMapper::submission).toList();
    }

    public List<SubmissionView> studentSubmissions(Long studentId) {
        if (!users.existsById(studentId)) {
            throw BusinessException.notFound("Пользователь");
        }
        return submissions.findByStudentIdOrderBySubmittedAtDesc(studentId)
                .stream().map(ViewMapper::submission).toList();
    }

    @Transactional
    public SubmissionView grade(Long id, @Valid GradeData data) {
        Submission submission = requireSubmission(id);
        Assignment assignment = submission.getAssignment();
        Long teacherId = assignment.getLesson().getModule().getCourse().getTeacher().getId();
        if (!teacherId.equals(data.teacherId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Оценку выставляет преподаватель этого курса");
        }
        if (data.score() > assignment.getMaxScore()) {
            throw BusinessException.invalid("Оценка не может превышать " + assignment.getMaxScore());
        }
        submission.setScore(data.score());
        submission.setFeedback(data.feedback());
        return ViewMapper.submission(submission);
    }

    private Assignment requireAssignment(Long id) {
        return assignments.findById(id).orElseThrow(() -> BusinessException.notFound("Задание"));
    }

    private Submission requireSubmission(Long id) {
        return submissions.findById(id).orElseThrow(() -> BusinessException.notFound("Решение"));
    }
}
