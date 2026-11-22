package ru.mifi.platform.web;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import ru.mifi.platform.service.BusinessException;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {
    public record ApiError(int status, String message, List<String> details, Instant timestamp) {
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> business(BusinessException exception) {
        log.info("Запрос отклонён: {}", exception.getMessage());
        return error(exception.getStatus(), exception.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
                .map(item -> item.getField() + ": " + item.getDefaultMessage()).sorted().toList();
        return error(HttpStatus.BAD_REQUEST, "Проверьте заполнение полей", details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> constraint(ConstraintViolationException exception) {
        List<String> details = exception.getConstraintViolations().stream()
                .map(item -> item.getPropertyPath() + ": " + item.getMessage()).sorted().toList();
        return error(HttpStatus.BAD_REQUEST, "Проверьте заполнение полей", details);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> unreadable(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "Неверный формат запроса: проверьте JSON, типы полей и идентификаторы", List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integrity(DataIntegrityViolationException exception) {
        return error(HttpStatus.CONFLICT, "Запись дублируется или связана с другими данными", List.of());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> concurrentUpdate(ObjectOptimisticLockingFailureException exception) {
        return error(HttpStatus.CONFLICT, "Данные уже изменились. Обновите страницу и повторите действие", List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> notFound(NoResourceFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Ресурс не найден", List.of());
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message, List<String> details) {
        return ResponseEntity.status(status).body(new ApiError(status.value(), message, details, Instant.now()));
    }
}
