package com.interviewprep.interviewservice.repository;

import com.interviewprep.interviewservice.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findBySessionId(Long sessionId);

    Optional<Answer> findBySessionIdAndQuestionId(Long sessionId, Long questionId);
    
    @Query("SELECT a FROM Answer a WHERE a.sessionId = :sessionId")
    List<Answer> findAnswersBySessionId(@Param("sessionId") Long sessionId);
    
    @Query("SELECT a FROM Answer a WHERE a.sessionId = :sessionId AND a.questionId = :questionId")
    Optional<Answer> findAnswerBySessionIdAndQuestionId(@Param("sessionId") Long sessionId, @Param("questionId") Long questionId);
}
