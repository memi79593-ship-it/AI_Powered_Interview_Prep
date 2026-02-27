package com.interviewprep.interviewservice.repository;

import com.interviewprep.interviewservice.entity.UserSkillProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserSkillProfileRepository extends JpaRepository<UserSkillProfile, Long> {
    Optional<UserSkillProfile> findByUserEmailAndRole(String userEmail, String role);

    List<UserSkillProfile> findByUserEmail(String userEmail);
}
