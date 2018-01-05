/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.service;

import java.util.Arrays;
import java.util.Locale;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.mediastep.beecow.common.config.BeecowConfig;
import com.mediastep.beecow.common.dto.PhoneDTO;
import com.mediastep.beecow.gateway.domain.User;
import com.mediastep.beecow.sms.service.SMSRequestGateway;
import com.mediastep.beecow.sms.service.dto.MessageDTO;

/**
 * Service for sending SMS.
 * <p>
 * We use the @Async annotation to send e-mails asynchronously.
 * </p>
 */
@Service
public class SMSService {

    private final Logger log = LoggerFactory.getLogger(SMSService.class);

    @Inject
    private BeecowConfig beecowConfig;

    @Inject
    private MessageSource messageSource;

    @Inject
    private SMSRequestGateway smsGateway;

    /**
     * Sending activation code to SMS.
     * 
     * @param bcOrderDTO
     */
    @Async
    public void sendActivationMessage(User user) {
        // Get phone number
        PhoneDTO phoneDTO = user.getMobileObject();
    	log.debug("Sending activation code to '{}'", phoneDTO);
    	sendSms(user, "sms.activation.code", new String[] {user.getActivationKey()});
        log.debug("Activation code has been sent: {}", phoneDTO);
    }

    /**
     * Send order confirmation SMS.
     * 
     * @param bcOrderDTO
     */
    private void sendSms(User user, String messageKey, String[] params) {
        // Get phone number
        PhoneDTO phoneDTO = user.getMobileObject();
        // Get language
        Locale locale = beecowConfig.getLocale(user.getLangKey());
        String smsContent = messageSource.getMessage(messageKey, params, locale);
        // Send message
        MessageDTO sms = new MessageDTO();
        sms.setTo(Arrays.asList(phoneDTO));
        sms.setContent(smsContent);
        smsGateway.send(sms);
    }

    /**
     * Sending password reset-code to SMS.
     * 
     * @param bcOrderDTO
     */
    @Async
    public void sendPasswordResetMessage(User user) {
        // Get phone number
        PhoneDTO phoneDTO = user.getMobileObject();
    	log.debug("Sending password reset-key to '{}'", phoneDTO);
    	sendSms(user, "sms.password.reset-key", new String[] {user.getResetKey()});
        log.debug("Password reset-key has been sent: {}", phoneDTO);
    }
}
