/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.web.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.RandomStringUtils;

import com.mediastep.beecow.common.domain.enumeration.AccountType;
import com.mediastep.beecow.common.dto.PhoneDTO;
import com.mediastep.beecow.common.security.AuthoritiesConstants;
import com.mediastep.beecow.common.util.PhoneDtoUtil;
import com.mediastep.beecow.gateway.domain.Authority;
import com.mediastep.beecow.gateway.domain.User;

/**
 * Test class for the UserResource REST controller.
 *
 * @see UserResource
 */
public class UserResourceIntTestUtil {

    public static final String DEFAULT_LOGIN = "john";
    public static final String DEFAULT_PASSWORD = "123456";
    public static final String ENCRYPTED_DEFAUT_PASSWORD = String.format("%60s", " ").replaceAll(" ", "a");
    public static final String DEFAULT_EMAIL = "john@example.com";
    public static final String DEFAULT_MOBILE = "+84:0123456789";
    public static final String DEFAULT_FIRSTNAME = "John";
    public static final String DEFAULT_LASTNAME = "Henry";
    public static final String DEFAULT_DISPLAYNAME = DEFAULT_FIRSTNAME + " " + DEFAULT_LASTNAME;

    public static final String DEFAULT_AVATAR = "DEFAULT_AVATAR";
    public static final String DEFAULT_MALE_AVATAR = "DEFAULT_MALE_AVATAR";
    public static final String DEFAULT_FEMALE_AVATAR = "DEFAULT_FEMALE_AVATAR";

    public static final String DEFAULT_LOCATION = "MM-05";
    public static final String DEFAULT_LOCATION2 = "NOCOUNTRY"; // no country
    public static final String DEFAULT_LANG = "my";
    public static final String DEFAULT_LANG2 = "my-zawgyi";

    public static final String DEFAULT_ACTIVATION_CODE = "123456";

    public static final String DEFAULT_PASSWORD_RESET_KEY = "ABCDEF";

    public static final String DEFAULT_GUEST_LOGIN = "guest";
    public static final String DEFAULT_GUEST_EMAIL = "guest@example.com";

    public static final Long BEECOW_USER_ID = 5L;
    public static final String BEECOW_USER_LOGIN = "beecowuser";

    public static final Long BEECOW_USER_ID2 = 6L;
    public static final String BEECOW_USER_LOGIN2 = "beecowuser_mm";

    public static final Long BEECOW_USER_ID3 = 7L;
    public static final String BEECOW_USER_LOGIN3 = "beecowuser_mm_my-zawgyi";

    public static final List<String> PRE_EXIST_GUEST_LOGIN_NAMES = Arrays.asList(BEECOW_USER_LOGIN, BEECOW_USER_LOGIN2, BEECOW_USER_LOGIN3,
        "beecowuser_vn", "beecowuser_cn", "beecowuser_tw");

    public static final List<String> PRE_EXIST_USER_LOGIN_NAMES = Arrays.asList("system", "anonymoususer", "admin", "user",
		"editor", "saleslead1@mediastep.com", "saleslead2@mediastep.com", "saleslead3@mediastep.com",
		"dev.support@mediastep.com", "qa.support@mediastep.com", "cs.support@mediastep.com",
		DEFAULT_LOGIN);

    public static final List<String> PRE_EXIST_LOGIN_NAMES = new ArrayList<>();
    static {
        PRE_EXIST_LOGIN_NAMES.addAll(PRE_EXIST_USER_LOGIN_NAMES);
        PRE_EXIST_LOGIN_NAMES.addAll(PRE_EXIST_GUEST_LOGIN_NAMES);
    }

    public static final int TEST_USER_COUNT = 5;
    public static final int TEST_GUEST_COUNT = 5;
    public static final int PRE_EXIST_USER_LOGIN_COUNT = PRE_EXIST_USER_LOGIN_NAMES.size();
    public static final int PRE_EXIST_GUEST_LOGIN_COUNT = PRE_EXIST_GUEST_LOGIN_NAMES.size();
    public static final int PRE_EXIST_LOGIN_COUNT = PRE_EXIST_USER_LOGIN_COUNT + PRE_EXIST_GUEST_LOGIN_COUNT;
    public static final int TEST_ACTIVATED_USER_COUNT = PRE_EXIST_USER_LOGIN_COUNT + TEST_USER_COUNT;
    public static final int TEST_COUNT = PRE_EXIST_LOGIN_COUNT + TEST_USER_COUNT + TEST_GUEST_COUNT;

