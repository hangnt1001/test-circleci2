/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import com.mediastep.beecow.common.config.UserServiceConfig;
import com.mediastep.beecow.common.domain.enumeration.AccountType;
import com.mediastep.beecow.common.dto.PhoneDTO;
import com.mediastep.beecow.common.errors.CommonExceptionTranslator;
import com.mediastep.beecow.common.security.AuthoritiesConstants;
import com.mediastep.beecow.common.util.PhoneDtoUtil;
import com.mediastep.beecow.gateway.BeecowGatewayApp;
import com.mediastep.beecow.gateway.client.BcUserSettingValueServiceClient;
import com.mediastep.beecow.gateway.domain.Authority;
import com.mediastep.beecow.gateway.domain.User;
import com.mediastep.beecow.gateway.repository.UserRepository;
import com.mediastep.beecow.gateway.security.jwt.TokenProviderService;
import com.mediastep.beecow.gateway.service.MailService;
import com.mediastep.beecow.gateway.service.SMSService;
import com.mediastep.beecow.gateway.service.UserService;
import com.mediastep.beecow.gateway.service.mapper.UserMapper;
import com.mediastep.beecow.gateway.web.rest.errors.ExceptionTranslator;
import com.mediastep.beecow.gateway.web.rest.vm.EmailVM;
import com.mediastep.beecow.gateway.web.rest.vm.KeyAndPasswordVM;
import com.mediastep.beecow.gateway.web.rest.vm.LoginVM;

