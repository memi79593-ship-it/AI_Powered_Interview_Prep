package com.interviewprep.interviewservice.repository;

import com.interviewprep.interviewservice.entity.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewSessionRepository
        extends JpaRepository<InterviewSession, Long> {

    List<InterviewSession> findByUserEmail(String userEmail);

    List<InterviewSession> findByUserEmailAndRole(String userEmail, String role);

    List<InterviewSession> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    long countByUserEmail(String userEmail);
}