    /**
     * Create a User.
     */
    public static User createUser(EntityManager em, String login, String password, AccountType accountType, String email, String mobile,
        String firstName, String lastName, String location, String language, List<String> authorities, boolean persist) {
        User user = new User();
        user.setLogin(login);
        user.setPassword(password);
        user.setActivated(true);
        user.setAccountType(accountType);
        user.setEmail(email);
        user.setMobile(mobile);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setDisplayName(firstName + " " + lastName);
        user.setLocationCode(location);
        user.setLangKey(language);
        user.setAuthorities(authorities.stream().map(authority -> new Authority(authority))
            .collect(Collectors.toSet()));
        if (persist) {
            em.persist(user);
            em.flush();
        }
        return user;
    }

    /**
     * Create a User.
     */
    public static User createUser(EntityManager em, boolean persist) {
        return createUser(em, DEFAULT_LOGIN, RandomStringUtils.random(60), AccountType.EMAIL, DEFAULT_EMAIL, null,
            DEFAULT_FIRSTNAME, DEFAULT_LASTNAME, DEFAULT_LOCATION, DEFAULT_LANG,
            Arrays.asList(AuthoritiesConstants.USER), persist);
    }

    /**
     * Create a User.
     */
    public static User createUser(EntityManager em, String login, String password, String email, String authority, boolean persist) {
        return createUser(em, login, password, AccountType.EMAIL, email, null,
            DEFAULT_FIRSTNAME, DEFAULT_LASTNAME, DEFAULT_LOCATION, DEFAULT_LANG,
            Arrays.asList(authority), persist);
    }

    /**
     * Create a User.
     */
    public static User createMobileUser(EntityManager em, String login, String password, String mobile, String authority, boolean persist) {
        return createUser(em, login, password, AccountType.MOBILE, null, mobile,
            DEFAULT_FIRSTNAME, DEFAULT_LASTNAME, DEFAULT_LOCATION, DEFAULT_LANG,
            Arrays.asList(authority), persist);
    }

    /**
     * Create a User.
     */
    public static User createUser(EntityManager em, String login, String password, String email, String firstName, String lastName, String authority, boolean persist) {
        return createUser(em, login, password, AccountType.EMAIL, email, null,
            firstName, lastName, DEFAULT_LOCATION, DEFAULT_LANG,
            Arrays.asList(authority), persist);
    }

    /**
     * Create a User.
     */
    public static User createUser(EntityManager em, String login, String password, String email, String firstName, String lastName,
        String location, String language, String authority, boolean persist) {
        return createUser(em, login, password, AccountType.EMAIL, email, null, firstName, lastName, location, language, Arrays.asList(authority), persist);
    }

    public static List<User> createUsers(EntityManager em, int count, String loginPrefix, String emailPrefix, String authority, boolean persist) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            User user = createUser(em, loginPrefix + i, RandomStringUtils.random(60), emailPrefix + i, authority, persist);
            users.add(user);
        }
        return users;
    }

    public static List<User> createMobileUsers(EntityManager em, int count, String loginPrefix, String mobilePrefix, String authority, boolean persist) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            User user = createMobileUser(em, loginPrefix + i, RandomStringUtils.random(60), mobilePrefix + i, authority, persist);
            users.add(user);
        }
        return users;
    }

    public static List<User> createUsers(EntityManager em, boolean persist) {
        return createUsers(em, TEST_USER_COUNT, DEFAULT_EMAIL, DEFAULT_EMAIL, AuthoritiesConstants.USER, persist);
    }

    public static List<User> createMobileUsers(EntityManager em, boolean persist) {
        return createMobileUsers(em, TEST_USER_COUNT, DEFAULT_MOBILE, DEFAULT_MOBILE, AuthoritiesConstants.USER, persist);
    }

    public static List<User> createGuests(EntityManager em, boolean persist) {
        return createUsers(em, TEST_GUEST_COUNT, DEFAULT_GUEST_LOGIN, DEFAULT_GUEST_EMAIL, AuthoritiesConstants.GUEST, persist);
    }

    public static List<String> getEmails(List<User> users) {
        List<String> logins = new ArrayList<>();
        for (User user : users) {
            logins.add(user.getEmail());
        }
        return logins;
    }

    public static List<String> getMobiles(List<User> users) {
        List<String> logins = new ArrayList<>();
        for (User user : users) {
            logins.add(user.getMobile());
        }
        return logins;
    }

    public static List<String> getRawMobiles(List<User> users) {
        List<String> logins = new ArrayList<>();
        for (User user : users) {
            PhoneDTO mobile = PhoneDtoUtil.stringToPhoneDTO(user.getMobile());
            logins.add(mobile.getCountryCode() + mobile.getPhoneNumber());
        }
        return logins;
    }

    public static List<String> getLoginNames(List<User> users) {
        List<String> logins = new ArrayList<>();
        getLoginNames(logins, users);
        return logins;
    }

    public static void getLoginNames(List<String> logins, List<User> users) {
        for (User user : users) {
            logins.add(user.getLogin());
        }
    }
}
