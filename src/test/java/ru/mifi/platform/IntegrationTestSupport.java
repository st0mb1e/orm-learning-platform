package ru.mifi.platform;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import ru.mifi.platform.dto.Requests.*;
import ru.mifi.platform.dto.Views.*;
import ru.mifi.platform.model.QuestionType;
import ru.mifi.platform.model.Role;
import ru.mifi.platform.repository.*;
import ru.mifi.platform.service.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withTmpFs(Map.of("/var/lib/postgresql/data", "rw"));

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired protected UserService users;
    @Autowired protected ReferenceService references;
    @Autowired protected CourseService courses;
    @Autowired protected ContentService content;
    @Autowired protected EnrollmentService enrollments;
    @Autowired protected SubmissionService submissions;
    @Autowired protected QuizService quizzes;
    @Autowired protected ReviewService reviews;
    @Autowired protected UserRepository userRepository;
    @Autowired protected ProfileRepository profileRepository;
    @Autowired protected CourseRepository courseRepository;
    @Autowired protected CourseModuleRepository moduleRepository;
    @Autowired protected LessonRepository lessonRepository;
    @Autowired protected AssignmentRepository assignmentRepository;
    @Autowired protected EnrollmentRepository enrollmentRepository;
    @Autowired protected SubmissionRepository submissionRepository;
    @Autowired protected QuizRepository quizRepository;
    @Autowired protected QuestionRepository questionRepository;
    @Autowired protected AnswerOptionRepository optionRepository;
    @Autowired protected QuizSubmissionRepository quizSubmissionRepository;
    @Autowired protected CourseReviewRepository reviewRepository;
    @Autowired protected TagRepository tagRepository;
    @Autowired protected CategoryRepository categoryRepository;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected EntityManager entityManager;
    @Autowired protected PlatformTransactionManager transactionManager;
    @Autowired protected MockMvc mvc;

    @BeforeEach
    void clearDatabase() {
        // Это отдельная временная база Testcontainers. Каждому тесту нужны одни и те же исходные условия.
        jdbc.execute("TRUNCATE TABLE users, categories, tags RESTART IDENTITY CASCADE");
    }

    protected record Fixture(Long teacherId, Long studentId, Long otherStudentId, Long categoryId,
                             Long tagId, Long courseId, Long moduleId, Long lessonId, Long assignmentId) {
    }

    protected Fixture fixture() {
        UserView teacher = users.create(new UserData("Преподаватель", "teacher@example.org", Role.TEACHER));
        UserView student = users.create(new UserData("Студент", "student@example.org", Role.STUDENT));
        UserView other = users.create(new UserData("Второй студент", "other@example.org", Role.STUDENT));
        NameView category = references.createCategory(new NameData("Разработка"));
        NameView tag = references.createTag(new NameData("Java"));
        CourseSummary course = courses.create(new CourseData("Hibernate", "Курс по ORM", teacher.id(),
                category.id(), Set.of(tag.id())));
        ModuleView module = content.createModule(course.id(), new ModuleData("Связи", 1));
        LessonView lesson = content.createLesson(module.id(), new LessonData("OneToMany", "Материалы урока", null, 1));
        AssignmentView assignment = content.createAssignment(lesson.id(), new AssignmentData("Модель курса",
                "Реализуйте связь курса и модулей", Instant.now().plus(3, ChronoUnit.DAYS), 100));
        return new Fixture(teacher.id(), student.id(), other.id(), category.id(), tag.id(),
                course.id(), module.id(), lesson.id(), assignment.id());
    }

    protected QuizData quizData() {
        return new QuizData("Связи JPA", List.of(
                new QuestionData("Сторона внешнего ключа?", QuestionType.SINGLE_CHOICE,
                        List.of(new OptionData("Many", true), new OptionData("One", false))),
                new QuestionData("Ленивые коллекции?", QuestionType.MULTIPLE_CHOICE,
                        List.of(new OptionData("OneToMany", true), new OptionData("ManyToMany", true),
                                new OptionData("ManyToOne", false)))));
    }

    protected QuizAttempt correctAttempt(QuizView quiz, Long studentId) {
        return new QuizAttempt(studentId, List.of(
                new AnswerData(quiz.questions().get(0).id(), Set.of(quiz.questions().get(0).options().get(0).id())),
                new AnswerData(quiz.questions().get(1).id(), Set.of(quiz.questions().get(1).options().get(0).id(),
                        quiz.questions().get(1).options().get(1).id()))));
    }

    protected <T> T inTransaction(Supplier<T> work) {
        return new TransactionTemplate(transactionManager).execute(status -> work.get());
    }
}
