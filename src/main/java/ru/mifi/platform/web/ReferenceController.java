package ru.mifi.platform.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.mifi.platform.dto.Requests.NameData;
import ru.mifi.platform.dto.Views.NameView;
import ru.mifi.platform.service.ReferenceService;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReferenceController {
    private final ReferenceService references;

    @GetMapping("/categories")
    public List<NameView> categories() {
        return references.categories();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public NameView createCategory(@RequestBody @Valid NameData data) {
        return references.createCategory(data);
    }

    @PutMapping("/categories/{id}")
    public NameView updateCategory(@PathVariable Long id, @RequestBody @Valid NameData data) {
        return references.updateCategory(id, data);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) {
        references.deleteCategory(id);
    }

    @GetMapping("/tags")
    public List<NameView> tags() {
        return references.tags();
    }

    @PostMapping("/tags")
    @ResponseStatus(HttpStatus.CREATED)
    public NameView createTag(@RequestBody @Valid NameData data) {
        return references.createTag(data);
    }

    @PutMapping("/tags/{id}")
    public NameView updateTag(@PathVariable Long id, @RequestBody @Valid NameData data) {
        return references.updateTag(id, data);
    }

    @DeleteMapping("/tags/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTag(@PathVariable Long id) {
        references.deleteTag(id);
    }
}
