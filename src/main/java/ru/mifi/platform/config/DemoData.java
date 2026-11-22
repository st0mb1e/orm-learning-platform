package ru.mifi.platform.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.mifi.platform.dto.Requests.*;
import ru.mifi.platform.dto.Views.*;
import ru.mifi.platform.model.QuestionType;
import ru.mifi.platform.model.Role;
import ru.mifi.platform.repository.UserRepository;
import ru.mifi.platform.service.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DemoData implements CommandLineRunner {
    private final UserRepository userRepository;
    private final UserService users;
    private final ReferenceService references;
    private final CourseService courses;
    private final ContentService content;
    private final EnrollmentService enrollments;
    private final SubmissionService submissions;
    private final QuizService quizzes;
    private final ReviewService reviews;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() != 0) {
            return;
        }
        UserView teacher = users.create(new UserData("Ведущий курса", "teacher@example.org", Role.TEACHER));
        UserView exampleStudent = users.create(new UserData(
                "Студент с готовой работой", "student-example@example.org", Role.STUDENT));
        UserView practiceStudent = users.create(new UserData(
                "Студент для проверки", "student-practice@example.org", Role.STUDENT));
        users.saveProfile(exampleStudent.id(), new ProfileData("Изучаю Java и базы данных", null));
        users.saveProfile(teacher.id(), new ProfileData("Веду занятия по серверной разработке", null));
        NameView programming = references.createCategory(new NameData("Программирование"));
        NameView databases = references.createCategory(new NameData("Базы данных"));
        NameView java = references.createTag(new NameData("Java"));
        NameView orm = references.createTag(new NameData("ORM"));
        CourseSummary course = courses.create(new CourseData("Основы Hibernate",
                "Работа с сущностями, связями и транзакциями на примере учебного приложения.",
                teacher.id(), programming.id(), Set.of(java.id(), orm.id())));
        courses.create(new CourseData("SQL для разработчика",
                "Таблицы, ограничения и запросы PostgreSQL. Курс готовится к открытию.",
                teacher.id(), databases.id(), Set.of()));
        ModuleView first = content.createModule(course.id(), new ModuleData("Сущности и связи", 1));
        ModuleView second = content.createModule(course.id(), new ModuleData("Загрузка данных и транзакции", 2));
        LessonView entities = content.createLesson(first.id(), new LessonData("От таблицы к сущности",
                "Сущность JPA описывает таблицу, а её поля — столбцы. Аннотация @Id задаёт первичный ключ. "
                        + "В этой работе связи между объектами хранятся с помощью внешних ключей.", null, 1));
        LessonView lazy = content.createLesson(second.id(), new LessonData("Когда загружается коллекция",
                "При LAZY коллекция читается при первом обращении. Если сессия уже закрыта, Hibernate "
                        + "не сможет выполнить запрос. Нужные данные загружают внутри транзакции и передают через DTO.", null, 1));
        AssignmentView assignment = content.createAssignment(entities.id(), new AssignmentData("Связь курса и модулей",
                "Опишите двустороннюю связь Course и CourseModule. Объясните, где хранится внешний ключ.",
                Instant.now().plus(30, ChronoUnit.DAYS), 100));
        content.createAssignment(lazy.id(), new AssignmentData("Разбор ленивой загрузки",
                "Воспроизведите LazyInitializationException и предложите способ получить данные без ошибки.",
                Instant.now().plus(30, ChronoUnit.DAYS), 100));
        enrollments.enroll(course.id(), exampleStudent.id());
        enrollments.enroll(course.id(), practiceStudent.id());
        SubmissionView solution = submissions.submit(assignment.id(), new SubmissionData(exampleStudent.id(),
                "У курса коллекция modules с mappedBy=course, у модуля — ManyToOne. FK course_id находится в course_modules."));
        submissions.grade(solution.id(), new GradeData(teacher.id(), 95, "Связь описана верно. Укажите также поведение каскадов."));
        QuizView quiz = quizzes.create(first.id(), new QuizData("Проверка по связям JPA", List.of(
                new QuestionData("Где хранится внешний ключ при ManyToOne?", QuestionType.SINGLE_CHOICE, List.of(
                        new OptionData("На стороне Many", true), new OptionData("Всегда в отдельной таблице", false))),
                new QuestionData("Какие связи коллекций по умолчанию загружаются лениво?", QuestionType.MULTIPLE_CHOICE, List.of(
                        new OptionData("OneToMany", true), new OptionData("ManyToMany", true), new OptionData("ManyToOne", false))))));
        quizzes.take(quiz.id(), new QuizAttempt(exampleStudent.id(), List.of(
                new AnswerData(quiz.questions().get(0).id(), Set.of(quiz.questions().get(0).options().get(0).id())),
                new AnswerData(quiz.questions().get(1).id(), Set.of(quiz.questions().get(1).options().get(0).id(),
                        quiz.questions().get(1).options().get(1).id())))));
        reviews.create(course.id(), new ReviewData(exampleStudent.id(), 5, "Примеры помогли разобраться с владельцем связи."));
        log.info("Добавлены демонстрационные пользователи, 2 курса, задания и тест с результатом");
    }
}
