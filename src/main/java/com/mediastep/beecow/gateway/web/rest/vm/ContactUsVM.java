/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <EmailDTO:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.web.rest.vm;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.mediastep.beecow.gateway.service.dto.EmailDTO;

/**
 * View Model for EmailDTO.
 */
public class ContactUsVM {

    @NotNull
    @Valid
    private EmailDTO from;

    @NotNull
    @Size(min = 10, max = 100000)
    private String content;

    /**
     * @return the from
     */
    public EmailDTO getFrom() {
        return from;
    }

    /**
     * @param from the from to set
     */
    public void setFrom(EmailDTO from) {
        this.from = from;
    }

    /**
     * @return the content
     */
    public String getContent() {
        return content;
    }

    /**
     * @param content the content to set
     */
    public void setContent(String content) {
        this.content = content;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ContactUsVM {from=" + from + ", content=" + content + "}";
    }
}
