/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.web.rest;

import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.BEECOW_USER_ID;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.BEECOW_USER_ID2;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.BEECOW_USER_ID3;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.BEECOW_USER_LOGIN;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.BEECOW_USER_LOGIN2;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.BEECOW_USER_LOGIN3;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.DEFAULT_DISPLAYNAME;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.DEFAULT_EMAIL;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.DEFAULT_FIRSTNAME;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.DEFAULT_LANG;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.DEFAULT_LANG2;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.DEFAULT_LASTNAME;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.DEFAULT_LOCATION;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.DEFAULT_LOCATION2;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.DEFAULT_LOGIN;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.DEFAULT_PASSWORD;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.createUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import com.mediastep.beecow.common.config.UserServiceConfig;
import com.mediastep.beecow.common.dto.PhoneDTO;
import com.mediastep.beecow.common.dto.UserStatus;
import com.mediastep.beecow.common.security.AuthoritiesConstants;
import com.mediastep.beecow.gateway.BeecowGatewayApp;
import com.mediastep.beecow.gateway.client.BcUserSettingValueServiceClient;
import com.mediastep.beecow.gateway.client.JobServiceClient;
import com.mediastep.beecow.gateway.client.UserStoreServiceClient;
import com.mediastep.beecow.gateway.domain.User;
import com.mediastep.beecow.gateway.security.jwt.TokenProviderService;
import com.mediastep.beecow.gateway.service.UserService;
import com.mediastep.beecow.gateway.service.mapper.UserMapper;
import com.mediastep.beecow.gateway.web.rest.vm.LoginVM;
import com.mediastep.beecow.gateway.web.rest.vm.PhoneLoginVM;
import com.mediastep.beecow.store.client.StoreServiceClient;

