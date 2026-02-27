package com.interviewprep.interviewservice.repository;

import com.interviewprep.interviewservice.entity.QuestionBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuestionBankRepository extends JpaRepository<QuestionBank, Long> {
    List<QuestionBank> findByRoleAndLevelAndType(String role, String level, String type);

    List<QuestionBank> findByRoleAndLevel(String role, String level);

    long countByRoleAndLevel(String role, String level);
}
