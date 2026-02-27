package com.interviewprep.interviewservice.controller;

import com.interviewprep.interviewservice.entity.UserSkillProfile;
import com.interviewprep.interviewservice.service.SkillProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Day 12 – Skill Profile Controller.
 *
 * GET /api/profile/{email} – All profiles for user
 * GET /api/profile/{email}/{role} – Profile for specific role
 */
@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "*")
public class SkillProfileController {

    private final SkillProfileService skillProfileService;

    public SkillProfileController(SkillProfileService skillProfileService) {
        this.skillProfileService = skillProfileService;
    }

    @GetMapping("/{email}")
    public ResponseEntity<List<UserSkillProfile>> getAllProfiles(@PathVariable String email) {
        return ResponseEntity.ok(skillProfileService.getAllProfiles(email));
    }

    @GetMapping("/{email}/{role}")
    public ResponseEntity<?> getProfile(@PathVariable String email,
            @PathVariable String role) {
        return skillProfileService.getProfile(email, role)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(Map.of(
                        "message", "No profile yet. Complete an interview first.",
                        "userEmail", email, "role", role)));
    }
}
