/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.service.mapper;

import java.time.ZonedDateTime;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.mediastep.beecow.common.domain.enumeration.AccountType;
import com.mediastep.beecow.common.domain.enumeration.Gender;
import com.mediastep.beecow.common.dto.PhoneDTO;
import com.mediastep.beecow.common.util.JsonMapObjectUtil;
import com.mediastep.beecow.gateway.domain.User;
import com.mediastep.beecow.gateway.domain.UserField;
import com.mediastep.beecow.gateway.service.util.UserUtil;
import com.mediastep.beecow.gateway.web.rest.errors.ErrorConstants;

@Component
public class UserPropertiesMapper {

    @Inject
    private PasswordEncoder passwordEncoder;

    public String getPassword(Map<String, Object> userProps) {
        return JsonMapObjectUtil.getValue(userProps, UserField.PASSWORD, String::valueOf, ErrorConstants.ERR_USER_PASSWORD_INVALID);
    }

    /**
     * Construct user object from given properties.
     */
    public void setSystemProperties(User user, Map<String, Object> userProps) {
        String fieldName = UserField.ACCOUNT_TYPE;
        if (userProps.containsKey(fieldName)) {
            AccountType accountType = JsonMapObjectUtil.getValue(userProps, fieldName, JsonMapObjectUtil::toAccountType, ErrorConstants.ERR_USER_ACCOUNT_TYPE_INVALID);
            user.setAccountType(accountType);
        }

        fieldName = UserField.LOGIN;
        if (userProps.containsKey(fieldName)) {
            String login = JsonMapObjectUtil.getValue(userProps, fieldName, String::valueOf, ErrorConstants.ERR_USER_LOGIN_INVALID);
            user.setLogin(login);
        }

        fieldName = UserField.PASSWORD;
        if (userProps.containsKey(fieldName)) {
            String password = getPassword(userProps);
            String encryptedPassword = passwordEncoder.encode(password);
            user.setPassword(encryptedPassword);
        }

        setEmail(user, userProps);

        setMobile(user, userProps);
    }

    /**
     * Set email.
     */
    public void setEmail(User user, Map<String, Object> userProps) {
        String fieldName = UserField.EMAIL;
        if (userProps.containsKey(fieldName)) {
            String email = JsonMapObjectUtil.getValue(userProps, fieldName, String::valueOf, ErrorConstants.ERR_USER_EMAIL_INVALID);
            if (StringUtils.isBlank(email)) {
                email = null;
            }
            user.setEmail(email);
        }
    }

    /**
     * Set mobile number
     */
    public void setMobile(User user, Map<String, Object> userProps) {
        String fieldName = UserField.MOBILE;
        if (userProps.containsKey(fieldName)) {
            PhoneDTO mobile = JsonMapObjectUtil.getValue(userProps, fieldName, JsonMapObjectUtil::toPhoneDTO, ErrorConstants.ERR_USER_MOBILE_INVALID);
            if (mobile != null && (StringUtils.isBlank(mobile.getCountryCode()) || StringUtils.isBlank(mobile.getPhoneNumber()))) {
                mobile = null;
            }
            user.setMobileObject(mobile);
        }
    }

    /**
     * Set user properties.
     * @param user user object to be set
     * @param userProps user properties
     */
    public void setProperties(User user, Map<String, Object> userProps) {
        // Update first-name, last-name and display-name
        setFirstNameLastNameAndDisplayName(user, userProps);
        
        // Update avatar URL
        String fieldName = UserField.AVATAR_URL;
        if (userProps.containsKey(fieldName)) {
            String avatarUrl = JsonMapObjectUtil.getValue(userProps, fieldName, JsonMapObjectUtil::toImageAsString, ErrorConstants.ERR_USER_AVATAR_URL_INVALID);
            user.setAvatarUrl(avatarUrl);
        }
        
        // Update cover photo URL
        fieldName = UserField.COVER_PHOTO_URL;
        if (userProps.containsKey(fieldName)) {
            String coverPhotoUrl = JsonMapObjectUtil.getValue(userProps, fieldName, JsonMapObjectUtil::toImageAsString, ErrorConstants.ERR_USER_COVER_PHOTO_URL_INVALID);
            user.setCoverPhotoUrl(coverPhotoUrl);
        }
        
        // Update date of birth
        fieldName = UserField.DATE_OF_BIRTH;
        if (userProps.containsKey(fieldName)) {
            ZonedDateTime dateOfBirth = JsonMapObjectUtil.getValue(userProps, fieldName, JsonMapObjectUtil::toZonedDateTime, ErrorConstants.ERR_USER_DATE_OF_BIRTH_INVALID);
            UserUtil.setDateOfBirth(user, dateOfBirth);
        }
        
        // Update gender
        fieldName = UserField.GENDER;
        if (userProps.containsKey(fieldName)) {
            Gender gender = JsonMapObjectUtil.getValue(userProps, fieldName, JsonMapObjectUtil::toGender, ErrorConstants.ERR_USER_DATE_OF_BIRTH_INVALID);
            user.setGender(gender);
        }
        else if (user.getGender() == null) {
            user.setGender(Gender.MALE);
        }
        
        // Update location code
        fieldName = UserField.LOCATION_CODE;
        if (userProps.containsKey(fieldName)) {
            String locationCode = JsonMapObjectUtil.getValue(userProps, fieldName, String::valueOf, ErrorConstants.ERR_USER_DATE_OF_BIRTH_INVALID);
            user.setLocationCode(locationCode);
        }
        
        // Update lang-key
        fieldName = UserField.LANG_KEY;
        if (userProps.containsKey(fieldName)) {
            String langKey = JsonMapObjectUtil.getValue(userProps, fieldName, String::valueOf, ErrorConstants.ERR_USER_DATE_OF_BIRTH_INVALID);
            user.setLangKey(langKey);
        }
    }

    /**
     * Set first-name, last-name and display-name.
     * <pre>
     * NOTE: always update first-name, last-name and display-name at once (using UserUtil.setFirstNameAndLastName())
     * </pre>
     * @param user user object to be set
     * @param userProps user properties
     */
    private void setFirstNameLastNameAndDisplayName(User user, Map<String, Object> userProps) {
        // Set first name
        String firstNameFiledName = UserField.FIRST_NAME;
        if (userProps.containsKey(firstNameFiledName)) {
            String firstName = JsonMapObjectUtil.getValue(userProps, firstNameFiledName, String::valueOf, ErrorConstants.ERR_USER_FIRST_NAME_INVALID);
            user.setFirstName(firstName);
        }
        // Set last name
        String lastNameFiledName = UserField.LAST_NAME;
        if (userProps.containsKey(lastNameFiledName)) {
            String lastName = JsonMapObjectUtil.getValue(userProps, lastNameFiledName, String::valueOf, ErrorConstants.ERR_USER_LAST_NAME_INVALID);
            user.setLastName(lastName);
        }
        // Set display name
        String displayNameFiledName = UserField.DISPLAY_NAME;
        String displayName;
        if (userProps.containsKey(displayNameFiledName)) {
            displayName = JsonMapObjectUtil.getValue(userProps, displayNameFiledName, String::valueOf, ErrorConstants.ERR_USER_LAST_NAME_INVALID);
        }
        else {
            displayName = null;
        }
        UserUtil.setDisplayName(user, displayName);
    }

    public void setLogin(User user, Map<String, Object> userProps) {
        String fieldName = UserField.LOGIN;
        String login = JsonMapObjectUtil.getValue(userProps, fieldName, String::valueOf, ErrorConstants.ERR_USER_LOGIN_INVALID);
        user.setLogin(login);
    }
}