/**
 * Test class for the AccountResource REST controller.
 *
 * @see UserService
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = BeecowGatewayApp.class)
public class UserJWTControllerTest {

    private static final String DEFAULT_ACTIVATION_KEY = "123456";

	@Inject
    private EntityManager em;

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

    @Mock
    private StoreServiceClient storeServiceClient;

    @Mock
    private JobServiceClient jobServiceClient;

    @Mock
    private UserStoreServiceClient userStoreServiceClient;

    @Inject
    private PasswordEncoder passwordEncoder;

    private MockMvc restMvc;

    private User user;

    /**
     * Create a User.
     */
    public User createExistingUser(EntityManager em, String login, String email, String location, String language) {
        return createUser(em, login, passwordEncoder.encode(DEFAULT_PASSWORD), email,
            DEFAULT_FIRSTNAME, DEFAULT_LASTNAME,
            location, language, AuthoritiesConstants.USER, true);
    }

    private void assertAuthUserDTO(ResultActions result, Long expId, String expLogin, boolean hasPassword, boolean hasToken, boolean hasRefreshToken) throws Exception {
        result.andExpect(jsonPath("$.id").value(expId.intValue()))
            .andExpect(jsonPath("$.login").value(expLogin));

        assertHasProperty(result, "$.password", hasPassword);
        assertHasProperty(result, "$.accessToken", hasToken);
        assertHasProperty(result, "$.refreshToken", hasRefreshToken);
    }

    private void assertAuthUserDTO(ResultActions result, Long expId, String expLogin, boolean hasPassword, String expEmail,
        String expFirstName, String expLastName, String expDisplayName, boolean hasToken, boolean hasRefreshToken) throws Exception {
        result.andExpect(jsonPath("$.id").value(expId.intValue()))
            .andExpect(jsonPath("$.login").value(expLogin))
            .andExpect(jsonPath("$.email").value(expEmail))
            .andExpect(jsonPath("$.firstName").value(expFirstName))
            .andExpect(jsonPath("$.lastName").value(expLastName))
            .andExpect(jsonPath("$.displayName").value(expDisplayName));

        assertHasProperty(result, "$.password", hasPassword);
        assertHasProperty(result, "$.accessToken", hasToken);
        assertHasProperty(result, "$.refreshToken", hasRefreshToken);
    }

    private void assertHasProperty(ResultActions result, String propJsonPath, boolean hasProperty) throws Exception {
        if (hasProperty) {
            result.andExpect(jsonPath(propJsonPath).isNotEmpty());
        }
        else {
            result.andExpect(jsonPath(propJsonPath).doesNotExist());
        }
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        AccountResourceIntTest.mockUserSettingValueServiceClient(userSettingValueServiceClient);
        AccountResourceIntTest.mockUserStoreServiceClient(userStoreServiceClient);
        AccountResourceIntTest.mockStoreServiceClient(storeServiceClient);
        ReflectionTestUtils.setField(tokenProviderService, "storeServiceClient", storeServiceClient);
        AccountResourceIntTest.mockJobServiceClient(jobServiceClient);
        ReflectionTestUtils.setField(tokenProviderService, "jobServiceClient", jobServiceClient);

        UserJWTController userJWTController = new UserJWTController();
        ReflectionTestUtils.setField(userJWTController, "authenticationManager", authenticationManager);
        ReflectionTestUtils.setField(userJWTController, "userServiceConfig", userServiceConfig);
        ReflectionTestUtils.setField(userJWTController, "userService", userService);
        ReflectionTestUtils.setField(userJWTController, "userMapper", userMapper);
        ReflectionTestUtils.setField(userJWTController, "tokenProviderService", tokenProviderService);
        ReflectionTestUtils.setField(userJWTController, "userSettingValueServiceClient", userSettingValueServiceClient);
        ReflectionTestUtils.setField(userJWTController, "userStoreServiceClient", userStoreServiceClient);

        this.restMvc = MockMvcBuilders.standaloneSetup(userJWTController).build();
    }

    @Before
    public void init() {
        user = createExistingUser(em, DEFAULT_LOGIN, DEFAULT_EMAIL,
            DEFAULT_LOCATION, DEFAULT_LANG);
    }

    @Test
    @Transactional
    public void testAuthenticate() throws Exception {
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername(DEFAULT_LOGIN);
        loginVM.setPassword(DEFAULT_PASSWORD);
        restMvc.perform(post("/api/authenticate")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isEmpty());
    }

    @Test
    @Transactional
    public void testAuthenticateRegistered() throws Exception {
    	user.setActivated(false);
    	user.setActivationKey(DEFAULT_ACTIVATION_KEY);
    	em.persist(user);
    	em.flush();
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername(DEFAULT_LOGIN);
        loginVM.setPassword(DEFAULT_PASSWORD);
        restMvc.perform(post("/api/authenticate")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.status").value(UserStatus.REGISTERED.toString()));
    }

    @Test
    @Transactional
    public void testAuthenticateLocked() throws Exception {
    	user.setActivated(false);
    	em.persist(user);
    	em.flush();
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername(DEFAULT_LOGIN);
        loginVM.setPassword(DEFAULT_PASSWORD);
        restMvc.perform(post("/api/authenticate")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.status").value(UserStatus.LOCKED.toString()));
    }

    @Test
    @Transactional
    public void testAuthenticateUserNotExist() throws Exception {
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername("nouser");
        loginVM.setPassword(DEFAULT_PASSWORD);
        restMvc.perform(post("/api/authenticate")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void testAuthenticateUsernameBlank() throws Exception {
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername("    ");
        loginVM.setPassword(DEFAULT_PASSWORD);
        restMvc.perform(post("/api/authenticate")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void testAuthenticateUsernameEmpty() throws Exception {
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername("");
        loginVM.setPassword(DEFAULT_PASSWORD);
        restMvc.perform(post("/api/authenticate")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void testAuthenticateUsernameNull() throws Exception {
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername(null);
        loginVM.setPassword(DEFAULT_PASSWORD);
        restMvc.perform(post("/api/authenticate")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void testAuthenticateWrongPassword() throws Exception {
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername(DEFAULT_LOGIN);
        loginVM.setPassword("wrong-password");
        restMvc.perform(post("/api/authenticate")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    public void testAuthenticatePasswordBlank() throws Exception {
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername(DEFAULT_LOGIN);
        loginVM.setPassword("    ");
        restMvc.perform(post("/api/authenticate")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    public void testAuthenticatePasswordEmpty() throws Exception {
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername(DEFAULT_LOGIN);
        loginVM.setPassword("");
        restMvc.perform(post("/api/authenticate")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void testAuthenticatePasswordNull() throws Exception {
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername(DEFAULT_LOGIN);
        loginVM.setPassword(null);
        restMvc.perform(post("/api/authenticate")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void testAuthenticateAdmin() throws Exception {
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername("admin");
        loginVM.setPassword("admin");
        restMvc.perform(post("/api/authenticate/admin")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isEmpty());
    }

    @Test
    @Transactional
    public void testAuthenticateAdminFaile() throws Exception {
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername(DEFAULT_LOGIN);
        loginVM.setPassword(DEFAULT_PASSWORD);
        restMvc.perform(post("/api/authenticate/admin")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    public void testAuthenticateMobile() throws Exception {
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername(DEFAULT_LOGIN);
        loginVM.setPassword(DEFAULT_PASSWORD);
        ResultActions result = restMvc.perform(post("/api/authenticate/mobile")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        assertAuthUserDTO(result, user.getId(), DEFAULT_LOGIN, false, DEFAULT_EMAIL,
                DEFAULT_FIRSTNAME, DEFAULT_LASTNAME, DEFAULT_DISPLAYNAME, true, false);
    }

    @Test
    @Transactional
    public void testAuthenticateMobileRegistered() throws Exception {
    	user.setActivated(false);
    	user.setActivationKey(DEFAULT_ACTIVATION_KEY);
    	em.persist(user);
    	em.flush();
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername(DEFAULT_LOGIN);
        loginVM.setPassword(DEFAULT_PASSWORD);
        restMvc.perform(post("/api/authenticate/mobile")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.status").value(UserStatus.REGISTERED.toString()));
    }

    @Test
    @Transactional
    public void testAuthenticateMobileLocked() throws Exception {
    	user.setActivated(false);
    	em.persist(user);
    	em.flush();
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername(DEFAULT_LOGIN);
        loginVM.setPassword(DEFAULT_PASSWORD);
        restMvc.perform(post("/api/authenticate/mobile")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.status").value(UserStatus.LOCKED.toString()));
    }

    @Test
    @Transactional
    public void testAuthenticateMobileUserNotExist() throws Exception {
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername("nouser");
        loginVM.setPassword(DEFAULT_PASSWORD);
        restMvc.perform(post("/api/authenticate/mobile")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void testAuthenticateMobileUsernameBlank() throws Exception {
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername("    ");
        loginVM.setPassword(DEFAULT_PASSWORD);
        restMvc.perform(post("/api/authenticate/mobile")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void testAuthenticateMobileUsernameEmpty() throws Exception {
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername("");
        loginVM.setPassword(DEFAULT_PASSWORD);
        restMvc.perform(post("/api/authenticate/mobile")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void testAuthenticateMobileUsernameNull() throws Exception {
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername(null);
        loginVM.setPassword(DEFAULT_PASSWORD);
        restMvc.perform(post("/api/authenticate/mobile")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void testAuthenticateMobileWrongPassword() throws Exception {
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername(DEFAULT_LOGIN);
        loginVM.setPassword("wrong-password");
        restMvc.perform(post("/api/authenticate/mobile")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    public void testAuthenticateMobilePasswordBlank() throws Exception {
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername(DEFAULT_LOGIN);
        loginVM.setPassword("    ");
        restMvc.perform(post("/api/authenticate/mobile")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    public void testAuthenticateMobilePasswordEmpty() throws Exception {
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername(DEFAULT_LOGIN);
        loginVM.setPassword("");
        restMvc.perform(post("/api/authenticate/mobile")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void testAuthenticateMobilePasswordNull() throws Exception {
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername(DEFAULT_LOGIN);
        loginVM.setPassword(null);
        restMvc.perform(post("/api/authenticate/mobile")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void testAuthenticateMobileWithPhoneNumber() throws Exception {
        // Initialize data
        PhoneDTO phoneDTO = new PhoneDTO();
        phoneDTO.setCountryCode("+84");
        phoneDTO.setPhoneNumber("0123456789");
        user = createExistingUser(em, phoneDTO.toStringFromPhoneWithZero(), null, DEFAULT_LOCATION, DEFAULT_LANG);

        // Login
        PhoneLoginVM loginVM = new PhoneLoginVM();
        loginVM.setMobile(phoneDTO);
        loginVM.setPassword(DEFAULT_PASSWORD);

        restMvc.perform(post("/api/authenticate/mobile/phone")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isEmpty());
    }

    @Test
    @Transactional
    public void testAuthenticateMobileWithPhoneNumber2() throws Exception {
        // Initialize data
        PhoneDTO phoneDTO = new PhoneDTO();
        phoneDTO.setCountryCode("+84");
        phoneDTO.setPhoneNumber("0123456789");
        user = createExistingUser(em, phoneDTO.toStringFromPhoneWithoutZero(), null, DEFAULT_LOCATION, DEFAULT_LANG);

        // Login
        PhoneLoginVM loginVM = new PhoneLoginVM();
        loginVM.setMobile(phoneDTO);
        loginVM.setPassword(DEFAULT_PASSWORD);

        restMvc.perform(post("/api/authenticate/mobile/phone")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isEmpty());
    }

    @Test
    @Transactional
    public void testAuthenticateMobileWithPhoneNumberRegistered() throws Exception {
        // Initialize data
        PhoneDTO phoneDTO = new PhoneDTO();
        phoneDTO.setCountryCode("+84");
        phoneDTO.setPhoneNumber("0123456789");
        user = createExistingUser(em, phoneDTO.toStringFromPhoneWithZero(), null, DEFAULT_LOCATION, DEFAULT_LANG);
    	user.setActivated(false);
    	user.setActivationKey(DEFAULT_ACTIVATION_KEY);
    	em.persist(user);
    	em.flush();

        // Login
        PhoneLoginVM loginVM = new PhoneLoginVM();
        loginVM.setMobile(phoneDTO);
        loginVM.setPassword(DEFAULT_PASSWORD);

        restMvc.perform(post("/api/authenticate/mobile/phone")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.status").value(UserStatus.REGISTERED.toString()));
    }

    @Test
    @Transactional
    public void testAuthenticateMobileWithPhoneNumberLocked() throws Exception {
        // Initialize data
        PhoneDTO phoneDTO = new PhoneDTO();
        phoneDTO.setCountryCode("+84");
        phoneDTO.setPhoneNumber("0123456789");
        user = createExistingUser(em, phoneDTO.toStringFromPhoneWithZero(), null, DEFAULT_LOCATION, DEFAULT_LANG);
    	user.setActivated(false);
    	em.persist(user);
    	em.flush();

        // Login
        PhoneLoginVM loginVM = new PhoneLoginVM();
        loginVM.setMobile(phoneDTO);
        loginVM.setPassword(DEFAULT_PASSWORD);

        restMvc.perform(post("/api/authenticate/mobile/phone")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.status").value(UserStatus.LOCKED.toString()));
    }

    @Test
    @Transactional
    public void testAuthenticateMobileWithPhoneNumberNotFound() throws Exception {
        // Initialize data
        PhoneDTO phoneDTO = new PhoneDTO();
        phoneDTO.setCountryCode("+84");
        phoneDTO.setPhoneNumber("0123456789");

        // Login
        PhoneLoginVM loginVM = new PhoneLoginVM();
        loginVM.setMobile(phoneDTO);
        loginVM.setPassword(DEFAULT_PASSWORD);

        restMvc.perform(post("/api/authenticate/mobile/phone")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void testAuthenticateMobileWithPhoneNumberNull() throws Exception {
        // Initialize data
        PhoneDTO phoneDTO = new PhoneDTO();
        phoneDTO.setCountryCode("+84");
        phoneDTO.setPhoneNumber("0123456789");
        user = createExistingUser(em, phoneDTO.toStringFromPhoneWithZero(), null, DEFAULT_LOCATION, DEFAULT_LANG);

        // Login
        PhoneLoginVM loginVM = new PhoneLoginVM();
        loginVM.setMobile(null);
        loginVM.setPassword(DEFAULT_PASSWORD);

        restMvc.perform(post("/api/authenticate/mobile/phone")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void testAuthenticateMobileWithCountryCodeInvalid() throws Exception {
        // Initialize data
        PhoneDTO phoneDTO = new PhoneDTO();
        phoneDTO.setCountryCode("invalid");
        phoneDTO.setPhoneNumber("0123456789");
        user = createExistingUser(em, phoneDTO.toStringFromPhoneWithZero(), null, DEFAULT_LOCATION, DEFAULT_LANG);

        // Login
        PhoneLoginVM loginVM = new PhoneLoginVM();
        loginVM.setMobile(null);
        loginVM.setPassword(DEFAULT_PASSWORD);

        restMvc.perform(post("/api/authenticate/mobile/phone")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void testAuthenticateMobileWithPhoneNumberInvalid() throws Exception {
        // Initialize data
        PhoneDTO phoneDTO = new PhoneDTO();
        phoneDTO.setCountryCode("+84");
        phoneDTO.setPhoneNumber("invalid");
        user = createExistingUser(em, phoneDTO.toStringFromPhoneWithZero(), null, DEFAULT_LOCATION, DEFAULT_LANG);

        // Login
        PhoneLoginVM loginVM = new PhoneLoginVM();
        loginVM.setMobile(null);
        loginVM.setPassword(DEFAULT_PASSWORD);

        restMvc.perform(post("/api/authenticate/mobile/phone")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(loginVM)))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void testLogout() throws Exception {
        String token = tokenProviderService.createToken(user, false);

        ResultActions result = restMvc.perform(get("/api/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        assertAuthUserDTO(result, BEECOW_USER_ID2, BEECOW_USER_LOGIN2, false, true, false);
    }

    @Test
    @Transactional
    public void testLogoutSpecialBeecowUser() throws Exception {
        user = createExistingUser(em, DEFAULT_LOGIN + 1, DEFAULT_EMAIL + 1,
                DEFAULT_LOCATION, DEFAULT_LANG2);
        String token = tokenProviderService.createToken(user, false);

        ResultActions result = restMvc.perform(get("/api/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        assertAuthUserDTO(result, BEECOW_USER_ID3, BEECOW_USER_LOGIN3, false, true, false);
    }

    @Test
    @Transactional
    public void testLogoutToGlobalBeecowUser() throws Exception {
        user = createExistingUser(em, DEFAULT_LOGIN + 1, DEFAULT_EMAIL + 1,
                DEFAULT_LOCATION2, DEFAULT_LANG);
        String token = tokenProviderService.createToken(user, false);

        ResultActions result = restMvc.perform(get("/api/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        assertAuthUserDTO(result, BEECOW_USER_ID, BEECOW_USER_LOGIN, false, true, false);
    }

    @Test
    @Transactional
    public void testLogoutWithoutToken() throws Exception {
        ResultActions result = restMvc.perform(get("/api/logout")
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        assertAuthUserDTO(result, BEECOW_USER_ID, BEECOW_USER_LOGIN, false, true, false);
    }

    @Test
    @Transactional
    public void testLogoutWithInvalidToken() throws Exception {
        String token = "INVALID_TOKEN";
        ResultActions result = restMvc.perform(get("/api/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        assertAuthUserDTO(result, BEECOW_USER_ID, BEECOW_USER_LOGIN, false, true, false);
    }

    @Test
    @Transactional
    public void testAuthorizeSwitchToStoreByURL() throws Exception {
        user = createExistingUser(em, DEFAULT_LOGIN + 1, DEFAULT_EMAIL + 1, DEFAULT_LOCATION2, DEFAULT_LANG);
        String token = tokenProviderService.createToken(user, false);
        restMvc.perform(get("/api/authenticate/store/{url}", "testurl")
        		.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isEmpty());
    }

    @Test
    @Transactional
    public void testAuthorizeSwitchToStoreByURLNotFound() throws Exception {
        user = createExistingUser(em, DEFAULT_LOGIN + 1, DEFAULT_EMAIL + 1, DEFAULT_LOCATION2, DEFAULT_LANG);
        String token = tokenProviderService.createToken(user, false);
        restMvc.perform(get("/api/authenticate/store/{url}", "notfound")
        		.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void testAuthorizeSwitchToStoreByURLBlank() throws Exception {
        user = createExistingUser(em, DEFAULT_LOGIN + 1, DEFAULT_EMAIL + 1, DEFAULT_LOCATION2, DEFAULT_LANG);
        String token = tokenProviderService.createToken(user, false);
        restMvc.perform(get("/api/authenticate/store/{url}", "    ")
        		.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void testAuthorizeSwitchToCompanyByURL() throws Exception {
        user = createExistingUser(em, DEFAULT_LOGIN + 1, DEFAULT_EMAIL + 1, DEFAULT_LOCATION2, DEFAULT_LANG);
        String token = tokenProviderService.createToken(user, false);
        restMvc.perform(get("/api/authenticate/company/{url}", "testurl")
        		.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isEmpty());
    }

    @Test
    @Transactional
    public void testAuthorizeSwitchToCompanyByURLNotFound() throws Exception {
        user = createExistingUser(em, DEFAULT_LOGIN + 1, DEFAULT_EMAIL + 1, DEFAULT_LOCATION2, DEFAULT_LANG);
        String token = tokenProviderService.createToken(user, false);
        restMvc.perform(get("/api/authenticate/company/{url}", "notfound")
        		.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isNotFound());
    }
}
