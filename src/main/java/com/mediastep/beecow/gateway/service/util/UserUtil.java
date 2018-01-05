/*******************************************************************************
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 01/01/2017
 * Author: Huyen Lam <email:huyen.lam@mediastep.com>
 *  
 *******************************************************************************/
package com.mediastep.beecow.gateway.service.util;

import java.time.ZonedDateTime;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

import com.mediastep.beecow.common.domain.enumeration.AccountType;
import com.mediastep.beecow.common.dto.PhoneDTO;
import com.mediastep.beecow.common.dto.UserDTO;
import com.mediastep.beecow.common.dto.UserStatus;
import com.mediastep.beecow.common.security.AuthoritiesConstants;
import com.mediastep.beecow.common.security.TokenUserAuthority;
import com.mediastep.beecow.common.security.TokenUserDetails;
import com.mediastep.beecow.gateway.domain.Authority;
import com.mediastep.beecow.gateway.domain.User;

public class UserUtil {

    public static final String DEFAULT_LANGUAGE = "en";

    static private final String NAME_SEP = " ";

    public static String getLanguage(String language) {
        if (StringUtils.isBlank(language)) {
            language = DEFAULT_LANGUAGE;
        }
        return language;
    }

    /**
     * Set first name and last name and then construct display name
     * @param user
     * @param firstName
     * @param lastName
     */
    public static void setFirstNameAndLastName(User user, String firstName, String lastName, String displayName) {
        user.setFirstName(firstName);
        user.setLastName(lastName);
        setDisplayName(user, displayName);
    }

    /**
     * Set display name to given display name or user first-name/last-name, email, phone number, login.
     * @param user
     * @param displayName
     */
    public static void setDisplayName(User user, String displayName) {
        if (StringUtils.isNotBlank(displayName)) {
            user.setDisplayName(displayName);
            return;
        }
        displayName = getDefaultDisplayName(user.getAccountType(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getMobileObject(), user.getLogin());
        user.setDisplayName(displayName);
    }

    /**
     * Set display name to given display name or user first-name/last-name, email, phone number, login.
     * @param user
     * @param displayName
     */
    public static void setDefaultDisplayName(User user) {
        String displayName = getDefaultDisplayName(user.getAccountType(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getMobileObject(), user.getLogin());
        user.setDisplayName(displayName);
    }

    /**
     * Set display name to given display name or user first-name/last-name, email, phone number, login.
     * @param user
     * @param displayName
     */
    public static void setDisplayName(UserDTO user, String displayName) {
        if (StringUtils.isBlank(displayName)) {
            displayName = getDefaultDisplayName(user.getAccountType(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getMobile(), user.getLogin());;
        }
        user.setDisplayName(displayName);
    }

    private static String getDefaultDisplayName(AccountType accountType, String firstName, String lastName, String email, PhoneDTO mobileDTO, String login) {
        String displayName;
        if (StringUtils.isNotBlank(firstName) || StringUtils.isNotBlank(lastName)) {
            displayName = getDisplayName(firstName, lastName);
        }
        else switch (accountType) {
            case EMAIL:
                displayName = getDefaultDisplayNameByEmail(email);
                break;
            case MOBILE:
                displayName = getDefaultDisplayNameByMobile(mobileDTO);
                break;
            default:
                displayName = login;
                break;
        }
        return displayName;
    }

    private static String getDisplayName(String firstName, String lastName) {
        boolean hasFirstName = StringUtils.isNoneBlank(firstName);
        boolean hasLastName = StringUtils.isNoneBlank(lastName);
        String displayName;
        if (hasFirstName && hasLastName) {
            displayName = firstName + NAME_SEP + lastName;
        }
        else if (hasFirstName) {
            displayName = firstName;
        }
        else if (hasLastName) {
            displayName = lastName;
        }
        else {
            displayName = null;
        }
        return displayName;
    }

    private static String getDefaultDisplayNameByEmail(String email) {
        if (StringUtils.isBlank(email)) {
            return null;
        }
        int end = email.indexOf("@");
        if (end < 0) {
            end = email.length();
        }
        String displayName = email.substring(0, end);
        return displayName;
    }

    private static String getDefaultDisplayNameByMobile(PhoneDTO mobileDTO) {
        if (mobileDTO == null || StringUtils.isBlank(mobileDTO.getPhoneNumber())) {
            return null;
        }
        else {
            return mobileDTO.getPhoneNumber();
        }
    }

    public static void setDateOfBirth(User user, ZonedDateTime dateOfBirth) {
        if (dateOfBirth != null) {
            user.setDateOfBirth(dateOfBirth.toLocalDate());
        }
        else {
            user.setDateOfBirth(null);
        }
    }

    public static boolean isSocialAccount(User user) {
        assert(user != null);
        return user.getAccountType() == AccountType.FACEBOOK;
    }

    public static boolean isGuest(User user) {
        assert(user != null);
        return hasRole(user.getAuthorities(), Authority.GUEST);
    }

    public static boolean isGuest(TokenUserDetails user) {
        assert(user != null);
        return hasRole(user.getAuthorities(), new TokenUserAuthority(AuthoritiesConstants.GUEST));
    }

    public static boolean isBeecowUser(User user) {
        assert(user != null);
        return hasRole(user.getAuthorities(), Authority.BEECOW);
    }

    public static boolean isBeecowUser(TokenUserDetails user) {
        assert(user != null);
        return hasRole(user.getAuthorities(), new TokenUserAuthority(AuthoritiesConstants.BEECOW));
    }

    /**
     * Check if given authorities has expected authority
     * @param authorities
     * @param expectedAuthority
     * @return
     */
    private static <T> boolean hasRole(Collection<T> authorities, T expectedAuthority) {
        assert(authorities != null);
        return (authorities.contains(expectedAuthority));
    }

    public static boolean isGuestAndNotBeecowUser(User user) {
        return isGuest(user) && !isBeecowUser(user);
    }

    /**
     * Check if user has activation key and still valid
     * @param user
     * @return
     */
    public static boolean isWaitingForActivate(User user) {
        assert(user != null);
        return user.getActivationKey() != null &&
            user.getActivationExpiredDate() != null && ZonedDateTime.now().isBefore(user.getActivationExpiredDate());
    }

    public static UserStatus getUserStatus(User user) {
    	if (user.getActivated()) {
        	if (isGuest(user)) {
        		return UserStatus.PRE_ACTIVATED;
        	}
        	else {
        		return UserStatus.ACTIVATED;
        	}
    	}
    	else {
	    	if (StringUtils.isNotBlank(user.getActivationKey())) {
	    		return UserStatus.REGISTERED;
	    	}
	    	else {
	    		return UserStatus.LOCKED;
	    	}
    	}
    }
}
