/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.web.rest.vm;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Email;

import com.mediastep.beecow.common.dto.UserDTO;

import io.swagger.annotations.ApiModelProperty;

/**
 * View Model for email.
 */
public class EmailVM {

    @ApiModelProperty(value = "Email")
    @NotNull
    @Email
    @Size(min = UserDTO.EMAIL_MIN_LENGTH, max = UserDTO.EMAIL_MAX_LENGTH)
    private String email;

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }
}
