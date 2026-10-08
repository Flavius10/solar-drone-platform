package com.solar.backend.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String toEmail, String username, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Solar Drone Platform - Password Reset Request");
        message.setText("Hello " + username + ",\n\n"
                + "We received a request to reset your password.\n"
                + "Click the link below to set a new password (valid for 30 minutes):\n\n"
                + resetLink + "\n\n"
                + "If you did not request this, you can safely ignore this email.\n\n"
                + "- Solar Drone Platform");
        mailSender.send(message);
    }

    public void sendCriticalDefectAlert(String toEmail, String panelLabel, String defectType, double confidenceScore) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Solar Drone Platform - Critical Defect Detected");
        message.setText("A critical defect was detected during the latest inspection.\n\n"
                + "Panel: " + panelLabel + "\n"
                + "Defect type: " + defectType + "\n"
                + "Confidence: " + Math.round(confidenceScore * 100) + "%\n\n"
                + "Please open a repair ticket as soon as possible.\n\n"
                + "- Solar Drone Platform");
        mailSender.send(message);
    }
}