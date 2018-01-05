/*******************************************************************************
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 01/01/2017
 * Author: Huyen Lam <email:huyen.lam@mediastep.com>
 *  
 *******************************************************************************/
package com.mediastep.beecow.gateway.service;

import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.BEECOW_USER_LOGIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionKey;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.UserProfile;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.mediastep.beecow.common.domain.enumeration.AccountType;
import com.mediastep.beecow.common.dto.MediaFileDTO;
import com.mediastep.beecow.gateway.BeecowGatewayApp;
import com.mediastep.beecow.gateway.client.MediaFileServiceClient;
import com.mediastep.beecow.gateway.domain.Authority;
import com.mediastep.beecow.gateway.domain.SocialUserProfile;
import com.mediastep.beecow.gateway.domain.User;
import com.mediastep.beecow.gateway.repository.AuthorityRepository;
import com.mediastep.beecow.gateway.repository.UserRepository;
import com.mediastep.beecow.gateway.repository.search.UserSearchRepository;
import com.mediastep.beecow.gateway.security.jwt.TokenProviderService;
import com.mediastep.beecow.gateway.service.mapper.UserMapper;
import com.mediastep.beecow.gateway.service.util.SocialUserUtil;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = BeecowGatewayApp.class)
@Transactional
public class SocialServiceIntTest {

    private static final Long TEST_IMAGE_ID = 0L;

    private static final String TEST_URL_PREFIX = "TEST_URL";

    @Inject
    private AuthorityRepository authorityRepository;

    @Inject
    private PasswordEncoder passwordEncoder;

    @Inject
    private UserRepository userRepository;

    @Inject
    private UserSearchRepository userSearchRepository;

    @Inject
    private UserMapper userMapper;

    @Inject
    private TokenProviderService tokenProviderService;

    @Mock
    private MediaFileServiceClient mediaFileServiceClient;

    @Mock
    private MailService mockMailService;

    @Mock
    private UsersConnectionRepository mockUsersConnectionRepository;

    @Mock
    private ConnectionRepository mockConnectionRepository;

    private SocialService socialService;

