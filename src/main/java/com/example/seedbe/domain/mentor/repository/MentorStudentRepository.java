package com.example.seedbe.domain.mentor.repository;

import com.example.seedbe.domain.mentor.entity.MentorStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MentorStudentRepository extends JpaRepository<MentorStudent, UUID> {
    @Query("""
        SELECT ms FROM MentorStudent ms
        JOIN FETCH ms.student
        WHERE ms.mentor.userId = :mentorId
          AND ms.status = com.example.seedbe.domain.mentor.enums.MentorStudentStatus.ACTIVE
        ORDER BY ms.student.nickname ASC, ms.student.userId ASC
    """)
    List<MentorStudent> findAllActiveByMentorId(@Param("mentorId") UUID mentorId);

    @Query("""
        SELECT ms FROM MentorStudent ms
        JOIN FETCH ms.mentor
        JOIN FETCH ms.student
        WHERE ms.mentor.userId = :mentorId
          AND ms.student.userId = :studentId
          AND ms.status = com.example.seedbe.domain.mentor.enums.MentorStudentStatus.ACTIVE
    """)
    Optional<MentorStudent> findActiveAssignment(
            @Param("mentorId") UUID mentorId,
            @Param("studentId") UUID studentId
    );
}
