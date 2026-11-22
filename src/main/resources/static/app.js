const state = {
  users: [],
  categories: [],
  tags: [],
  courses: [],
  enrollments: [],
  submissions: [],
  results: [],
  page: 0,
  totalPages: 0,
  selectedId: null,
  detail: null,
  userId: null,
};
const $ = (selector) => document.querySelector(selector);
const escapeHtml = (value) =>
  String(value ?? "").replace(
    /[&<>"']/g,
    (char) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        char
      ],
  );
const currentUser = () => state.users.find((user) => user.id === state.userId);
const isTeacher = () => ["TEACHER", "ADMIN"].includes(currentUser()?.role);
const roleGroups = {
  STUDENT: "Студенты",
  TEACHER: "Преподаватели",
  ADMIN: "Администраторы",
};
const dateText = (value) =>
  value
    ? new Date(value).toLocaleString("ru-RU", {
        dateStyle: "short",
        timeStyle: "short",
      })
    : "без срока";

async function api(path, options = {}) {
  const config = { ...options };
  if (config.body !== undefined) {
    config.headers = { "Content-Type": "application/json" };
    config.body = JSON.stringify(config.body);
  }
  const response = await fetch("/api" + path, config);
  if (response.status === 204) return null;
  const data = await response.json();
  if (!response.ok)
    throw new Error(
      [
        data.message || "Не удалось выполнить запрос",
        ...(data.details || []),
      ].join("\n"),
    );
  return data;
}

function notice(message, error = false) {
  $("#notice").textContent = message;
  $("#notice").classList.toggle("error", error);
  $("#notice").hidden = !message;
}

async function run(action) {
  try {
    await action();
  } catch (error) {
    notice(error.message || "Не удалось связаться с сервером", true);
  }
}

function nameOptions(items, selected) {
  return items
    .map(
      (item) =>
        `<option value="${item.id}" ${item.id === selected ? "selected" : ""}>${escapeHtml(item.name)}</option>`,
    )
    .join("");
}

function courseFields(course = {}) {
  return `<label>Название<input name="title" required maxlength="200" value="${escapeHtml(course.title)}"></label>
        <label>Описание<textarea name="description" required maxlength="5000">${escapeHtml(course.description)}</textarea></label>
        <label>Преподаватель<select name="teacherId" required>${nameOptions(
          state.users.filter((user) => user.role === "TEACHER"),
          course.teacherId || state.userId,
        )}</select></label>
        <label>Категория<select name="categoryId" required>${nameOptions(state.categories, course.categoryId)}</select></label>
        <fieldset><legend>Теги</legend>${state.tags.map((tag) => `<label class="option"><input type="checkbox" name="tagIds" value="${tag.id}" ${state.detail?.tags.some((item) => item.id === tag.id) && course.id ? "checked" : ""}>${escapeHtml(tag.name)}</label>`).join("") || '<span class="muted">Теги пока не добавлены</span>'}</fieldset>
        <button type="submit">${course.id ? "Сохранить изменения" : "Создать курс"}</button>`;
}

async function loadReferences() {
  [state.users, state.categories, state.tags] = await Promise.all([
    api("/users"),
    api("/categories"),
    api("/tags"),
  ]);
  if (!state.users.some((user) => user.id === state.userId))
    state.userId =
      state.users.find((user) => user.role === "STUDENT")?.id ||
      state.users[0]?.id;
  $("#current-user").innerHTML = Object.entries(roleGroups)
    .map(([role, label]) => {
      const usersInRole = state.users.filter((user) => user.role === role);
      if (usersInRole.length === 0) {
        return "";
      }
      const options = nameOptions(usersInRole, state.userId);
      return `<optgroup label="${label}">${options}</optgroup>`;
    })
    .join("");
  const filter = $("#category-filter").value;
  $("#category-filter").innerHTML =
    '<option value="">Все категории</option>' +
    nameOptions(state.categories, Number(filter));
  $("#management").hidden = !isTeacher();
  $("#new-course").innerHTML = courseFields();
}

