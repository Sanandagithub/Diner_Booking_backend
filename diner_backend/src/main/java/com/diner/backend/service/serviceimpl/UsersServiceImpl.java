package com.diner.backend.service.serviceimpl;

import com.diner.backend.enitiy.AppRole;
import com.diner.backend.enitiy.PasswordResetToken;
import com.diner.backend.enitiy.Role;
import com.diner.backend.enitiy.Users;
import com.diner.backend.repositories.PasswordResetTokenRepository;
import com.diner.backend.repository.RoleRepo;
import com.diner.backend.repository.UsersRepo;
import com.diner.backend.service.UsersService;
import com.diner.backend.utiil.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UsersServiceImpl implements UsersService {

    @Value("${frontend.url}")
    String frontendUrl;

    @Autowired
    UsersRepo userRepository;

    @Autowired
    RoleRepo roleRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    EmailService emailService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void updateUserRole(Long userId, String roleName) {
        Users user = userRepository.findById(userId).orElseThrow(()
                -> new RuntimeException("User not found"));
        AppRole appRole = AppRole.valueOf(roleName);
        Role role = roleRepository.findByRoleName(appRole)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        user.setRole(role);
        userRepository.save(user);
    }


    @Override
    public List<Users> getAllUsers() {
        return userRepository.findAll();
    }


    @Override
    public Users findByUsername(String username) {
        Optional<Users> user = userRepository.findByUserName(username);
        return user.orElseThrow(
                () -> new RuntimeException("User not found with username: " + username));
    }

    @Override
    public void generatePasswordResetToken(String email){
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plus(24, ChronoUnit.HOURS);
        PasswordResetToken resetToken = new PasswordResetToken(token, expiryDate, user);
        passwordResetTokenRepository.save(resetToken);

        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        // Send email to user
        emailService.sendPasswordResetEmail(user.getEmail(), resetUrl);
    }


    @Override
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid password reset token"));

        if (resetToken.isUsed())
            throw new RuntimeException("Password reset token has already been used");

        if (resetToken.getExpiryDate().isBefore(Instant.now()))
            throw new RuntimeException("Password reset token has expired");

        Users user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }
}
