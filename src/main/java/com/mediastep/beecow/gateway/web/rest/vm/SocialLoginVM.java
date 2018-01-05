/*******************************************************************************
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 01/01/2017
 * Author: Huyen Lam <email:huyen.lam@mediastep.com>
 *  
 *******************************************************************************/
package com.mediastep.beecow.gateway.web.rest.vm;

/**
 * View Model object for storing a user's credentials.
 */
public class SocialLoginVM {

    private String token;

    private String location;

    private String langKey;

    private Long mergeWithUserId;

    /**
     * @return the token
     */
    public String getToken() {
        return token;
    }

    /**
     * @param token the token to set
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * @return the langKey
     */
    public String getLangKey() {
        return langKey;
    }

    /**
     * @param langKey the langKey to set
     */
    public void setLangKey(String langKey) {
        this.langKey = langKey;
    }

    /**
     * @return the mergeWithUserId
     */
    public Long getMergeWithUserId() {
        return mergeWithUserId;
    }

    /**
     * @param mergeWithUserId the mergeWithUserId to set
     */
    public void setMergeWithUserId(Long mergeWithUserId) {
        this.mergeWithUserId = mergeWithUserId;
    }
}
