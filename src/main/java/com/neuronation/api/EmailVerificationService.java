package com.neuronation.api;

import com.neuronation.config.ConfigManager;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads verification emails from the shared @nntest.de IMAP mailbox,
 * extracts the validation link, and calls it to verify the user's email.
 *
 * IMAP credentials are loaded from config.properties.
 * Password MUST be provided via IMAP_PASSWORD env var in CI.
 *
 * IMPORTANT: This is a shared mailbox — do NOT change the password.
 */
public class EmailVerificationService {
    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final Pattern VALIDATION_URL_PATTERN =
            Pattern.compile("(https?://[^\\s\"<>]*?/api/v2/users/validate/[a-f0-9\\-]+)");

    /**
     * Wait for verification email, extract validation link, and call it.
     *
     * @param recipientEmail the email address used during registration (e.g. qa.mci+ts@nntest.de)
     * @param maxWaitSeconds max seconds to poll for the email
     * @return the validation URL that was called
     */
    @Step("Verify email for {recipientEmail} via IMAP → extract link → call validation URL")
    public String verifyEmail(String recipientEmail, int maxWaitSeconds) {
        log.info("Waiting for verification email to: {}", recipientEmail);

        String validationUrl = pollForValidationUrl(recipientEmail, maxWaitSeconds);
        if (validationUrl == null) {
            throw new RuntimeException("Verification email not received within " + maxWaitSeconds + "s for: " + recipientEmail);
        }

        log.info("Found validation URL: {}", validationUrl);

        // Call the validation URL (GET request — same as clicking the link)
        RestAssured.given()
                .get(validationUrl)
                .then()
                .statusCode(200);

        log.info("Email verified successfully for: {}", recipientEmail);
        return validationUrl;
    }

    /**
     * Convenience method with default 60s timeout.
     */
    @Step("Verify email for {recipientEmail}")
    public String verifyEmail(String recipientEmail) {
        return verifyEmail(recipientEmail, 60);
    }

    /**
     * Polls the IMAP mailbox for a verification email to the given recipient.
     * Checks every 5 seconds until found or timeout.
     */
    private String pollForValidationUrl(String recipientEmail, int maxWaitSeconds) {
        int elapsed = 0;
        while (elapsed < maxWaitSeconds) {
            String url = searchMailbox(recipientEmail);
            if (url != null) return url;

            try {
                log.info("No verification email yet for {} — retrying in 5s ({}s elapsed)", recipientEmail, elapsed);
                Thread.sleep(5000);
                elapsed += 5;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    /**
     * Connects to IMAP, searches for email TO the given address,
     * extracts the validation URL from the most recent matching email.
     */
    private String searchMailbox(String recipientEmail) {
        ConfigManager config = ConfigManager.getInstance();
        String host = config.getString("imap.host");
        int port = config.getInt("imap.port");
        String username = config.getString("imap.username");
        String password = config.getString("imap.password");

        if (password == null || password.isEmpty() || password.startsWith("${")) {
            throw new RuntimeException("IMAP password not configured. Set IMAP_PASSWORD env var.");
        }

        Properties props = new Properties();
        props.put("mail.imap.host", host);
        props.put("mail.imap.port", String.valueOf(port));
        props.put("mail.imap.starttls.enable", "true");
        props.put("mail.imap.connectiontimeout", "10000");
        props.put("mail.imap.timeout", "10000");

        Store store = null;
        Folder inbox = null;
        try {
            Session session = Session.getInstance(props);
            store = session.getStore("imap");
            store.connect(host, port, username, password);

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            int count = inbox.getMessageCount();
            // Search last 20 emails (newest first) for matching TO address
            int start = Math.max(1, count - 19);
            for (int i = count; i >= start; i--) {
                Message msg = inbox.getMessage(i);
                Address[] toAddrs = msg.getRecipients(Message.RecipientType.TO);
                if (toAddrs == null) continue;

                boolean matches = false;
                for (Address addr : toAddrs) {
                    if (addr.toString().contains(recipientEmail)) {
                        matches = true;
                        break;
                    }
                }
                if (!matches) continue;

                // Check if this is the confirmation email (not recovery codes)
                String subject = msg.getSubject();
                if (subject == null || !subject.toLowerCase().contains("confirmation")) continue;

                String body = extractTextContent(msg);
                if (body == null) continue;

                Matcher matcher = VALIDATION_URL_PATTERN.matcher(body);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
            return null;

        } catch (Exception e) {
            log.warn("IMAP error: {}", e.getMessage());
            return null;
        } finally {
            try { if (inbox != null && inbox.isOpen()) inbox.close(false); } catch (Exception ignored) {}
            try { if (store != null && store.isConnected()) store.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * Extracts text content from a Message (handles multipart and plain text).
     */
    private String extractTextContent(Message message) throws Exception {
        Object content = message.getContent();
        if (content instanceof String) {
            return (String) content;
        }
        if (content instanceof MimeMultipart) {
            return extractFromMultipart((MimeMultipart) content);
        }
        return content.toString();
    }

    private String extractFromMultipart(MimeMultipart multipart) throws Exception {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            if (part.getContent() instanceof MimeMultipart) {
                result.append(extractFromMultipart((MimeMultipart) part.getContent()));
            } else {
                String partContent = part.getContent().toString();
                result.append(partContent);
            }
        }
        return result.toString();
    }
}
