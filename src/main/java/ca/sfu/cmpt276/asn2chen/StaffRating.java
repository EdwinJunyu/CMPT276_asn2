package ca.sfu.cmpt276.asn2chen;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
@Entity
@Table(name = "staff_rating", uniqueConstraints = {
        @UniqueConstraint(name = "uk_staff_rating_email", columnNames = "email")
})
public class StaffRating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required.")
    @Size(max = 100, message = "Name must be at most 100 characters.")
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Email is required.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 254, message = "Email must be at most 254 characters.")
    @Column(nullable = false, length = 254)
    private String email;

    @NotNull(message = "Select a role.")
    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false, length = 4)
    private RoleType roleType;

    @NotNull(message = "Score is required.")
    @Min(value = 1, message = "Score must be between 1 and 10.")
    @Max(value = 10, message = "Score must be between 1 and 10.")
    @Column(nullable = false)
    private Integer clarity;

    @NotNull(message = "Score is required.")
    @Min(value = 1, message = "Score must be between 1 and 10.")
    @Max(value = 10, message = "Score must be between 1 and 10.")
    @Column(nullable = false)
    private Integer niceness;

    @NotNull(message = "Score is required.")
    @Min(value = 1, message = "Score must be between 1 and 10.")
    @Max(value = 10, message = "Score must be between 1 and 10.")
    @Column(name = "knowledge_score", nullable = false)
    private Integer knowledgeableScore;

    @Size(max = 500, message = "Comment must be at most 500 characters.")
    @Column(length = 500)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void initializeTimestamps() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void refreshUpdatedAt() {
        updatedAt = Instant.now();
    }

    @Transient
    public double getOverallScore() {
        return (clarity + niceness + knowledgeableScore) / 3.0;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public RoleType getRoleType() {
        return roleType;
    }

    public void setRoleType(RoleType roleType) {
        this.roleType = roleType;
    }

    public Integer getClarity() {
        return clarity;
    }

    public void setClarity(Integer clarity) {
        this.clarity = clarity;
    }

    public Integer getNiceness() {
        return niceness;
    }

    public void setNiceness(Integer niceness) {
        this.niceness = niceness;
    }

    public Integer getKnowledgeableScore() {
        return knowledgeableScore;
    }

    public void setKnowledgeableScore(Integer knowledgeableScore) {
        this.knowledgeableScore = knowledgeableScore;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
