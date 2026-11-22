package ru.mifi.platform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import ru.mifi.platform.dto.Requests.*;
import ru.mifi.platform.dto.Views.*;
import ru.mifi.platform.model.*;
import ru.mifi.platform.service.BusinessException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class PlatformIntegrationTest extends IntegrationTestSupport {
    @Test
    @DisplayName("Hibernate создаёт все таблицы и таблицу связи курсов с тегами")
    void createsSchema() {
        assertThat(entityManager.getMetamodel().getEntities()).hasSize(15);
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);
        assertThat(tables).contains("users", "profiles", "categories", "courses", "course_modules", "lessons",
                "assignments", "submissions", "enrollments", "quizzes", "questions", "answer_options",
                "quiz_submissions", "course_reviews", "tags", "course_tags");
    }

    @Test
    @DisplayName("Пользователя и его профиль можно создать, прочитать, изменить и удалить")
    void userAndProfileCrud() {
        UserView user = users.create(new UserData("Анна", "anna@example.org", Role.STUDENT));
        ProfileView profile = users.saveProfile(user.id(), new ProfileData("Изучаю Java", null));
        assertThat(users.getProfile(user.id()).bio()).isEqualTo("Изучаю Java");
        ProfileView changed = users.saveProfile(user.id(), new ProfileData("Изучаю JPA", "https://example.org/avatar.png"));
        assertThat(changed.id()).isEqualTo(profile.id());
        users.update(user.id(), new UserData("Анна Петрова", "anna.new@example.org", Role.STUDENT));
        assertThat(users.get(user.id()).name()).isEqualTo("Анна Петрова");
        users.deleteProfile(user.id());
        assertThat(profileRepository.count()).isZero();
        users.delete(user.id());
        assertThat(userRepository.existsById(user.id())).isFalse();
    }

    @Test
    @DisplayName("Email нормализуется, повторная регистрация отклоняется")
    void preventsDuplicateEmails() {
        users.create(new UserData("Анна", "Anna@Example.org", Role.STUDENT));
        assertThat(users.list().get(0).email()).isEqualTo("anna@example.org");
        assertThatThrownBy(() -> users.create(new UserData("Другая Анна", "ANNA@example.org", Role.STUDENT)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("email");
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Структура курса читается целиком, изменения контента сохраняются")
    void courseAndContentCrud() {
        Fixture data = fixture();
        CourseDetail course = courses.get(data.courseId());
        assertThat(course.modules()).hasSize(1);
        assertThat(course.modules().get(0).lessons().get(0).assignments()).hasSize(1);
        courses.update(data.courseId(), new CourseData("JPA и Hibernate", "Новое описание", data.teacherId(),
                data.categoryId(), Set.of(data.tagId())));
        content.updateModule(data.moduleId(), new ModuleData("Сущности", 2));
        content.updateLesson(data.lessonId(), new LessonData("Связи JPA", "Новый текст", null, 2));
        content.updateAssignment(data.assignmentId(), new AssignmentData("Модель обучения", "Новое условие", null, 80));
        assertThat(courses.get(data.courseId()).course().title()).isEqualTo("JPA и Hibernate");
        assertThat(content.getModule(data.moduleId()).orderIndex()).isEqualTo(2);
        assertThat(content.getLesson(data.lessonId()).content()).isEqualTo("Новый текст");
        assertThat(content.getAssignment(data.assignmentId()).maxScore()).isEqualTo(80);
        content.deleteAssignment(data.assignmentId());
        content.deleteLesson(data.lessonId());
        content.deleteModule(data.moduleId());
        courses.delete(data.courseId());
        assertThat(courseRepository.count()).isZero();
    }

    @Test
    @DisplayName("Курс нельзя назначить студенту вместо преподавателя")
    void rejectsStudentAsTeacher() {
        Fixture data = fixture();
        assertThatThrownBy(() -> courses.create(new CourseData("Другой курс", "Описание", data.studentId(),
                data.categoryId(), Set.of()))).isInstanceOf(BusinessException.class).hasMessageContaining("TEACHER");
        assertThat(courseRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Запись уникальна; работают списки студентов, отписка и повторная запись")
    void enrollmentLifecycle() {
        Fixture data = fixture();
        EnrollmentView enrollment = enrollments.enroll(data.courseId(), data.studentId());
        assertThat(enrollments.studentCourses(data.studentId())).extracting(EnrollmentView::courseId).containsExactly(data.courseId());
        assertThat(enrollments.courseStudents(data.courseId())).extracting(UserView::id).containsExactly(data.studentId());
        assertThatThrownBy(() -> enrollments.enroll(data.courseId(), data.studentId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("уже записан");
        assertThatThrownBy(() -> enrollments.enroll(data.courseId(), data.teacherId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("студенту");
        enrollments.cancel(data.courseId(), data.studentId());
        assertThat(enrollmentRepository.existsById(enrollment.id())).isFalse();
        assertThat(enrollments.courseStudents(data.courseId())).isEmpty();
        assertThat(enrollments.enroll(data.courseId(), data.studentId()).id()).isNotEqualTo(enrollment.id());
    }

    @Test
    @DisplayName("Ограничение PostgreSQL запрещает дубли Enrollment даже в обход сервиса")
    void databaseRejectsDuplicateEnrollment() {
        Fixture data = fixture();
        enrollments.enroll(data.courseId(), data.studentId());
        assertThatThrownBy(() -> inTransaction(() -> {
            Enrollment duplicate = new Enrollment();
            duplicate.setCourse(courseRepository.findById(data.courseId()).orElseThrow());
            duplicate.setStudent(userRepository.findById(data.studentId()).orElseThrow());
            return enrollmentRepository.saveAndFlush(duplicate);
        })).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(enrollmentRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Студент сдаёт работу, преподаватель видит её и выставляет оценку")
    void submitsAndGradesAssignment() {
        Fixture data = fixture();
        enrollments.enroll(data.courseId(), data.studentId());
        SubmissionView submission = submissions.submit(data.assignmentId(), new SubmissionData(data.studentId(), "Моё решение"));
        assertThat(submission.score()).isNull();
        SubmissionView graded = submissions.grade(submission.id(), new GradeData(data.teacherId(), 90, "Хорошая работа"));
        assertThat(graded.score()).isEqualTo(90);
        assertThat(submissions.get(submission.id()).feedback()).isEqualTo("Хорошая работа");
        assertThat(submissions.list(data.assignmentId())).hasSize(1);
        assertThat(submissions.studentSubmissions(data.studentId())).hasSize(1);
        assertThatThrownBy(() -> submissions.submit(data.assignmentId(), new SubmissionData(data.studentId(), "Вторая попытка")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("повторная сдача");
        assertThat(submissionRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Без активной записи сдавать задание нельзя")
    void requiresActiveEnrollment() {
        Fixture data = fixture();
        assertThatThrownBy(() -> submissions.submit(data.assignmentId(), new SubmissionData(data.studentId(), "Ответ")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("активная запись");
        EnrollmentView enrollment = enrollments.enroll(data.courseId(), data.studentId());
        enrollments.changeStatus(enrollment.id(), EnrollmentStatus.COMPLETED);
        assertThatThrownBy(() -> submissions.submit(data.assignmentId(), new SubmissionData(data.studentId(), "Ответ")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("активная запись");
        assertThat(submissionRepository.count()).isZero();
    }

    @Test
    @DisplayName("Просроченное задание не принимает новое решение")
    void rejectsLateSubmission() {
        Fixture data = fixture();
        enrollments.enroll(data.courseId(), data.studentId());
        content.updateAssignment(data.assignmentId(), new AssignmentData("Работа", "Условие", Instant.now().minus(1, ChronoUnit.DAYS), 100));
        assertThatThrownBy(() -> submissions.submit(data.assignmentId(), new SubmissionData(data.studentId(), "Ответ")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("истёк");
    }

    @Test
    @DisplayName("Проверяется преподаватель и диапазон оценки")
    void validatesGrading() {
        Fixture data = fixture();
        enrollments.enroll(data.courseId(), data.studentId());
        SubmissionView submission = submissions.submit(data.assignmentId(), new SubmissionData(data.studentId(), "Ответ"));
        assertThatThrownBy(() -> submissions.grade(submission.id(), new GradeData(data.teacherId(), 101, "Ошибка")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("100");
        assertThatThrownBy(() -> submissions.grade(submission.id(), new GradeData(data.otherStudentId(), 80, "Чужая оценка")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        submissions.grade(submission.id(), new GradeData(data.teacherId(), 95, "Проверено"));
        assertThatThrownBy(() -> content.updateAssignment(data.assignmentId(), new AssignmentData("Работа", "Условие", null, 90)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("выставленной оценки");
    }

    @Test
    @DisplayName("Тест сохраняет вопросы, варианты и результаты прохождения")
    void quizWorkflow() {
        Fixture data = fixture();
        enrollments.enroll(data.courseId(), data.studentId());
        QuizView quiz = quizzes.create(data.moduleId(), quizData());
        assertThat(questionRepository.count()).isEqualTo(2);
        assertThat(optionRepository.count()).isEqualTo(5);
        QuizResult result = quizzes.take(quiz.id(), correctAttempt(quiz, data.studentId()));
        assertThat(result.score()).isEqualTo(100);
        assertThat(quizzes.studentResults(data.studentId())).hasSize(1);
        assertThat(quizzes.quizResults(quiz.id(), null)).hasSize(1);
        assertThat(quizzes.quizResults(quiz.id(), data.studentId()).get(0).id()).isEqualTo(result.id());
        QuizResult empty = quizzes.take(quiz.id(), new QuizAttempt(data.studentId(), List.of()));
        assertThat(empty.score()).isZero();
        assertThatThrownBy(() -> quizzes.update(quiz.id(), quizData()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("уже есть результаты");
    }

    @Test
    @DisplayName("Некорректная попытка не сохраняет результат теста")
    void quizRejectsForeignOptions() {
        Fixture data = fixture();
        enrollments.enroll(data.courseId(), data.studentId());
        QuizView quiz = quizzes.create(data.moduleId(), quizData());
        QuizAttempt invalid = new QuizAttempt(data.studentId(), List.of(new AnswerData(quiz.questions().get(0).id(), Set.of(9999L))));
        assertThatThrownBy(() -> quizzes.take(quiz.id(), invalid))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Вариант ответа");
        assertThat(quizSubmissionRepository.count()).isZero();
    }

    @Test
    @DisplayName("Тест без попыток можно изменить, удалить вместе с вопросами и создать заново")
    void quizCrudAndOrphanRemoval() {
        Fixture data = fixture();
        QuizView quiz = quizzes.create(data.moduleId(), quizData());
        quizzes.update(quiz.id(), new QuizData("Новая версия", List.of(quizData().questions().get(0))));
        assertThat(quizzes.get(quiz.id()).title()).isEqualTo("Новая версия");
        assertThat(questionRepository.count()).isEqualTo(1);
        assertThat(optionRepository.count()).isEqualTo(2);
        quizzes.delete(quiz.id());
        assertThat(questionRepository.count()).isZero();
        assertThat(optionRepository.count()).isZero();
        assertThat(quizzes.create(data.moduleId(), quizData()).id()).isNotEqualTo(quiz.id());
    }

    @Test
    @DisplayName("Отзыв уникален для пары студент–курс, его можно изменить и удалить")
    void reviewsCrud() {
        Fixture data = fixture();
        enrollments.enroll(data.courseId(), data.studentId());
        ReviewView review = reviews.create(data.courseId(), new ReviewData(data.studentId(), 4, "Полезный курс"));
        assertThat(reviews.list(data.courseId())).hasSize(1);
        assertThatThrownBy(() -> reviews.create(data.courseId(), new ReviewData(data.studentId(), 5, "Ещё один")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("уже оставлен");
        reviews.update(review.id(), new ReviewData(data.studentId(), 5, "Разобрался с транзакциями"));
        assertThat(reviews.list(data.courseId()).get(0).rating()).isEqualTo(5);
        reviews.delete(review.id());
        assertThat(reviewRepository.count()).isZero();
    }

    @Test
    @DisplayName("Удаление курса удаляет контент и результаты, сохраняя пользователей и общие теги")
    void courseDeletionCascades() {
        Fixture data = fixture();
        enrollments.enroll(data.courseId(), data.studentId());
        submissions.submit(data.assignmentId(), new SubmissionData(data.studentId(), "Решение"));
        QuizView quiz = quizzes.create(data.moduleId(), quizData());
        quizzes.take(quiz.id(), correctAttempt(quiz, data.studentId()));
        reviews.create(data.courseId(), new ReviewData(data.studentId(), 5, "Хорошо"));
        courses.delete(data.courseId());
        assertThat(moduleRepository.count()).isZero();
        assertThat(lessonRepository.count()).isZero();
        assertThat(assignmentRepository.count()).isZero();
        assertThat(enrollmentRepository.count()).isZero();
        assertThat(submissionRepository.count()).isZero();
        assertThat(quizRepository.count()).isZero();
        assertThat(questionRepository.count()).isZero();
        assertThat(optionRepository.count()).isZero();
        assertThat(quizSubmissionRepository.count()).isZero();
        assertThat(reviewRepository.count()).isZero();
        assertThat(userRepository.count()).isEqualTo(3);
        assertThat(tagRepository.count()).isEqualTo(1);
        assertThat(categoryRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("ManyToMany сохраняет общий тег при удалении одного из курсов")
    void sharedTagsSurviveCourseDeletion() {
        Fixture data = fixture();
        CourseSummary another = courses.create(new CourseData("Spring", "Курс Spring", data.teacherId(), data.categoryId(), Set.of(data.tagId())));
        courses.delete(data.courseId());
        assertThat(courses.get(another.id()).tags()).extracting(NameView::id).containsExactly(data.tagId());
        assertThat(tagRepository.count()).isEqualTo(1);
        references.updateCategory(data.categoryId(), new NameData("Java-разработка"));
        references.updateTag(data.tagId(), new NameData("Java 17"));
        assertThat(references.categories().get(0).name()).isEqualTo("Java-разработка");
        courses.delete(another.id());
        references.deleteTag(data.tagId());
        references.deleteCategory(data.categoryId());
        assertThat(tagRepository.count()).isZero();
        assertThat(categoryRepository.count()).isZero();
    }

    @Test
    @DisplayName("Удаление студента удаляет его данные, но сохраняет курс и преподавателя")
    void studentDeletionCascades() {
        Fixture data = fixture();
        users.saveProfile(data.studentId(), new ProfileData("О себе", null));
        enrollments.enroll(data.courseId(), data.studentId());
        submissions.submit(data.assignmentId(), new SubmissionData(data.studentId(), "Ответ"));
        QuizView quiz = quizzes.create(data.moduleId(), quizData());
        quizzes.take(quiz.id(), correctAttempt(quiz, data.studentId()));
        reviews.create(data.courseId(), new ReviewData(data.studentId(), 5, "Отзыв"));
        users.delete(data.studentId());
        assertThat(profileRepository.count()).isZero();
        assertThat(enrollmentRepository.count()).isZero();
        assertThat(submissionRepository.count()).isZero();
        assertThat(quizSubmissionRepository.count()).isZero();
        assertThat(reviewRepository.count()).isZero();
        assertThat(courseRepository.existsById(data.courseId())).isTrue();
        assertThat(userRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Нельзя удалить используемые категорию, тег и преподавателя")
    void protectsReferencedEntities() {
        Fixture data = fixture();
        assertThatThrownBy(() -> references.deleteCategory(data.categoryId())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> references.deleteTag(data.tagId())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> users.delete(data.teacherId())).isInstanceOf(BusinessException.class);
        assertThat(courseRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Каскадное сохранение курса создаёт модуль и урок без отдельных save")
    void cascadePersistCreatesContent() {
        Fixture data = fixture();
        Long id = inTransaction(() -> {
            Course course = new Course();
            course.setTitle("Каскад");
            course.setDescription("Проверка persist");
            course.setTeacher(userRepository.findById(data.teacherId()).orElseThrow());
            course.setCategory(categoryRepository.findById(data.categoryId()).orElseThrow());
            CourseModule module = new CourseModule();
            module.setTitle("Модуль");
            module.setOrderIndex(1);
            module.setCourse(course);
            course.getModules().add(module);
            Lesson lesson = new Lesson();
            lesson.setTitle("Урок");
            lesson.setContent("Материал");
            lesson.setOrderIndex(1);
            lesson.setModule(module);
            module.getLessons().add(lesson);
            return courseRepository.saveAndFlush(course).getId();
        });
        assertThat(courses.get(id).modules().get(0).lessons()).hasSize(1);
        assertThat(moduleRepository.count()).isEqualTo(2);
        assertThat(lessonRepository.count()).isEqualTo(2);
    }
}
