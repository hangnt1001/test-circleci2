/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Config for UserResource and UserService
 */
@Configuration
public class EmailServiceConfig {

    private static final String DEFAULT_SENDER_EMAIL = "admin@mediastep.com";

    private static final String DEFAULT_CONTACT_US_EMAIL = "admin@mediastep.com";

    @Value("${beecow.gateway.emailService.senderEmail:" + DEFAULT_SENDER_EMAIL + "}")
    private String senderEmail;

    @Value("${beecow.gateway.emailService.contactUsEmail:" + DEFAULT_CONTACT_US_EMAIL + "}")
    private String contactUsEmail;

    /**
     * @return the senderEmail
     */
    public String getSenderEmail() {
        return senderEmail;
    }

    /**
     * @param senderEmail the senderEmail to set
     */
    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    /**
     * @return the contactUsEmail
     */
    public String getContactUsEmail() {
        return contactUsEmail;
    }

    /**
     * @param contactUsEmail the contactUsEmail to set
     */
    public void setContactUsEmail(String contactUsEmail) {
        this.contactUsEmail = contactUsEmail;
    }
}
