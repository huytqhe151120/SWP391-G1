package com.swp391.g1.service.impl;

import com.swp391.g1.dto.response.ChecklistTaskAlertDTO;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

final class ChecklistTaskEmailSender {

    private static final Logger LOGGER = Logger.getLogger(ChecklistTaskEmailSender.class.getName());
    private static final int SMTP_TIMEOUT_MILLIS = 10_000;

    void send(ChecklistTaskAlertDTO task) {
        List<InternetAddress> recipients = validRecipients(task.recipientEmails());
        if (recipients.isEmpty()) {
            LOGGER.warning("No email addresses are available for the high-priority checklist task.");
            return;
        }

        String host = setting("MAIL_HOST");
        String from = setting("MAIL_FROM");
        String username = setting("MAIL_USERNAME");
        String password = setting("MAIL_PASSWORD");
        String port = settingOrDefault("MAIL_PORT", "587");
        if (host == null || from == null) {
            LOGGER.warning("High-priority checklist email was not sent: MAIL_HOST or MAIL_FROM is not configured.");
            return;
        }
        if ((username == null) != (password == null)) {
            LOGGER.warning("High-priority checklist email was not sent: configure both MAIL_USERNAME and MAIL_PASSWORD.");
            return;
        }

        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", port);
        properties.put("mail.smtp.auth", Boolean.toString(username != null && password != null));
        properties.put("mail.smtp.starttls.enable",
                Boolean.toString(Boolean.parseBoolean(settingOrDefault("MAIL_STARTTLS", "true"))));
        properties.put("mail.smtp.connectiontimeout", Integer.toString(SMTP_TIMEOUT_MILLIS));
        properties.put("mail.smtp.timeout", Integer.toString(SMTP_TIMEOUT_MILLIS));
        properties.put("mail.smtp.writetimeout", Integer.toString(SMTP_TIMEOUT_MILLIS));

        Authenticator authenticator = username != null && password != null
                ? new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                }
                : null;

        try {
            Session session = Session.getInstance(properties, authenticator);
            MimeMessage message = new MimeMessage(session);
            InternetAddress sender = new InternetAddress(from);
            sender.validate();
            message.setFrom(sender);
            for (InternetAddress recipient : recipients) {
                message.addRecipient(Message.RecipientType.BCC, recipient);
            }
            message.setSubject("Cảnh báo công việc ưu tiên cao: " + task.code(), StandardCharsets.UTF_8.name());
            message.setText(emailBody(task), StandardCharsets.UTF_8.name());
            Transport.send(message);
        } catch (MessagingException e) {
            LOGGER.log(Level.WARNING, "Could not send high-priority checklist email.", e);
        }
    }

    private List<InternetAddress> validRecipients(List<String> addresses) {
        List<InternetAddress> recipients = new ArrayList<>();
        if (addresses == null) {
            return recipients;
        }
        for (String address : addresses) {
            try {
                InternetAddress recipient = new InternetAddress(address.trim());
                recipient.validate();
                recipients.add(recipient);
            } catch (MessagingException | NullPointerException e) {
                LOGGER.warning("Skipping an invalid email address for a high-priority checklist task.");
            }
        }
        return recipients;
    }

    private String emailBody(ChecklistTaskAlertDTO task) {
        String baseUrl = setting("APP_BASE_URL");
        String link = baseUrl == null ? task.targetUrl()
                : baseUrl.replaceAll("/+$", "") + task.targetUrl();
        return "Công việc checklist có mức ưu tiên cao.\n\n"
                + "Mã công việc: " + task.code() + "\n"
                + "Tên công việc: " + task.name() + "\n"
                + "Deadline: " + task.deadlineDisplay() + "\n\n"
                + "Mở công việc: " + link;
    }

    private String setting(String name) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String settingOrDefault(String name, String defaultValue) {
        String value = setting(name);
        return value == null ? defaultValue : value;
    }
}
