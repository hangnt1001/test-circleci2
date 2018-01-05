/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.web.rest;

import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.BEECOW_USER_LOGIN;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.DEFAULT_AVATAR;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.DEFAULT_FEMALE_AVATAR;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.DEFAULT_FIRSTNAME;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.DEFAULT_LASTNAME;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.DEFAULT_MALE_AVATAR;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.ENCRYPTED_DEFAUT_PASSWORD;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.createMobileUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import com.mediastep.beecow.common.domain.enumeration.AccountType;
import com.mediastep.beecow.common.domain.enumeration.Gender;
import com.mediastep.beecow.common.dto.PhoneDTO;
import com.mediastep.beecow.common.dto.UserDTO;
import com.mediastep.beecow.common.errors.CommonExceptionTranslator;
import com.mediastep.beecow.common.security.AuthoritiesConstants;
import com.mediastep.beecow.common.util.CommonTestUtil;
import com.mediastep.beecow.common.util.PhoneDtoUtil;
import com.mediastep.beecow.gateway.BeecowGatewayApp;
import com.mediastep.beecow.gateway.client.BcUserSettingValueServiceClient;
import com.mediastep.beecow.gateway.client.JobServiceClient;
import com.mediastep.beecow.gateway.client.UserStoreServiceClient;
import com.mediastep.beecow.gateway.domain.Authority;
import com.mediastep.beecow.gateway.domain.User;
import com.mediastep.beecow.gateway.repository.UserRepository;
import com.mediastep.beecow.gateway.security.jwt.TokenProviderService;
import com.mediastep.beecow.gateway.service.MailService;
import com.mediastep.beecow.gateway.service.SMSService;
import com.mediastep.beecow.gateway.service.SocialService;
import com.mediastep.beecow.gateway.service.UserService;
import com.mediastep.beecow.gateway.service.mapper.UserMapper;
import com.mediastep.beecow.gateway.web.rest.errors.ExceptionTranslator;
import com.mediastep.beecow.gateway.web.rest.vm.ManagedUserVM;
import com.mediastep.beecow.job.dto.CompanyDTO;
import com.mediastep.beecow.store.client.StoreServiceClient;
import com.mediastep.beecow.store.service.dto.StoreDTO;
import com.mediastep.beecow.user.dto.UserStoreDTO;

