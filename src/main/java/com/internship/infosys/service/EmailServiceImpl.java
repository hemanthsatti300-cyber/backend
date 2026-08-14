package com.internship.infosys.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // =====================================================
    // GENERIC EMAIL
    // =====================================================

    @Override
    public void sendEmail(
            String to,
            String subject,
            String body) {

        try {

            SimpleMailMessage message =
                    new SimpleMailMessage();

            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            System.out.println("======================================");
            System.out.println("📧 SENDING EMAIL");
            System.out.println("To: " + to);
            System.out.println("Subject: " + subject);
            System.out.println("======================================");

            mailSender.send(message);

            System.out.println("✅ EMAIL SENT SUCCESSFULLY");
            System.out.println("To: " + to);
            System.out.println("======================================");

        } catch (MailException e) {

            /*
             * IMPORTANT:
             * Do NOT throw the exception here.
             *
             * If SMTP fails, registration should still succeed.
             */

            System.out.println("======================================");
            System.out.println("❌ EMAIL SENDING FAILED");
            System.out.println("To: " + to);
            System.out.println("Reason: " + e.getMessage());
            System.out.println("======================================");

        } catch (Exception e) {

            System.out.println("======================================");
            System.out.println("❌ UNEXPECTED EMAIL ERROR");
            System.out.println("To: " + to);
            System.out.println("Reason: " + e.getMessage());
            System.out.println("======================================");
        }
    }

    // =====================================================
    // VERIFICATION EMAIL
    // =====================================================

    @Override
    public void sendVerificationEmail(
            String to,
            String username,
            String verificationLink) {

        String subject =
                "Verify Your SentinelCore SecureOps Account";

        String body = """
                Hello %s,

                Welcome to SentinelCore SecureOps.

                Thank you for registering.

                Please click the link below to verify your account:

                %s

                This verification link will expire in 24 hours.

                If you did not create this account,
                please ignore this email.

                Regards,
                SentinelCore SecureOps Team
                """
                .formatted(
                        username,
                        verificationLink
                );

        // =================================================
        // ALWAYS PRINT VERIFICATION LINK
        // =================================================

        System.out.println();
        System.out.println("======================================");
        System.out.println("🔐 EMAIL VERIFICATION");
        System.out.println("======================================");
        System.out.println("User: " + username);
        System.out.println("Email: " + to);
        System.out.println();
        System.out.println("VERIFICATION LINK:");
        System.out.println(verificationLink);
        System.out.println("======================================");
        System.out.println();

        // =================================================
        // TRY TO SEND EMAIL
        // =================================================

        sendEmail(
                to,
                subject,
                body
        );
    }
}
