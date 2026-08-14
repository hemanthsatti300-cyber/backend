package com.internship.infosys.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public String register(RegisterRequest request) {

        // =================================================
        // NORMALIZE EMAIL
        // =================================================

        String email =
                request.getEmail()
                        .trim()
                        .toLowerCase();

        // =================================================
        // NORMALIZE USERNAME
        // =================================================

        String username =
                request.getUsername()
                        .trim();

        System.out.println(
            "======================================"
        );

        System.out.println(
            "🚀 REGISTER REQUEST"
        );

        System.out.println(
            "Email: [" + email + "]"
        );

        System.out.println(
            "Username: [" + username + "]"
        );

        System.out.println(
            "======================================"
        );

        // =================================================
        // CHECK EMAIL
        // =================================================

        User existingUser =
                userRepository
                    .findByEmail(email)
                    .orElse(null);

        // =================================================
        // EXISTING USER
        // =================================================

        if (existingUser != null) {

            System.out.println(
                "⚠️ EMAIL ALREADY REGISTERED: "
                + email
            );

            // ---------------------------------------------
            // ALREADY VERIFIED
            // ---------------------------------------------

            if (existingUser.isEmailVerified()) {

                throw new RuntimeException(
                    "Email already exists. Please login."
                );
            }

            // ---------------------------------------------
            // EXISTING BUT NOT VERIFIED
            // ---------------------------------------------

            System.out.println(
                "⚠️ EXISTING ACCOUNT IS NOT VERIFIED"
            );

            // Delete old tokens

            tokenRepository.deleteByUserId(
                existingUser.getId()
            );

            // Create new token

            String newToken =
                    UUID.randomUUID().toString();

            VerificationToken verificationToken =
                    new VerificationToken(
                        newToken,
                        existingUser,
                        LocalDateTime.now()
                            .plusHours(24)
                    );

            tokenRepository.save(
                verificationToken
            );

            // Create deployed verification link

            String verificationLink =
                    "https://backend-fwcy.onrender.com"
                    + "/api/auth/verify?token="
                    + newToken;

            System.out.println(
                "======================================"
            );

            System.out.println(
                "🔗 NEW VERIFICATION LINK:"
            );

            System.out.println(
                verificationLink
            );

            System.out.println(
                "======================================"
            );

            // Send again

            try {

                emailService.sendVerificationEmail(
                    existingUser.getEmail(),
                    existingUser.getUsername(),
                    verificationLink
                );

                return
                    "Account already exists but is not verified. "
                    + "A new verification email has been sent.";

            } catch (Exception e) {

                System.out.println(
                    "❌ EMAIL RESEND FAILED"
                );

                System.out.println(
                    e.getMessage()
                );

                return
                    "Account exists but verification email "
                    + "could not be sent. "
                    + "Please check the STS console for the "
                    + "verification link.";
            }
        }

        // =================================================
        // CHECK USERNAME
        // =================================================

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

        // =================================================
        // PASSWORD
        // =================================================

        user.setPassword(
            passwordEncoder.encode(
                request.getPassword()
            )
        );

        // =================================================
        // ROLE
        // =================================================

        if (request.getRole() == null) {

            user.setRole(Role.USER);

        } else {

            user.setRole(
                request.getRole()
            );
        }

        // =================================================
        // ACCOUNT STATUS
        // =================================================

        user.setEnabled(false);

        user.setEmailVerified(false);

        // =================================================
        // SAVE USER
        // =================================================

        User savedUser =
                userRepository.save(user);

        System.out.println(
            "✅ USER CREATED"
        );

        System.out.println(
            "User ID: "
            + savedUser.getId()
        );

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
        // DEPLOYED VERIFICATION LINK
        // =================================================

        String verificationLink =
                "https://backend-fwcy.onrender.com"
                + "/api/auth/verify?token="
                + token;

        System.out.println(
            "======================================"
        );

        System.out.println(
            "🔗 VERIFICATION LINK:"
        );

        System.out.println(
            verificationLink
        );

        System.out.println(
            "======================================"
        );

        // =================================================
        // SEND EMAIL
        // =================================================

        try {

            emailService.sendVerificationEmail(
                savedUser.getEmail(),
                savedUser.getUsername(),
                verificationLink
            );

            System.out.println(
                "✅ VERIFICATION EMAIL SENT"
            );

            return
                "Registration successful. "
                + "Please check your email.";

        } catch (Exception e) {

            System.out.println(
                "❌ EMAIL SENDING FAILED"
            );

            System.out.println(
                "Email: "
                + savedUser.getEmail()
            );

            System.out.println(
                "Reason: "
                + e.getMessage()
            );

            /*
             * The verification link is still printed
             * above, so you can use it during deployment
             * when an email provider rejects an address.
             */

            return
                "Registration successful, but the "
                + "verification email could not be sent. "
                + "Please check the backend console for "
                + "the verification link.";
        }
    }

    // =====================================================
    // LOGIN
    // =====================================================

    @Override
    public LoginResponse login(LoginRequest request) {

        String email =
                request.getEmail()
                    .trim()
                    .toLowerCase();

        System.out.println(
            "======================================"
        );

        System.out.println(
            "🔐 LOGIN REQUEST"
        );

        System.out.println(
            "Email: [" + email + "]"
        );

        System.out.println(
            "======================================"
        );

        // =================================================
        // FIND USER
        // =================================================

        User user =
            userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                    new RuntimeException(
                        "Invalid email or password."
                    )
                );

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
            "Enabled: "
            + user.isEnabled()
        );

        System.out.println(
            "Role: "
            + user.getRole()
        );

        // =================================================
        // VERIFY EMAIL
        // =================================================

        if (!user.isEmailVerified()) {

            throw new RuntimeException(
                "Please verify your email before logging in."
            );
        }

        // =================================================
        // ENABLED
        // =================================================

        if (!user.isEnabled()) {

            throw new RuntimeException(
                "Your account is disabled."
            );
        }

        // =================================================
        // AUTHENTICATE PASSWORD
        // =================================================

        try {

            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    email,
                    request.getPassword()
                )
            );

        } catch (Exception e) {

            System.out.println(
                "❌ PASSWORD AUTHENTICATION FAILED"
            );

            throw new RuntimeException(
                "Invalid email or password."
            );
        }

        System.out.println(
            "✅ PASSWORD AUTHENTICATION SUCCESS"
        );

        // =================================================
        // JWT
        // =================================================

        String token =
                jwtUtil.generateToken(email);

        System.out.println(
            "✅ JWT GENERATED"
        );

        // =================================================
        // RESPONSE
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
            "======================================"
        );

        System.out.println(
            "✅ LOGIN SUCCESS"
        );

        System.out.println(
            "======================================"
        );

        return response;
    }

    // =====================================================
    // VERIFY EMAIL
    // =====================================================

    @Override
    @Transactional
    public String verifyEmail(String token) {

        System.out.println(
            "======================================"
        );

        System.out.println(
            "📧 EMAIL VERIFICATION REQUEST"
        );

        System.out.println(
            "Token: "
            + token
        );

        System.out.println(
            "======================================"
        );

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
        // EXPIRY
        // =================================================

        if (verificationToken
                .getExpiryDate()
                .isBefore(LocalDateTime.now())) {

            tokenRepository.delete(
                verificationToken
            );

            throw new RuntimeException(
                "Verification token has expired."
            );
        }

        // =================================================
        // USER
        // =================================================

        User user =
            verificationToken.getUser();

        System.out.println(
            "User being verified: "
            + user.getEmail()
        );

        // =================================================
        // VERIFY
        // =================================================

        user.setEnabled(true);

        user.setEmailVerified(true);

        userRepository.save(user);

        // =================================================
        // DELETE TOKEN
        // =================================================

        tokenRepository.delete(
            verificationToken
        );

        System.out.println(
            "======================================"
        );

        System.out.println(
            "✅ EMAIL VERIFIED"
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
            "======================================"
        );

        return
            "Email verified successfully. "
            + "You can now login.";
    }
}
