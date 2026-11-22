package ru.mifi.platform.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.mifi.platform.dto.Requests.*;
import ru.mifi.platform.dto.Views.*;
import ru.mifi.platform.service.ContentService;
import ru.mifi.platform.service.SubmissionService;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ContentController {
    private final ContentService content;
    private final SubmissionService submissions;

    @GetMapping("/modules/{id}")
    public ModuleView getModule(@PathVariable Long id) {
        return content.getModule(id);
    }

    @PutMapping("/modules/{id}")
    public ModuleView updateModule(@PathVariable Long id, @RequestBody @Valid ModuleData data) {
        return content.updateModule(id, data);
    }

    @DeleteMapping("/modules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteModule(@PathVariable Long id) {
        content.deleteModule(id);
    }

    @PostMapping("/modules/{id}/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    public LessonView createLesson(@PathVariable Long id, @RequestBody @Valid LessonData data) {
        return content.createLesson(id, data);
    }

    @GetMapping("/lessons/{id}")
    public LessonView getLesson(@PathVariable Long id) {
        return content.getLesson(id);
    }

    @PutMapping("/lessons/{id}")
    public LessonView updateLesson(@PathVariable Long id, @RequestBody @Valid LessonData data) {
        return content.updateLesson(id, data);
    }

    @DeleteMapping("/lessons/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLesson(@PathVariable Long id) {
        content.deleteLesson(id);
    }

    @PostMapping("/lessons/{id}/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentView createAssignment(@PathVariable Long id, @RequestBody @Valid AssignmentData data) {
        return content.createAssignment(id, data);
    }

    @GetMapping("/assignments/{id}")
    public AssignmentView getAssignment(@PathVariable Long id) {
        return content.getAssignment(id);
    }

    @PutMapping("/assignments/{id}")
    public AssignmentView updateAssignment(@PathVariable Long id, @RequestBody @Valid AssignmentData data) {
        return content.updateAssignment(id, data);
    }

    @DeleteMapping("/assignments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssignment(@PathVariable Long id) {
        content.deleteAssignment(id);
    }

    @PostMapping("/assignments/{id}/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    public SubmissionView submit(@PathVariable Long id, @RequestBody @Valid SubmissionData data) {
        return submissions.submit(id, data);
    }

    @GetMapping("/assignments/{id}/submissions")
    public List<SubmissionView> submissions(@PathVariable Long id) {
        return submissions.list(id);
    }

    @GetMapping("/submissions/{id}")
    public SubmissionView getSubmission(@PathVariable Long id) {
        return submissions.get(id);
    }

    @PatchMapping("/submissions/{id}/grade")
    public SubmissionView grade(@PathVariable Long id, @RequestBody @Valid GradeData data) {
        return submissions.grade(id, data);
    }
}
