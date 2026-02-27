package com.interviewprep.interviewservice.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Day 15 – Application User with role-based access control.
 */
@Entity
@Table(name = "app_users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    /** Bcrypt hashed password */
    private String passwordHash;

    /** USER | ADMIN */
    @Builder.Default
    private String role = "USER";

    @Builder.Default
    private boolean active = true;

    private java.time.LocalDateTime createdAt;
}
