package ru.mifi.platform.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.mifi.platform.dto.Requests.*;
import ru.mifi.platform.dto.Views.*;
import ru.mifi.platform.model.*;
import ru.mifi.platform.repository.*;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentService {
    private final CourseRepository courses;
    private final CourseModuleRepository modules;
    private final LessonRepository lessons;
    private final AssignmentRepository assignments;

    public ModuleView getModule(Long id) {
        return ViewMapper.module(requireModule(id));
    }

    @Transactional
    public ModuleView createModule(Long courseId, @Valid ModuleData data) {
        Course course = courses.findById(courseId).orElseThrow(() -> BusinessException.notFound("Курс"));
        CourseModule module = new CourseModule();
        module.setCourse(course);
        module.setTitle(data.title().trim());
        module.setOrderIndex(data.orderIndex());
        modules.save(module);
        return ViewMapper.module(module);
    }

    @Transactional
    public ModuleView updateModule(Long id, @Valid ModuleData data) {
        CourseModule module = requireModule(id);
        module.setTitle(data.title().trim());
        module.setOrderIndex(data.orderIndex());
        return ViewMapper.module(module);
    }

    @Transactional
    public void deleteModule(Long id) {
        modules.delete(requireModule(id));
    }

    public LessonView getLesson(Long id) {
        return ViewMapper.lesson(requireLesson(id));
    }

    @Transactional
    public LessonView createLesson(Long moduleId, @Valid LessonData data) {
        Lesson lesson = new Lesson();
        lesson.setModule(requireModule(moduleId));
        fillLesson(lesson, data);
        return ViewMapper.lesson(lessons.save(lesson));
    }

    @Transactional
    public LessonView updateLesson(Long id, @Valid LessonData data) {
        Lesson lesson = requireLesson(id);
        fillLesson(lesson, data);
        return ViewMapper.lesson(lesson);
    }

    @Transactional
    public void deleteLesson(Long id) {
        lessons.delete(requireLesson(id));
    }

    public AssignmentView getAssignment(Long id) {
        return ViewMapper.assignment(requireAssignment(id));
    }

    @Transactional
    public AssignmentView createAssignment(Long lessonId, @Valid AssignmentData data) {
        Assignment assignment = new Assignment();
        assignment.setLesson(requireLesson(lessonId));
        fillAssignment(assignment, data);
        return ViewMapper.assignment(assignments.save(assignment));
    }

    @Transactional
    public AssignmentView updateAssignment(Long id, @Valid AssignmentData data) {
        Assignment assignment = requireAssignment(id);
        if (assignment.getSubmissions().stream().anyMatch(submission -> submission.getScore() != null
                && submission.getScore() > data.maxScore())) {
            throw BusinessException.conflict("Новый максимум меньше уже выставленной оценки");
        }
        fillAssignment(assignment, data);
        return ViewMapper.assignment(assignment);
    }

    @Transactional
    public void deleteAssignment(Long id) {
        assignments.delete(requireAssignment(id));
    }

    private CourseModule requireModule(Long id) {
        return modules.findById(id).orElseThrow(() -> BusinessException.notFound("Модуль"));
    }

    private Lesson requireLesson(Long id) {
        return lessons.findById(id).orElseThrow(() -> BusinessException.notFound("Урок"));
    }

    private Assignment requireAssignment(Long id) {
        return assignments.findById(id).orElseThrow(() -> BusinessException.notFound("Задание"));
    }

    private void fillLesson(Lesson lesson, LessonData data) {
        lesson.setTitle(data.title().trim());
        lesson.setContent(data.content().trim());
        lesson.setVideoUrl(data.videoUrl());
        lesson.setOrderIndex(data.orderIndex());
    }

    private void fillAssignment(Assignment assignment, AssignmentData data) {
        assignment.setTitle(data.title().trim());
        assignment.setDescription(data.description().trim());
        assignment.setDueDate(data.dueDate());
        assignment.setMaxScore(data.maxScore());
    }
}
