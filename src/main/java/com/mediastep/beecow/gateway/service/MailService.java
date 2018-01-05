/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.service;

import java.util.Locale;

import javax.inject.Inject;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring4.SpringTemplateEngine;

import com.mediastep.beecow.gateway.config.EmailServiceConfig;
import com.mediastep.beecow.gateway.config.JHipsterProperties;
import com.mediastep.beecow.gateway.domain.User;
import com.mediastep.beecow.gateway.service.dto.EmailDTO;
import com.mediastep.beecow.gateway.web.rest.vm.ContactUsVM;

/**
 * Service for sending e-mails.
 * <p>
 * We use the @Async annotation to send e-mails asynchronously.
 * </p>
 */
@Service
public class MailService {

    private final Logger log = LoggerFactory.getLogger(MailService.class);

    private static final String USER = "user";

    private static final String BASE_URL = "baseUrl";

    @Inject
    private JHipsterProperties jHipsterProperties;

    @Inject
    private JavaMailSenderImpl javaMailSender;

    @Inject
    private EmailServiceConfig emailServiceConfig;

    @Inject
    private MessageSource messageSource;

    @Inject
    private SpringTemplateEngine templateEngine;

    @Async
    public void sendEmail(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        log.debug("Sending e-mail to '{}'", to);
        String from = jHipsterProperties.getMail().getFrom();
        String replyTo = "cskh@beecow.com";
        sendEmail(from, replyTo, to, subject, content, isMultipart, isHtml);
    }

    private void sendEmail(String from, String replyTo, String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        log.debug("Send e-mail[multipart '{}' and html '{}'] from '{}' to '{}' with subject '{}' and content={}",
            isMultipart, isHtml, from, to, subject, content);

        // Prepare message using a Spring helper
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, isMultipart, CharEncoding.UTF_8);
            message.setTo(to);
            message.setFrom(from);
            message.setReplyTo(replyTo);
            message.setSubject(subject);
            message.setText(content, isHtml);
            javaMailSender.send(mimeMessage);
            log.debug("Sent e-mail to User '{}'", to);
        } catch (Exception e) {
            log.warn("E-mail could not be sent to user '{}'", to, e);
        }
    }

    @Async
    public void contactUs(ContactUsVM contactUs) {
        log.debug("Sending e-mail to web-admin: {}", contactUs);
        String from = emailServiceConfig.getSenderEmail();
        String to = emailServiceConfig.getContactUsEmail();
        EmailDTO replyToDTO = contactUs.getFrom();
        String replyTo = replyToDTO.getEmail();
        String subject = getContactUsSubject(replyToDTO);
        String content = contactUs.getContent();
        sendEmail(from, replyTo, to, subject, content, false, true);
    }

    private String getContactUsSubject(EmailDTO fromDTO) {
        StringBuilder name = new StringBuilder();
        if (StringUtils.isNotBlank(fromDTO.getName())) {
            name.append(fromDTO.getName());
        }
        if (StringUtils.isNotBlank(fromDTO.getCompany())) {
            if (name.length() > 0) {
                name.append(" - ");
            }
            name.append(fromDTO.getCompany());
        }
        if (name.length() == 0) {
            name.append(fromDTO.getEmail());
        }
        String subject = "Email from " + name;
        return subject;
    }

    @Async
    public void sendActivationEmail(User user) {
        log.debug("Sending activation e-mail to '{}'", user.getEmail());
        Locale locale = getLocale(user);
        Context context = new Context(locale);
        context.setVariable(USER, user);
        context.setVariable(BASE_URL, jHipsterProperties.getMail().getBaseUrl());
        String content = templateEngine.process("activationEmail", context);
        String subject = messageSource.getMessage("email.activation.title", new Object[]{user.getActivationKey()}, locale);
        sendEmail(user.getEmail(), subject, content, false, true);
    }

    private Locale getLocale(User user) {
        String language = user.getLangKey();
        if (!"vi".equals(language) && !"en".equals(language)) {
            language = "en";
        }
        return Locale.forLanguageTag(language);
    }

    @Async
    public void sendWelcomeEmail(User user) {
        log.debug("Sending activation e-mail to '{}'", user.getEmail());
        Locale locale = getLocale(user);
        Context context = new Context(locale);
        context.setVariable(USER, user);
        context.setVariable(BASE_URL, jHipsterProperties.getMail().getBaseUrl());
        String content = templateEngine.process("welcomeEmail", context);
        String subject = messageSource.getMessage("email.welcome.title", new Object[]{user.getDisplayName()}, locale);
        sendEmail(user.getEmail(), subject, content, false, true);
    }

    @Async
    public void sendPasswordChangedEmail(User user) {
        log.debug("Sending activation e-mail to '{}'", user.getEmail());
        Locale locale = getLocale(user);
        Context context = new Context(locale);
        context.setVariable(USER, user);
        context.setVariable(BASE_URL, jHipsterProperties.getMail().getBaseUrl());
        String content = templateEngine.process("passwordChangedEmail", context);
        String subject = messageSource.getMessage("email.password-changed.title", new Object[]{user.getDisplayName()}, locale);
        sendEmail(user.getEmail(), subject, content, false, true);
    }

    @Async
    public void sendCreationEmail(User user) {
        log.debug("Sending creation e-mail to '{}'", user.getEmail());
        Locale locale = getLocale(user);
        Context context = new Context(locale);
        context.setVariable(USER, user);
        context.setVariable(BASE_URL, jHipsterProperties.getMail().getBaseUrl());
        String content = templateEngine.process("creationEmail", context);
        String subject = messageSource.getMessage("email.activation.title", null, locale);
        sendEmail(user.getEmail(), subject, content, false, true);
    }

    @Async
    public void sendPasswordResetMail(User user) {
        log.debug("Sending password reset e-mail to '{}'", user.getEmail());
        Locale locale = getLocale(user);
        Context context = new Context(locale);
        context.setVariable(USER, user);
        context.setVariable(BASE_URL, jHipsterProperties.getMail().getBaseUrl());
        String content = templateEngine.process("passwordResetEmail", context);
        String subject = messageSource.getMessage("email.reset.title", new Object[]{user.getResetKey()}, locale);
        sendEmail(user.getEmail(), subject, content, false, true);
    }

    @Async
    public void sendSocialRegistrationValidationEmail(User user, String provider, String email) {
        log.debug("Sending social registration validation e-mail to '{}'", user.getEmail());
        Locale locale = getLocale(user);
        Context context = new Context(locale);
        context.setVariable(USER, user);
        context.setVariable("provider", StringUtils.capitalize(provider));
        String content = templateEngine.process("socialRegistrationValidationEmail", context);
        String subject = messageSource.getMessage("email.social.registration.title", null, locale);
        sendEmail(email, subject, content, false, true);
    }
}
