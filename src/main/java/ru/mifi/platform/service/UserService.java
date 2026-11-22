package ru.mifi.platform.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.mifi.platform.dto.Requests.ProfileData;
import ru.mifi.platform.dto.Requests.UserData;
import ru.mifi.platform.dto.Views.ProfileView;
import ru.mifi.platform.dto.Views.UserView;
import ru.mifi.platform.model.Profile;
import ru.mifi.platform.model.User;
import ru.mifi.platform.repository.CourseRepository;
import ru.mifi.platform.repository.UserRepository;

import java.util.List;
import java.util.Locale;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository users;
    private final CourseRepository courses;

    public List<UserView> list() {
        return users.findAll(Sort.by("id")).stream().map(ViewMapper::user).toList();
    }

    public UserView get(Long id) {
        return ViewMapper.user(requireUser(id));
    }

    @Transactional
    public UserView create(@Valid UserData data) {
        if (users.existsByEmailIgnoreCase(data.email().trim())) {
            throw BusinessException.conflict("Пользователь с таким email уже существует");
        }
        User user = new User();
        fill(user, data);
        return ViewMapper.user(users.save(user));
    }

    @Transactional
    public UserView update(Long id, @Valid UserData data) {
        User user = requireUser(id);
        if (users.existsByEmailIgnoreCaseAndIdNot(data.email().trim(), id)) {
            throw BusinessException.conflict("Пользователь с таким email уже существует");
        }
        if (user.getRole() != data.role() && (!user.getCoursesTaught().isEmpty()
                || !user.getEnrollments().isEmpty() || !user.getSubmissions().isEmpty()
                || !user.getQuizSubmissions().isEmpty() || !user.getReviews().isEmpty())) {
            throw BusinessException.conflict("Нельзя менять роль пользователя, пока с ним связаны учебные данные");
        }
        fill(user, data);
        return ViewMapper.user(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = requireUser(id);
        if (courses.existsByTeacherId(id)) {
            throw BusinessException.conflict("Сначала назначьте курсам другого преподавателя или удалите курсы");
        }
        users.delete(user);
    }

    public ProfileView getProfile(Long userId) {
        Profile profile = requireUser(userId).getProfile();
        if (profile == null) {
            throw BusinessException.notFound("Профиль");
        }
        return ViewMapper.profile(profile);
    }

    @Transactional
    public ProfileView saveProfile(Long userId, @Valid ProfileData data) {
        User user = requireUser(userId);
        Profile profile = user.getProfile();
        if (profile == null) {
            profile = new Profile();
            profile.setUser(user);
            user.setProfile(profile);
        }
        profile.setBio(data.bio());
        profile.setAvatarUrl(data.avatarUrl());
        User saved = users.saveAndFlush(user);
        return ViewMapper.profile(saved.getProfile());
    }

    @Transactional
    public void deleteProfile(Long userId) {
        requireUser(userId).setProfile(null);
    }

    private User requireUser(Long id) {
        return users.findById(id).orElseThrow(() -> BusinessException.notFound("Пользователь"));
    }

    private void fill(User user, UserData data) {
        user.setName(data.name().trim());
        user.setEmail(data.email().trim().toLowerCase(Locale.ROOT));
        user.setRole(data.role());
    }
}
