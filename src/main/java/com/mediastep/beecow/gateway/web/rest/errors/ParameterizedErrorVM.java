/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.web.rest.errors;

import java.io.Serializable;

/**
 * View Model for sending a parameterized error message.
 */
public class ParameterizedErrorVM implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String message;
    private final String[] params;

    public ParameterizedErrorVM(String message, String... params) {
        this.message = message;
        this.params = params;
    }

    public String getMessage() {
        return message;
    }

    public String[] getParams() {
        return params;
    }

}
