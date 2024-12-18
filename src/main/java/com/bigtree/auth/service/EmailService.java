package com.bigtree.auth.service;

import com.bigtree.auth.config.ResourcesConfig;
import com.bigtree.auth.entity.Account;
import com.bigtree.auth.entity.PartnerSignup;
import com.bigtree.auth.entity.User;
import com.bigtree.auth.model.PasswordResetEmail;
import com.bigtree.auth.security.CryptoHelper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    @Autowired
    JavaMailSender javaMailSender;

    @Autowired
    EmailContentHelper emailContentHelper;

    @Autowired
    ResourcesConfig resourcesConfig;

    @Autowired
    CryptoHelper cryptoHelper;

    public void setOnetimePasscode(PasswordResetEmail passwordResetEmail) {
        log.info("Sending otp to user email {}", passwordResetEmail.getEmail());

        try {
            Map<String, String> queries = new HashMap<>();
            queries.put("email", passwordResetEmail.getEmail());
            queries.put("otp", passwordResetEmail.getOtp());
            Map<String, Object> params = new HashMap<>();
            params.put("data", passwordResetEmail);
            params.put("resetUrl", passwordResetEmail.getTargetUrl()+"?qs="+ cryptoHelper.encryptUrl(mapToQueryString(queries)));
            sendMail(passwordResetEmail.getEmail(), "Reset your password | EATem", "password-reset-instructions", params);
        } catch (Exception e) {
            log.error("Error when preparing mail message. {}", e.getMessage());
        }

    }

    public void sendMail(String to, String subject, String template, Map<String, Object> params) {
        final MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = null;
        try {
            helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom("noreply@zcoop.co.uk");
            helper.setText(emailContentHelper.build(template, params), true);
            javaMailSender.send(mimeMessage);
            log.info("Email sent to user {}", to);
        } catch (MessagingException e) {
            log.info("Exception while sending email to user {}", to);
        }
    }

    private String encode(String value) {
        String encoded=  UriUtils.encodeQueryParam(value, "UTF-8");
        log.info("Encoded value {}", encoded);
        return  encoded;
    }

    private String mapToQueryString(Map<String, String> query) {
        List<String> entries = new LinkedList<>();
        for (Map.Entry<String, String> entry : query.entrySet()) {
            try {
                entries.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "=" + URLEncoder.encode(entry.getValue(), "UTF-8"));
            } catch(Exception e) {
                log.error("Unable to encode string for URL: " + entry.getKey() + " / " + entry.getValue(), e);
            }
        }
        return String.join("&", entries);
    }

    public void setPasswordResetConfirmation(String email, String fullName) {
        log.info("Sending password confirmation email {}", email);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("customerName", fullName);
            sendMail(email, "Your password has been changed | homegrub", "password-reset-successful-email", params);
        } catch (Exception e) {
            log.error("Error when preparing mail message. {}", e.getMessage());
        }
    }

    public void sendAccountActivationEmail(Account account, User user) {
        log.info("Sending account activation email {}", user.getEmail());
        try {
            Map<String, String> queries = new HashMap<>();
            queries.put("activationCode", account.getActivationCode());
            queries.put("accountId", account.get_id());
            final String queryString = mapToQueryString(queries);

            Map<String, Object> params = new HashMap<>();
            params.put("customerName", user.getName());
            params.put("queryString", cryptoHelper.encryptUrl(queryString));

            sendMail(user.getEmail(), user.getName().toUpperCase()+ ", finish setting up your new homegrub Account", "signup-confirmation", params);
        } catch (Exception e) {
            log.error("Error when preparing mail message. {}", e.getMessage());
        }
    }

    public void sendPartnerSignupAcknowledgement(PartnerSignup partnerSignup) {
        log.info("Sending partner signup acknowledgement email {}", partnerSignup.getEmail());
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("email", partnerSignup.getEmail());
            params.put("mobile", partnerSignup.getMobile());
            params.put("name", partnerSignup.getName());
            sendMail(partnerSignup.getEmail(), partnerSignup.getName().toUpperCase()+ ", Homegrub Partner Interest", "partner-signup-acknowledgement", params);
        } catch (Exception e) {
            log.error("Error when preparing mail message. {}", e.getMessage());
        }
    }
}
