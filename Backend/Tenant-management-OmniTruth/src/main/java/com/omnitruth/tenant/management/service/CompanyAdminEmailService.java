package com.omnitruth.tenant.management.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class CompanyAdminEmailService {

    private final JavaMailSender mailSender;

    @Value("${omnitruth.mail.from:${spring.mail.username}}")
    private String fromEmail;

    public CompanyAdminEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendInvitation(
            String recipientEmail,
            String tenantCode,
            String invitationLink,
            Instant expiresAt) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(recipientEmail);

        message.setSubject(
                "OmniTruth - Company Admin Invitation"
        );

        String expiryDate = DateTimeFormatter
                .ofPattern("dd MMM yyyy, hh:mm a z")
                .withZone(ZoneId.of("Asia/Kolkata"))
                .format(expiresAt);

        String emailBody = """
                Hello,

                You have been invited to become the Company Administrator
                of an organization on the OmniTruth Platform.

                Tenant Code: %s

                Please use the following secure link to accept your invitation:

                %s

                Invitation Expiry: %s

                Important:
                - This invitation link is valid only until the expiry time.
                - Do not share this invitation link with anyone.
                - If you did not expect this invitation, please ignore this email.

                Regards,
                OmniTruth Platform Team
                """.formatted(
                tenantCode,
                invitationLink,
                expiryDate
        );

        message.setText(emailBody);

        mailSender.send(message);
    }
}