async function loadUserData() {
  if (currentUser()?.role !== "STUDENT") {
    state.enrollments = [];
    state.submissions = [];
    state.results = [];
    $("#my-progress").innerHTML =
      '<h3>Преподавателю</h3><p class="muted">Откройте курс, чтобы добавить материалы или проверить решения студентов.</p>';
    return;
  }
  [state.enrollments, state.submissions, state.results] = await Promise.all([
    api(`/users/${state.userId}/enrollments`),
    api(`/users/${state.userId}/submissions`),
    api(`/users/${state.userId}/quiz-results`),
  ]);
  $("#my-progress").innerHTML =
    `<h3>Моя учёба</h3><p>Активных курсов: <strong>${state.enrollments.filter((item) => item.status === "ACTIVE").length}</strong></p>
        <p>Сдано работ: <strong>${state.submissions.length}</strong></p><p>Пройдено тестов: <strong>${state.results.length}</strong></p>`;
}

async function loadCourses() {
  const query = new URLSearchParams({
    search: $("#search").value,
    page: state.page,
    size: 5,
  });
  if ($("#category-filter").value)
    query.set("categoryId", $("#category-filter").value);
  const page = await api("/courses?" + query);
  state.courses = page.content;
  state.totalPages = page.totalPages;
  $("#course-count").textContent = page.totalElements;
  $("#page-info").textContent =
    `${page.totalPages ? page.page + 1 : 0} / ${page.totalPages}`;
  $("#previous-page").disabled = state.page === 0;
  $("#next-page").disabled = state.page + 1 >= page.totalPages;
  renderCatalog();
}

function renderCatalog() {
  $("#course-list").innerHTML =
    state.courses
      .map(
        (
          course,
        ) => `<button type="button" class="course-card ${course.id === state.selectedId ? "active" : ""}" data-action="open" data-id="${course.id}">
        <small>${escapeHtml(course.categoryName)}</small><strong>${escapeHtml(course.title)}</strong><small>${escapeHtml(course.teacherName)}</small></button>`,
      )
      .join("") ||
    '<p class="muted">Подходящих курсов нет. Попробуйте изменить поиск.</p>';
}

function assignmentHtml(assignment) {
  const solution = state.submissions.find(
    (item) => item.assignmentId === assignment.id,
  );
  let controls = "";
  if (isTeacher())
    controls = `<button class="secondary small" type="button" data-action="solutions" data-id="${assignment.id}">Проверить решения</button><div id="solutions-${assignment.id}"></div>`;
  else if (solution)
    controls = `<p class="score">${solution.score === null ? "Работа ожидает проверки" : `Оценка: ${solution.score} / ${assignment.maxScore}`}</p><p class="answer-text">${escapeHtml(solution.content)}</p>${solution.feedback ? `<p>Комментарий: ${escapeHtml(solution.feedback)}</p>` : ""}`;
  else if (currentUser()?.role === "STUDENT")
    controls = `<form data-form="submission" data-id="${assignment.id}" class="stack"><label>Ваше решение<textarea name="content" required maxlength="10000" placeholder="Введите ответ или ссылку на работу"></textarea></label><button type="submit">Отправить на проверку</button></form>`;
  return `<section class="assignment"><h4>${escapeHtml(assignment.title)}</h4><p>${escapeHtml(assignment.description)}</p><p class="meta">Срок: ${dateText(assignment.dueDate)} · Максимум ${assignment.maxScore} баллов</p>${controls}</section>`;
}

