/*******************************************************************************
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 01/01/2017
 * Author: Huyen Lam <email:huyen.lam@mediastep.com>
 *  
 *******************************************************************************/
package com.mediastep.beecow.gateway.domain;

/**
 * A user field names which <u>allow to be updated by user</u>.
 */
public final class UserField {
    // Read-only fields
    public static final String LOGIN = "login";
    public static final String PASSWORD = "password";
    public static final String EMAIL = "email";
    public static final String MOBILE = "mobile";
    public static final String ACCOUNT_TYPE = "accountType";

    // Updatable fields
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String DISPLAY_NAME = "displayName";
    public static final String AVATAR_URL = "avatarUrl";
    public static final String DATE_OF_BIRTH = "dateOfBirth";
    public static final String GENDER = "gender";
    public static final String LOCATION_CODE = "locationCode";
    public static final String LANG_KEY = "langKey";
    public static final String COVER_PHOTO_URL = "coverPhotoUrl";
}
