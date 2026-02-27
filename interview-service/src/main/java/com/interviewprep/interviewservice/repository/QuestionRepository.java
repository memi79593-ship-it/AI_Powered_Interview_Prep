package com.interviewprep.interviewservice.repository;

import com.interviewprep.interviewservice.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findBySessionIdOrderByQuestionOrder(Long sessionId);

    long countBySessionId(Long sessionId);
}