function lessonHtml(lesson) {
  let video = "";
  if (lesson.videoUrl && /^https?:\/\//i.test(lesson.videoUrl))
    video = `<p><a href="${escapeHtml(lesson.videoUrl)}" target="_blank" rel="noopener noreferrer">Открыть видео к уроку</a></p>`;
  return `<details class="lesson" open><summary>${lesson.orderIndex}. ${escapeHtml(lesson.title)}</summary><div class="lesson-body"><p>${escapeHtml(lesson.content)}</p>${video}${lesson.assignments.map(assignmentHtml).join("")}
        ${isTeacher() ? `<details class="authoring"><summary>Добавить задание</summary><form data-form="assignment" data-id="${lesson.id}" class="stack"><label>Название<input name="title" required maxlength="200"></label><label>Условие<textarea name="description" required maxlength="5000"></textarea></label><div class="form-grid"><label>Срок сдачи<input name="dueDate" type="datetime-local"></label><label>Максимальный балл<input name="maxScore" type="number" value="100" min="1" max="1000" required></label></div><button type="submit">Добавить задание</button></form></details>` : ""}</div></details>`;
}

function moduleHtml(module) {
  const result = state.results.find((item) => item.quizId === module.quizId);
  return `<section class="module"><h3>${module.orderIndex}. ${escapeHtml(module.title)}</h3>${module.lessons.map(lessonHtml).join("") || '<p class="muted">Уроки ещё не добавлены.</p>'}
        ${module.quizId ? `<section class="quiz"><h4>Тест по модулю</h4>${result ? `<p class="score">Последний результат: ${result.score}%</p>` : ""}<button class="secondary" type="button" data-action="quiz" data-id="${module.quizId}">${isTeacher() ? "Посмотреть тест и результаты" : "Открыть тест"}</button><div id="quiz-${module.quizId}"></div></section>` : ""}
        ${
          isTeacher()
            ? `<details class="authoring"><summary>Добавить урок</summary><form data-form="lesson" data-id="${module.id}" class="stack"><label>Название<input name="title" required maxlength="200"></label><label>Материалы<textarea name="content" required maxlength="10000"></textarea></label><div class="form-grid"><label>Номер урока<input name="orderIndex" type="number" min="1" value="${module.lessons.length + 1}" required></label><label>Ссылка на видео<input name="videoUrl" type="url" maxlength="1000"></label></div><button type="submit">Добавить урок</button></form></details>
        ${!module.quizId ? `<details class="authoring"><summary>Добавить тест</summary><form data-form="quiz-create" data-id="${module.id}" class="stack"><label>Название теста<input name="title" required maxlength="200"></label><label>Вопрос<input name="question" required maxlength="2000"></label><label>Первый вариант<input name="first" required maxlength="2000"></label><label>Второй вариант<input name="second" required maxlength="2000"></label><label>Верный ответ<select name="correct"><option value="1">Первый вариант</option><option value="2">Второй вариант</option></select></label><button type="submit">Создать тест</button></form></details>` : ""}`
            : ""
        }</section>`;
}

async function openCourse(id) {
  state.detail = await api(`/courses/${id}`);
  state.selectedId = id;
  const { course, modules, tags } = state.detail;
  const enrollment = state.enrollments.find((item) => item.courseId === id);
  const active = enrollment?.status === "ACTIVE";
  $("#course-detail").innerHTML =
    `<div class="course-heading"><p class="eyebrow">${escapeHtml(course.categoryName)}</p><h2>${escapeHtml(course.title)}</h2><p>${escapeHtml(course.description)}</p><p class="meta">Преподаватель: ${escapeHtml(course.teacherName)}</p><div class="tags">${tags.map((tag) => `<span class="tag">${escapeHtml(tag.name)}</span>`).join("")}</div>
        ${currentUser()?.role === "STUDENT" ? `<div class="actions"><button type="button" class="${active ? "secondary" : ""}" data-action="${active ? "leave" : "enroll"}" data-id="${id}">${active ? "Отписаться от курса" : "Записаться на курс"}</button><span class="muted">${active ? "Вы записаны на этот курс" : enrollment?.status === "COMPLETED" ? "Курс завершён" : "Для сдачи работ нужна запись на курс"}</span></div>` : ""}
        ${isTeacher() ? `<details class="authoring"><summary>Изменить курс</summary><form data-form="course-edit" data-id="${id}" class="stack">${courseFields(course)}</form></details><button type="button" class="danger small" data-action="delete-course" data-id="${id}">Удалить курс</button>` : ""}</div>
        ${modules.map(moduleHtml).join("") || '<p class="muted">Учебные материалы готовятся.</p>'}
        ${isTeacher() ? `<details class="authoring"><summary>Добавить модуль</summary><form data-form="module" data-id="${id}" class="stack"><label>Название<input name="title" required maxlength="200"></label><label>Номер модуля<input name="orderIndex" type="number" min="1" value="${modules.length + 1}" required></label><button type="submit">Добавить модуль</button></form></details>` : ""}
        <section><h3>Отзывы о курсе</h3><div id="reviews"></div>${currentUser()?.role === "STUDENT" && enrollment ? `<form data-form="review" data-id="${id}" class="stack"><label>Оценка<select name="rating">${[5, 4, 3, 2, 1].map((rating) => `<option>${rating}</option>`).join("")}</select></label><label>Ваш отзыв<textarea name="comment" required maxlength="3000"></textarea></label><button type="submit">Оставить отзыв</button></form>` : ""}</section>`;
  renderCatalog();
  const reviews = await api(`/courses/${id}/reviews`);
  $("#reviews").innerHTML =
    reviews
      .map(
        (review) =>
          `<article class="review"><p><strong>${escapeHtml(review.studentName)}</strong> · ${review.rating} / 5</p><p>${escapeHtml(review.comment)}</p></article>`,
      )
      .join("") || '<p class="muted">Пока нет отзывов.</p>';
}

async function showQuiz(id) {
  const quiz = await api(`/quizzes/${id}`);
  let html = `<h4>${escapeHtml(quiz.title)}</h4><form data-form="quiz-submit" data-id="${id}" class="stack">${quiz.questions.map((question) => `<fieldset data-question="${question.id}"><legend>${escapeHtml(question.text)}</legend><p class="meta">${question.type === "SINGLE_CHOICE" ? "Выберите один вариант" : "Выберите все верные варианты"}</p>${question.options.map((option) => `<label class="option"><input type="${question.type === "SINGLE_CHOICE" ? "radio" : "checkbox"}" name="q${question.id}" value="${option.id}">${escapeHtml(option.text)}</label>`).join("")}</fieldset>`).join("")}${currentUser()?.role === "STUDENT" ? '<button type="submit">Завершить тест</button>' : ""}</form>`;
  if (isTeacher()) {
    const results = await api(`/quizzes/${id}/submissions`);
    html +=
      "<h4>Результаты студентов</h4>" +
      (results
        .map(
          (result) =>
            `<p>${escapeHtml(state.users.find((user) => user.id === result.studentId)?.name)}: <strong>${result.score}%</strong> · ${dateText(result.takenAt)}</p>`,
        )
        .join("") || '<p class="muted">Тест ещё никто не проходил.</p>');
  }
  $(`#quiz-${id}`).innerHTML = html;
}

async function showSolutions(id) {
  const solutions = await api(`/assignments/${id}/submissions`);
  $(`#solutions-${id}`).innerHTML =
    solutions
      .map(
        (solution) =>
          `<article class="review"><h4>${escapeHtml(solution.studentName)}</h4><p class="answer-text">${escapeHtml(solution.content)}</p><p class="meta">${dateText(solution.submittedAt)}</p><form data-form="grade" data-id="${solution.id}" class="stack"><label>Баллы<input name="score" type="number" min="0" required value="${solution.score ?? ""}"></label><label>Комментарий<textarea name="feedback" maxlength="3000">${escapeHtml(solution.feedback)}</textarea></label><button type="submit">Сохранить оценку</button></form></article>`,
      )
      .join("") || '<p class="muted">Решений пока нет.</p>';
}

async function refresh() {
  await loadUserData();
  await loadCourses();
  if (state.selectedId) await openCourse(state.selectedId);
}

document.addEventListener("click", (event) => {
  const button = event.target.closest("button[data-action]");
  if (!button) return;
  run(async () => {
    const id = Number(button.dataset.id);
    switch (button.dataset.action) {
      case "open":
        await openCourse(id);
        break;
      case "enroll":
        await api(`/courses/${id}/enrollments`, {
          method: "POST",
          body: { studentId: state.userId },
        });
        await refresh();
        notice("Вы записаны на курс. Можно приступать к заданиям.");
        break;
      case "leave":
        await api(`/courses/${id}/students/${state.userId}`, {
          method: "DELETE",
        });
        await refresh();
        notice("Запись на курс отменена.");
        break;
      case "quiz":
        await showQuiz(id);
        break;
      case "solutions":
        await showSolutions(id);
        break;
      case "delete-course":
        if (
          !window.confirm(
            "Удалить курс вместе с уроками, заданиями и результатами студентов?",
          )
        )
          return;
        await api(`/courses/${id}`, { method: "DELETE" });
        state.selectedId = null;
        state.detail = null;
        $("#course-detail").innerHTML =
          '<div class="empty"><h2>Курс удалён</h2><p>Выберите другой курс из каталога.</p></div>';
        state.page = 0;
        await refresh();
        notice("Курс и связанные учебные данные удалены.");
        break;
    }
  });
});

document.addEventListener("submit", (event) => {
  const form = event.target;
  if (!form.dataset.form) return;
  event.preventDefault();
  run(async () => {
    const data = new FormData(form);
    const text = (key) => String(data.get(key) ?? "").trim();
    const number = (key) => Number(data.get(key));
    const id = Number(form.dataset.id);
    switch (form.dataset.form) {
      case "search":
        state.page = 0;
        await loadCourses();
        return;
      case "course":
      case "course-edit": {
        const body = {
          title: text("title"),
          description: text("description"),
          teacherId: number("teacherId"),
          categoryId: number("categoryId"),
          tagIds: data.getAll("tagIds").map(Number),
        };
        const course = await api(
          form.dataset.form === "course" ? "/courses" : `/courses/${id}`,
          { method: form.dataset.form === "course" ? "POST" : "PUT", body },
        );
        state.selectedId = course.id;
        break;
      }
      case "user":
        await api("/users", {
          method: "POST",
          body: {
            name: text("name"),
            email: text("email"),
            role: text("role"),
          },
        });
        await loadReferences();
        break;
      case "category":
      case "tag":
        await api(form.dataset.form === "category" ? "/categories" : "/tags", {
          method: "POST",
          body: { name: text("name") },
        });
        await loadReferences();
        break;
      case "module":
        await api(`/courses/${id}/modules`, {
          method: "POST",
          body: { title: text("title"), orderIndex: number("orderIndex") },
        });
        break;
      case "lesson":
        await api(`/modules/${id}/lessons`, {
          method: "POST",
          body: {
            title: text("title"),
            content: text("content"),
            videoUrl: text("videoUrl") || null,
            orderIndex: number("orderIndex"),
          },
        });
        break;
      case "assignment":
        await api(`/lessons/${id}/assignments`, {
          method: "POST",
          body: {
            title: text("title"),
            description: text("description"),
            maxScore: number("maxScore"),
            dueDate: text("dueDate")
              ? new Date(text("dueDate")).toISOString()
              : null,
          },
        });
        break;
      case "submission":
        await api(`/assignments/${id}/submissions`, {
          method: "POST",
          body: { studentId: state.userId, content: text("content") },
        });
        break;
      case "grade":
        await api(`/submissions/${id}/grade`, {
          method: "PATCH",
          body: {
            teacherId: state.userId,
            score: number("score"),
            feedback: text("feedback"),
          },
        });
        notice("Оценка сохранена.");
        return;
      case "quiz-create":
        await api(`/modules/${id}/quiz`, {
          method: "POST",
          body: {
            title: text("title"),
            questions: [
              {
                text: text("question"),
                type: "SINGLE_CHOICE",
                options: [
                  { text: text("first"), correct: text("correct") === "1" },
                  { text: text("second"), correct: text("correct") === "2" },
                ],
              },
            ],
          },
        });
        break;
      case "quiz-submit": {
        const answers = [
          ...form.querySelectorAll("fieldset[data-question]"),
        ].map((fieldset) => ({
          questionId: Number(fieldset.dataset.question),
          optionIds: [...fieldset.querySelectorAll("input:checked")].map(
            (input) => Number(input.value),
          ),
        }));
        const result = await api(`/quizzes/${id}/submissions`, {
          method: "POST",
          body: { studentId: state.userId, answers },
        });
        await refresh();
        notice(`Тест завершён. Ваш результат: ${result.score}%.`);
        return;
      }
      case "review":
        await api(`/courses/${id}/reviews`, {
          method: "POST",
          body: {
            studentId: state.userId,
            rating: number("rating"),
            comment: text("comment"),
          },
        });
        break;
    }
    form.reset();
    await refresh();
    notice("Данные сохранены.");
  });
});

$("#current-user").addEventListener("change", () =>
  run(async () => {
    state.userId = Number($("#current-user").value);
    $("#management").hidden = !isTeacher();
    $("#new-course").innerHTML = courseFields();
    notice("");
    await refresh();
  }),
);
$("#previous-page").addEventListener("click", () =>
  run(async () => {
    state.page--;
    await loadCourses();
  }),
);
$("#next-page").addEventListener("click", () =>
  run(async () => {
    state.page++;
    await loadCourses();
  }),
);

run(async () => {
  await loadReferences();
  await loadUserData();
  await loadCourses();
  if (state.courses.length) await openCourse(state.courses[0].id);
});
