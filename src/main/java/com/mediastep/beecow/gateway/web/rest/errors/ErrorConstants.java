/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.web.rest.errors;

public final class ErrorConstants {

    public static final String ERR_CONCURRENCY_FAILURE = "error.concurrencyFailure";
    public static final String ERR_UNAUTHORIZED = "error.unauthorized";
    public static final String ERR_ACCESS_DENIED = "error.accessDenied";
    public static final String ERR_VALIDATION = "error.validation";
    public static final String ERR_METHOD_NOT_SUPPORTED = "error.methodNotSupported";
    public static final String ERR_INTERNAL_SERVER_ERROR = "error.internalServerError";

    public static final String ERR_USER_PREFIX = "error.user.";
    public static final String ERR_USER_INVALID_SUBFIX = "Invalid";
    public static final String ERR_USER_INVALID = "error.user.invalid";
    public static final String ERR_USER_ACCOUNT_TYPE_INVALID = "error.user.accountTypeInvalid";
    public static final String ERR_USER_LOGIN_INVALID = "error.user.loginInvalid";
    public static final String ERR_USER_LOGIN_MISSING = "error.user.loginMissing";
    public static final String ERR_USER_LOGIN_EXISTS = "error.user.loginExists";
    public static final String ERR_USER_PASSWORD_INVALID = "error.user.passwordInvalid";
    public static final String ERR_USER_EMAIL_INVALID = "error.user.emailInvalid";
    public static final String ERR_USER_EMAIL_EXISTS = "error.user.emailExists";
    public static final String ERR_USER_MOBILE_INVALID = "error.user.mobileInvalid";
    public static final String ERR_USER_MOBILE_EXISTS = "error.user.mobileExists";
    public static final String ERR_USER_EMAIL_MISSING = "error.user.emailMissing";
    public static final String ERR_USER_MOBILE_MISSING = "error.user.mobileMissing";
    public static final String ERR_USER_FIRST_NAME_INVALID = "error.user.firstNameInvalid";
    public static final String ERR_USER_LAST_NAME_INVALID = "error.user.lastNameInvalid";
    public static final String ERR_USER_DISPLAY_NAME_INVALID = "error.user.displayNameInvalid";
    public static final String ERR_USER_DATE_OF_BIRTH_INVALID = "error.user.dateOfBirthInvalid";
    public static final String ERR_USER_GENDER_INVALID = "error.user.genderInvalid";
    public static final String ERR_USER_LOCATION_CODE_INVALID = "error.user.locationCodeInvalid";
    public static final String ERR_USER_LANG_KEY_INVALID = "error.user.langKeyInvalid";
    public static final String ERR_USER_AVATAR_URL_INVALID = "error.user.avatarUrlInvalid";
    public static final String ERR_USER_COVER_PHOTO_URL_INVALID = "error.user.coverPhotoUrlInvalid";

    public static final String ERR_USER_NOT_FOUND = "error.user.notFound";
    public static final String ERR_USER_NOT_ACTIVATED = "error.user.notActivated";

    private ErrorConstants() {
    }

}