/**
 * Test class for the AccountResource REST controller.
 *
 * @see UserService
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = BeecowGatewayApp.class)
public class AccountResourceResetPasswordIntTest {

    private static final String DEFAULT_EMAIL = "john.doe@jhipter.com";

    private static final String DEFAULT_PHONE_NUMBER = "+84:0123456789";
    private static final String DEFAULT_PHONE_NUMBER_WITHOUT_ZERO = "+84:123456789";

    private static final String DEFAULT_PASSWORD = "123456";

    private static final String UPDATED_PASSWORD = "654321";

    @Inject
    private AuthenticationManager authenticationManager;

    @Inject
    private UserServiceConfig userServiceConfig;

    @Inject
    private UserService userService;

    @Inject
    private UserMapper userMapper;

    @Inject
    private TokenProviderService tokenProviderService;

    @Mock
    private BcUserSettingValueServiceClient userSettingValueServiceClient;

    @Inject
    private PasswordEncoder passwordEncoder;

    @Inject
    private UserRepository userRepository;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Mock
    private UserService mockUserService;

    @Mock
    private MailService mockMailService;

    @Mock
    private SMSService mockSMSService;

    private MockMvc jwtRestMvc;

    private MockMvc restMvc;

    private User user;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        doNothing().when(mockMailService).sendActivationEmail((User) anyObject());
        doNothing().when(mockSMSService).sendActivationMessage((User) anyObject());
        AccountResourceIntTest.mockUserSettingValueServiceClient(userSettingValueServiceClient);

        UserJWTController userJWTController = new UserJWTController();
        ReflectionTestUtils.setField(userJWTController, "authenticationManager", authenticationManager);
        ReflectionTestUtils.setField(userJWTController, "userServiceConfig", userServiceConfig);
        ReflectionTestUtils.setField(userJWTController, "userService", userService);
        ReflectionTestUtils.setField(userJWTController, "userMapper", userMapper);
        ReflectionTestUtils.setField(userJWTController, "tokenProviderService", tokenProviderService);
        ReflectionTestUtils.setField(userJWTController, "userSettingValueServiceClient", userSettingValueServiceClient);

        this.jwtRestMvc = MockMvcBuilders.standaloneSetup(userJWTController)
            .setControllerAdvice(new ExceptionTranslator(), new CommonExceptionTranslator()).build();

        AccountResource accountResource = new AccountResource();
        ReflectionTestUtils.setField(accountResource, "userService", userService);
        ReflectionTestUtils.setField(accountResource, "userMapper", userMapper);
        ReflectionTestUtils.setField(accountResource, "mailService", mockMailService);
        ReflectionTestUtils.setField(accountResource, "smsService", mockSMSService);
        ReflectionTestUtils.setField(accountResource, "tokenProviderService", tokenProviderService);
        ReflectionTestUtils.setField(accountResource, "userSettingValueServiceClient", userSettingValueServiceClient);

        this.restMvc = MockMvcBuilders.standaloneSetup(accountResource)
            .setControllerAdvice(new ExceptionTranslator(), new CommonExceptionTranslator())
            .setMessageConverters(jacksonMessageConverter).build();
    }

    @Before
    public void initTest() {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.USER);
        authorities.add(authority);

        user = new User();
        user.setLogin(DEFAULT_EMAIL);
        String encoderPassword = passwordEncoder.encode(DEFAULT_PASSWORD);
        user.setPassword(encoderPassword);
        user.setAccountType(AccountType.EMAIL);
        user.setFirstName("john");
        user.setLastName("deo");
        user.setDisplayName("john doe");
        user.setEmail(DEFAULT_EMAIL);
        user.setMobile(DEFAULT_PHONE_NUMBER);
        user.setLangKey("en");
        user.setAuthorities(authorities);
        user.setActivated(true);
        userRepository.saveAndFlush(user);
    }

    private void testLogin(String login, String password) throws Exception {
        // Try to login
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername(login);
        loginVM.setPassword(password);
        jwtRestMvc.perform(post("/api/authenticate")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(loginVM)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isEmpty());
    }

    @Test
    @Transactional
    public void testGetExistingAccountByEmail() throws Exception {
        // Request reset password
        EmailVM emailVM = new EmailVM();
        emailVM.setEmail(user.getEmail());

        restMvc.perform(
            post("/api/account/reset_password/email/init")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(emailVM)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk());

        // Check if old password still usable
        String login = user.getLogin();
        testLogin(login, DEFAULT_PASSWORD);

        // Change password
        Optional<User> userOptional = userRepository.findOneByLogin(login);
        assertThat(userOptional.isPresent()).isTrue();
        User testUser = userOptional.get();

        KeyAndPasswordVM keyAndPasswordVM = new KeyAndPasswordVM();
        keyAndPasswordVM.setKey(testUser.getResetKey());
        keyAndPasswordVM.setNewPassword(UPDATED_PASSWORD);

        restMvc.perform(
            post("/api/account/reset_password/finish")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(keyAndPasswordVM)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value(login))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // Check if old password still usable
        testLogin(DEFAULT_EMAIL, UPDATED_PASSWORD);
    }

    @Test
    @Transactional
    public void testGetExistingAccountByEmailNotFound() throws Exception {
        // Request reset password
        EmailVM emailVM = new EmailVM();
        emailVM.setEmail("notfound@email.com");

        restMvc.perform(
            post("/api/account/reset_password/email/init")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(emailVM)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void testGetExistingAccountByEmailInvalidInit() throws Exception {
        // Request reset password
        EmailVM emailVM = new EmailVM();
        emailVM.setEmail("invalid");

        restMvc.perform(
            post("/api/account/reset_password/email/init")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(emailVM)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void testGetExistingAccountByEmailInvalid() throws Exception {
        // Request reset password
        EmailVM emailVM = new EmailVM();
        emailVM.setEmail("invalid");

        restMvc.perform(
            post("/api/account/reset_password/email/init")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(emailVM)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());

        // Check if old password still usable
        String login = user.getLogin();
        testLogin(login, DEFAULT_PASSWORD);
    }

    @Test
    @Transactional
    public void testGetExistingAccountByEmailNull() throws Exception {
        // Request reset password
        EmailVM emailVM = new EmailVM();
        emailVM.setEmail(null);

        restMvc.perform(
            post("/api/account/reset_password/email/init")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(emailVM)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());

        // Check if old password still usable
        String login = user.getLogin();
        testLogin(login, DEFAULT_PASSWORD);
    }

    @Test
    @Transactional
    public void testGetExistingAccountByEmailResetKeyNotFound() throws Exception {
        // Initial data
        String key = "123456";
        user.setResetKey(key);
        user.setResetDate(ZonedDateTime.now().minusMonths(10));
        userRepository.saveAndFlush(user);

        // Set new password
        KeyAndPasswordVM keyAndPasswordVM = new KeyAndPasswordVM();
        keyAndPasswordVM.setKey("notfound");
        keyAndPasswordVM.setNewPassword(UPDATED_PASSWORD);

        restMvc.perform(
            post("/api/account/reset_password/finish")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(keyAndPasswordVM)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isNotFound());

        // Check if old password still usable
        testLogin(user.getLogin(), DEFAULT_PASSWORD);
    }

    @Test
    @Transactional
    public void testGetExistingAccountByEmailResetKeyExpired() throws Exception {
        // Initial data
        String key = "123456";
        user.setResetKey(key);
        user.setResetDate(ZonedDateTime.now().minusMonths(10));
        userRepository.saveAndFlush(user);

        // Set new password
        KeyAndPasswordVM keyAndPasswordVM = new KeyAndPasswordVM();
        keyAndPasswordVM.setKey(key);
        keyAndPasswordVM.setNewPassword(UPDATED_PASSWORD);

        restMvc.perform(
            post("/api/account/reset_password/finish")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(keyAndPasswordVM)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isGone());

        // Check if old password still usable
        testLogin(user.getLogin(), DEFAULT_PASSWORD);
    }

    @Test
    @Transactional
    public void testGetExistingAccountByEmailResetKeyNull() throws Exception {
        // Initial data
        String key = "123456";
        user.setResetKey(key);
        user.setResetDate(ZonedDateTime.now().minusMonths(10));
        userRepository.saveAndFlush(user);

        // Set new password
        KeyAndPasswordVM keyAndPasswordVM = new KeyAndPasswordVM();
        keyAndPasswordVM.setKey(null);
        keyAndPasswordVM.setNewPassword(UPDATED_PASSWORD);

        restMvc.perform(
            post("/api/account/reset_password/finish")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(keyAndPasswordVM)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());

        // Check if old password still usable
        testLogin(user.getLogin(), DEFAULT_PASSWORD);
    }

    @Test
    @Transactional
    public void testGetExistingAccountByEmailNewPasswordInvalid() throws Exception {
        // Initial data
        String key = "123456";
        user.setResetKey(key);
        user.setResetDate(ZonedDateTime.now().minusMonths(10));
        userRepository.saveAndFlush(user);

        // Set new password
        KeyAndPasswordVM keyAndPasswordVM = new KeyAndPasswordVM();
        keyAndPasswordVM.setKey(key);
        keyAndPasswordVM.setNewPassword("123"); // too short

        restMvc.perform(
            post("/api/account/reset_password/finish")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(keyAndPasswordVM)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());

        // Check if old password still usable
        testLogin(user.getLogin(), DEFAULT_PASSWORD);
    }

    @Test
    @Transactional
    public void testGetExistingAccountByEmailNewPasswordEmpty() throws Exception {
        // Initial data
        String key = "123456";
        user.setResetKey(key);
        user.setResetDate(ZonedDateTime.now().minusMonths(10));
        userRepository.saveAndFlush(user);

        // Set new password
        KeyAndPasswordVM keyAndPasswordVM = new KeyAndPasswordVM();
        keyAndPasswordVM.setKey(key);
        keyAndPasswordVM.setNewPassword("");

        restMvc.perform(
            post("/api/account/reset_password/finish")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(keyAndPasswordVM)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());

        // Check if old password still usable
        testLogin(user.getLogin(), DEFAULT_PASSWORD);
    }

    @Test
    @Transactional
    public void testGetExistingAccountByEmailNewPasswordNull() throws Exception {
        // Initial data
        String key = "123456";
        user.setResetKey(key);
        user.setResetDate(ZonedDateTime.now().minusMonths(10));
        userRepository.saveAndFlush(user);

        // Set new password
        KeyAndPasswordVM keyAndPasswordVM = new KeyAndPasswordVM();
        keyAndPasswordVM.setKey(key);
        keyAndPasswordVM.setNewPassword(null);

        restMvc.perform(
            post("/api/account/reset_password/finish")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(keyAndPasswordVM)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());

        // Check if old password still usable
        testLogin(user.getLogin(), DEFAULT_PASSWORD);
    }

    @Test
    @Transactional
    public void testGetExistingAccountByMobile() throws Exception {
    	user.setAccountType(AccountType.MOBILE);
    	userRepository.saveAndFlush(user);
        // Request reset password
        PhoneDTO mobile = PhoneDtoUtil.stringToPhoneDTO(DEFAULT_PHONE_NUMBER);

        restMvc.perform(
            post("/api/account/reset_password/mobile/init")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(mobile)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk());

        // Check if old password still usable
        String login = user.getLogin();
        testLogin(login, DEFAULT_PASSWORD);

        // Change password
        Optional<User> userOptional = userRepository.findOneByLogin(login);
        assertThat(userOptional.isPresent()).isTrue();
        User testUser = userOptional.get();

        KeyAndPasswordVM keyAndPasswordVM = new KeyAndPasswordVM();
        keyAndPasswordVM.setKey(testUser.getResetKey());
        keyAndPasswordVM.setNewPassword(UPDATED_PASSWORD);

        restMvc.perform(
            post("/api/account/reset_password/finish")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(keyAndPasswordVM)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value(login))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // Check if old password still usable
        testLogin(DEFAULT_EMAIL, UPDATED_PASSWORD);
    }

    @Test
    @Transactional
    public void testGetExistingAccountByMobile2() throws Exception {
    	user.setAccountType(AccountType.MOBILE);
    	userRepository.saveAndFlush(user);
        // Request reset password
        PhoneDTO mobile = PhoneDtoUtil.stringToPhoneDTO(DEFAULT_PHONE_NUMBER_WITHOUT_ZERO);

        restMvc.perform(
            post("/api/account/reset_password/mobile/init")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(mobile)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk());

        // Check if old password still usable
        String login = user.getLogin();
        testLogin(login, DEFAULT_PASSWORD);

        // Change password
        Optional<User> userOptional = userRepository.findOneByLogin(login);
        assertThat(userOptional.isPresent()).isTrue();
        User testUser = userOptional.get();

        KeyAndPasswordVM keyAndPasswordVM = new KeyAndPasswordVM();
        keyAndPasswordVM.setKey(testUser.getResetKey());
        keyAndPasswordVM.setNewPassword(UPDATED_PASSWORD);

        restMvc.perform(
            post("/api/account/reset_password/finish")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(keyAndPasswordVM)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value(login))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // Check if old password still usable
        testLogin(DEFAULT_EMAIL, UPDATED_PASSWORD);
    }

    @Test
    @Transactional
    public void testGetExistingAccountByMobileNotFound() throws Exception {
        // Request reset password
        PhoneDTO mobile = PhoneDtoUtil.stringToPhoneDTO("+84:0987654321");

        restMvc.perform(
            post("/api/account/reset_password/mobile/init")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(mobile)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void testGetExistingAccountByMobileCountryCodeInvalid() throws Exception {
        // Request reset password
        PhoneDTO mobile = PhoneDtoUtil.stringToPhoneDTO(DEFAULT_PHONE_NUMBER);
        mobile.setCountryCode("invalid");

        restMvc.perform(
            post("/api/account/reset_password/mobile/init")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(mobile)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void testGetExistingAccountByMobilePhoneNumberInvalid() throws Exception {
        // Request reset password
        PhoneDTO mobile = PhoneDtoUtil.stringToPhoneDTO(DEFAULT_PHONE_NUMBER);
        mobile.setPhoneNumber("invalid");

        restMvc.perform(
            post("/api/account/reset_password/mobile/init")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(mobile)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());
    }
}
