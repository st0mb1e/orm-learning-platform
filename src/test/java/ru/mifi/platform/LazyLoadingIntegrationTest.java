package ru.mifi.platform;

import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mifi.platform.dto.Requests.*;
import ru.mifi.platform.dto.Views.*;
import ru.mifi.platform.model.Course;
import ru.mifi.platform.model.Role;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class LazyLoadingIntegrationTest extends IntegrationTestSupport {
    @Test
    @DisplayName("Обращение к незагруженной коллекции после закрытия сессии вызывает LazyInitializationException")
    void detachedCollectionThrows() {
        Fixture data = fixture();
        Course detached = inTransaction(() -> courseRepository.findById(data.courseId()).orElseThrow());
        assertThat(Hibernate.isInitialized(detached.getModules())).isFalse();
        assertThatThrownBy(() -> detached.getModules().size()).isInstanceOf(LazyInitializationException.class);
    }

    @Test
    @DisplayName("EntityGraph позволяет читать модули после закрытия транзакции")
    void entityGraphInitializesModules() {
        Fixture data = fixture();
        Course detached = inTransaction(() -> courseRepository.findDetailedById(data.courseId()).orElseThrow());
        assertThat(Hibernate.isInitialized(detached.getModules())).isTrue();
        assertThat(detached.getModules()).hasSize(1);
        assertThat(detached.getModules().get(0).getTitle()).isEqualTo("Связи");
    }

    @Test
    @DisplayName("DTO содержит вложенный контент и не зависит от открытой сессии")
    void dtoCanBeReadOutsideTransaction() {
        Fixture data = fixture();
        CourseDetail course = courses.get(data.courseId());
        assertThat(course.modules().get(0).lessons().get(0).assignments().get(0).title()).isEqualTo("Модель курса");
        assertThat(course.tags()).hasSize(1);
    }

    @Test
    @DisplayName("Страница курсов с разными преподавателями не создаёт N+1 запросов")
    void courseListUsesBoundedQueries() {
        Fixture data = fixture();
        for (int index = 0; index < 6; index++) {
            UserView teacher = users.create(new UserData("Преподаватель " + index, "teacher" + index + "@example.org", Role.TEACHER));
            users.saveProfile(teacher.id(), new ProfileData("Профиль преподавателя", null));
            courses.create(new CourseData("Курс " + index, "Описание", teacher.id(), data.categoryId(), Set.of()));
        }
        Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        PageView<CourseSummary> page = courses.list("", null, null, 0, 5);
        assertThat(page.content()).hasSize(5);
        assertThat(page.totalElements()).isEqualTo(7);
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Несколько тегов и модулей не дублируют модули в DTO")
    void multipleCollectionsDoNotDuplicateModules() {
        Fixture data = fixture();
        NameView tag = references.createTag(new NameData("Hibernate"));
        courses.update(data.courseId(), new CourseData("Hibernate", "Описание", data.teacherId(), data.categoryId(),
                Set.of(data.tagId(), tag.id())));
        content.createModule(data.courseId(), new ModuleData("Транзакции", 2));
        CourseDetail course = courses.get(data.courseId());
        assertThat(course.modules()).hasSize(2);
        assertThat(course.tags()).hasSize(2);
    }
}