    public static void mockMediaFileServiceClient(MediaFileServiceClient mediaFileServiceClient) {
        when(mediaFileServiceClient.clone(anyString(), anyString())).then(new Answer<MediaFileDTO>() {
            @Override
            public MediaFileDTO answer(InvocationOnMock invocation) throws Throwable {
                MediaFileDTO mediaFileDTO = new MediaFileDTO();
                mediaFileDTO.setId(TEST_IMAGE_ID);
                mediaFileDTO.setUrlPrefix(TEST_URL_PREFIX);
                return mediaFileDTO;
            }
        });
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mockMediaFileServiceClient(mediaFileServiceClient);
        doNothing().when(mockMailService).sendSocialRegistrationValidationEmail(anyObject(), anyString(), anyString());
        doNothing().when(mockConnectionRepository).addConnection(anyObject());
        when(mockUsersConnectionRepository.createConnectionRepository(anyString())).thenReturn(mockConnectionRepository);

        socialService = new SocialService();
        ReflectionTestUtils.setField(socialService, "authorityRepository", authorityRepository);
        ReflectionTestUtils.setField(socialService, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(socialService, "mailService", mockMailService);
        ReflectionTestUtils.setField(socialService, "userRepository", userRepository);
        ReflectionTestUtils.setField(socialService, "userSearchRepository", userSearchRepository);
        ReflectionTestUtils.setField(socialService, "usersConnectionRepository", mockUsersConnectionRepository);
        ReflectionTestUtils.setField(socialService, "userMapper", userMapper);
        ReflectionTestUtils.setField(socialService, "tokenProviderService", tokenProviderService);
        ReflectionTestUtils.setField(socialService, "mediaFileServiceClient", mediaFileServiceClient);
    }

    @Test
    public void testDeleteUserSocialConnection() throws Exception {
        // Setup
        Connection<?> connection = createConnection("@login",
            "mail@mail.com",
            "FIRST_NAME",
            "LAST_NAME",
            "facebook");
        socialService.createSocialUser(connection, null, "fr", null);
        MultiValueMap<String, Connection<?>> connectionsByProviderId = new LinkedMultiValueMap<>();
        connectionsByProviderId.put("facebook", null);
        when(mockConnectionRepository.findAllConnections()).thenReturn(connectionsByProviderId);

        // Exercise
        socialService.deleteUserSocialConnection("@login");

        // Verify
        verify(mockConnectionRepository, times(1)).removeConnections("facebook");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateSocialUserShouldThrowExceptionIfConnectionIsNull() {
        // Exercise
        socialService.createSocialUser(null, null, "fr", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateSocialUserShouldThrowExceptionIfConnectionHasNoEmailAndNoLogin() {
        // Setup
        Connection<?> connection = createConnection("",
            "",
            "FIRST_NAME",
            "LAST_NAME",
            "facebook");

        // Exercise
        socialService.createSocialUser(connection, null, "fr", null);
    }

    @Ignore // Email is allowed to be null
    @Test(expected = IllegalArgumentException.class)
    public void testCreateSocialUserShouldThrowExceptionIfConnectionHasNoEmailAndLoginAlreadyExist() {
        // Setup
        User user = createExistingUser("@login",
            "mail@mail.com",
            "OTHER_FIRST_NAME",
            "OTHER_LAST_NAME");
        Connection<?> connection = createConnection("@login",
            "",
            "FIRST_NAME",
            "LAST_NAME",
            "facebook");

        // Exercise
        try {
            // Exercise
            socialService.createSocialUser(connection, null, "fr", null);
        } finally {
            // Teardown
            userRepository.delete(user);
        }
    }

    @Test
    public void testCreateSocialUserShouldCreateUserIfNotExist() {
        // Setup
        String provider = "facebook";
        Connection<?> connection = createConnection("@login",
            "mail@mail.com",
            "FIRST_NAME",
            "LAST_NAME",
            provider);

        // Exercise
        socialService.createSocialUser(connection, null, "fr", null);

        // Verify
        final Optional<User> user = userRepository.findOneByLogin(getLoginName(connection));
        assertThat(user).isPresent();
        assertThat(user.get().getAccountType()).isEqualTo(AccountType.FACEBOOK);

        // Teardown
        userRepository.delete(user.get());
    }

    private String getLoginName(Connection<?> connection) {
    	SocialUserProfile user = SocialUserUtil.fetchUserProfile(connection);
    	String login = SocialUserUtil.getBcLoginName(user);
    	return login;
    }

    @Test
    public void testCreateSocialUserShouldCreateUserWithSocialInformation() {
        // Setup
        String provider = "facebook";
        Connection<?> connection = createConnection("@login",
            "mail@mail.com",
            "FIRST_NAME",
            "LAST_NAME",
            provider);

        // Exercise
        socialService.createSocialUser(connection, null, "fr", null);

        //Verify
        User user = userRepository.findOneByLogin(getLoginName(connection)).get();
        assertThat(user.getFirstName()).isEqualTo("FIRST_NAME");
        assertThat(user.getLastName()).isEqualTo("LAST_NAME");
        assertThat(user.getAccountType()).isEqualTo(AccountType.FACEBOOK);

        // Teardown
        userRepository.delete(user);
    }

    @Test
    public void testCreateSocialUserMergeWithExistingUser() {
        Set<Authority> authorities = new HashSet<>();
        authorities.add(Authority.GUEST);
        User user = createExistingUser("@OTHER_LOGIN",
                "other_mail@mail.com",
                "OTHER_FIRST_NAME",
                "OTHER_LAST_NAME", authorities);

        // Setup
        String provider = "facebook";
        Connection<?> connection = createConnection("@login",
            "mail@mail.com",
            "FIRST_NAME",
            "LAST_NAME",
            provider);

        // Exercise
        socialService.createSocialUser(connection, null, "fr", user.getId());

        //Verify
        User testUser = userRepository.findOneByLogin(getLoginName(connection)).get();
        assertThat(testUser.getId()).isEqualTo(user.getId());
        assertThat(testUser.getFirstName()).isEqualTo("FIRST_NAME");
        assertThat(testUser.getLastName()).isEqualTo("LAST_NAME");
        assertThat(testUser.getAccountType()).isEqualTo(AccountType.FACEBOOK);

        // Teardown
        userRepository.delete(testUser);
    }

    @Test
    public void testCreateSocialUserMergeWithBeecowUser() {
        User beecowUser = userRepository.findOneByLogin(BEECOW_USER_LOGIN).get();

        // Setup
        String provider = "facebook";
        Connection<?> connection = createConnection("@login",
            "mail@mail.com",
            "FIRST_NAME",
            "LAST_NAME",
            provider);

        // Exercise
        socialService.createSocialUser(connection, null, "fr", beecowUser.getId());

        //Verify
        User testUser = userRepository.findOneByLogin(getLoginName(connection)).get();
        assertThat(testUser.getId()).isNotEqualTo(beecowUser.getId());
        assertThat(testUser.getFirstName()).isEqualTo("FIRST_NAME");
        assertThat(testUser.getLastName()).isEqualTo("LAST_NAME");
        assertThat(testUser.getAccountType()).isEqualTo(AccountType.FACEBOOK);

        // Teardown
        userRepository.delete(testUser);
    }

    @Test
    public void testCreateSocialUserShouldCreateActivatedUserWithRoleUserAndPassword() {
        // Setup
        String provider = "facebook";
        Connection<?> connection = createConnection("@login",
            "mail@mail.com",
            "FIRST_NAME",
            "LAST_NAME",
            provider);

        // Exercise
        socialService.createSocialUser(connection, null, "fr", null);

        //Verify
        User user = userRepository.findOneByLogin(getLoginName(connection)).get();
        assertThat(user.getActivated()).isEqualTo(true);
        assertThat(user.getPassword()).isNotEmpty();
        assertThat(user.getAccountType()).isEqualTo(AccountType.FACEBOOK);
        Authority userAuthority = authorityRepository.findOne("ROLE_USER");
        assertThat(user.getAuthorities().toArray()).containsExactly(userAuthority);

        // Teardown
        userRepository.delete(user);
    }

    @Test
    public void testCreateSocialUserShouldCreateUserWithExactLangKey() {
        // Setup
        String provider = "facebook";
        Connection<?> connection = createConnection("@login",
            "mail@mail.com",
            "FIRST_NAME",
            "LAST_NAME",
            provider);

        // Exercise
        socialService.createSocialUser(connection, null, "fr", null);

        //Verify
        User user = userRepository.findOneByLogin(getLoginName(connection)).get();
        assertThat(user.getLangKey()).isEqualTo("fr");

        // Teardown
        userRepository.delete(user);
    }

    @Test
    public void testCreateSocialUserShouldCreateUserWithSocialLoginWhenIsTwitter() {
        // Setup
        Connection<?> connection = createConnection("@login",
            "mail@mail.com",
            "FIRST_NAME",
            "LAST_NAME",
            "twitter");

        // Exercise
        User user = socialService.createSocialUser(connection, null, "fr", null);

        //Verify
        User validateUser = userRepository.findOneByLogin("twitter: @login").get();
        assertThat(validateUser.getEmail()).isNull();;
        assertThat(validateUser.getFirstName()).isEqualTo(user.getFirstName());
        assertThat(validateUser.getAccountType()).isEqualTo(AccountType.TWITTER);

        // Teardown
        userRepository.delete(validateUser);
    }

    @Test
    public void testCreateSocialUserShouldCreateSocialConnection() {
        // Setup
        String provider = "facebook";
        Connection<?> connection = createConnection("@login",
            "mail@mail.com",
            "FIRST_NAME",
            "LAST_NAME",
            provider);

        // Exercise
        socialService.createSocialUser(connection, null, "fr", null);

        //Verify
        verify(mockConnectionRepository, times(1)).addConnection(connection);

        // Teardown
        User user = userRepository.findOneByLogin(getLoginName(connection)).get();
        userRepository.delete(user);
    }
    
    /**
     * Test create social user should create social connection if email exist but not associate with social network.
     */
    @Test
    public void testCreateSocialUserShouldCreateUserIfEmailExistButNotAssociateWithSocialNetwork() {
        // Setup
        String provider = "facebook";
        User existUser = createExistingUser("@login",
            "mail@mail.com",
            "OTHER_FIRST_NAME",
            "OTHER_LAST_NAME");
        long initialUserCount = userRepository.count();
        Connection<?> connection = createConnection("@login",
            "mail@mail.com",
            "FIRST_NAME",
            "LAST_NAME",
            provider);

        // Exercise
        User socialUser = socialService.createSocialUser(connection, null, "fr", null);

        //Verify
        assertThat(userRepository.count()).isEqualTo(initialUserCount + 1);
        User userToVerify = userRepository.findOneByLogin(getLoginName(connection)).get();
        assertThat(userToVerify.getLogin()).isEqualTo(socialUser.getLogin());
        assertThat(userToVerify.getEmail()).isNull();
        assertThat(userToVerify.getAccountType()).isEqualTo(AccountType.FACEBOOK);

        // Teardown
        userRepository.delete(existUser);
        userRepository.delete(socialUser);
    }

    @Test
    public void testCreateSocialUserShouldNotCreateIfExistSocialUserWithSameEmail() {
        // Setup
        String provider = "facebook";
        User existUser = createExistingUser(provider + ": @login",
            "mail@mail.com",
            "OTHER_FIRST_NAME",
            "OTHER_LAST_NAME");
        long initialUserCount = userRepository.count();
        Connection<?> connection = createConnection("@login",
            "mail@mail.com",
            "FIRST_NAME",
            "LAST_NAME",
            provider);

        // Exercise
        socialService.createSocialUser(connection, null, "fr", null);

        //Verify
        assertThat(userRepository.count()).isEqualTo(initialUserCount);

        // Teardown
        userRepository.delete(existUser);
    }

//    @Test =====> social account not have field email, not this case will not happen
//    public void testCreateSocialUserShouldNotChangeUserIfEmailAlreadyExist() {
//        // Setup
//        String provider = "facebook";
//        createExistingUser(provider + ": mail@mail.com",
//            "mail@mail.com",
//            "OTHER_FIRST_NAME",
//            "OTHER_LAST_NAME");
//        Connection<?> connection = createConnection("@login",
//            "mail@mail.com",
//            "FIRST_NAME",
//            "LAST_NAME",
//            provider);
//
//        // Exercise
//        socialService.createSocialUser(connection, null, "fr", null);
//
//        //Verify
//        User userToVerify = userRepository.findOneByEmail("mail@mail.com").get();
//        assertThat(userToVerify.getLogin()).isEqualTo("@other_login");
//        assertThat(userToVerify.getFirstName()).isEqualTo("OTHER_FIRST_NAME");
//        assertThat(userToVerify.getLastName()).isEqualTo("OTHER_LAST_NAME");
//
//        // Teardown
//        userRepository.delete(userToVerify);
//    }

    @Test
    public void testCreateSocialUserShouldSendRegistrationValidationEmail() {
        // Setup
        Connection<?> connection = createConnection("@login",
            "mail@mail.com",
            "FIRST_NAME",
            "LAST_NAME",
            "facebook");

        // Exercise
        User user = socialService.createSocialUser(connection, null, "fr", null);

        //Verify
        verify(mockMailService, times(1)).sendSocialRegistrationValidationEmail(user, "facebook", "mail@mail.com");

        // Teardown
//        User userToDelete = userRepository.findOneByEmail("mail@mail.com").get();
        userRepository.delete(user);
    }

    private Connection<?> createConnection(String login,
                                           String email,
                                           String firstName,
                                           String lastName,
                                           String providerId) {
        UserProfile userProfile = mock(UserProfile.class);
        when(userProfile.getEmail()).thenReturn(email);
        when(userProfile.getUsername()).thenReturn(login);
        when(userProfile.getFirstName()).thenReturn(firstName);
        when(userProfile.getLastName()).thenReturn(lastName);

        Connection<?> connection = mock(Connection.class);
        ConnectionKey key = new ConnectionKey(providerId, "PROVIDER_USER_ID");
        when(connection.fetchUserProfile()).thenReturn(userProfile);
        when(connection.getKey()).thenReturn(key);
        when(connection.getDisplayName()).thenReturn(firstName + " " + lastName);

        return connection;
    }

    private User createExistingUser(String login, String email, String firstName, String lastName) {
        User user = new User();
        user.setLogin(login);
        user.setPassword(passwordEncoder.encode("password"));
        user.setAccountType(AccountType.EMAIL);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setDisplayName(firstName + " " + lastName);
        user.setActivated(true);
        return userRepository.saveAndFlush(user);
    }

    private User createExistingUser(String login, String email, String firstName, String lastName, Set<Authority> authorities) {
        User user = createExistingUser(login, email, firstName, lastName);
        user.setAuthorities(authorities);
        return userRepository.saveAndFlush(user);
    }
}
