/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.web.rest.vm;

import com.mediastep.beecow.common.dto.PhoneDTO;

/**
 * View Model object for storing a user's credentials.
 */
public class PhoneLoginVM extends LoginVM {

    private PhoneDTO mobile;

    /**
     * @return the mobile
     */
    public PhoneDTO getMobile() {
        return mobile;
    }

    /**
     * @param mobile the mobile to set
     */
    public void setMobile(PhoneDTO mobile) {
        this.mobile = mobile;
        if (mobile != null) {
            this.username = mobile.toString();
        }
        else {
            this.username = null;
        }
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
            "password='*****'" +
            ", mobile='" + mobile + '\'' +
            ", rememberMe=" + rememberMe +
            '}';
    }
}
