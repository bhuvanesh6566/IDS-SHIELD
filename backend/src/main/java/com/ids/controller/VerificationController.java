package com.ids.controller;

import com.ids.entity.AuditLog;
import com.ids.entity.User;
import com.ids.repository.AuditLogRepository;
import com.ids.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class VerificationController {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public VerificationController(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/logs")
    public List<AuditLog> getLogs() {
        return auditLogRepository.findAll();
    }

    @GetMapping("/users")
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @PostMapping("/users")
    public User createUser(@RequestBody User user) {
        if (user.getBalance() == null) {
            user.setBalance(new java.math.BigDecimal("0.00"));
        }
        user.setLastUpdated(java.time.LocalDateTime.now());
        return userRepository.save(user);
    }

    @PostMapping("/simulate-attack")
    public String simulateAttack(@RequestParam Long userId, @RequestParam String newValue) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setBalance(new java.math.BigDecimal(newValue));
        userRepository.save(user);
        return "Attack simulated! Check Discord/Logs.";
    }
}
