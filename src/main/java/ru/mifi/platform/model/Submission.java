package ru.mifi.platform.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "submissions", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "assignment_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Submission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false)
    private Instant submittedAt = Instant.now();

    @Column(nullable = false, length = 10000)
    private String content;

    private Integer score;

    @Column(length = 3000)
    private String feedback;
}
