package com.internship.infosys.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.internship.infosys.dto.LoginRequest;
import com.internship.infosys.dto.LoginResponse;
import com.internship.infosys.dto.RegisterRequest;
import com.internship.infosys.model.Role;
import com.internship.infosys.model.User;
import com.internship.infosys.model.VerificationToken;
import com.internship.infosys.repositary.UserRepository;
import com.internship.infosys.repositary.VerificationTokenRepository;
import com.internship.infosys.security.JwtUtil;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private EmailService emailService;

    // =====================================================
    // REGISTER
    // =====================================================

    @Override
    public String register(RegisterRequest request) {

        // ---------------------------------------------
        // Normalize email
        // ---------------------------------------------

        String email = request.getEmail()
                .trim()
                .toLowerCase();

        // ---------------------------------------------
        // Normalize username
        // ---------------------------------------------

        String username = request.getUsername()
                .trim();

        // ---------------------------------------------
        // Validate email
        // ---------------------------------------------

        if (email.isEmpty()) {
            throw new RuntimeException("Email is required.");
        }

        // ---------------------------------------------
        // Validate username
        // ---------------------------------------------

        if (username.isEmpty()) {
            throw new RuntimeException("Username is required.");
        }

        // ---------------------------------------------
        // Check existing email
        // ---------------------------------------------

        if (userRepository.existsByEmail(email)) {

            throw new RuntimeException(
                    "Email already exists."
            );
        }

        // ---------------------------------------------
        // Check existing username
        // ---------------------------------------------

        if (userRepository.existsByUsername(username)) {

            throw new RuntimeException(
                    "Username already exists."
            );
        }

        // =================================================
        // CREATE USER
        // =================================================

        User user = new User();

        user.setUsername(username);

        user.setEmail(email);

        user.setDepartment(
                request.getDepartment()
        );

        // ---------------------------------------------
        // Encrypt password
        // ---------------------------------------------

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        // ---------------------------------------------
        // Set role
        // ---------------------------------------------

        if (request.getRole() == null) {

            user.setRole(Role.USER);

        } else {

            user.setRole(
                    request.getRole()
            );
        }

        // ---------------------------------------------
        // Account disabled until verification
        // ---------------------------------------------

        user.setEnabled(false);

        user.setEmailVerified(false);

        // =================================================
        // SAVE USER
        // =================================================

        User savedUser =
                userRepository.save(user);

        // =================================================
        // CREATE VERIFICATION TOKEN
        // =================================================

        String token =
                UUID.randomUUID().toString();

        VerificationToken verificationToken =
                new VerificationToken(
                        token,
                        savedUser,
                        LocalDateTime.now()
                                .plusHours(24)
                );

        tokenRepository.save(
                verificationToken
        );

        // =================================================
        // CREATE RENDER VERIFICATION LINK
        // =================================================

        String verificationLink =
                "https://backend-fwcy.onrender.com"
                + "/api/auth/verify?token="
                + token;

        // =================================================
        // DEBUG
        // =================================================

        System.out.println(
                "=========================================="
        );

        System.out.println(
                "REGISTRATION SUCCESS"
        );

        System.out.println(
                "User: " + savedUser.getEmail()
        );

        System.out.println(
                "Role: " + savedUser.getRole()
        );

        System.out.println(
                "Email Verified: "
                        + savedUser.isEmailVerified()
        );

        System.out.println(
                "Account Enabled: "
                        + savedUser.isEnabled()
        );

        System.out.println(
                "VERIFICATION LINK:"
        );

        System.out.println(
                verificationLink
        );

        System.out.println(
                "=========================================="
        );

        // =================================================
        // SEND VERIFICATION EMAIL
        // =================================================

        try {

            emailService.sendVerificationEmail(
                    savedUser.getEmail(),
                    savedUser.getUsername(),
                    verificationLink
            );

            System.out.println(
                    "✅ VERIFICATION EMAIL SENT TO: "
                            + savedUser.getEmail()
            );

        } catch (Exception e) {

            System.out.println(
                    "❌ FAILED TO SEND VERIFICATION EMAIL"
            );

            System.out.println(
                    "Email: "
                            + savedUser.getEmail()
            );

            System.out.println(
                    "Reason: "
                            + e.getMessage()
            );

            e.printStackTrace();

            throw new RuntimeException(
                    "Registration completed, but verification email could not be sent."
            );
        }

        return "Registration successful. Please check your email.";
    }

    // =====================================================
    // LOGIN
    // =====================================================

    @Override
    public LoginResponse login(LoginRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase();

        System.out.println(
                "=========================================="
        );

        System.out.println(
                "LOGIN REQUEST"
        );

        System.out.println(
                "Email: [" + email + "]"
        );

        System.out.println(
                "=========================================="
        );

        // =================================================
        // FIND USER
        // =================================================

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() -> {

                            System.out.println(
                                    "❌ USER NOT FOUND"
                            );

                            return new RuntimeException(
                                    "Invalid email or password."
                            );
                        });

        // =================================================
        // USER INFORMATION
        // =================================================

        System.out.println(
                "✅ USER FOUND"
        );

        System.out.println(
                "User ID: "
                        + user.getId()
        );

        System.out.println(
                "Username: "
                        + user.getUsername()
        );

        System.out.println(
                "Email Verified: "
                        + user.isEmailVerified()
        );

        System.out.println(
                "Account Enabled: "
                        + user.isEnabled()
        );

        System.out.println(
                "Role: "
                        + user.getRole()
        );

        // =================================================
        // CHECK EMAIL VERIFICATION
        // =================================================

        if (!user.isEmailVerified()) {

            System.out.println(
                    "❌ EMAIL NOT VERIFIED"
            );

            throw new RuntimeException(
                    "Please verify your email before logging in."
            );
        }

        // =================================================
        // CHECK ACCOUNT STATUS
        // =================================================

        if (!user.isEnabled()) {

            System.out.println(
                    "❌ ACCOUNT DISABLED"
            );

            throw new RuntimeException(
                    "Your account is disabled."
            );
        }

        // =================================================
        // AUTHENTICATE USER
        // =================================================

        try {

            authenticationManager.authenticate(

                    new UsernamePasswordAuthenticationToken(
                            email,
                            request.getPassword()
                    )
            );

            System.out.println(
                    "✅ PASSWORD AUTHENTICATION SUCCESS"
            );

        } catch (Exception e) {

            System.out.println(
                    "❌ PASSWORD AUTHENTICATION FAILED"
            );

            System.out.println(
                    "Reason: "
                            + e.getMessage()
            );

            throw new RuntimeException(
                    "Invalid email or password."
            );
        }

        // =================================================
        // GENERATE JWT
        // =================================================

        String token =
                jwtUtil.generateToken(email);

        System.out.println(
                "✅ JWT GENERATED"
        );

        // =================================================
        // LOGIN RESPONSE
        // =================================================

        LoginResponse response =
                new LoginResponse();

        response.setId(
                user.getId()
        );

        response.setToken(
                token
        );

        response.setUsername(
                user.getUsername()
        );

        response.setEmail(
                user.getEmail()
        );

        response.setDepartment(
                user.getDepartment()
        );

        response.setRole(
                user.getRole()
        );

        System.out.println(
                "=========================================="
        );

        System.out.println(
                "✅ LOGIN SUCCESS"
        );

        System.out.println(
                "=========================================="
        );

        return response;
    }

    // =====================================================
    // VERIFY EMAIL
    // =====================================================

    @Override
    public String verifyEmail(String token) {

        System.out.println(
                "=========================================="
        );

        System.out.println(
                "EMAIL VERIFICATION REQUEST"
        );

        System.out.println(
                "Token: " + token
        );

        System.out.println(
                "=========================================="
        );

        // =================================================
        // VALIDATE TOKEN INPUT
        // =================================================

        if (token == null || token.trim().isEmpty()) {

            throw new RuntimeException(
                    "Verification token is missing."
            );
        }

        // =================================================
        // FIND TOKEN
        // =================================================

        VerificationToken verificationToken =
                tokenRepository
                        .findByToken(token)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invalid verification token."
                                )
                        );

        // =================================================
        // CHECK TOKEN EXPIRY
        // =================================================

        if (verificationToken
                .getExpiryDate()
                .isBefore(LocalDateTime.now())) {

            throw new RuntimeException(
                    "Verification token has expired."
            );
        }

        // =================================================
        // GET USER
        // =================================================

        User user =
                verificationToken.getUser();

        System.out.println(
                "User being verified: "
                        + user.getEmail()
        );

        // =================================================
        // VERIFY USER
        // =================================================

        user.setEmailVerified(true);

        user.setEnabled(true);

        userRepository.save(user);

        // =================================================
        // DELETE USED TOKEN
        // =================================================

        tokenRepository.delete(
                verificationToken
        );

        // =================================================
        // SUCCESS LOG
        // =================================================

        System.out.println(
                "=========================================="
        );

        System.out.println(
                "✅ EMAIL VERIFIED SUCCESSFULLY"
        );

        System.out.println(
                "User: "
                        + user.getEmail()
        );

        System.out.println(
                "Enabled: "
                        + user.isEnabled()
        );

        System.out.println(
                "Email Verified: "
                        + user.isEmailVerified()
        );

        System.out.println(
                "=========================================="
        );

        return "Email verified successfully. You can now login.";
    }
}