/**
 * Test class for the AccountResource REST controller.
 *
 * @see UserService
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = BeecowGatewayApp.class)
public class AccountResourceIntTest {

	public static final Long DEFAULT_STORE_ID = 10L;

	public static final Long DEFAULT_COMPANY_ID = 100L;

	@Inject
    private UserRepository userRepository;

    @Inject
    private UserService userService;

    @Inject
    private TokenProviderService tokenProviderService;

    @Inject
    private UserMapper userMapper;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private EntityManager em;

    @Mock
    private UserService mockUserService;

    @Mock
    private MailService mockMailService;

    @Mock
    private SMSService mockSMSService;

    private MockMvc restUserMockMvc;

    private MockMvc restMvc;

    public static void mockUserSettingValueServiceClient(BcUserSettingValueServiceClient userSettingValueServiceClient) {
        when(userSettingValueServiceClient.getAllBcUserSettingValues(anyLong(), (ZonedDateTime) anyObject(), anyString())).then(new Answer<Map<String, Object>>() {
            @Override
            public Map<String, Object> answer(InvocationOnMock invocation) throws Throwable {
                return new HashMap<>();
            }
        });
    }

    public static void mockUserStoreServiceClient(UserStoreServiceClient userStoreServiceClient) {
        when(userStoreServiceClient.findStoreByUser(anyLong(), anyString())).then(new Answer<UserStoreDTO>() {
            @Override
            public UserStoreDTO answer(InvocationOnMock invocation) throws Throwable {
                Long id = (Long) invocation.getArguments()[0];
                UserStoreDTO storeDTO = new UserStoreDTO();
                storeDTO.setId(id);
                return storeDTO;
            }
        });
    }

    public static void mockStoreServiceClient(StoreServiceClient storeServiceClient) {
        when(storeServiceClient.getStoreByURL(anyString(), anyString())).then(new Answer<StoreDTO>() {
            @Override
            public StoreDTO answer(InvocationOnMock invocation) throws Throwable {
                String url = (String) invocation.getArguments()[0];
                if ("notfound".equals(url)) {
                	return null;
                }
                StoreDTO storeDTO = new StoreDTO();
                storeDTO.setName(url);
                storeDTO.setId(DEFAULT_STORE_ID);
                return storeDTO;
            }
        });
    }

    public static void mockJobServiceClient(JobServiceClient jobServiceClient) {
        when(jobServiceClient.getCompanyByURL(anyString(), anyString())).then(new Answer<CompanyDTO>() {
            @Override
            public CompanyDTO answer(InvocationOnMock invocation) throws Throwable {
                String url = (String) invocation.getArguments()[0];
                if ("notfound".equals(url)) {
                	return null;
                }
                CompanyDTO companyDTO = new CompanyDTO();
                companyDTO.setName(url);
                companyDTO.setId(DEFAULT_COMPANY_ID);
                return companyDTO;
            }
        });
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        doNothing().when(mockMailService).sendActivationEmail((User) anyObject());
        doNothing().when(mockSMSService).sendActivationMessage((User) anyObject());

        AccountResource accountResource = new AccountResource();
        ReflectionTestUtils.setField(accountResource, "userService", userService);
        ReflectionTestUtils.setField(accountResource, "userMapper", userMapper);
        ReflectionTestUtils.setField(accountResource, "mailService", mockMailService);
        ReflectionTestUtils.setField(accountResource, "smsService", mockSMSService);
        ReflectionTestUtils.setField(accountResource, "tokenProviderService", tokenProviderService);

        AccountResource accountUserMockResource = new AccountResource();
        ReflectionTestUtils.setField(accountUserMockResource, "userService", mockUserService);
        ReflectionTestUtils.setField(accountUserMockResource, "userMapper", userMapper);
        ReflectionTestUtils.setField(accountUserMockResource, "mailService", mockMailService);
        ReflectionTestUtils.setField(accountUserMockResource, "tokenProviderService", tokenProviderService);

        this.restMvc = MockMvcBuilders.standaloneSetup(accountResource)
            .setControllerAdvice(new ExceptionTranslator(), new CommonExceptionTranslator())
            .setMessageConverters(jacksonMessageConverter).build();
        this.restUserMockMvc = MockMvcBuilders.standaloneSetup(accountUserMockResource).build();
    }

    @Test
    @Transactional
    public void testNonAuthenticatedUser() throws Exception {
        restUserMockMvc.perform(get("/api/authenticate")
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    @Transactional
    public void testAuthenticatedUser() throws Exception {
        restUserMockMvc.perform(get("/api/authenticate")
                .with(request -> {
                    request.setRemoteUser("test");
                    return request;
                })
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isOk())
                .andExpect(content().string("test"));
    }

    @Test
    @Transactional
    public void testGetExistingAccount() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.ADMIN);
        authorities.add(authority);

        User user = new User();
        user.setLogin("test");
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail("john.doe@jhipter.com");
        user.setAuthorities(authorities);
        when(mockUserService.getUserWithAuthorities()).thenReturn(user);

        restUserMockMvc.perform(get("/api/account")
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.login").value("test"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.firstName").value("john"))
                .andExpect(jsonPath("$.lastName").value("doe"))
                .andExpect(jsonPath("$.displayName").value("john doe"))
                .andExpect(jsonPath("$.email").value("john.doe@jhipter.com"))
                .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_AVATAR))
                .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.ADMIN));
    }

    @Test
    @Transactional
    public void testGetExistingMaleAccount() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.ADMIN);
        authorities.add(authority);

        User user = new User();
        user.setLogin("test");
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail("john.doe@jhipter.com");
        user.setGender(Gender.MALE);
        user.setAuthorities(authorities);
        when(mockUserService.getUserWithAuthorities()).thenReturn(user);

        restUserMockMvc.perform(get("/api/account")
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.login").value("test"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.firstName").value("john"))
                .andExpect(jsonPath("$.lastName").value("doe"))
                .andExpect(jsonPath("$.displayName").value("john doe"))
                .andExpect(jsonPath("$.email").value("john.doe@jhipter.com"))
                .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
                .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
                .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.ADMIN));
    }

    @Test
    @Transactional
    public void testGetExistingFemaleAccount() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.ADMIN);
        authorities.add(authority);

        User user = new User();
        user.setLogin("test");
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail("john.doe@jhipter.com");
        user.setGender(Gender.FEMALE);
        user.setAuthorities(authorities);
        when(mockUserService.getUserWithAuthorities()).thenReturn(user);

        restUserMockMvc.perform(get("/api/account")
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.login").value("test"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.firstName").value("john"))
                .andExpect(jsonPath("$.lastName").value("doe"))
                .andExpect(jsonPath("$.displayName").value("john doe"))
                .andExpect(jsonPath("$.email").value("john.doe@jhipter.com"))
                .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_FEMALE_AVATAR))
                .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.ADMIN));
    }

    @Test
    @Transactional
    public void testGetExistingAccountByEmail() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.USER);
        authorities.add(authority);

        User user = new User();
        user.setLogin("test");
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setAccountType(AccountType.EMAIL);
        user.setFirstName("john");
        user.setLastName("deo");
        user.setDisplayName("john doe");
        user.setEmail("john.doe@jhipter.com");
        user.setLangKey("en");
        user.setAuthorities(authorities);
        userRepository.saveAndFlush(user);

        restMvc.perform(get("/api/account/email?email={email}", user.getEmail())
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.login").value("test"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
                .andExpect(jsonPath("$.firstName").value("john"))
                .andExpect(jsonPath("$.lastName").value("deo"))
                .andExpect(jsonPath("$.email").value("john.doe@jhipter.com"))
                .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_AVATAR))
                .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.USER));
    }

    @Test
    @Transactional
    public void testGetExistingAccountByEmailNotFound() throws Exception {
        restMvc.perform(get("/api/account/email?email={email}", "not.exist@example.com")
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void testGetExistingAccountByMobile() throws Exception {
        PhoneDTO mobile = PhoneDtoUtil.stringToPhoneDTO("+84:0123456799");
        User user = createMobileUser(em, mobile.toString(), ENCRYPTED_DEFAUT_PASSWORD, mobile.toString(), AuthoritiesConstants.USER, true);

        restMvc.perform(get("/api/account/mobile?mobile={mobile}", mobile.toString())
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.login").value(mobile.toString()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.accountType").value(AccountType.MOBILE.toString()))
                .andExpect(jsonPath("$.firstName").value(DEFAULT_FIRSTNAME))
                .andExpect(jsonPath("$.lastName").value(DEFAULT_LASTNAME))
                .andExpect(jsonPath("$.mobile.phoneNumber").value(mobile.getPhoneNumber()))
                .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_AVATAR))
                .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.USER));
    }

    @Test
    @Transactional
    public void testGetExistingAccountByMobileNotFound() throws Exception {
        restMvc.perform(get("/api/account/mobile?mobile={mobile}", "+84:0123456789")
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void testGetUnknownAccount() throws Exception {
        when(mockUserService.getUserWithAuthorities()).thenReturn(null);

        restUserMockMvc.perform(get("/api/account")
                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void testRegisterValid() throws Exception {
        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            "Joe",                  // firstName
            "Shmoe",                // lastName
            "joe@example.com",      // e-mail
            true,                   // 
            "en",                   // langKey
            new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.login").value("joe"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.firstName").value("Joe"))
            .andExpect(jsonPath("$.lastName").value("Shmoe"))
            .andExpect(jsonPath("$.displayName").value("Joe Shmoe"))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST)); // email is not activate yet

        Optional<User> userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        ZonedDateTime nextHour = ZonedDateTime.now().plusHours(1);
        User user = userOptional.get();
        assertThat(user.getActivationExpiredDate()).isAfter(nextHour.minusMinutes(15)).isBefore(nextHour.plusMinutes(15));
    }

    @Test
    @Transactional
    public void testRegisterInvalidLogin() throws Exception {
        String password = "password";
        ManagedUserVM invalidUser = new ManagedUserVM(
            null,                   // id
            "funky-log!n",          // login <-- invalid
            password,               // password
            "Funky",                // firstName
            "One",                  // lastName
            "funky@example.com",    // e-mail
            true,                   // 
            "en",                   // langKey
            new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());

        Optional<User> user = userRepository.findOneByEmail("funky@example.com");
        assertThat(user.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegisterInvalidEmail() throws Exception {
        String password = "password";
        ManagedUserVM invalidUser = new ManagedUserVM(
            null,               // id
            "bob",              // login
            password,           // password
            "Bob",              // firstName
            "Green",            // lastName
            "invalid",          // e-mail <-- invalid
            true,               // 
            "en",               // langKey
            new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            null,               // createdBy
            null,               // createdDate
            null,               // lastModifiedBy
            null                // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());

        Optional<User> user = userRepository.findOneByLogin("bob");
        assertThat(user.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegisterInvalidPassword() throws Exception {
        String password = "123";
        ManagedUserVM invalidUser = new ManagedUserVM(
            null,               // id
            "bob",              // login
            password,           // password with only 3 digits
            "Bob",              // firstName
            "Green",            // lastName
            "bob@example.com",  // e-mail
            true,               // 
            "en",               // langKey
            new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            null,               // createdBy
            null,               // createdDate
            null,               // lastModifiedBy
            null                // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());

        Optional<User> user = userRepository.findOneByLogin("bob");
        assertThat(user.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegisterDuplicateLogin() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.USER);
        authorities.add(authority);
        User validUser = new User();
        validUser.setLogin("alice");
        validUser.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        validUser.setAccountType(AccountType.EMAIL);
        validUser.setFirstName("Alice");
        validUser.setLastName("Something");
        validUser.setDisplayName("Alice Something");
        validUser.setEmail("alice@example.com");
        validUser.setLangKey("en");
        validUser.setAuthorities(authorities);
        userRepository.saveAndFlush(validUser);

        // Duplicate login, different e-mail
        ManagedUserVM duplicatedUser = new ManagedUserVM(null, validUser.getLogin(), validUser.getPassword(), validUser.getFirstName(), validUser.getLastName(),
            "alicejr@example.com", true, validUser.getLangKey(), new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            validUser.getCreatedBy(), validUser.getCreatedDate(), validUser.getLastModifiedBy(), validUser.getLastModifiedDate());

        // Duplicate login
        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(duplicatedUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isConflict());

        Optional<User> userDup = userRepository.findOneByEmail("alicejr@example.com");
        assertThat(userDup.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegisterDuplicateLoginWhichNotActivated() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.GUEST);
        authorities.add(authority);
        User validUser = new User();
        validUser.setLogin("alice");
        validUser.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        validUser.setAccountType(AccountType.EMAIL);
        validUser.setFirstName("Alice");
        validUser.setLastName("Something");
        validUser.setDisplayName("Alice Something");
        validUser.setEmail("alice@example.com");
        validUser.setLangKey("en");
        validUser.setAuthorities(authorities);
        validUser.setActivationKey("SOMEKEY");
        validUser.setActivationExpiredDate(ZonedDateTime.now().plusHours(1));
        userRepository.saveAndFlush(validUser);

        // Duplicate login, different e-mail
        ManagedUserVM duplicatedUser = new ManagedUserVM(null, validUser.getLogin(), validUser.getPassword(), validUser.getFirstName(), validUser.getLastName(),
            "alicejr@example.com", true, validUser.getLangKey(), new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            validUser.getCreatedBy(), validUser.getCreatedDate(), validUser.getLastModifiedBy(), validUser.getLastModifiedDate());

        // Duplicate login
        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(duplicatedUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isConflict());

        Optional<User> userOptional = userRepository.findOneByEmail("alicejr@example.com");
        assertThat(userOptional.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegisterDuplicateEmail() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.USER);
        authorities.add(authority);
        User validUser = new User();
        validUser.setLogin("john");
        validUser.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        validUser.setAccountType(AccountType.EMAIL);
        validUser.setFirstName("John");
        validUser.setLastName("Doe");
        validUser.setDisplayName("John Doe");
        validUser.setEmail("john@example.com");
        validUser.setLangKey("en");
        validUser.setAuthorities(authorities);
        userRepository.saveAndFlush(validUser);

        // Duplicate e-mail, different login
        ManagedUserVM duplicatedUser = new ManagedUserVM(null, "johnjr", validUser.getPassword(), validUser.getFirstName(), validUser.getLastName(),
            validUser.getEmail(), true, validUser.getLangKey(), new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            validUser.getCreatedBy(), validUser.getCreatedDate(), validUser.getLastModifiedBy(), validUser.getLastModifiedDate());

        // Duplicate e-mail
        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(duplicatedUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isConflict());

        Optional<User> userDup = userRepository.findOneByLogin("johnjr");
        assertThat(userDup.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegisterDuplicateEmailWhichNotActivated() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.GUEST);
        authorities.add(authority);
        User validUser = new User();
        validUser.setLogin("john");
        validUser.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        validUser.setAccountType(AccountType.EMAIL);
        validUser.setFirstName("John");
        validUser.setLastName("Doe");
        validUser.setDisplayName("John Doe");
        validUser.setEmail("john@example.com");
        validUser.setLangKey("en");
        validUser.setAuthorities(authorities);
        validUser.setActivationKey("SOMEKEY");
        validUser.setActivationExpiredDate(ZonedDateTime.now().plusHours(1));
        userRepository.saveAndFlush(validUser);

        // Duplicate e-mail, different login
        ManagedUserVM duplicatedUser = new ManagedUserVM(null, "johnjr", validUser.getPassword(), validUser.getFirstName(), validUser.getLastName(),
            validUser.getEmail(), true, validUser.getLangKey(), new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            validUser.getCreatedBy(), validUser.getCreatedDate(), validUser.getLastModifiedBy(), validUser.getLastModifiedDate());

        // Duplicate e-mail
        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(duplicatedUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isConflict());

        Optional<User> userOptional = userRepository.findOneByLogin("johnjr");
        assertThat(userOptional.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegisterAdminIsIgnored() throws Exception {
        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "badguy",               // login
            password,               // password
            "Bad",                  // firstName
            "Guy",                  // lastName
            "badguy@example.com",   // e-mail
            true,                   // activated
            "en",                   // langKey
            new HashSet<>(Arrays.asList(AuthoritiesConstants.ADMIN)),
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isCreated());

        Optional<User> userDup = userRepository.findOneByLogin("badguy");
        assertThat(userDup.isPresent()).isTrue();
        assertThat(userDup.get().getAuthorities()).hasSize(1).containsExactly(Authority.GUEST); // ADMIN is not set
    }

    @Test
    @Transactional
    public void testRegisterMobile() throws Exception {
        User user = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String firstName = "Joe";
        String lastName = "Shmoe";
        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            firstName,              // firstName
            lastName,               // lastName
            "joe@example.com",      // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value("joe"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.firstName").value(firstName))
            .andExpect(jsonPath("$.lastName").value(lastName))
            .andExpect(jsonPath("$.displayName").value(firstName + " " + lastName))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST));

        Optional<User> userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(firstName + " " + lastName);
    }

    @Test
    @Transactional
    public void testRegisterMobileWithBeecowUser() throws Exception {
        User user = userRepository.findOneByLogin(BEECOW_USER_LOGIN).get();
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String firstName = "Joe";
        String lastName = "Shmoe";
        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            firstName,              // firstName
            lastName,               // lastName
            "joe@example.com",      // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.login").value("joe"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.firstName").value(firstName))
            .andExpect(jsonPath("$.lastName").value(lastName))
            .andExpect(jsonPath("$.displayName").value(firstName + " " + lastName))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST)) // email is not activate yet
            .andExpect(jsonPath("$.accessToken").isNotEmpty());

        Optional<User> userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(firstName + " " + lastName);
    }

    @Test
    @Transactional
    public void testRegisterMobileTwiceWithBeecowUser() throws Exception {
        User user = userRepository.findOneByLogin(BEECOW_USER_LOGIN).get();
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String firstName = "Joe";
        String lastName = "Shmoe";
        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            firstName,              // firstName
            lastName,               // lastName
            "joe@example.com",      // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        ResultActions result = restMvc.perform(
            post("/api/register/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.login").value("joe"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.firstName").value(firstName))
            .andExpect(jsonPath("$.lastName").value(lastName))
            .andExpect(jsonPath("$.displayName").value(firstName + " " + lastName))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST))
            .andExpect(jsonPath("$.accessToken").isNotEmpty());

        Optional<User> userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(firstName + " " + lastName);

        String response = result.andReturn().getResponse().getContentAsString();
        Map<String, Object> resultProps = CommonTestUtil.convertJsonToMap(response);
        token = (String) resultProps.get("accessToken");
        restMvc.perform(
            post("/api/register/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value("joe"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.firstName").value(firstName))
            .andExpect(jsonPath("$.lastName").value(lastName))
            .andExpect(jsonPath("$.displayName").value(firstName + " " + lastName))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST))
            .andExpect(jsonPath("$.accessToken").doesNotExist());

        userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(firstName + " " + lastName);
    }

    @Test
    @Transactional
    public void testRegisterMobileTwiceChangeEmailWithBeecowUser() throws Exception {
        User user = userRepository.findOneByLogin(BEECOW_USER_LOGIN).get();
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String firstName = "Joe";
        String lastName = "Shmoe";
        String password = "password";
        String email = "joe@example.com";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            firstName,              // firstName
            lastName,               // lastName
            email,      // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        ResultActions result = restMvc.perform(
            post("/api/register/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.login").value("joe"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.firstName").value(firstName))
            .andExpect(jsonPath("$.lastName").value(lastName))
            .andExpect(jsonPath("$.displayName").value(firstName + " " + lastName))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.mobile").value(nullValue()))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST))
            .andExpect(jsonPath("$.accessToken").isNotEmpty());

        Optional<User> userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(firstName + " " + lastName);
        assertThat(userOptional.get().getEmail()).isEqualTo(email);
        assertThat(userOptional.get().getMobile()).isNull();

        String email2 = "joe2@example.com";
        validUser.setEmail(email2);
        String response = result.andReturn().getResponse().getContentAsString();
        Map<String, Object> resultProps = CommonTestUtil.convertJsonToMap(response);
        token = (String) resultProps.get("accessToken");
        restMvc.perform(
            post("/api/register/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value("joe"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.firstName").value(firstName))
            .andExpect(jsonPath("$.lastName").value(lastName))
            .andExpect(jsonPath("$.displayName").value(firstName + " " + lastName))
            .andExpect(jsonPath("$.email").value(email2))
            .andExpect(jsonPath("$.mobile").value(nullValue()))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST))
            .andExpect(jsonPath("$.accessToken").doesNotExist());

        userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(firstName + " " + lastName);
        assertThat(userOptional.get().getEmail()).isEqualTo(email2);
        assertThat(userOptional.get().getMobile()).isNull();
    }

    @Test
    @Transactional
    public void testRegisterMobileTwiceChangeEmailWithBeecowUser2() throws Exception {
        User user = userRepository.findOneByLogin(BEECOW_USER_LOGIN).get();
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String displayName = "joe";
        String password = "password";
        String email = displayName + "@example.com";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            null,              // firstName
            null,               // lastName
            email,      // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        ResultActions result = restMvc.perform(
            post("/api/register/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.login").value("joe"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.displayName").value(displayName))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.mobile").value(nullValue()))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST))
            .andExpect(jsonPath("$.accessToken").isNotEmpty());

        Optional<User> userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(displayName);
        assertThat(userOptional.get().getEmail()).isEqualTo(email);
        assertThat(userOptional.get().getMobile()).isNull();

        String displayName2 = "joe2";
        String email2 = displayName2 + "@example.com";
        validUser.setEmail(email2);
        String response = result.andReturn().getResponse().getContentAsString();
        Map<String, Object> resultProps = CommonTestUtil.convertJsonToMap(response);
        token = (String) resultProps.get("accessToken");
        restMvc.perform(
            post("/api/register/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value("joe"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.displayName").value(displayName2))
            .andExpect(jsonPath("$.email").value(email2))
            .andExpect(jsonPath("$.mobile").value(nullValue()))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST))
            .andExpect(jsonPath("$.accessToken").doesNotExist());

        userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(displayName2);
        assertThat(userOptional.get().getEmail()).isEqualTo(email2);
        assertThat(userOptional.get().getMobile()).isNull();
    }

    @Test
    @Transactional
    public void testRegisterMobileTwiceChangePasswordWithBeecowUser() throws Exception {
        User user = userRepository.findOneByLogin(BEECOW_USER_LOGIN).get();
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String firstName = "Joe";
        String lastName = "Shmoe";
        String password = "password";
        String email = "joe@example.com";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            firstName,              // firstName
            lastName,               // lastName
            email,      // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        ResultActions result = restMvc.perform(
            post("/api/register/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.login").value("joe"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.firstName").value(firstName))
            .andExpect(jsonPath("$.lastName").value(lastName))
            .andExpect(jsonPath("$.displayName").value(firstName + " " + lastName))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.mobile").value(nullValue()))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST))
            .andExpect(jsonPath("$.accessToken").isNotEmpty());

        Optional<User> userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(firstName + " " + lastName);
        assertThat(userOptional.get().getEmail()).isEqualTo(email);
        assertThat(userOptional.get().getMobile()).isNull();

        String password2 = "password2";
        validUser = new ManagedUserVM(
                null,                   // id
                "joe",                  // login
                password2,              // password
                firstName,              // firstName
                lastName,               // lastName
                email,      // e-mail
                true,                   // activated
                "en",                   // langKey
                null,
                null,                   // createdBy
                null,                   // createdDate
                null,                   // lastModifiedBy
                null                    // lastModifiedDate
            );
        String response = result.andReturn().getResponse().getContentAsString();
        Map<String, Object> resultProps = CommonTestUtil.convertJsonToMap(response);
        token = (String) resultProps.get("accessToken");
        restMvc.perform(
            post("/api/register/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value("joe"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.firstName").value(firstName))
            .andExpect(jsonPath("$.lastName").value(lastName))
            .andExpect(jsonPath("$.displayName").value(firstName + " " + lastName))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.mobile").value(nullValue()))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST))
            .andExpect(jsonPath("$.accessToken").doesNotExist());

        userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(firstName + " " + lastName);
        assertThat(userOptional.get().getEmail()).isEqualTo(email);
        assertThat(userOptional.get().getMobile()).isNull();
    }

    @Test
    @Transactional
    public void testRegisterMobileTwiceChangeFromEmailToPhone() throws Exception {
        User user = userRepository.findOneByLogin(BEECOW_USER_LOGIN).get();
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String password = "password";
        String displayName = "joe";
        String email = displayName + "@example.com";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            null,                  // login
            password,               // password
            null,              // firstName
            null,               // lastName
            email,      // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        ResultActions result = restMvc.perform(
            post("/api/register/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.login").value(email))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.displayName").value(displayName))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.mobile").value(nullValue()))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST))
            .andExpect(jsonPath("$.accessToken").isNotEmpty());

        Optional<User> userOptional = userRepository.findOneByLogin(email);
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(displayName);
        assertThat(userOptional.get().getEmail()).isEqualTo(email);
        assertThat(userOptional.get().getMobile()).isNull();

        validUser = new ManagedUserVM(
            null,                   // id
            null,                   // login
            password,               // password
            null,                   // firstName
            null,                   // lastName
            null,                   // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("0123456789");
        validUser.setMobile(mobileDTO);
        String mobileAsString = mobileDTO.toString();

        String response = result.andReturn().getResponse().getContentAsString();
        Map<String, Object> resultProps = CommonTestUtil.convertJsonToMap(response);
        token = (String) resultProps.get("accessToken");
        restMvc.perform(
            post("/api/register2/mobile/phone")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value(mobileAsString))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.MOBILE.toString()))
            .andExpect(jsonPath("$.displayName").value(mobileDTO.getPhoneNumber()))
            .andExpect(jsonPath("$.email").value(nullValue()))
            .andExpect(jsonPath("$.mobile.countryCode").value("+84"))
            .andExpect(jsonPath("$.mobile.phoneNumber").value("0123456789"))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST))
            .andExpect(jsonPath("$.accessToken").doesNotExist());

        userOptional = userRepository.findOneByLogin(mobileAsString);
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(mobileDTO.getPhoneNumber());
        assertThat(userOptional.get().getEmail()).isNull();
        assertThat(userOptional.get().getMobile()).isEqualTo(mobileDTO.toString());
    }

    @Test
    @Transactional
    public void testRegisterMobileFourTimesChangeFromEmailToPhone() throws Exception {
        User user = userRepository.findOneByLogin(BEECOW_USER_LOGIN).get();
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String password = "password";
        String displayName = "joe";
        String email = displayName + "@example.com";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            null,                  // login
            password,               // password
            null,              // firstName
            null,               // lastName
            email,      // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        // 1st time
        restMvc.perform(
            post("/api/register/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.login").value(email))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.displayName").value(displayName))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.mobile").value(nullValue()))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST))
            .andExpect(jsonPath("$.accessToken").isNotEmpty());

        Optional<User> userOptional = userRepository.findOneByLogin(email);
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(displayName);
        assertThat(userOptional.get().getEmail()).isEqualTo(email);
        assertThat(userOptional.get().getMobile()).isNull();

        // 2nd time
        validUser = new ManagedUserVM(
            null,                   // id
            null,                   // login
            password,               // password
            null,                   // firstName
            null,                   // lastName
            null,                   // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("0123456789");
        validUser.setMobile(mobileDTO);
        String mobileAsString = mobileDTO.toString();

        restMvc.perform(
            post("/api/register2/mobile/phone")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value(mobileAsString))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.MOBILE.toString()))
            .andExpect(jsonPath("$.displayName").value(mobileDTO.getPhoneNumber()))
            .andExpect(jsonPath("$.email").value(nullValue()))
            .andExpect(jsonPath("$.mobile.countryCode").value("+84"))
            .andExpect(jsonPath("$.mobile.phoneNumber").value("0123456789"))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST))
            .andExpect(jsonPath("$.accessToken").doesNotExist());

        userOptional = userRepository.findOneByLogin(mobileAsString);
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(mobileDTO.getPhoneNumber());
        assertThat(userOptional.get().getEmail()).isNull();
        assertThat(userOptional.get().getMobile()).isEqualTo(mobileDTO.toString());

        // 3rd time
        validUser = new ManagedUserVM(
                null,                   // id
                null,                  // login
                password,               // password
                null,              // firstName
                null,               // lastName
                email,      // e-mail
                true,                   // activated
                "en",                   // langKey
                null,
                null,                   // createdBy
                null,                   // createdDate
                null,                   // lastModifiedBy
                null                    // lastModifiedDate
            );

        restMvc.perform(
            post("/api/register/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value(email))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.displayName").value(displayName))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.mobile").value(nullValue()))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST))
            .andExpect(jsonPath("$.accessToken").doesNotExist());

        userOptional = userRepository.findOneByLogin(email);
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(displayName);
        assertThat(userOptional.get().getEmail()).isEqualTo(email);
        assertThat(userOptional.get().getMobile()).isNull();

        // 4th time
        validUser = new ManagedUserVM(
            null,                   // id
            null,                   // login
            password,               // password
            null,                   // firstName
            null,                   // lastName
            null,                   // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        validUser.setMobile(mobileDTO);

        restMvc.perform(
            post("/api/register2/mobile/phone")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value(mobileAsString))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.MOBILE.toString()))
            .andExpect(jsonPath("$.displayName").value(mobileDTO.getPhoneNumber()))
            .andExpect(jsonPath("$.email").value(nullValue()))
            .andExpect(jsonPath("$.mobile.countryCode").value("+84"))
            .andExpect(jsonPath("$.mobile.phoneNumber").value("0123456789"))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST))
            .andExpect(jsonPath("$.accessToken").doesNotExist());

        userOptional = userRepository.findOneByLogin(mobileAsString);
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(mobileDTO.getPhoneNumber());
        assertThat(userOptional.get().getEmail()).isNull();
        assertThat(userOptional.get().getMobile()).isEqualTo(mobileDTO.toString());
    }

    @Test
    @Transactional
    public void testRegisterMobileInvalidLogin() throws Exception {
        User user = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String password = "password";
        ManagedUserVM invalidUser = new ManagedUserVM(
            null,                   // id
            "funky-log!n",          // login <-- invalid
            password,               // password
            "Funky",                // firstName
            "One",                  // lastName
            "funky@example.com",    // e-mail
            true,                   // activated
            "en",                   // langKey
            new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());

        Optional<User> userOptional = userRepository.findOneByEmail("funky@example.com");
        assertThat(userOptional.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegisterMobileInvalidEmail() throws Exception {
        User user = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String password = "password";
        ManagedUserVM invalidUser = new ManagedUserVM(
            null,               // id
            "bob",              // login
            password,           // password
            "Bob",              // firstName
            "Green",            // lastName
            "invalid",          // e-mail <-- invalid
            true,               // activated
            "en",               // langKey
            new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            null,               // createdBy
            null,               // createdDate
            null,               // lastModifiedBy
            null                // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());

        Optional<User> userOptional = userRepository.findOneByLogin("bob");
        assertThat(userOptional.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegisterMobileInvalidPassword() throws Exception {
        User user = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String password = "123";
        ManagedUserVM invalidUser = new ManagedUserVM(
            null,               // id
            "bob",              // login
            password,           // password with only 3 digits
            "Bob",              // firstName
            "Green",            // lastName
            "bob@example.com",  // e-mail
            true,               // activated
            "en",               // langKey
            new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            null,               // createdBy
            null,               // createdDate
            null,               // lastModifiedBy
            null                // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());

        Optional<User> userOptional = userRepository.findOneByLogin("bob");
        assertThat(userOptional.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegisterMobileWithoutGuestUser() throws Exception {
        String firstName = "Joe";
        String lastName = "Shmoe";
        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            firstName,              // firstName
            lastName,               // lastName
            "joe@example.com",      // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register/mobile")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.login").value("joe"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.firstName").value("Joe"))
            .andExpect(jsonPath("$.lastName").value("Shmoe"))
            .andExpect(jsonPath("$.displayName").value("Joe Shmoe"))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST));

        Optional<User> userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        ZonedDateTime nextHour = ZonedDateTime.now().plusHours(1);
        User user = userOptional.get();
        assertThat(user.getActivationExpiredDate()).isAfter(nextHour.minusMinutes(15)).isBefore(nextHour.plusMinutes(15));
    }

    @Test
    @Transactional
    public void testRegisterMobileTwice() throws Exception {
        User user = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String firstName = "Joe";
        String lastName = "Shmoe";
        String email = "joe@example.com";
        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            firstName,              // firstName
            lastName,               // lastName
            email,                  // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        // Register the first time
        restMvc.perform(
            post("/api/register/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk());

        Optional<User> userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        userOptional.ifPresent(user1 -> {
            assertThat(user1.getId()).isEqualTo(user.getId());
            assertThat(user1.getEmail()).isEqualTo(email);
            assertThat(user1.getDisplayName()).isEqualTo(firstName + " " + lastName);
        });

        // Register the second time
        restMvc.perform(
            post("/api/register/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk());

        userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        userOptional.ifPresent(user1 -> {
            assertThat(user1.getId()).isEqualTo(user.getId());
            assertThat(user1.getEmail()).isEqualTo(email);
            assertThat(user1.getDisplayName()).isEqualTo(firstName + " " + lastName);
        });
    }

    @Test
    @Transactional
    public void testRegisterMobileTwiceChangeEmail() throws Exception {
        User user = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String firstName = "Joe";
        String lastName = "Shmoe";
        String email = "joe@example.com";
        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            firstName,              // firstName
            lastName,               // lastName
            email,                  // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        // Register the first time
        restMvc.perform(
            post("/api/register/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk());

        Optional<User> userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        userOptional.ifPresent(user1 -> {
            assertThat(user1.getId()).isEqualTo(user.getId());
            assertThat(user1.getEmail()).isEqualTo(email);
            assertThat(user1.getDisplayName()).isEqualTo(firstName + " " + lastName);
        });

        // Register the second time
        String email2 = "joe2@example.com";
        validUser.setEmail(email2);
        restMvc.perform(
            post("/api/register/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk());

        userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        userOptional.ifPresent(user1 -> {
            assertThat(user1.getId()).isEqualTo(user.getId());
            assertThat(user1.getEmail()).isEqualTo(email2);
            assertThat(user1.getDisplayName()).isEqualTo(firstName + " " + lastName);
        });
    }

    @Test
    @Transactional
    public void testRegisterMobileTwiceChangeEmail2() throws Exception {
        User user = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String displayname = "joe";
        String email = displayname + "@example.com";
        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            null,              // firstName
            null,               // lastName
            email,                  // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        // Register the first time
        restMvc.perform(
            post("/api/register/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk());

        Optional<User> userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        userOptional.ifPresent(user1 -> {
            assertThat(user1.getId()).isEqualTo(user.getId());
            assertThat(user1.getEmail()).isEqualTo(email);
            assertThat(user1.getDisplayName()).isEqualTo(displayname);
        });

        // Register the second time
        String displayName2 = "joe2";
        String email2 = displayName2 + "@example.com";
        validUser.setEmail(email2);
        restMvc.perform(
            post("/api/register/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk());

        userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        userOptional.ifPresent(user1 -> {
            assertThat(user1.getId()).isEqualTo(user.getId());
            assertThat(user1.getEmail()).isEqualTo(email2);
            assertThat(user1.getDisplayName()).isEqualTo(displayName2);
        });
    }

    @Test
    @Transactional
    public void testRegisterMobileTwiceChangePassword() throws Exception {
        User user = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String firstName = "Joe";
        String lastName = "Shmoe";
        String email = "joe@example.com";
        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            firstName,              // firstName
            lastName,               // lastName
            email,                  // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        // Register the first time
        restMvc.perform(
            post("/api/register/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk());

        Optional<User> userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        userOptional.ifPresent(user1 -> {
            assertThat(user1.getId()).isEqualTo(user.getId());
            assertThat(user1.getEmail()).isEqualTo(email);
            assertThat(user1.getDisplayName()).isEqualTo(firstName + " " + lastName);
        });

        // Register the second time
        String password2 = "password2";
        validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password2,              // password
            firstName,              // firstName
            lastName,               // lastName
            email,                  // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        restMvc.perform(
            post("/api/register/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk());

        userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        userOptional.ifPresent(user1 -> {
            assertThat(user1.getId()).isEqualTo(user.getId());
            assertThat(user1.getEmail()).isEqualTo(email);
            assertThat(user1.getDisplayName()).isEqualTo(firstName + " " + lastName);
        });
    }

    @Test
    @Transactional
    public void testRegisterMobileGuestEmailExisted() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.USER);
        authorities.add(authority);
        String email = "john.doe@jhipter.com";
        User user = new User();
        user.setLogin("test");
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setAccountType(AccountType.EMAIL);
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail(email);
        user.setAuthorities(authorities);
        userRepository.saveAndFlush(user);

        User guest = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(guest.getLogin(), false);

        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            "Joe",                  // firstName
            "Shmoe",                // lastName
            email,                  // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isConflict());
    }

    @Test
    @Transactional
    public void testRegisterMobileGuestEmailExistedWaitingForActivate() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.GUEST);
        authorities.add(authority);
        String email = "john.doe@jhipter.com";
        User user = new User();
        user.setLogin("test");
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setAccountType(AccountType.EMAIL);
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail(email);
        user.setAuthorities(authorities);
        user.setActivationKey("somecode");
        user.setActivationExpiredDate(ZonedDateTime.now().plusDays(1));
        userRepository.saveAndFlush(user);

        User guest = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(guest.getLogin(), false);

        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            "Joe",                  // firstName
            "Shmoe",                // lastName
            email,                  // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isConflict());

        Optional<User> userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegisterMobileUserEmailExisted() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.USER);
        authorities.add(authority);
        String email = "john.doe@jhipter.com";
        User user = new User();
        user.setLogin("test");
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setAccountType(AccountType.EMAIL);
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail(email);
        user.setAuthorities(authorities);
        userRepository.saveAndFlush(user);

        User guest = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(guest.getLogin(), false);

        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            "Joe",                  // firstName
            "Shmoe",                // lastName
            email,                  // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isConflict());
    }

    @Test
    @Transactional
    public void testRegisterMobileUserLoginExisted() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.USER);
        authorities.add(authority);
        String login = "test";
        User user = new User();
        user.setLogin(login);
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setAccountType(AccountType.EMAIL);
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail("john.doe@jhipter.com");
        user.setAuthorities(authorities);
        userRepository.saveAndFlush(user);

        User guest = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(guest.getLogin(), false);

        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            login,                  // login
            password,               // password
            "Joe",                  // firstName
            "Shmoe",                // lastName
            "john.doe2@jhipter.com",    // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isConflict());
    }

    @Test
    @Transactional
    public void testRegister2Valid() throws Exception {
        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            "Joe",                  // firstName
            "Shmoe",                // lastName
            "joe@example.com",      // e-mail
            true,                   // activated
            "en",                   // langKey
            new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register2")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.login").value("joe"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.firstName").value("Joe"))
            .andExpect(jsonPath("$.lastName").value("Shmoe"))
            .andExpect(jsonPath("$.displayName").value("Joe Shmoe"))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST)); // email is not activate yet

        Optional<User> userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        ZonedDateTime nextHour = ZonedDateTime.now().plusHours(1);
        User user = userOptional.get();
        assertThat(user.getActivationExpiredDate()).isAfter(nextHour.minusMinutes(15)).isBefore(nextHour.plusMinutes(15));
    }

    @Test
    @Transactional
    public void testRegister2InvalidLogin() throws Exception {
        String password = "password";
        ManagedUserVM invalidUser = new ManagedUserVM(
            null,                   // id
            "funky-log!n",          // login <-- invalid
            password,               // password
            "Funky",                // firstName
            "One",                  // lastName
            "funky@example.com",    // e-mail
            true,                   // activated
            "en",                   // langKey
            new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register2")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());

        Optional<User> user = userRepository.findOneByEmail("funky@example.com");
        assertThat(user.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegister2InvalidEmail() throws Exception {
        String password = "password";
        ManagedUserVM invalidUser = new ManagedUserVM(
            null,               // id
            "bob",              // login
            password,           // password
            "Bob",              // firstName
            "Green",            // lastName
            "invalid",          // e-mail <-- invalid
            true,               // activated
            "en",               // langKey
            new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            null,               // createdBy
            null,               // createdDate
            null,               // lastModifiedBy
            null                // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register2")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());

        Optional<User> user = userRepository.findOneByLogin("bob");
        assertThat(user.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegister2InvalidPassword() throws Exception {
        String password = "123";
        ManagedUserVM invalidUser = new ManagedUserVM(
            null,               // id
            "bob",              // login
            password,           // password with only 3 digits
            "Bob",              // firstName
            "Green",            // lastName
            "bob@example.com",  // e-mail
            true,               // activated
            "en",               // langKey
            new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            null,               // createdBy
            null,               // createdDate
            null,               // lastModifiedBy
            null                // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register2")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());

        Optional<User> user = userRepository.findOneByLogin("bob");
        assertThat(user.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegister2DuplicateLogin() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.USER);
        authorities.add(authority);
        User validUser = new User();
        validUser.setLogin("alice");
        validUser.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        validUser.setAccountType(AccountType.EMAIL);
        validUser.setFirstName("Alice");
        validUser.setLastName("Something");
        validUser.setDisplayName("Alice Something");
        validUser.setEmail("alice@example.com");
        validUser.setLangKey("en");
        validUser.setAuthorities(authorities);
        userRepository.saveAndFlush(validUser);

        // Duplicate login, different e-mail
        ManagedUserVM duplicatedUser = new ManagedUserVM(null, validUser.getLogin(), validUser.getPassword(), validUser.getFirstName(), validUser.getLastName(),
            "alicejr@example.com", true, validUser.getLangKey(), new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            validUser.getCreatedBy(), validUser.getCreatedDate(), validUser.getLastModifiedBy(), validUser.getLastModifiedDate());

        // Duplicate login
        restMvc.perform(
            post("/api/register2")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(duplicatedUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isConflict());

        Optional<User> userDup = userRepository.findOneByEmail("alicejr@example.com");
        assertThat(userDup.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegister2DuplicateLoginWhichNotActivated() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.GUEST);
        authorities.add(authority);
        User validUser = new User();
        validUser.setLogin("alice");
        validUser.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        validUser.setAccountType(AccountType.EMAIL);
        validUser.setFirstName("Alice");
        validUser.setLastName("Something");
        validUser.setDisplayName("Alice Something");
        validUser.setEmail("alice@example.com");
        validUser.setLangKey("en");
        validUser.setAuthorities(authorities);
        validUser.setActivationKey("SOMEKEY");
        validUser.setActivationExpiredDate(ZonedDateTime.now().plusHours(1));
        userRepository.saveAndFlush(validUser);

        // Duplicate login, different e-mail
        ManagedUserVM duplicatedUser = new ManagedUserVM(null, validUser.getLogin(), validUser.getPassword(), validUser.getFirstName(), validUser.getLastName(),
            "alicejr@example.com", true, validUser.getLangKey(), new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            validUser.getCreatedBy(), validUser.getCreatedDate(), validUser.getLastModifiedBy(), validUser.getLastModifiedDate());

        // Duplicate login
        restMvc.perform(
            post("/api/register2")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(duplicatedUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isConflict());

        Optional<User> userOptional = userRepository.findOneByEmail("alicejr@example.com");
        assertThat(userOptional.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegister2DuplicateEmail() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.USER);
        authorities.add(authority);
        User validUser = new User();
        validUser.setLogin("john");
        validUser.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        validUser.setAccountType(AccountType.EMAIL);
        validUser.setFirstName("John");
        validUser.setLastName("Doe");
        validUser.setDisplayName("John Doe");
        validUser.setEmail("john@example.com");
        validUser.setLangKey("en");
        validUser.setAuthorities(authorities);
        userRepository.saveAndFlush(validUser);

        // Duplicate e-mail, different login
        ManagedUserVM duplicatedUser = new ManagedUserVM(null, "johnjr", validUser.getPassword(), validUser.getFirstName(), validUser.getLastName(),
            validUser.getEmail(), true, validUser.getLangKey(), new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            validUser.getCreatedBy(), validUser.getCreatedDate(), validUser.getLastModifiedBy(), validUser.getLastModifiedDate());

        // Duplicate e-mail
        restMvc.perform(
            post("/api/register2")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(duplicatedUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isConflict());

        Optional<User> userDup = userRepository.findOneByLogin("johnjr");
        assertThat(userDup.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegister2DuplicateEmailWhichNotActivated() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.GUEST);
        authorities.add(authority);
        User validUser = new User();
        validUser.setLogin("john");
        validUser.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        validUser.setAccountType(AccountType.EMAIL);
        validUser.setFirstName("John");
        validUser.setLastName("Doe");
        validUser.setDisplayName("John Doe");
        validUser.setEmail("john@example.com");
        validUser.setLangKey("en");
        validUser.setAuthorities(authorities);
        validUser.setActivationKey("SOMEKEY");
        validUser.setActivationExpiredDate(ZonedDateTime.now().plusHours(1));
        userRepository.saveAndFlush(validUser);

        // Duplicate e-mail, different login
        ManagedUserVM duplicatedUser = new ManagedUserVM(null, "johnjr", validUser.getPassword(), validUser.getFirstName(), validUser.getLastName(),
            validUser.getEmail(), true, validUser.getLangKey(), new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            validUser.getCreatedBy(), validUser.getCreatedDate(), validUser.getLastModifiedBy(), validUser.getLastModifiedDate());

        // Duplicate e-mail
        restMvc.perform(
            post("/api/register2")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(duplicatedUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isConflict());

        Optional<User> userOptional = userRepository.findOneByLogin("johnjr");
        assertThat(userOptional.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegister2AdminIsIgnored() throws Exception {
        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "badguy",               // login
            password,               // password
            "Bad",                  // firstName
            "Guy",                  // lastName
            "badguy@example.com",   // e-mail
            true,                   // activated
            "en",                   // langKey
            new HashSet<>(Arrays.asList(AuthoritiesConstants.ADMIN)),
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register2")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isCreated());

        Optional<User> userDup = userRepository.findOneByLogin("badguy");
        assertThat(userDup.isPresent()).isTrue();
        assertThat(userDup.get().getAuthorities()).hasSize(1).containsExactly(Authority.GUEST); // ADMIN is not set
    }

    @Test
    @Transactional
    public void testRegister2ByPhoneValid() throws Exception {
        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            null,                   // login
            password,               // password
            null,                   // firstName
            null,                   // lastName
            null,                   // e-mail
            true,                   // activated
            "en",                   // langKey
            new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("0123456789");
        validUser.setMobile(mobileDTO);

        restMvc.perform(
            post("/api/register2")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.login").value(mobileDTO.toString()))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.MOBILE.toString()))
            .andExpect(jsonPath("$.displayName").value(mobileDTO.getPhoneNumber().toString()))
            .andExpect(jsonPath("$.mobile.countryCode").value(mobileDTO.getCountryCode()))
            .andExpect(jsonPath("$.mobile.phoneNumber").value(mobileDTO.getPhoneNumber()))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST)); // email is not activate yet

        Optional<User> userOptional = userRepository.findOneByLogin(mobileDTO.toString());
        assertThat(userOptional.isPresent()).isTrue();
        ZonedDateTime nextHour = ZonedDateTime.now().plusHours(1);
        User user = userOptional.get();
        assertThat(user.getActivationExpiredDate()).isAfter(nextHour.minusMinutes(15)).isBefore(nextHour.plusMinutes(15));
    }

    @Test
    @Transactional
    public void testRegister2ByPhoneValid2() throws Exception {
        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            null,                   // login
            password,               // password
            "Joe",                  // firstName
            "Shmoe",                // lastName
            null,                   // e-mail
            true,                   // activated
            "en",                   // langKey
            new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("   +84   ");
        mobileDTO.setPhoneNumber("   0123456789 ");
        validUser.setMobile(mobileDTO);

        PhoneDTO validMobileDTO = new PhoneDTO();
        validMobileDTO.setCountryCode("+84");
        validMobileDTO.setPhoneNumber("0123456789");
        restMvc.perform(
            post("/api/register2")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.login").value(validMobileDTO.toString()))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.MOBILE.toString()))
            .andExpect(jsonPath("$.firstName").value("Joe"))
            .andExpect(jsonPath("$.lastName").value("Shmoe"))
            .andExpect(jsonPath("$.displayName").value("Joe Shmoe"))
            .andExpect(jsonPath("$.mobile.countryCode").value(validMobileDTO.getCountryCode()))
            .andExpect(jsonPath("$.mobile.phoneNumber").value(validMobileDTO.getPhoneNumber()))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST)); // email is not activate yet

        Optional<User> userOptional = userRepository.findOneByLogin(validMobileDTO.toString());
        assertThat(userOptional.isPresent()).isTrue();
        ZonedDateTime nextHour = ZonedDateTime.now().plusHours(1);
        User user = userOptional.get();
        assertThat(user.getActivationExpiredDate()).isAfter(nextHour.minusMinutes(15)).isBefore(nextHour.plusMinutes(15));
    }

    @Test
    @Transactional
    public void testRegister2ByPhoneCountryCodeInvalid() throws Exception {
        String password = "password";
        ManagedUserVM user = new ManagedUserVM(
            null,                   // id
            null,                   // login
            password,               // password
            null,                   // firstName
            null,                   // lastName
            null,                   // e-mail
            true,                   // activated
            "en",                   // langKey
            new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("invalid");
        mobileDTO.setPhoneNumber("0123456789");
        user.setMobile(mobileDTO);

        restMvc.perform(
            post("/api/register2")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(user)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());

        Optional<User> testUser = userRepository.findOneByLogin(mobileDTO.toString());
        assertThat(testUser.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegister2ByPhoneInvalid() throws Exception {
        String password = "password";
        ManagedUserVM user = new ManagedUserVM(
            null,                   // id
            null,                   // login
            password,               // password
            "Joe",                  // firstName
            "Shmoe",                // lastName
            null,                   // e-mail
            true,                   // activated
            "en",                   // langKey
            new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("invalid");
        user.setMobile(mobileDTO);

        restMvc.perform(
            post("/api/register2")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(user)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());

        Optional<User> tsetUser = userRepository.findOneByLogin(mobileDTO.toString());
        assertThat(tsetUser.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegister2ByPhoneInvalidPassword() throws Exception {
        String password = "123";
        ManagedUserVM invalidUser = new ManagedUserVM(
            null,                   // id
            null,                   // login
            password,               // password
            null,                   // firstName
            null,                   // lastName
            null,                   // e-mail
            true,                   // activated
            "en",                   // langKey
            new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("0123456789");
        invalidUser.setMobile(mobileDTO);

        restMvc.perform(
            post("/api/register2")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());

        Optional<User> user = userRepository.findOneByLogin(mobileDTO.toString());
        assertThat(user.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegister2ByPhoneDuplicateLogin() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.USER);
        authorities.add(authority);

        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("0123456789");

        User validUser = new User();
        validUser.setLogin(mobileDTO.toString());
        validUser.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        validUser.setMobileObject(mobileDTO);
        validUser.setDisplayName(mobileDTO.getPhoneNumber());
        validUser.setAccountType(AccountType.MOBILE);
        validUser.setLangKey("en");
        validUser.setAuthorities(authorities);
        userRepository.saveAndFlush(validUser);

        // Duplicate login, different phone number
        ManagedUserVM duplicatedUser = new ManagedUserVM(null, validUser.getLogin(), validUser.getPassword(), validUser.getFirstName(), validUser.getLastName(),
            null, true, validUser.getLangKey(), new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            validUser.getCreatedBy(), validUser.getCreatedDate(), validUser.getLastModifiedBy(), validUser.getLastModifiedDate());
        PhoneDTO otherMobileDTO = new PhoneDTO();
        otherMobileDTO.setCountryCode("+84");
        otherMobileDTO.setPhoneNumber("0987654321");
        duplicatedUser.setMobile(otherMobileDTO);

        // Duplicate login
        restMvc.perform(
            post("/api/register2")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(duplicatedUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isConflict());

        Optional<User> userDup = userRepository.findOneByLogin(otherMobileDTO.toString());
        assertThat(userDup.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegister2ByPhoneDuplicateLoginWhichNotActivated() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.GUEST);
        authorities.add(authority);

        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("0123456789");

        User validUser = new User();
        validUser.setLogin(mobileDTO.toString());
        validUser.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        validUser.setMobileObject(mobileDTO);
        validUser.setAccountType(AccountType.MOBILE);
        validUser.setDisplayName(mobileDTO.getPhoneNumber());
        validUser.setLangKey("en");
        validUser.setAuthorities(authorities);
        validUser.setActivationKey("SOMEKEY");
        validUser.setActivationExpiredDate(ZonedDateTime.now().plusHours(1));
        userRepository.saveAndFlush(validUser);

        // Duplicate login, different phone number
        ManagedUserVM duplicatedUser = new ManagedUserVM(null, validUser.getLogin(), validUser.getPassword(), validUser.getFirstName(), validUser.getLastName(),
            null, true, validUser.getLangKey(), new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            validUser.getCreatedBy(), validUser.getCreatedDate(), validUser.getLastModifiedBy(), validUser.getLastModifiedDate());
        PhoneDTO otherMobileDTO = new PhoneDTO();
        otherMobileDTO.setCountryCode("+84");
        otherMobileDTO.setPhoneNumber("0987654321");
        duplicatedUser.setMobile(otherMobileDTO);

        // Duplicate login
        restMvc.perform(
            post("/api/register2")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(duplicatedUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isConflict());

        Optional<User> userOptional = userRepository.findOneByLogin(otherMobileDTO.toString());
        assertThat(userOptional.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegister2ByPhoneDuplicateNumber() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.USER);
        authorities.add(authority);

        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("0123456789");

        User validUser = new User();
        validUser.setLogin("john");
        validUser.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        validUser.setMobileObject(mobileDTO);
        validUser.setDisplayName(mobileDTO.getPhoneNumber());
        validUser.setAccountType(AccountType.MOBILE);
        validUser.setLangKey("en");
        validUser.setAuthorities(authorities);
        userRepository.saveAndFlush(validUser);

        // Duplicate login, different phone number
        ManagedUserVM duplicatedUser = new ManagedUserVM(null, "johnjr", validUser.getPassword(), validUser.getFirstName(), validUser.getLastName(),
            null, true, validUser.getLangKey(), new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            validUser.getCreatedBy(), validUser.getCreatedDate(), validUser.getLastModifiedBy(), validUser.getLastModifiedDate());
        duplicatedUser.setMobile(mobileDTO);

        // Duplicate e-mail
        restMvc.perform(
            post("/api/register2")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(duplicatedUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isConflict());

        Optional<User> userDup = userRepository.findOneByLogin("johnjr");
        assertThat(userDup.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegister2ByPhoneDuplicateNumber2() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.USER);
        authorities.add(authority);

        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("0123456789");

        User validUser = new User();
        validUser.setLogin("john");
        validUser.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        validUser.setMobileObject(mobileDTO);
        validUser.setDisplayName(mobileDTO.getPhoneNumber());
        validUser.setAccountType(AccountType.MOBILE);
        validUser.setLangKey("en");
        validUser.setAuthorities(authorities);
        userRepository.saveAndFlush(validUser);

        // Duplicate login, different phone number
        ManagedUserVM duplicatedUser = new ManagedUserVM(null, "johnjr", validUser.getPassword(), validUser.getFirstName(), validUser.getLastName(),
            null, true, validUser.getLangKey(), new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            validUser.getCreatedBy(), validUser.getCreatedDate(), validUser.getLastModifiedBy(), validUser.getLastModifiedDate());
        PhoneDTO duplicateMobileDTO = new PhoneDTO();
        duplicateMobileDTO.setCountryCode("+84");
        duplicateMobileDTO.setPhoneNumber("123456789");
        duplicatedUser.setMobile(duplicateMobileDTO);

        // Duplicate e-mail
        restMvc.perform(
            post("/api/register2")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(duplicatedUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isConflict());

        Optional<User> userDup = userRepository.findOneByLogin("johnjr");
        assertThat(userDup.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegister2ByPhoneDuplicateNumberWhichNotActivated() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.GUEST);
        authorities.add(authority);

        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("0123456789");

        User validUser = new User();
        validUser.setLogin("john");
        validUser.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        validUser.setMobileObject(mobileDTO);
        validUser.setDisplayName(mobileDTO.getPhoneNumber());
        validUser.setAccountType(AccountType.MOBILE);
        validUser.setLangKey("en");
        validUser.setAuthorities(authorities);
        validUser.setActivationKey("SOMEKEY");
        validUser.setActivationExpiredDate(ZonedDateTime.now().plusHours(1));
        userRepository.saveAndFlush(validUser);

        // Duplicate e-mail, different login
        ManagedUserVM duplicatedUser = new ManagedUserVM(null, "johnjr", validUser.getPassword(), validUser.getFirstName(), validUser.getLastName(),
            null, true, validUser.getLangKey(), new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            validUser.getCreatedBy(), validUser.getCreatedDate(), validUser.getLastModifiedBy(), validUser.getLastModifiedDate());
        duplicatedUser.setMobile(mobileDTO);

        // Duplicate e-mail
        restMvc.perform(
            post("/api/register2")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(duplicatedUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isConflict());

        Optional<User> userOptional = userRepository.findOneByLogin("johnjr");
        assertThat(userOptional.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegister2ByPhoneAdminIsIgnored() throws Exception {
        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            null,                   // login
            password,               // password
            null,                   // firstName
            null,                   // lastName
            null,                   // e-mail
            true,                   // activated
            "en",                   // langKey
            new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("0123456789");
        validUser.setMobile(mobileDTO);

        restMvc.perform(
            post("/api/register2")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isCreated());

        Optional<User> userDup = userRepository.findOneByLogin(mobileDTO.toString());
        assertThat(userDup.isPresent()).isTrue();
        assertThat(userDup.get().getAuthorities()).hasSize(1).containsExactly(Authority.GUEST); // ADMIN is not set
    }

    @Test
    @Transactional
    public void testRegister2Mobile() throws Exception {
        User user = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String firstName = "Joe";
        String lastName = "Shmoe";
        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            firstName,              // firstName
            lastName,               // lastName
            "joe@example.com",      // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register2/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value("joe"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.firstName").value(firstName))
            .andExpect(jsonPath("$.lastName").value(lastName))
            .andExpect(jsonPath("$.displayName").value(firstName + " " + lastName))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST));

        Optional<User> userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(firstName + " " + lastName);
    }

    @Test
    @Transactional
    public void testRegister2MobileWithBeecowUser() throws Exception {
        User user = userRepository.findOneByLogin(BEECOW_USER_LOGIN).get();
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String firstName = "Joe";
        String lastName = "Shmoe";
        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            firstName,              // firstName
            lastName,               // lastName
            "joe@example.com",      // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register2/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.login").value("joe"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.firstName").value(firstName))
            .andExpect(jsonPath("$.lastName").value(lastName))
            .andExpect(jsonPath("$.displayName").value(firstName + " " + lastName))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST)) // email is not activate yet
            .andExpect(jsonPath("$.accessToken").isNotEmpty());

        Optional<User> userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(firstName + " " + lastName);
    }

    @Test
    @Transactional
    public void testRegister2MobileTwiceWithBeecowUser() throws Exception {
        User user = userRepository.findOneByLogin(BEECOW_USER_LOGIN).get();
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String firstName = "Joe";
        String lastName = "Shmoe";
        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            firstName,              // firstName
            lastName,               // lastName
            "joe@example.com",      // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        ResultActions result = restMvc.perform(
            post("/api/register2/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.login").value("joe"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.firstName").value(firstName))
            .andExpect(jsonPath("$.lastName").value(lastName))
            .andExpect(jsonPath("$.displayName").value(firstName + " " + lastName))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST))
            .andExpect(jsonPath("$.accessToken").isNotEmpty());

        Optional<User> userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(firstName + " " + lastName);

        String response = result.andReturn().getResponse().getContentAsString();
        Map<String, Object> resultProps = CommonTestUtil.convertJsonToMap(response);
        token = (String) resultProps.get("accessToken");
        restMvc.perform(
            post("/api/register2/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value("joe"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.firstName").value(firstName))
            .andExpect(jsonPath("$.lastName").value(lastName))
            .andExpect(jsonPath("$.displayName").value(firstName + " " + lastName))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST))
            .andExpect(jsonPath("$.accessToken").doesNotExist());

        userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(firstName + " " + lastName);
    }

    @Test
    @Transactional
    public void testRegister2MobileTwiceChangeEmailWithBeecowUser() throws Exception {
        User user = userRepository.findOneByLogin(BEECOW_USER_LOGIN).get();
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String firstName = "Joe";
        String lastName = "Shmoe";
        String password = "password";
        String email = "joe@example.com";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            firstName,              // firstName
            lastName,               // lastName
            email,      // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        ResultActions result = restMvc.perform(
            post("/api/register2/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.login").value("joe"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.firstName").value(firstName))
            .andExpect(jsonPath("$.lastName").value(lastName))
            .andExpect(jsonPath("$.displayName").value(firstName + " " + lastName))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.mobile").value(nullValue()))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST))
            .andExpect(jsonPath("$.accessToken").isNotEmpty());

        Optional<User> userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(firstName + " " + lastName);
        assertThat(userOptional.get().getEmail()).isEqualTo(email);
        assertThat(userOptional.get().getMobile()).isNull();

        String email2 = "joe2@example.com";
        validUser.setEmail(email2);
        String response = result.andReturn().getResponse().getContentAsString();
        Map<String, Object> resultProps = CommonTestUtil.convertJsonToMap(response);
        token = (String) resultProps.get("accessToken");
        restMvc.perform(
            post("/api/register2/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value("joe"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.firstName").value(firstName))
            .andExpect(jsonPath("$.lastName").value(lastName))
            .andExpect(jsonPath("$.displayName").value(firstName + " " + lastName))
            .andExpect(jsonPath("$.email").value(email2))
            .andExpect(jsonPath("$.mobile").value(nullValue()))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST))
            .andExpect(jsonPath("$.accessToken").doesNotExist());

        userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(firstName + " " + lastName);
        assertThat(userOptional.get().getEmail()).isEqualTo(email2);
        assertThat(userOptional.get().getMobile()).isNull();
    }

    @Test
    @Transactional
    public void testRegister2MobileTwiceChangePasswordWithBeecowUser() throws Exception {
        User user = userRepository.findOneByLogin(BEECOW_USER_LOGIN).get();
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String firstName = "Joe";
        String lastName = "Shmoe";
        String password = "password";
        String email = "joe@example.com";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            firstName,              // firstName
            lastName,               // lastName
            email,      // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        ResultActions result = restMvc.perform(
            post("/api/register2/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.login").value("joe"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.firstName").value(firstName))
            .andExpect(jsonPath("$.lastName").value(lastName))
            .andExpect(jsonPath("$.displayName").value(firstName + " " + lastName))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.mobile").value(nullValue()))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST))
            .andExpect(jsonPath("$.accessToken").isNotEmpty());

        Optional<User> userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(firstName + " " + lastName);
        assertThat(userOptional.get().getEmail()).isEqualTo(email);
        assertThat(userOptional.get().getMobile()).isNull();

        String password2 = "password2";
        validUser = new ManagedUserVM(
                null,                   // id
                "joe",                  // login
                password2,              // password
                firstName,              // firstName
                lastName,               // lastName
                email,      // e-mail
                true,                   // activated
                "en",                   // langKey
                null,
                null,                   // createdBy
                null,                   // createdDate
                null,                   // lastModifiedBy
                null                    // lastModifiedDate
            );
        String response = result.andReturn().getResponse().getContentAsString();
        Map<String, Object> resultProps = CommonTestUtil.convertJsonToMap(response);
        token = (String) resultProps.get("accessToken");
        restMvc.perform(
            post("/api/register2/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value("joe"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.firstName").value(firstName))
            .andExpect(jsonPath("$.lastName").value(lastName))
            .andExpect(jsonPath("$.displayName").value(firstName + " " + lastName))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.mobile").value(nullValue()))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST))
            .andExpect(jsonPath("$.accessToken").doesNotExist());

        userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(firstName + " " + lastName);
        assertThat(userOptional.get().getEmail()).isEqualTo(email);
        assertThat(userOptional.get().getMobile()).isNull();
    }

    @Test
    @Transactional
    public void testRegister2MobileTwiceChangeFromEmailToPhone() throws Exception {
        User user = userRepository.findOneByLogin(BEECOW_USER_LOGIN).get();
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String password = "password";
        String displayName = "joe";
        String email = displayName + "@example.com";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            null,                  // login
            password,               // password
            null,              // firstName
            null,               // lastName
            email,      // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        ResultActions result = restMvc.perform(
            post("/api/register2/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.login").value(email))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.displayName").value(displayName))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.mobile").value(nullValue()))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST))
            .andExpect(jsonPath("$.accessToken").isNotEmpty());

        Optional<User> userOptional = userRepository.findOneByLogin(email);
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(displayName);
        assertThat(userOptional.get().getEmail()).isEqualTo(email);
        assertThat(userOptional.get().getMobile()).isNull();

        validUser = new ManagedUserVM(
            null,                   // id
            null,                   // login
            password,               // password
            null,                   // firstName
            null,                   // lastName
            null,                   // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("0123456789");
        validUser.setMobile(mobileDTO);
        String mobileAsString = mobileDTO.toString();

        String response = result.andReturn().getResponse().getContentAsString();
        Map<String, Object> resultProps = CommonTestUtil.convertJsonToMap(response);
        token = (String) resultProps.get("accessToken");
        restMvc.perform(
            post("/api/register2/mobile/phone")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value(mobileAsString))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.MOBILE.toString()))
            .andExpect(jsonPath("$.displayName").value(mobileDTO.getPhoneNumber()))
            .andExpect(jsonPath("$.email").value(nullValue()))
            .andExpect(jsonPath("$.mobile.countryCode").value("+84"))
            .andExpect(jsonPath("$.mobile.phoneNumber").value("0123456789"))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST))
            .andExpect(jsonPath("$.accessToken").doesNotExist());

        userOptional = userRepository.findOneByLogin(mobileAsString);
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(mobileDTO.getPhoneNumber());
        assertThat(userOptional.get().getEmail()).isNull();
        assertThat(userOptional.get().getMobile()).isEqualTo(mobileDTO.toString());
    }

    @Test
    @Transactional
    public void testRegister2MobileFourTimesChangeFromEmailToPhone() throws Exception {
        User user = userRepository.findOneByLogin(BEECOW_USER_LOGIN).get();
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String password = "password";
        String displayName = "joe";
        String email = displayName + "@example.com";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            null,                  // login
            password,               // password
            null,              // firstName
            null,               // lastName
            email,      // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        // 1st time
        restMvc.perform(
            post("/api/register2/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.login").value(email))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.displayName").value(displayName))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.mobile").value(nullValue()))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST))
            .andExpect(jsonPath("$.accessToken").isNotEmpty());

        Optional<User> userOptional = userRepository.findOneByLogin(email);
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(displayName);
        assertThat(userOptional.get().getEmail()).isEqualTo(email);
        assertThat(userOptional.get().getMobile()).isNull();

        // 2nd time
        validUser = new ManagedUserVM(
            null,                   // id
            null,                   // login
            password,               // password
            null,                   // firstName
            null,                   // lastName
            null,                   // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("0123456789");
        validUser.setMobile(mobileDTO);
        String mobileAsString = mobileDTO.toString();

        restMvc.perform(
            post("/api/register2/mobile/phone")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value(mobileAsString))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.MOBILE.toString()))
            .andExpect(jsonPath("$.displayName").value(mobileDTO.getPhoneNumber()))
            .andExpect(jsonPath("$.email").value(nullValue()))
            .andExpect(jsonPath("$.mobile.countryCode").value("+84"))
            .andExpect(jsonPath("$.mobile.phoneNumber").value("0123456789"))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST))
            .andExpect(jsonPath("$.accessToken").doesNotExist());

        userOptional = userRepository.findOneByLogin(mobileAsString);
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(mobileDTO.getPhoneNumber());
        assertThat(userOptional.get().getEmail()).isNull();
        assertThat(userOptional.get().getMobile()).isEqualTo(mobileDTO.toString());

        // 3rd time
        validUser = new ManagedUserVM(
                null,                   // id
                null,                  // login
                password,               // password
                null,              // firstName
                null,               // lastName
                email,      // e-mail
                true,                   // activated
                "en",                   // langKey
                null,
                null,                   // createdBy
                null,                   // createdDate
                null,                   // lastModifiedBy
                null                    // lastModifiedDate
            );

        restMvc.perform(
            post("/api/register2/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value(email))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.displayName").value(displayName))
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.mobile").value(nullValue()))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST))
            .andExpect(jsonPath("$.accessToken").doesNotExist());

        userOptional = userRepository.findOneByLogin(email);
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(displayName);
        assertThat(userOptional.get().getEmail()).isEqualTo(email);
        assertThat(userOptional.get().getMobile()).isNull();

        // 4th time
        validUser = new ManagedUserVM(
            null,                   // id
            null,                   // login
            password,               // password
            null,                   // firstName
            null,                   // lastName
            null,                   // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        validUser.setMobile(mobileDTO);

        restMvc.perform(
            post("/api/register2/mobile/phone")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value(mobileAsString))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.MOBILE.toString()))
            .andExpect(jsonPath("$.displayName").value(mobileDTO.getPhoneNumber()))
            .andExpect(jsonPath("$.email").value(nullValue()))
            .andExpect(jsonPath("$.mobile.countryCode").value("+84"))
            .andExpect(jsonPath("$.mobile.phoneNumber").value("0123456789"))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST))
            .andExpect(jsonPath("$.accessToken").doesNotExist());

        userOptional = userRepository.findOneByLogin(mobileAsString);
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(mobileDTO.getPhoneNumber());
        assertThat(userOptional.get().getEmail()).isNull();
        assertThat(userOptional.get().getMobile()).isEqualTo(mobileDTO.toString());
    }

    @Test
    @Transactional
    public void testRegister2MobileInvalidLogin() throws Exception {
        User user = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String password = "password";
        ManagedUserVM invalidUser = new ManagedUserVM(
            null,                   // id
            "funky-log!n",          // login <-- invalid
            password,               // password
            "Funky",                // firstName
            "One",                  // lastName
            "funky@example.com",    // e-mail
            true,                   // activated
            "en",                   // langKey
            new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register2/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());

        Optional<User> userOptional = userRepository.findOneByEmail("funky@example.com");
        assertThat(userOptional.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegister2MobileInvalidEmail() throws Exception {
        User user = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String password = "password";
        ManagedUserVM invalidUser = new ManagedUserVM(
            null,               // id
            "bob",              // login
            password,           // password
            "Bob",              // firstName
            "Green",            // lastName
            "invalid",          // e-mail <-- invalid
            true,               // activated
            "en",               // langKey
            new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            null,               // createdBy
            null,               // createdDate
            null,               // lastModifiedBy
            null                // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register2/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());

        Optional<User> userOptional = userRepository.findOneByLogin("bob");
        assertThat(userOptional.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegister2MobileInvalidPassword() throws Exception {
        User user = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String password = "123";
        ManagedUserVM invalidUser = new ManagedUserVM(
            null,               // id
            "bob",              // login
            password,           // password with only 3 digits
            "Bob",              // firstName
            "Green",            // lastName
            "bob@example.com",  // e-mail
            true,               // activated
            "en",               // langKey
            new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)),
            null,               // createdBy
            null,               // createdDate
            null,               // lastModifiedBy
            null                // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register2/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());

        Optional<User> userOptional = userRepository.findOneByLogin("bob");
        assertThat(userOptional.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegister2MobileWithoutGuestUser() throws Exception {
        String firstName = "Joe";
        String lastName = "Shmoe";
        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            firstName,              // firstName
            lastName,               // lastName
            "joe@example.com",      // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register2/mobile")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.login").value("joe"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.firstName").value("Joe"))
            .andExpect(jsonPath("$.lastName").value("Shmoe"))
            .andExpect(jsonPath("$.displayName").value("Joe Shmoe"))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST));

        Optional<User> userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        ZonedDateTime nextHour = ZonedDateTime.now().plusHours(1);
        User user = userOptional.get();
        assertThat(user.getActivationExpiredDate()).isAfter(nextHour.minusMinutes(15)).isBefore(nextHour.plusMinutes(15));
    }

    @Test
    @Transactional
    public void testRegister2MobileTwice() throws Exception {
        User user = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String firstName = "Joe";
        String lastName = "Shmoe";
        String email = "joe@example.com";
        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            firstName,              // firstName
            lastName,               // lastName
            email,                  // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        // Register the first time
        restMvc.perform(
            post("/api/register2/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk());

        Optional<User> userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        userOptional.ifPresent(user1 -> {
            assertThat(user1.getId()).isEqualTo(user.getId());
            assertThat(user1.getEmail()).isEqualTo(email);
            assertThat(user1.getDisplayName()).isEqualTo(firstName + " " + lastName);
        });

        // Register the second time
        restMvc.perform(
            post("/api/register2/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk());

        userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        userOptional.ifPresent(user1 -> {
            assertThat(user1.getId()).isEqualTo(user.getId());
            assertThat(user1.getEmail()).isEqualTo(email);
            assertThat(user1.getDisplayName()).isEqualTo(firstName + " " + lastName);
        });
    }

    @Test
    @Transactional
    public void testRegister2MobileTwiceChangeEmail() throws Exception {
        User user = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String firstName = "Joe";
        String lastName = "Shmoe";
        String email = "joe@example.com";
        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            firstName,              // firstName
            lastName,               // lastName
            email,                  // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        // Register the first time
        restMvc.perform(
            post("/api/register2/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk());

        Optional<User> userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        userOptional.ifPresent(user1 -> {
            assertThat(user1.getId()).isEqualTo(user.getId());
            assertThat(user1.getEmail()).isEqualTo(email);
            assertThat(user1.getDisplayName()).isEqualTo(firstName + " " + lastName);
        });

        // Register the second time
        String email2 = "joe2@example.com";
        validUser.setEmail(email2);
        restMvc.perform(
            post("/api/register2/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk());

        userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        userOptional.ifPresent(user1 -> {
            assertThat(user1.getId()).isEqualTo(user.getId());
            assertThat(user1.getEmail()).isEqualTo(email2);
            assertThat(user1.getDisplayName()).isEqualTo(firstName + " " + lastName);
        });
    }

    @Test
    @Transactional
    public void testRegister2MobileTwiceChangeEmail2() throws Exception {
        User user = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String displayName = "joe";
        String email = displayName + "@example.com";
        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            null,              // firstName
            null,               // lastName
            email,                  // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        // Register the first time
        restMvc.perform(
            post("/api/register2/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk());

        Optional<User> userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        userOptional.ifPresent(user1 -> {
            assertThat(user1.getId()).isEqualTo(user.getId());
            assertThat(user1.getEmail()).isEqualTo(email);
            assertThat(user1.getDisplayName()).isEqualTo(displayName);
        });

        // Register the second time
        String displayName2 = "joe2";
        String email2 = displayName2 + "@example.com";
        validUser.setEmail(email2);
        restMvc.perform(
            post("/api/register2/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk());

        userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        userOptional.ifPresent(user1 -> {
            assertThat(user1.getId()).isEqualTo(user.getId());
            assertThat(user1.getEmail()).isEqualTo(email2);
            assertThat(user1.getDisplayName()).isEqualTo(displayName2);
        });
    }

    @Test
    @Transactional
    public void testRegister2MobileTwiceChangePassword() throws Exception {
        User user = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String firstName = "Joe";
        String lastName = "Shmoe";
        String email = "joe@example.com";
        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            firstName,              // firstName
            lastName,               // lastName
            email,                  // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        // Register the first time
        restMvc.perform(
            post("/api/register2/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk());

        Optional<User> userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        userOptional.ifPresent(user1 -> {
            assertThat(user1.getId()).isEqualTo(user.getId());
            assertThat(user1.getEmail()).isEqualTo(email);
            assertThat(user1.getDisplayName()).isEqualTo(firstName + " " + lastName);
        });

        // Register the second time
        String password2 = "password2";
        validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password2,              // password
            firstName,              // firstName
            lastName,               // lastName
            email,                  // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        restMvc.perform(
            post("/api/register2/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk());

        userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        userOptional.ifPresent(user1 -> {
            assertThat(user1.getId()).isEqualTo(user.getId());
            assertThat(user1.getEmail()).isEqualTo(email);
            assertThat(user1.getDisplayName()).isEqualTo(firstName + " " + lastName);
        });
    }

    @Test
    @Transactional
    public void testRegister2MobileGuestEmailExisted() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.USER);
        authorities.add(authority);
        String email = "john.doe@jhipter.com";
        User user = new User();
        user.setLogin("test");
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setAccountType(AccountType.EMAIL);
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail(email);
        user.setAuthorities(authorities);
        userRepository.saveAndFlush(user);

        User guest = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(guest.getLogin(), false);

        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            "Joe",                  // firstName
            "Shmoe",                // lastName
            email,                  // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register2/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isConflict());
    }

    @Test
    @Transactional
    public void testRegister2MobileGuestEmailExistedWaitingForActivate() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.GUEST);
        authorities.add(authority);
        String email = "john.doe@jhipter.com";
        User user = new User();
        user.setLogin("test");
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setAccountType(AccountType.EMAIL);
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail(email);
        user.setAuthorities(authorities);
        user.setActivationKey("somecode");
        user.setActivationExpiredDate(ZonedDateTime.now().plusDays(1));
        userRepository.saveAndFlush(user);

        User guest = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(guest.getLogin(), false);

        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            "Joe",                  // firstName
            "Shmoe",                // lastName
            email,                  // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register2/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isConflict());

        Optional<User> userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegister2MobileUserEmailExisted() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.USER);
        authorities.add(authority);
        String email = "john.doe@jhipter.com";
        User user = new User();
        user.setLogin("test");
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setAccountType(AccountType.EMAIL);
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail(email);
        user.setAuthorities(authorities);
        userRepository.saveAndFlush(user);

        User guest = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(guest.getLogin(), false);

        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            password,               // password
            "Joe",                  // firstName
            "Shmoe",                // lastName
            email,                  // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register2/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isConflict());
    }

    @Test
    @Transactional
    public void testRegister2MobileUserLoginExisted() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.USER);
        authorities.add(authority);
        String login = "test";
        User user = new User();
        user.setLogin(login);
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setAccountType(AccountType.EMAIL);
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail("john.doe@jhipter.com");
        user.setAuthorities(authorities);
        userRepository.saveAndFlush(user);

        User guest = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(guest.getLogin(), false);

        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            login,                  // login
            password,               // password
            "Joe",                  // firstName
            "Shmoe",                // lastName
            "john.doe2@jhipter.com",    // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );

        restMvc.perform(
            post("/api/register2/mobile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isConflict());
    }

    @Test
    @Transactional
    public void testRegister2MobileByPhone() throws Exception {
        User user = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            null,                   // login
            password,               // password
            null,                   // firstName
            null,                   // lastName
            null,                   // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("0123456789");
        validUser.setMobile(mobileDTO);

        restMvc.perform(
            post("/api/register2/mobile/phone")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value(mobileDTO.toString()))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.MOBILE.toString()))
            .andExpect(jsonPath("$.displayName").value(mobileDTO.getPhoneNumber().toString()))
            .andExpect(jsonPath("$.mobile.countryCode").value(mobileDTO.getCountryCode()))
            .andExpect(jsonPath("$.mobile.phoneNumber").value(mobileDTO.getPhoneNumber()))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST));

        Optional<User> userOptional = userRepository.findOneByLogin(mobileDTO.toString());
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(mobileDTO.getPhoneNumber());
    }

    @Test
    @Transactional
    public void testRegister2MobileByPhoneWithBeecowUser() throws Exception {
        User user = userRepository.findOneByLogin(BEECOW_USER_LOGIN).get();
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            null,                   // login
            password,               // password
            null,                   // firstName
            null,                   // lastName
            null,                   // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("0123456789");
        validUser.setMobile(mobileDTO);

        restMvc.perform(
            post("/api/register2/mobile/phone")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.login").value(mobileDTO.toString()))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.MOBILE.toString()))
            .andExpect(jsonPath("$.displayName").value(mobileDTO.getPhoneNumber().toString()))
            .andExpect(jsonPath("$.mobile.countryCode").value(mobileDTO.getCountryCode()))
            .andExpect(jsonPath("$.mobile.phoneNumber").value(mobileDTO.getPhoneNumber()))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST));

        Optional<User> userOptional = userRepository.findOneByLogin(mobileDTO.toString());
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(mobileDTO.getPhoneNumber());
    }

    @Test
    @Transactional
    public void testRegister2MobileByPhoneTwiceWithBeecowUser() throws Exception {
        User user = userRepository.findOneByLogin(BEECOW_USER_LOGIN).get();
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            null,                   // login
            password,               // password
            null,                   // firstName
            null,                   // lastName
            null,                   // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("0123456789");
        validUser.setMobile(mobileDTO);

        ResultActions result = restMvc.perform(
            post("/api/register2/mobile/phone")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.login").value(mobileDTO.toString()))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.MOBILE.toString()))
            .andExpect(jsonPath("$.displayName").value(mobileDTO.getPhoneNumber().toString()))
            .andExpect(jsonPath("$.mobile.countryCode").value(mobileDTO.getCountryCode()))
            .andExpect(jsonPath("$.mobile.phoneNumber").value(mobileDTO.getPhoneNumber()))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST));

        Optional<User> userOptional = userRepository.findOneByLogin(mobileDTO.toString());
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(mobileDTO.getPhoneNumber());

        String response = result.andReturn().getResponse().getContentAsString();
        Map<String, Object> resultProps = CommonTestUtil.convertJsonToMap(response);
        token = (String) resultProps.get("accessToken");
        restMvc.perform(
            post("/api/register2/mobile/phone")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value(mobileDTO.toString()))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.MOBILE.toString()))
            .andExpect(jsonPath("$.displayName").value(mobileDTO.getPhoneNumber().toString()))
            .andExpect(jsonPath("$.mobile.countryCode").value(mobileDTO.getCountryCode()))
            .andExpect(jsonPath("$.mobile.phoneNumber").value(mobileDTO.getPhoneNumber()))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST));

        userOptional = userRepository.findOneByLogin(mobileDTO.toString());
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(mobileDTO.getPhoneNumber());
    }

    @Test
    @Transactional
    public void testRegister2MobileByPhoneTwiceChangePhoneNumberWithBeecowUser() throws Exception {
        User user = userRepository.findOneByLogin(BEECOW_USER_LOGIN).get();
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            null,                   // login
            password,               // password
            null,                   // firstName
            null,                   // lastName
            null,                   // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("0123456789");
        validUser.setMobile(mobileDTO);

        ResultActions result = restMvc.perform(
            post("/api/register2/mobile/phone")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.login").value(mobileDTO.toString()))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.MOBILE.toString()))
            .andExpect(jsonPath("$.displayName").value(mobileDTO.getPhoneNumber().toString()))
            .andExpect(jsonPath("$.mobile.countryCode").value(mobileDTO.getCountryCode()))
            .andExpect(jsonPath("$.mobile.phoneNumber").value(mobileDTO.getPhoneNumber()))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST));

        Optional<User> userOptional = userRepository.findOneByLogin(mobileDTO.toString());
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(mobileDTO.getPhoneNumber());

        PhoneDTO otherMobileDTO = new PhoneDTO();
        otherMobileDTO.setCountryCode("+84");
        otherMobileDTO.setPhoneNumber("0987654321");
        validUser.setMobile(otherMobileDTO);
        String response = result.andReturn().getResponse().getContentAsString();
        Map<String, Object> resultProps = CommonTestUtil.convertJsonToMap(response);
        token = (String) resultProps.get("accessToken");
        restMvc.perform(
            post("/api/register2/mobile/phone")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value(otherMobileDTO.toString()))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.MOBILE.toString()))
            .andExpect(jsonPath("$.displayName").value(otherMobileDTO.getPhoneNumber().toString()))
            .andExpect(jsonPath("$.mobile.countryCode").value(otherMobileDTO.getCountryCode()))
            .andExpect(jsonPath("$.mobile.phoneNumber").value(otherMobileDTO.getPhoneNumber()))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST));

        userOptional = userRepository.findOneByLogin(otherMobileDTO.toString());
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(otherMobileDTO.getPhoneNumber());
    }

    @Test
    @Transactional
    public void testRegister2MobileByPhoneTwiceChangePasswordWithBeecowUser() throws Exception {
        User user = userRepository.findOneByLogin(BEECOW_USER_LOGIN).get();
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            null,                   // login
            password,               // password
            null,                   // firstName
            null,                   // lastName
            null,                   // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("0123456789");
        validUser.setMobile(mobileDTO);

        ResultActions result = restMvc.perform(
            post("/api/register2/mobile/phone")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.login").value(mobileDTO.toString()))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.MOBILE.toString()))
            .andExpect(jsonPath("$.displayName").value(mobileDTO.getPhoneNumber().toString()))
            .andExpect(jsonPath("$.mobile.countryCode").value(mobileDTO.getCountryCode()))
            .andExpect(jsonPath("$.mobile.phoneNumber").value(mobileDTO.getPhoneNumber()))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST));

        Optional<User> userOptional = userRepository.findOneByLogin(mobileDTO.toString());
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(mobileDTO.getPhoneNumber());

        String password2 = "password2";
        validUser = new ManagedUserVM(
                null,                   // id
                null,                   // login
                password2,              // password
                null,                   // firstName
                null,                   // lastName
                null,                   // e-mail
                true,                   // activated
                "en",                   // langKey
                null,
                null,                   // createdBy
                null,                   // createdDate
                null,                   // lastModifiedBy
                null                    // lastModifiedDate
            );
        validUser.setMobile(mobileDTO);

        String response = result.andReturn().getResponse().getContentAsString();
        Map<String, Object> resultProps = CommonTestUtil.convertJsonToMap(response);
        token = (String) resultProps.get("accessToken");
        restMvc.perform(
            post("/api/register2/mobile/phone")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value(mobileDTO.toString()))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.MOBILE.toString()))
            .andExpect(jsonPath("$.displayName").value(mobileDTO.getPhoneNumber().toString()))
            .andExpect(jsonPath("$.mobile.countryCode").value(mobileDTO.getCountryCode()))
            .andExpect(jsonPath("$.mobile.phoneNumber").value(mobileDTO.getPhoneNumber()))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST));

        userOptional = userRepository.findOneByLogin(mobileDTO.toString());
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(mobileDTO.getPhoneNumber());
    }

    @Test
    @Transactional
    public void testRegister2MobileByPhoneInvalidCountryCode() throws Exception {
        User user = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String password = "password";
        ManagedUserVM invalidUser = new ManagedUserVM(
            null,                   // id
            null,                   // login
            password,               // password
            null,                   // firstName
            null,                   // lastName
            null,                   // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("invalid");
        mobileDTO.setPhoneNumber("0123456789");
        invalidUser.setMobile(mobileDTO);

        restMvc.perform(
            post("/api/register2/mobile/phone")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());

        Optional<User> userOptional = userRepository.findOneByLogin(mobileDTO.toString());
        assertThat(userOptional.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegister2MobileByPhoneInvalidPhoneNumber() throws Exception {
        User user = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String password = "password";
        ManagedUserVM invalidUser = new ManagedUserVM(
            null,                   // id
            null,                   // login
            password,               // password
            null,                   // firstName
            null,                   // lastName
            null,                   // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("invalid");
        invalidUser.setMobile(mobileDTO);

        restMvc.perform(
            post("/api/register2/mobile/phone")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());

        Optional<User> userOptional = userRepository.findOneByLogin(mobileDTO.toString());
        assertThat(userOptional.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegister2MobileByPhoneInvalidPassword() throws Exception {
        User user = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String password = "123";
        ManagedUserVM invalidUser = new ManagedUserVM(
            null,                   // id
            null,                   // login
            password,               // password
            null,                   // firstName
            null,                   // lastName
            null,                   // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("0123456789");
        invalidUser.setMobile(mobileDTO);

        restMvc.perform(
            post("/api/register2/mobile/phone")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());

        Optional<User> userOptional = userRepository.findOneByLogin(mobileDTO.toString());
        assertThat(userOptional.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testRegister2MobileByPhoneWithoutGuestUser() throws Exception {
        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            null,                   // login
            password,               // password
            null,                   // firstName
            null,                   // lastName
            null,                   // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("0123456789");
        validUser.setMobile(mobileDTO);

        restMvc.perform(
            post("/api/register2/mobile/phone")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.login").value(mobileDTO.toString()))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.MOBILE.toString()))
            .andExpect(jsonPath("$.displayName").value(mobileDTO.getPhoneNumber().toString()))
            .andExpect(jsonPath("$.mobile.countryCode").value(mobileDTO.getCountryCode()))
            .andExpect(jsonPath("$.mobile.phoneNumber").value(mobileDTO.getPhoneNumber()))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST));

        Optional<User> userOptional = userRepository.findOneByLogin(mobileDTO.toString());
        assertThat(userOptional.isPresent()).isTrue();
        ZonedDateTime nextHour = ZonedDateTime.now().plusHours(1);
        User user = userOptional.get();
        assertThat(user.getActivationExpiredDate()).isAfter(nextHour.minusMinutes(15)).isBefore(nextHour.plusMinutes(15));
    }

    @Test
    @Transactional
    public void testRegister2MobileByPhoneTwice() throws Exception {
        User user = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            null,                   // login
            password,               // password
            null,                   // firstName
            null,                   // lastName
            null,                   // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("0123456789");
        validUser.setMobile(mobileDTO);

        // Register the first time
        restMvc.perform(
            post("/api/register2/mobile/phone")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk());

        Optional<User> userOptional = userRepository.findOneByLogin(mobileDTO.toString());
        assertThat(userOptional.isPresent()).isTrue();
        userOptional.ifPresent(user1 -> {
            assertThat(user1.getId()).isEqualTo(user.getId());
            assertThat(user1.getMobile()).isEqualTo(mobileDTO.toString());
            assertThat(user1.getDisplayName()).isEqualTo(mobileDTO.getPhoneNumber());
        });

        // Register the second time
        restMvc.perform(
            post("/api/register2/mobile/phone")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk());

        userOptional = userRepository.findOneByLogin(mobileDTO.toString());
        assertThat(userOptional.isPresent()).isTrue();
        userOptional.ifPresent(user1 -> {
            assertThat(user1.getId()).isEqualTo(user.getId());
            assertThat(user1.getMobile()).isEqualTo(mobileDTO.toString());
            assertThat(user1.getDisplayName()).isEqualTo(mobileDTO.getPhoneNumber());
        });
    }

    @Test
    @Transactional
    public void testRegister2MobileByPhoneTwiceChangeEmail() throws Exception {
        User user = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            null,                   // login
            password,               // password
            null,                   // firstName
            null,                   // lastName
            null,                   // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("0123456789");
        validUser.setMobile(mobileDTO);

        // Register the first time
        restMvc.perform(
            post("/api/register2/mobile/phone")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk());

        Optional<User> userOptional = userRepository.findOneByLogin(mobileDTO.toString());
        assertThat(userOptional.isPresent()).isTrue();
        userOptional.ifPresent(user1 -> {
            assertThat(user1.getId()).isEqualTo(user.getId());
            assertThat(user1.getMobile()).isEqualTo(mobileDTO.toString());
            assertThat(user1.getDisplayName()).isEqualTo(mobileDTO.getPhoneNumber());
        });

        // Register the second time
        PhoneDTO otherMobileDTO = new PhoneDTO();
        otherMobileDTO.setCountryCode("+84");
        otherMobileDTO.setPhoneNumber("0123456789");
        validUser.setMobile(otherMobileDTO);
        restMvc.perform(
            post("/api/register2/mobile/phone")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk());

        userOptional = userRepository.findOneByLogin(otherMobileDTO.toString());
        assertThat(userOptional.isPresent()).isTrue();
        userOptional.ifPresent(user1 -> {
            assertThat(user1.getId()).isEqualTo(user.getId());
            assertThat(user1.getMobile()).isEqualTo(otherMobileDTO.toString());
            assertThat(user1.getDisplayName()).isEqualTo(otherMobileDTO.getPhoneNumber());
        });
    }

    @Test
    @Transactional
    public void testRegister2MobileByPhoneTwiceChangePassword() throws Exception {
        User user = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(user.getLogin(), false);

        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            null,                   // login
            password,               // password
            null,                   // firstName
            null,                   // lastName
            null,                   // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("0123456789");
        validUser.setMobile(mobileDTO);

        // Register the first time
        restMvc.perform(
            post("/api/register2/mobile/phone")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk());

        Optional<User> userOptional = userRepository.findOneByLogin(mobileDTO.toString());
        assertThat(userOptional.isPresent()).isTrue();
        userOptional.ifPresent(user1 -> {
            assertThat(user1.getId()).isEqualTo(user.getId());
            assertThat(user1.getMobile()).isEqualTo(mobileDTO.toString());
            assertThat(user1.getDisplayName()).isEqualTo(mobileDTO.getPhoneNumber());
        });

        // Register the second time
        String password2 = "password2";
        validUser = new ManagedUserVM(
            null,                   // id
            null,                   // login
            password2,               // password
            null,                   // firstName
            null,                   // lastName
            null,                   // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        validUser.setMobile(mobileDTO);
        restMvc.perform(
            post("/api/register2/mobile/phone")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk());

        userOptional = userRepository.findOneByLogin(mobileDTO.toString());
        assertThat(userOptional.isPresent()).isTrue();
        userOptional.ifPresent(user1 -> {
            assertThat(user1.getId()).isEqualTo(user.getId());
            assertThat(user1.getMobile()).isEqualTo(mobileDTO.toString());
            assertThat(user1.getDisplayName()).isEqualTo(mobileDTO.getPhoneNumber());
        });
    }

    @Test
    @Transactional
    public void testRegister2MobileByPhoneExisted() throws Exception {
        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("0123456789");
        User user = new User();
        user.setLogin(mobileDTO.toString());
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setAccountType(AccountType.MOBILE);
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail("joe@example.com");
        user.setMobileObject(mobileDTO);
        user.setActivated(true);
        user.setAuthorities(new HashSet<>(Arrays.asList(Authority.USER)));
        user = userRepository.saveAndFlush(user);

        User guest = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(guest.getLogin(), false);

        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            null,                   // login
            password,               // password
            null,                   // firstName
            null,                   // lastName
            null,                   // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        validUser.setMobile(mobileDTO);

        restMvc.perform(
            post("/api/register2/mobile/phone")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isConflict());
    }

    @Test
    @Transactional
    public void testRegister2MobileByPhoneExistedWaitingForActivate() throws Exception {
        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("0123456789");
        User user = new User();
        user.setLogin(mobileDTO.toString());
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setAccountType(AccountType.EMAIL);
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail("joe@example.com");
        user.setMobileObject(mobileDTO);
        user.setActivated(true);
        user.setAuthorities(new HashSet<>(Arrays.asList(Authority.GUEST)));
        user.setActivationKey("somecode");
        user.setActivationExpiredDate(ZonedDateTime.now().plusDays(1));
        user = userRepository.saveAndFlush(user);

        User guest = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(guest.getLogin(), false);

        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            null,                   // login
            password,               // password
            null,                   // firstName
            null,                   // lastName
            null,                   // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        validUser.setMobile(mobileDTO);

        restMvc.perform(
            post("/api/register2/mobile/phone")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isConflict());
    }

    @Test
    @Transactional
    public void testRegister2MobileByPhoneExistedAndActivationKeyExpired() throws Exception {
        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("0123456789");
        User user = new User();
        user.setLogin(mobileDTO.toString());
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail("joe@example.com");
        user.setMobileObject(mobileDTO);
        user.setAccountType(AccountType.MOBILE);
        user.setLangKey("vi");
        user.setLocationCode("VN-SG");
        user.setActivated(false);
        user.setAuthorities(new HashSet<>(Arrays.asList(Authority.GUEST)));
        user.setActivationKey("somecode");
        user.setActivationExpiredDate(ZonedDateTime.now().minusDays(1));
        user = userRepository.saveAndFlush(user);

        User guest = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(guest.getLogin(), false);

        String password = "password";
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            null,                   // login
            password,               // password
            null,                   // firstName
            null,                   // lastName
            null,                   // e-mail
            true,                   // activated
            "en",                   // langKey
            null,
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null                    // lastModifiedDate
        );
        validUser.setMobile(mobileDTO);

        restMvc.perform(post("/api/register2/mobile/phone")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value(mobileDTO.toString()))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.MOBILE.toString()))
            .andExpect(jsonPath("$.displayName").value(mobileDTO.getPhoneNumber()))
            .andExpect(jsonPath("$.email").value(nullValue()))
            .andExpect(jsonPath("$.mobile.countryCode").value("+84"))
            .andExpect(jsonPath("$.mobile.phoneNumber").value("0123456789"))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST))
            .andExpect(jsonPath("$.accessToken").doesNotExist());

        Optional<User> userOptional = userRepository.findOneByLogin(mobileDTO.toString());
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getId()).isNotEqualTo(user.getId());
        assertThat(userOptional.get().getDisplayName()).isEqualTo(mobileDTO.getPhoneNumber());
        assertThat(userOptional.get().getEmail()).isNull();
        assertThat(userOptional.get().getMobile()).isEqualTo(mobileDTO.toString());
    }

    @Test
    @Transactional
    public void testSaveUser() throws Exception {
        String login = "test";
        User user = new User();
        user.setLogin(login);
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setAccountType(AccountType.EMAIL);
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail("joe@example.com");
        user.setActivated(true);
        user.setAuthorities(new HashSet<>(Arrays.asList(Authority.USER)));
        user = userRepository.saveAndFlush(user);
        String token = tokenProviderService.createToken(login, false);

        UserDTO userDTO = userMapper.userToUserDTO(user);
        userDTO.setFirstName("newjohn");
        userDTO.setDisplayName(null); // Simulate initial condition

        restMvc.perform(
            post("/api/account")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(userDTO)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value("test"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.firstName").value("newjohn"))
            .andExpect(jsonPath("$.lastName").value("doe"))
            .andExpect(jsonPath("$.displayName").value("newjohn doe"))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.USER));

        Optional<User> userOptional = userRepository.findOneByEmail("joe@example.com");
        assertThat(userOptional.isPresent()).isTrue();
    }
    
    @Test
    @Transactional
    public void testSaveUserWhenExistFBAccount() throws Exception {
        String socialLogin = SocialService.FACEBOOK_LOGIN_PREFIX + "joe@example.com";
        User socialUser = new User();
        socialUser.setLogin(socialLogin);
        socialUser.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        socialUser.setFirstName("john");
        socialUser.setLastName("doe");
        socialUser.setDisplayName("john doe");
        socialUser.setActivated(true);
        socialUser.setAuthorities(new HashSet<>(Arrays.asList(Authority.USER)));
        socialUser.setAccountType(AccountType.FACEBOOK);
        socialUser = userRepository.saveAndFlush(socialUser);
        
        String login = "test";
        User user = new User();
        user.setLogin(login);
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setAccountType(AccountType.EMAIL);
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail("joe@example.com");
        user.setActivated(true);
        user.setAuthorities(new HashSet<>(Arrays.asList(Authority.USER)));
        user = userRepository.saveAndFlush(user);
        String token = tokenProviderService.createToken(login, false);

        UserDTO userDTO = userMapper.userToUserDTO(user);
        userDTO.setFirstName("newjohn");
        userDTO.setDisplayName(null); // Simulate initial condition

        restMvc.perform(
            post("/api/account")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(userDTO)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value("test"))
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.firstName").value("newjohn"))
            .andExpect(jsonPath("$.lastName").value("doe"))
            .andExpect(jsonPath("$.displayName").value("newjohn doe"))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.USER));

        Optional<User> userOptional = userRepository.findOneByEmail("joe@example.com");
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getFirstName()).isEqualTo("newjohn");
    }

    @Test
    @Transactional
    public void testSaveInvalidLogin() throws Exception {
        UserDTO invalidUser = new UserDTO(
            "funky-log!n",          // login <-- invalid
            "Funky",                // firstName
            "One",                  // lastName
            "funky@example.com",    // e-mail
            true,                   // activated
            "en",                   // langKey
            new HashSet<>(Arrays.asList(AuthoritiesConstants.USER))
        );

        restMvc.perform(
            post("/api/account")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());

        Optional<User> user = userRepository.findOneByEmail("funky@example.com");
        assertThat(user.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void testSaveUser2() throws Exception {
        String login = "test";
        User user = new User();
        user.setLogin(login);
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail("joe@example.com");
        user.setActivated(true);
        user.setAuthorities(new HashSet<>(Arrays.asList(Authority.USER)));
        user.setAccountType(AccountType.EMAIL);
        user = userRepository.saveAndFlush(user);
        String token = tokenProviderService.createToken(login, false);

        Map<String, Object> userProps = new HashMap<>();
        userProps.put("firstName", "newjohn");
        userProps.put("lastName", "newdoe");
        Map<String, Object> avatar = new HashMap<>();
        avatar.put("imageId", 0);
        avatar.put("urlPrefix", "TEST_URL");
        userProps.put("avatarUrl", avatar);
        userProps.put("dateOfBirth", "2000-10-15T00:00:00Z");
        userProps.put("gender", "MALE");
        userProps.put("locationCode", "vn-sg");
        userProps.put("langKey", "vi");

        restMvc.perform(
            post("/api/account2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(userProps)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(user.getId().intValue()))
            .andExpect(jsonPath("$.login").value("test"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.firstName").value("newjohn"))
            .andExpect(jsonPath("$.lastName").value("newdoe"))
            .andExpect(jsonPath("$.displayName").value("newjohn newdoe"))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(0))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value("TEST_URL"))
            .andExpect(jsonPath("$.dateOfBirth").value("2000-10-15T00:00:00Z"))
            .andExpect(jsonPath("$.gender").value("MALE"))
            .andExpect(jsonPath("$.locationCode").value("VN-SG"))
            .andExpect(jsonPath("$.langKey").value("vi"))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.USER));

        Optional<User> userOptional = userRepository.findOneByEmail("joe@example.com");
        assertThat(userOptional.isPresent()).isTrue();
    }

    @Test
    @Transactional
    public void testSaveUser2SocialAccount() throws Exception {
        String login = SocialService.FACEBOOK_LOGIN_PREFIX + "test";
        User user = new User();
        user.setLogin(login);
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setAccountType(AccountType.FACEBOOK);
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setActivated(true);
        user.setAuthorities(new HashSet<>(Arrays.asList(Authority.USER)));
        user = userRepository.saveAndFlush(user);
        String token = tokenProviderService.createToken(login, false);

        Map<String, Object> userProps = new HashMap<>();
        userProps.put("firstName", "newjohn");
        userProps.put("lastName", "newdoe");
        Map<String, Object> avatar = new HashMap<>();
        avatar.put("imageId", 0);
        avatar.put("urlPrefix", "TEST_URL");
        userProps.put("avatarUrl", avatar);
        userProps.put("dateOfBirth", "2000-10-15T00:00:00Z");
        userProps.put("gender", "MALE");
        userProps.put("locationCode", "vn-sg");
        userProps.put("langKey", "vi");

        restMvc.perform(
            post("/api/account2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(userProps)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(user.getId().intValue()))
            .andExpect(jsonPath("$.login").value(login))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.FACEBOOK.toString()))
            .andExpect(jsonPath("$.firstName").value("newjohn"))
            .andExpect(jsonPath("$.lastName").value("newdoe"))
            .andExpect(jsonPath("$.displayName").value("newjohn newdoe"))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(0))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value("TEST_URL"))
            .andExpect(jsonPath("$.dateOfBirth").value("2000-10-15T00:00:00Z"))
            .andExpect(jsonPath("$.gender").value("MALE"))
            .andExpect(jsonPath("$.locationCode").value("VN-SG"))
            .andExpect(jsonPath("$.langKey").value("vi"))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.USER));

        Optional<User> userOptional = userRepository.findOneByLogin(login);
        assertThat(userOptional.isPresent()).isTrue();
    }

    @Test
    @Transactional
    public void testSaveUser2FirstName() throws Exception {
        String login = "test";
        User user = new User();
        user.setLogin(login);
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setAccountType(AccountType.EMAIL);
        user.setFirstName("john");
        user.setLastName("doe");
        user.setEmail("joe@example.com");
        user.setDisplayName("john doe");
        user.setActivated(true);
        user.setAuthorities(new HashSet<>(Arrays.asList(Authority.USER)));
        user = userRepository.saveAndFlush(user);
        String token = tokenProviderService.createToken(login, false);

        Map<String, Object> userProps = new HashMap<>();
        userProps.put("firstName", "newjohn");

        restMvc.perform(
            post("/api/account2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(userProps)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(user.getId().intValue()))
            .andExpect(jsonPath("$.login").value("test"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.firstName").value("newjohn"))
            .andExpect(jsonPath("$.lastName").value("doe"))
            .andExpect(jsonPath("$.displayName").value("newjohn doe"))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.dateOfBirth").value(nullValue()))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.locationCode").doesNotExist())
            .andExpect(jsonPath("$.langKey").doesNotExist())
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.USER));

        Optional<User> userOptional = userRepository.findOneByEmail("joe@example.com");
        assertThat(userOptional.isPresent()).isTrue();
    }

    @Test
    @Transactional
    public void testSaveUser2LastName() throws Exception {
        String login = "test";
        User user = new User();
        user.setLogin(login);
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setAccountType(AccountType.EMAIL);
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail("joe@example.com");
        user.setActivated(true);
        user.setAuthorities(new HashSet<>(Arrays.asList(Authority.USER)));
        user = userRepository.saveAndFlush(user);
        String token = tokenProviderService.createToken(login, false);

        Map<String, Object> userProps = new HashMap<>();
        userProps.put("lastName", "newdoe");

        restMvc.perform(
            post("/api/account2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(userProps)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(user.getId().intValue()))
            .andExpect(jsonPath("$.login").value("test"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.firstName").value("john"))
            .andExpect(jsonPath("$.lastName").value("newdoe"))
            .andExpect(jsonPath("$.displayName").value("john newdoe"))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.dateOfBirth").value(nullValue()))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.locationCode").doesNotExist())
            .andExpect(jsonPath("$.langKey").doesNotExist())
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.USER));

        Optional<User> userOptional = userRepository.findOneByEmail("joe@example.com");
        assertThat(userOptional.isPresent()).isTrue();
    }

    @Test
    @Transactional
    public void testSaveUser2DisplayName() throws Exception {
        String login = "test";
        User user = new User();
        user.setLogin(login);
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setAccountType(AccountType.EMAIL);
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail("joe@example.com");
        user.setActivated(true);
        user.setAuthorities(new HashSet<>(Arrays.asList(Authority.USER)));
        user = userRepository.saveAndFlush(user);
        String token = tokenProviderService.createToken(login, false);

        Map<String, Object> userProps = new HashMap<>();
        userProps.put("displayName", "john hehe");

        restMvc.perform(
            post("/api/account2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(userProps)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(user.getId().intValue()))
            .andExpect(jsonPath("$.login").value("test"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.firstName").value("john"))
            .andExpect(jsonPath("$.lastName").value("doe"))
            .andExpect(jsonPath("$.displayName").value("john hehe"))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.dateOfBirth").value(nullValue()))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.locationCode").doesNotExist())
            .andExpect(jsonPath("$.langKey").doesNotExist())
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.USER));

        Optional<User> userOptional = userRepository.findOneByEmail("joe@example.com");
        assertThat(userOptional.isPresent()).isTrue();
    }

    @Test
    @Transactional
    public void testSaveUser2AvatarUrl() throws Exception {
        String login = "test";
        User user = new User();
        user.setLogin(login);
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setAccountType(AccountType.EMAIL);
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail("joe@example.com");
        user.setActivated(true);
        user.setAuthorities(new HashSet<>(Arrays.asList(Authority.USER)));
        user = userRepository.saveAndFlush(user);
        String token = tokenProviderService.createToken(login, false);

        Map<String, Object> avatar = new HashMap<>();
        avatar.put("imageId", 0);
        avatar.put("urlPrefix", "TEST_URL");
        Map<String, Object> userProps = new HashMap<>();
        userProps.put("avatarUrl", avatar);

        restMvc.perform(
            post("/api/account2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(userProps)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(user.getId().intValue()))
            .andExpect(jsonPath("$.login").value("test"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.firstName").value("john"))
            .andExpect(jsonPath("$.lastName").value("doe"))
            .andExpect(jsonPath("$.displayName").value("john doe"))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(0))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value("TEST_URL"))
            .andExpect(jsonPath("$.dateOfBirth").value(nullValue()))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.locationCode").doesNotExist())
            .andExpect(jsonPath("$.langKey").doesNotExist())
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.USER));

        Optional<User> userOptional = userRepository.findOneByEmail("joe@example.com");
        assertThat(userOptional.isPresent()).isTrue();
    }

    @Test
    @Transactional
    public void testSaveUser2DateOfBirth() throws Exception {
        String login = "test";
        User user = new User();
        user.setLogin(login);
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail("joe@example.com");
        user.setAccountType(AccountType.EMAIL);
        user.setActivated(true);
        user.setAuthorities(new HashSet<>(Arrays.asList(Authority.USER)));
        user = userRepository.saveAndFlush(user);
        String token = tokenProviderService.createToken(login, false);

        Map<String, Object> userProps = new HashMap<>();
        userProps.put("dateOfBirth", "2000-10-15T00:00:00Z");

        restMvc.perform(
            post("/api/account2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(userProps)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(user.getId().intValue()))
            .andExpect(jsonPath("$.login").value("test"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.firstName").value("john"))
            .andExpect(jsonPath("$.lastName").value("doe"))
            .andExpect(jsonPath("$.displayName").value("john doe"))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.dateOfBirth").value("2000-10-15T00:00:00Z"))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.locationCode").doesNotExist())
            .andExpect(jsonPath("$.langKey").doesNotExist())
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.USER));

        Optional<User> userOptional = userRepository.findOneByEmail("joe@example.com");
        assertThat(userOptional.isPresent()).isTrue();
    }

    @Test
    @Transactional
    public void testSaveUser2DateOfBirthFromOtherTimeZone() throws Exception {
        String login = "test";
        User user = new User();
        user.setLogin(login);
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail("joe@example.com");
        user.setAccountType(AccountType.EMAIL);
        user.setActivated(true);
        user.setAuthorities(new HashSet<>(Arrays.asList(Authority.USER)));
        user = userRepository.saveAndFlush(user);
        String token = tokenProviderService.createToken(login, false);

        Map<String, Object> userProps = new HashMap<>();
        userProps.put("dateOfBirth", "2000-10-15T00:00:00+07:00");

        restMvc.perform(
            post("/api/account2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(userProps)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(user.getId().intValue()))
            .andExpect(jsonPath("$.login").value("test"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.firstName").value("john"))
            .andExpect(jsonPath("$.lastName").value("doe"))
            .andExpect(jsonPath("$.displayName").value("john doe"))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.dateOfBirth").value("2000-10-15T00:00:00Z"))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.locationCode").doesNotExist())
            .andExpect(jsonPath("$.langKey").doesNotExist())
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.USER));

        Optional<User> userOptional = userRepository.findOneByEmail("joe@example.com");
        assertThat(userOptional.isPresent()).isTrue();
    }

    @Test
    @Transactional
    public void testSaveUser2Gender() throws Exception {
        String login = "test";
        User user = new User();
        user.setLogin(login);
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setAccountType(AccountType.EMAIL);
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail("joe@example.com");
        user.setActivated(true);
        user.setAuthorities(new HashSet<>(Arrays.asList(Authority.USER)));
        user = userRepository.saveAndFlush(user);
        String token = tokenProviderService.createToken(login, false);

        Map<String, Object> userProps = new HashMap<>();
        userProps.put("gender", "FEMALE");

        restMvc.perform(
            post("/api/account2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(userProps)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(user.getId().intValue()))
            .andExpect(jsonPath("$.login").value("test"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.firstName").value("john"))
            .andExpect(jsonPath("$.lastName").value("doe"))
            .andExpect(jsonPath("$.displayName").value("john doe"))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(2))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_FEMALE_AVATAR))
            .andExpect(jsonPath("$.gender").value(Gender.FEMALE.toString()))
            .andExpect(jsonPath("$.locationCode").doesNotExist())
            .andExpect(jsonPath("$.langKey").doesNotExist())
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.USER));

        Optional<User> userOptional = userRepository.findOneByEmail("joe@example.com");
        assertThat(userOptional.isPresent()).isTrue();
    }

    @Test
    @Transactional
    public void testSaveUser2LocationCode() throws Exception {
        String login = "test";
        User user = new User();
        user.setLogin(login);
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setAccountType(AccountType.EMAIL);
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail("joe@example.com");
        user.setActivated(true);
        user.setAuthorities(new HashSet<>(Arrays.asList(Authority.USER)));
        user = userRepository.saveAndFlush(user);
        String token = tokenProviderService.createToken(login, false);

        Map<String, Object> userProps = new HashMap<>();
        userProps.put("locationCode", "vn-sg");

        restMvc.perform(
            post("/api/account2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(userProps)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(user.getId().intValue()))
            .andExpect(jsonPath("$.login").value("test"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.firstName").value("john"))
            .andExpect(jsonPath("$.lastName").value("doe"))
            .andExpect(jsonPath("$.displayName").value("john doe"))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.locationCode").value("VN-SG"))
            .andExpect(jsonPath("$.langKey").doesNotExist())
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.USER));

        Optional<User> userOptional = userRepository.findOneByEmail("joe@example.com");
        assertThat(userOptional.isPresent()).isTrue();
    }

    @Test
    @Transactional
    public void testSaveUser2LangKey() throws Exception {
        String login = "test";
        User user = new User();
        user.setLogin(login);
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setAccountType(AccountType.EMAIL);
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail("joe@example.com");
        user.setActivated(true);
        user.setAuthorities(new HashSet<>(Arrays.asList(Authority.USER)));
        user = userRepository.saveAndFlush(user);
        String token = tokenProviderService.createToken(login, false);

        Map<String, Object> userProps = new HashMap<>();
        userProps.put("langKey", "VI");

        restMvc.perform(
            post("/api/account2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(userProps)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(user.getId().intValue()))
            .andExpect(jsonPath("$.login").value("test"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.firstName").value("john"))
            .andExpect(jsonPath("$.lastName").value("doe"))
            .andExpect(jsonPath("$.displayName").value("john doe"))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.locationCode").doesNotExist())
            .andExpect(jsonPath("$.langKey").value("vi"))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.USER));

        Optional<User> userOptional = userRepository.findOneByEmail("joe@example.com");
        assertThat(userOptional.isPresent()).isTrue();
    }

    @Test
    @Transactional
    public void testSaveUser2EmailNotUpdated() throws Exception {
        String email = "joe@example.com";
        User user = new User();
        user.setLogin(email);
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setAccountType(AccountType.EMAIL);
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail(email);
        user.setActivated(true);
        user.setAuthorities(new HashSet<>(Arrays.asList(Authority.USER)));
        user = userRepository.saveAndFlush(user);
        String token = tokenProviderService.createToken(email, false);

        Map<String, Object> userProps = new HashMap<>();
        userProps.put("email", "joe2@example.com");

        restMvc.perform(
            post("/api/account2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(userProps)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(user.getId().intValue()))
            .andExpect(jsonPath("$.login").value(email))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.email").value(email)) // Not changed
            .andExpect(jsonPath("$.firstName").value("john"))
            .andExpect(jsonPath("$.lastName").value("doe"))
            .andExpect(jsonPath("$.displayName").value("john doe"))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.locationCode").doesNotExist())
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.USER));

        Optional<User> userOptional = userRepository.findOneByLogin(email);
        assertThat(userOptional.isPresent()).isTrue();
    }

    @Test
    @Transactional
    public void testSaveUser2EmailUpdated() throws Exception {
        String email = "joe@example.com";
        User user = new User();
        user.setLogin(email);
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setAccountType(AccountType.EMAIL);
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail(null);
        user.setActivated(true);
        user.setAuthorities(new HashSet<>(Arrays.asList(Authority.USER)));
        user = userRepository.saveAndFlush(user);
        String token = tokenProviderService.createToken(email, false);

        Map<String, Object> userProps = new HashMap<>();
        userProps.put("email", email);

        restMvc.perform(
            post("/api/account2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(userProps)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(user.getId().intValue()))
            .andExpect(jsonPath("$.login").value(email))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.email").value(email)) // Updated
            .andExpect(jsonPath("$.firstName").value("john"))
            .andExpect(jsonPath("$.lastName").value("doe"))
            .andExpect(jsonPath("$.displayName").value("john doe"))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.locationCode").doesNotExist())
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.USER));

        Optional<User> userOptional = userRepository.findOneByLogin(email);
        assertThat(userOptional.isPresent()).isTrue();
    }

    @Test
    @Transactional
    public void testSaveUser2MobileUpdated() throws Exception {
        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("0123456789");
        String mobile = mobileDTO.toString();
        User user = new User();
        user.setLogin(mobile);
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setAccountType(AccountType.MOBILE);
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail("joe@example.com");
        user.setMobileObject(null);
        user.setActivated(true);
        user.setAuthorities(new HashSet<>(Arrays.asList(Authority.USER)));
        user = userRepository.saveAndFlush(user);
        String token = tokenProviderService.createToken(mobile, false);

        PhoneDTO newMobileDTO = new PhoneDTO();
        newMobileDTO.setCountryCode("+84");
        newMobileDTO.setPhoneNumber("0987654321");
        Map<String, Object> userProps = new HashMap<>();
        userProps.put("mobile", newMobileDTO);

        restMvc.perform(
            post("/api/account2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(userProps)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(user.getId().intValue()))
            .andExpect(jsonPath("$.login").value(mobile))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.MOBILE.toString()))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.mobile.countryCode").value(newMobileDTO.getCountryCode())) // Updated
            .andExpect(jsonPath("$.mobile.phoneNumber").value(newMobileDTO.getPhoneNumber())) // Updated
            .andExpect(jsonPath("$.firstName").value("john"))
            .andExpect(jsonPath("$.lastName").value("doe"))
            .andExpect(jsonPath("$.displayName").value("john doe"))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.locationCode").doesNotExist())
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.USER));

        Optional<User> userOptional = userRepository.findOneByLogin(mobile);
        assertThat(userOptional.isPresent()).isTrue();
    }

    @Test
    @Transactional
    public void testSaveUser2MobileUpdated2() throws Exception {
        PhoneDTO mobileDTO = new PhoneDTO();
        mobileDTO.setCountryCode("+84");
        mobileDTO.setPhoneNumber("0123456789");
        User user = new User();
        user.setLogin("joe");
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setAccountType(AccountType.EMAIL);
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail("joe@example.com");
        user.setMobileObject(mobileDTO);
        user.setActivated(true);
        user.setAuthorities(new HashSet<>(Arrays.asList(Authority.USER)));
        user = userRepository.saveAndFlush(user);
        String token = tokenProviderService.createToken("joe", false);

        PhoneDTO newMobileDTO = new PhoneDTO();
        newMobileDTO.setCountryCode("+84");
        newMobileDTO.setPhoneNumber("0987654321");
        Map<String, Object> userProps = new HashMap<>();
        userProps.put("mobile", newMobileDTO);

        restMvc.perform(
            post("/api/account2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(userProps)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(user.getId().intValue()))
            .andExpect(jsonPath("$.login").value("joe"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.EMAIL.toString()))
            .andExpect(jsonPath("$.email").value("joe@example.com"))
            .andExpect(jsonPath("$.mobile.countryCode").value(newMobileDTO.getCountryCode())) // Updated
            .andExpect(jsonPath("$.mobile.phoneNumber").value(newMobileDTO.getPhoneNumber())) // Updated
            .andExpect(jsonPath("$.firstName").value("john"))
            .andExpect(jsonPath("$.lastName").value("doe"))
            .andExpect(jsonPath("$.displayName").value("john doe"))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.gender").value(Gender.MALE.toString()))
            .andExpect(jsonPath("$.locationCode").doesNotExist())
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.USER));

        Optional<User> userOptional = userRepository.findOneByLogin("joe");
        assertThat(userOptional.isPresent()).isTrue();
        assertThat(userOptional.get().getMobile()).isEqualTo(newMobileDTO.toString());
    }

    @Test
    @Transactional
    public void testSaveUser2Guest() throws Exception {
        User user = userService.createPreavtivateUser("vn-sg", "vi");
        String token = tokenProviderService.createToken(user.getLogin(), false);

        Map<String, Object> userProps = new HashMap<>();
        userProps.put("firstName", "newjohn");
        userProps.put("lastName", "newdoe");
        Map<String, Object> avatar = new HashMap<>();
        avatar.put("imageId", 0);
        avatar.put("urlPrefix", "TEST_URL");
        userProps.put("avatarUrl", avatar);
        userProps.put("dateOfBirth", "2000-10-15T00:00:00Z");
        userProps.put("gender", "MALE");
        userProps.put("locationCode", "vn-hn");
        userProps.put("langKey", "en");

        restMvc.perform(
            post("/api/account2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(userProps)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(user.getId().intValue()))
            .andExpect(jsonPath("$.login").value(user.getLogin()))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.accountType").value(AccountType.PRE_ACTIVATE.toString()))
            .andExpect(jsonPath("$.email").value(nullValue()))
            .andExpect(jsonPath("$.mobile").value(nullValue()))
            .andExpect(jsonPath("$.firstName").value(nullValue()))
            .andExpect(jsonPath("$.lastName").value(nullValue()))
            .andExpect(jsonPath("$.displayName").value(user.getLogin()))
            .andExpect(jsonPath("$.avatarUrl.imageId").value(1))
            .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_MALE_AVATAR))
            .andExpect(jsonPath("$.dateOfBirth").value(nullValue()))
            .andExpect(jsonPath("$.gender").value("MALE"))
            .andExpect(jsonPath("$.locationCode").value("VN-HN")) // expecting that only location-code and lang-key is updated 
            .andExpect(jsonPath("$.langKey").value("en")) // expecting that only location-code and lang-key is updated
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.GUEST));
    }

    @Test
    @Transactional
    public void testSaveUser2Invalid() throws Exception {
        String login = "test";
        User user = new User();
        user.setLogin(login);
        user.setPassword(String.format("%60s", " ").replaceAll(" ", "a"));
        user.setAccountType(AccountType.EMAIL);
        user.setFirstName("john");
        user.setLastName("doe");
        user.setDisplayName("john doe");
        user.setEmail("joe@example.com");
        user.setActivated(true);
        user.setAuthorities(new HashSet<>(Arrays.asList(Authority.USER)));
        user = userRepository.saveAndFlush(user);
        String token = tokenProviderService.createToken(login, false);

        Map<String, Object> userProps = new HashMap<>();
        userProps.put("firstName", "newjohn");
        userProps.put("lastName", "newdoe");
        Map<String, Object> avatar = new HashMap<>();
        avatar.put("imageId", 0);
        avatar.put("urlPrefix", "TEST_URL");
        userProps.put("avatarUrl", avatar);
        userProps.put("dateOfBirth", "INVALID_DATE");
        userProps.put("gender", "MALE");
        userProps.put("locationCode", "vn-sg");
        userProps.put("langKey", "vi");

        restMvc.perform(
            post("/api/account2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(userProps)))
            .andDo(MockMvcResultHandlers.print(System.err))
            .andExpect(status().isBadRequest());

        Optional<User> userOptional = userRepository.findOneByEmail("joe@example.com");
        assertThat(userOptional.isPresent()).isTrue();
    }
}
