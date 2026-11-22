package ru.mifi.platform.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.mifi.platform.dto.Requests.NameData;
import ru.mifi.platform.dto.Views.NameView;
import ru.mifi.platform.model.Category;
import ru.mifi.platform.model.Tag;
import ru.mifi.platform.repository.CategoryRepository;
import ru.mifi.platform.repository.CourseRepository;
import ru.mifi.platform.repository.TagRepository;

import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReferenceService {
    private final CategoryRepository categories;
    private final TagRepository tags;
    private final CourseRepository courses;

    public List<NameView> categories() {
        return categories.findAll(Sort.by("name")).stream()
                .map(category -> new NameView(category.getId(), category.getName())).toList();
    }

    @Transactional
    public NameView createCategory(@Valid NameData data) {
        if (categories.existsByNameIgnoreCase(data.name().trim())) {
            throw BusinessException.conflict("Такая категория уже существует");
        }
        Category category = new Category();
        category.setName(data.name().trim());
        categories.save(category);
        return new NameView(category.getId(), category.getName());
    }

    @Transactional
    public NameView updateCategory(Long id, @Valid NameData data) {
        Category category = categories.findById(id).orElseThrow(() -> BusinessException.notFound("Категория"));
        if (categories.existsByNameIgnoreCaseAndIdNot(data.name().trim(), id)) {
            throw BusinessException.conflict("Такая категория уже существует");
        }
        category.setName(data.name().trim());
        return new NameView(id, category.getName());
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categories.findById(id).orElseThrow(() -> BusinessException.notFound("Категория"));
        if (courses.existsByCategoryId(id)) {
            throw BusinessException.conflict("Нельзя удалить категорию, пока в ней есть курсы");
        }
        categories.delete(category);
    }

    public List<NameView> tags() {
        return tags.findAll(Sort.by("name")).stream()
                .map(tag -> new NameView(tag.getId(), tag.getName())).toList();
    }

    @Transactional
    public NameView createTag(@Valid NameData data) {
        if (tags.existsByNameIgnoreCase(data.name().trim())) {
            throw BusinessException.conflict("Такой тег уже существует");
        }
        Tag tag = new Tag();
        tag.setName(data.name().trim());
        tags.save(tag);
        return new NameView(tag.getId(), tag.getName());
    }

    @Transactional
    public NameView updateTag(Long id, @Valid NameData data) {
        Tag tag = tags.findById(id).orElseThrow(() -> BusinessException.notFound("Тег"));
        if (tags.existsByNameIgnoreCaseAndIdNot(data.name().trim(), id)) {
            throw BusinessException.conflict("Такой тег уже существует");
        }
        tag.setName(data.name().trim());
        return new NameView(id, tag.getName());
    }

    @Transactional
    public void deleteTag(Long id) {
        Tag tag = tags.findById(id).orElseThrow(() -> BusinessException.notFound("Тег"));
        if (!tag.getCourses().isEmpty()) {
            throw BusinessException.conflict("Сначала уберите тег из курсов");
        }
        tags.delete(tag);
    }
}
