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

import io.swagger.annotations.ApiModelProperty;

/**
 * View Model for Change password API.
 */
public class CurrentAndNewPasswordVM {

    @ApiModelProperty(value = "currentPassword")
    @NotNull
    private String currentPassword;
    
    @ApiModelProperty(value = "newPassword")
    @NotNull
    private String newPassword;
    
    @ApiModelProperty(value = "deviceToken")
    @NotNull
    private String deviceToken;
    
    /**
     * @return the current password
     */
    public String getCurrentPassword() {
        return currentPassword;
    }

    /**
     * @param currentPassword the current password to set
     */
    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }
    
    /**
     * @return the new password
     */
    public String getNewPassword() {
        return newPassword;
    }

    /**
     * @param newPassword the new password to set
     */
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
    
    /**
     * @return The token's device we need to ignore push to.
     */
    public String getDeviceToken() {
        return deviceToken;
    }

    /**
     * @param deviceToken the token's device we need to ignore push to.
     */
    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }
}
