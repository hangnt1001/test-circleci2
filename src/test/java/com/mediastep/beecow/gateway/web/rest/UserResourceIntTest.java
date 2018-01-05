/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.web.rest;

import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.*;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.createGuests;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.createMobileUsers;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.createUser;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.createUsers;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.getEmails;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.getLoginNames;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.getMobiles;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.getRawMobiles;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import com.mediastep.beecow.common.security.AuthoritiesConstants;
import com.mediastep.beecow.common.util.PhoneDtoUtil;
import com.mediastep.beecow.gateway.BeecowGatewayApp;
import com.mediastep.beecow.gateway.domain.User;
import com.mediastep.beecow.gateway.repository.UserRepository;
import com.mediastep.beecow.gateway.service.UserService;
import com.mediastep.beecow.gateway.service.mapper.UserMapper;
import com.mediastep.beecow.gateway.web.rest.vm.ContactListVM;

/**
 * Test class for the UserResource REST controller.
 *
 * @see UserResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = BeecowGatewayApp.class)
public class UserResourceIntTest {

    @Inject
    private UserRepository userRepository;

    @Inject
    private UserService userService;

    @Inject
    private UserMapper userMapper;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Inject
    private EntityManager em;

    private User user;

    private MockMvc restUserMockMvc;

    @Before
    public void setup() {
        UserResource userResource = new UserResource();
        ReflectionTestUtils.setField(userResource, "userRepository", userRepository);
        ReflectionTestUtils.setField(userResource, "userService", userService);
        ReflectionTestUtils.setField(userResource, "userMapper", userMapper);
        this.restUserMockMvc = MockMvcBuilders.standaloneSetup(userResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .build();
    }

    @Before
    public void init() {
        user = createUser(em, true);
    }

    @Test
    @Transactional
    public void testGetUsers() throws Exception {
        // Create initial data
        List<User> users = createUsers(em, true);
        List<User> guests = createGuests(em, true);
        // Run test
        ResultActions result = restUserMockMvc.perform(get("/api/users?page=0&size=100&sort=id,asc")
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print(System.err));
        // Check result
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.length()").value(TEST_COUNT))
                .andExpect(jsonPath("$.[*].authorities[*]").value(hasItem(AuthoritiesConstants.ADMIN)))
                .andExpect(jsonPath("$.[*].authorities[*]").value(hasItem(AuthoritiesConstants.USER)))
                .andExpect(jsonPath("$.[*].authorities[*]").value(hasItem(AuthoritiesConstants.GUEST)))
                .andExpect(jsonPath("$.[*].password").value(not(hasItem(notNullValue()))));
        System.out.println(result.andReturn().getResponse().getContentAsString());
        List<String> logins = new ArrayList<>(PRE_EXIST_LOGIN_NAMES);
        getLoginNames(logins, users);
        getLoginNames(logins, guests);
        for (String login : logins) {
            result.andExpect(jsonPath("$.[*].login").value(hasItem(login)));
        }
    }

    @Test
    @Transactional
    public void testGetActivatedUsers() throws Exception {
        // Create initial data
        List<User> users = createUsers(em, true);
        List<User> guests = createGuests(em, true);
        // Run test
        ResultActions result = restUserMockMvc.perform(get("/api/users/activated?page=0&size=100&sort=id,asc")
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print(System.err));
        // Check result
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.length()").value(TEST_ACTIVATED_USER_COUNT))
                .andExpect(jsonPath("$.[*].authorities[*]").value(hasItem(AuthoritiesConstants.ADMIN)))
                .andExpect(jsonPath("$.[*].authorities[*]").value(hasItem(AuthoritiesConstants.USER)))
                .andExpect(jsonPath("$.[*].authorities[*]").value(not(hasItem(AuthoritiesConstants.GUEST))))
                .andExpect(jsonPath("$.[*].password").value(not(hasItem(notNullValue()))));;
        List<String> logins = new ArrayList<>(PRE_EXIST_USER_LOGIN_NAMES);
        getLoginNames(logins, users);
        for (String login : logins) {
            result.andExpect(jsonPath("$.[*].login").value(hasItem(login)));
        }
        List<String> guestLogins = new ArrayList<>(PRE_EXIST_GUEST_LOGIN_NAMES);
        getLoginNames(guestLogins, guests);
        for (String guestLogin : guestLogins) {
            result.andExpect(jsonPath("$.[*].login").value(not(hasItem(guestLogin))));
        }
    }

    @Test
    @Transactional
    public void testGetUsersByEmailsAndMobiles() throws Exception {
        // Create initial data
        createGuests(em, true);
        List<User> emailUsers = createUsers(em, true);
        List<User> mobileUsers = createMobileUsers(em, true);
        List<String> existingEmails = getEmails(emailUsers);
        List<String> existingMobiles = getRawMobiles(mobileUsers);
        // Create input param
        List<String> emails = new ArrayList<>(existingEmails);
        emails.add("notfound1@example.com");
        emails.add("notfound2@example.com");
        emails.add("notfound3@example.com");
        List<String> mobiles = new ArrayList<>(existingMobiles);
        mobiles.add("+84:0987654321");
        mobiles.add("+84:0987654322");
        mobiles.add("+84:0987654323");

        // Run test
        ContactListVM contactListVM = new ContactListVM();
        contactListVM.setEmails(emails);
        contactListVM.setMobiles(mobiles);

        ResultActions result = restUserMockMvc.perform(
                post("/api/users/contact?sort=id,asc")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(contactListVM))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print(System.err));
        // Check result
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.length()").value(emailUsers.size() + mobileUsers.size()))
                .andExpect(jsonPath("$.[*].password").value(not(hasItem(notNullValue()))));;
        for (String email : existingEmails) {
            result.andExpect(jsonPath("$.[*].login").value(hasItem(email)));
            result.andExpect(jsonPath("$.[*].email").value(hasItem(email)));
        }
        for (String mobile : getMobiles(mobileUsers)) {
            result.andExpect(jsonPath("$.[*].login").value(hasItem(mobile)));
            String phoneNumber = PhoneDtoUtil.stringToPhoneDTO(mobile).getPhoneNumber();
            result.andExpect(jsonPath("$.[*].mobile.phoneNumber").value(hasItem(phoneNumber)));
        }
    }

    @Test
    @Transactional
    public void testGetUsersByEmails() throws Exception {
        // Create initial data
        createGuests(em, true);
        List<User> emailUsers = createUsers(em, true);
        createMobileUsers(em, true);
        List<String> existingEmails = getEmails(emailUsers);
        // Create input param
        List<String> emails = new ArrayList<>(existingEmails);
        emails.add("notfound1@example.com");
        emails.add("notfound2@example.com");
        emails.add("notfound3@example.com");

        // Run test
        ContactListVM contactListVM = new ContactListVM();
        contactListVM.setEmails(emails);

        ResultActions result = restUserMockMvc.perform(
                post("/api/users/contact?sort=id,asc")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(contactListVM))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print(System.err));
        // Check result
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.length()").value(emailUsers.size()))
                .andExpect(jsonPath("$.[*].password").value(not(hasItem(notNullValue()))));;
        for (String email : existingEmails) {
            result.andExpect(jsonPath("$.[*].login").value(hasItem(email)));
            result.andExpect(jsonPath("$.[*].email").value(hasItem(email)));
        }
    }

    @Test
    @Transactional
    public void testGetUsersByMobiles() throws Exception {
        // Create initial data
        createGuests(em, true);
        List<User> emailUsers = createUsers(em, true);
        List<User> mobileUsers = createMobileUsers(em, true);
        List<String> existingMobiles = getMobiles(mobileUsers);
        // Create input param
        List<String> mobiles = new ArrayList<>(existingMobiles);
        mobiles.add("+84:0987654321");
        mobiles.add("+84:0987654322");
        mobiles.add("+84:0987654323");

        // Run test
        ContactListVM contactListVM = new ContactListVM();
        contactListVM.setMobiles(mobiles);

        System.err.println(new String(TestUtil.convertObjectToJsonBytes(contactListVM)));
        ResultActions result = restUserMockMvc.perform(
                post("/api/users/contact?sort=id,asc")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(contactListVM))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print(System.err));
        // Check result
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.length()").value(emailUsers.size()))
                .andExpect(jsonPath("$.[*].password").value(not(hasItem(notNullValue()))));;
        for (String mobile : existingMobiles) {
            result.andExpect(jsonPath("$.[*].login").value(hasItem(mobile)));
            String phoneNumber = PhoneDtoUtil.stringToPhoneDTO(mobile).getPhoneNumber();
            result.andExpect(jsonPath("$.[*].mobile.phoneNumber").value(hasItem(phoneNumber)));
        }
    }

    @Test
    @Transactional
    public void testGetUsersByMobiles2() throws Exception {
        // Create initial data
        createGuests(em, true);
        List<User> emailUsers = createUsers(em, true);
        List<User> mobileUsers = createMobileUsers(em, true);
        List<String> existingMobiles = getRawMobiles(mobileUsers);
        // Create input param
        List<String> mobiles = new ArrayList<>(existingMobiles);
        mobiles.add("+840987654321");
        mobiles.add("+840987654322");
        mobiles.add("+840987654323");

        // Run test
        ContactListVM contactListVM = new ContactListVM();
        contactListVM.setMobiles(mobiles);

        System.err.println(new String(TestUtil.convertObjectToJsonBytes(contactListVM)));
        ResultActions result = restUserMockMvc.perform(
                post("/api/users/contact?sort=id,asc")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(contactListVM))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print(System.err));
        // Check result
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.length()").value(emailUsers.size()))
                .andExpect(jsonPath("$.[*].password").value(not(hasItem(notNullValue()))));;
        for (String mobile : getMobiles(mobileUsers)) {
            result.andExpect(jsonPath("$.[*].login").value(hasItem(mobile)));
            String phoneNumber = PhoneDtoUtil.stringToPhoneDTO(mobile).getPhoneNumber();
            result.andExpect(jsonPath("$.[*].mobile.phoneNumber").value(hasItem(phoneNumber)));
        }
    }

    @Test
    @Transactional
    public void testGetUsersByMobilesWithCountryCodeStartWith00() throws Exception {
        // Create initial data
        createGuests(em, true);
        List<User> emailUsers = createUsers(em, true);
        List<User> mobileUsers = createMobileUsers(em, true);
        List<String> existingMobiles = getRawMobilesWithCountryCodeStartWith00(mobileUsers);
        // Create input param
        List<String> mobiles = new ArrayList<>(existingMobiles);
        mobiles.add("00840987654321");
        mobiles.add("00840987654322");
        mobiles.add("00840987654323");

        // Run test
        ContactListVM contactListVM = new ContactListVM();
        contactListVM.setMobiles(mobiles);

        System.err.println(new String(TestUtil.convertObjectToJsonBytes(contactListVM)));
        ResultActions result = restUserMockMvc.perform(
                post("/api/users/contact?sort=id,asc")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(contactListVM))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print(System.err));
        // Check result
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.length()").value(emailUsers.size()))
                .andExpect(jsonPath("$.[*].password").value(not(hasItem(notNullValue()))));;
        for (String mobile : getMobiles(mobileUsers)) {
            result.andExpect(jsonPath("$.[*].login").value(hasItem(mobile)));
            String phoneNumber = PhoneDtoUtil.stringToPhoneDTO(mobile).getPhoneNumber();
            result.andExpect(jsonPath("$.[*].mobile.phoneNumber").value(hasItem(phoneNumber)));
        }
    }

    private List<String> getRawMobilesWithCountryCodeStartWith00(List<User> users) {
        List<String> existingMobiles = new ArrayList<>();
        for (String mobile : getRawMobiles(users)) {
            mobile = mobile.replace("+", "00");
            existingMobiles.add(mobile);
        }
        return existingMobiles;
    }

    @Test
    @Transactional
    public void testGetUsersByMobilesWithRawPhoneNumber() throws Exception {
        // Create initial data
        createGuests(em, true);
        List<User> emailUsers = createUsers(em, true);
        List<User> mobileUsers = createMobileUsers(em, true);
        List<String> existingMobiles = getRawMobilesWithRawPhoneNumber(mobileUsers);
        // Create input param
        List<String> mobiles = new ArrayList<>(existingMobiles);
        mobiles.add("00840987654321");
        mobiles.add("00840987654322");
        mobiles.add("00840987654323");

        // Run test
        ContactListVM contactListVM = new ContactListVM();
        contactListVM.setMobiles(mobiles);

        System.err.println(new String(TestUtil.convertObjectToJsonBytes(contactListVM)));
        ResultActions result = restUserMockMvc.perform(
                post("/api/users/contact?sort=id,asc")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(contactListVM))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print(System.err));
        // Check result
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.length()").value(emailUsers.size()))
                .andExpect(jsonPath("$.[*].password").value(not(hasItem(notNullValue()))));;
        for (String mobile : getMobiles(mobileUsers)) {
            result.andExpect(jsonPath("$.[*].login").value(hasItem(mobile)));
            String phoneNumber = PhoneDtoUtil.stringToPhoneDTO(mobile).getPhoneNumber();
            result.andExpect(jsonPath("$.[*].mobile.phoneNumber").value(hasItem(phoneNumber)));
        }
    }

    private List<String> getRawMobilesWithRawPhoneNumber(List<User> users) {
        List<String> existingMobiles = new ArrayList<>();
        String invalidChars = "(). -_";
        int i = 0;
        for (String mobile : getRawMobiles(users)) {
            mobile = mobile.replace("+", "00");
            mobile = mobile.substring(0, i + 2) + invalidChars + mobile.substring(i + 2);
            existingMobiles.add(mobile);
            i++;
        }
        return existingMobiles;
    }

    @Test
    @Transactional
    public void testGetUsersByContactEmpty() throws Exception {
        // Create initial data
        createUsers(em, true);
        createMobileUsers(em, true);

        // Run test
        ContactListVM contactListVM = new ContactListVM();
        contactListVM.setEmails(new ArrayList<>());
        contactListVM.setMobiles(new ArrayList<>());

        System.err.println(new String(TestUtil.convertObjectToJsonBytes(contactListVM)));
        ResultActions result = restUserMockMvc.perform(
                post("/api/users/contact?sort=id,asc")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(contactListVM))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print(System.err));
        // Check result
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.length()").value(0))
                .andExpect(jsonPath("$.[*].password").value(not(hasItem(notNullValue()))));;
    }

    @Test
    @Transactional
    public void testGetUsersByContactNull() throws Exception {
        // Create initial data
        createUsers(em, true);
        createMobileUsers(em, true);

        // Run test
        ContactListVM contactListVM = new ContactListVM();

        ResultActions result = restUserMockMvc.perform(
                post("/api/users/contact?sort=id,asc")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(contactListVM))
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print(System.err));
        // Check result
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.length()").value(0))
                .andExpect(jsonPath("$.[*].password").value(not(hasItem(notNullValue()))));;
    }

    @Test
    @Transactional
    public void testGetExistingUser() throws Exception {
        restUserMockMvc.perform(get("/api/users/{login}", DEFAULT_LOGIN)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.id").value(user.getId().intValue()))
                .andExpect(jsonPath("$.login").value(DEFAULT_LOGIN))
                .andExpect(jsonPath("$.password").doesNotExist()) // Password must not return
                .andExpect(jsonPath("$.email").value(DEFAULT_EMAIL))
                .andExpect(jsonPath("$.firstName").value(DEFAULT_FIRSTNAME))
                .andExpect(jsonPath("$.lastName").value(DEFAULT_LASTNAME))
                .andExpect(jsonPath("$.displayName").value(DEFAULT_DISPLAYNAME));
    }

    @Test
    @Transactional
    public void testGetUnknownUser() throws Exception {
        restUserMockMvc.perform(get("/api/users/unknown")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void testGetExistingUserWithAnEmailLogin() throws Exception {
        User user = createUser(em, "john.doe@localhost.com", ENCRYPTED_DEFAUT_PASSWORD, "john.doe@localhost", "John", "Doe", AuthoritiesConstants.USER, true);

        restUserMockMvc.perform(get("/api/users/john.doe@localhost.com")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.login").value("john.doe@localhost.com"))
                .andExpect(jsonPath("$.[*].password").value(not(hasItem(notNullValue()))));;

        userRepository.delete(user);
    }

    @Test
    @Transactional
    public void testDeleteExistingUserWithAnEmailLogin() throws Exception {
        User user = createUser(em, "john.doe@localhost.com", ENCRYPTED_DEFAUT_PASSWORD, "john.doe@localhost", "John", "Doe", AuthoritiesConstants.USER, true);

        restUserMockMvc.perform(delete("/api/users/john.doe@localhost.com")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        assertThat(userRepository.findOneByLogin("john.doe@localhost.com").isPresent()).isFalse();

        userRepository.delete(user);
    }
}
