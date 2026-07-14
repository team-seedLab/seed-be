package com.example.seedbe.domain.mentor.entity;

import com.example.seedbe.domain.mentor.enums.MentorStudentStatus;
import com.example.seedbe.domain.user.entity.User;
import com.example.seedbe.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "mentor_students", uniqueConstraints =
        @UniqueConstraint(name = "uk_mentor_students_pair", columnNames = {"mentor_id", "student_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MentorStudent extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "mentor_student_id", nullable = false, updatable = false)
    private UUID mentorStudentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mentor_id", nullable = false)
    private User mentor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MentorStudentStatus status;

    @Builder
    public MentorStudent(User mentor, User student, MentorStudentStatus status) {
        this.mentor = mentor;
        this.student = student;
        this.status = status == null ? MentorStudentStatus.ACTIVE : status;
    }
}
