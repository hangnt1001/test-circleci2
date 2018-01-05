/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.service;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.NativeWebRequest;

import com.mediastep.beecow.common.domain.enumeration.AccountType;
import com.mediastep.beecow.common.dto.MediaFileDTO;
import com.mediastep.beecow.common.errors.BeecowException;
import com.mediastep.beecow.gateway.client.BcUserSettingValueServiceClient;
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
import com.mediastep.beecow.gateway.service.util.UserUtil;
import com.mediastep.beecow.user.dto.AuthUserDTO;

@Service
@Transactional
public class SocialService {

    public static final String PROVIDER_FACEBOOK = "facebook";

    public static final String PROVIDER_GOOGLE = "google";

    public static final String PROVIDER_TWITTER = "twitter";

    public static final String SOCIAL_LOGIN_SEP = ": ";

    public static final String FACEBOOK_LOGIN_PREFIX = PROVIDER_FACEBOOK + SOCIAL_LOGIN_SEP;

    private final Logger log = LoggerFactory.getLogger(SocialService.class);

    @Inject
    private UsersConnectionRepository usersConnectionRepository;

    @Inject
    private AuthorityRepository authorityRepository;

    @Inject
    private PasswordEncoder passwordEncoder;

    @Inject
    private UserRepository userRepository;

    @Inject
    private UserSearchRepository userSearchRepository;

    @Inject
    private MailService mailService;

    @Inject
    private UserMapper userMapper;

    @Inject
    private TokenProviderService tokenProviderService;

    @Inject
    private BcUserSettingValueServiceClient userSettingValueServiceClient;

    @Inject
    private MediaFileServiceClient mediaFileServiceClient;

    @PostConstruct
    private void init() {
        try {
            String[] fieldsToMap = { "id", "about", "age_range", "birthday",
                "context", "cover", "currency", "devices", "education",
                "email", "favorite_athletes", "favorite_teams",
                "first_name", "gender", "hometown", "inspirational_people",
                "installed", "install_type", "is_verified", "languages",
                "last_name", "link", "locale", "location", "meeting_for",
                "middle_name", "name", "name_format", "political",
                "quotes", "payment_pricepoints", "relationship_status",
                "religion", "security_settings", "significant_other",
                "sports", "test_group", "timezone", "third_party_id",
                "updated_time", "verified", "viewer_can_send_gift",
                "website", "work" };

            Field field = Class.forName(
                "org.springframework.social.facebook.api.UserOperations")
                .getDeclaredField("PROFILE_FIELDS");
            field.setAccessible(true);

            Field modifiers = field.getClass().getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            field.set(null, fieldsToMap);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void deleteUserSocialConnection(String login) {
        ConnectionRepository connectionRepository = usersConnectionRepository.createConnectionRepository(login);
        connectionRepository.findAllConnections().keySet().stream()
            .forEach(providerId -> {
                connectionRepository.removeConnections(providerId);
                log.debug("Delete user social connection providerId: {}", providerId);
            });
    }

    /**
     * Sign in the system with social token
     * @param connection
     * @param mergeWithUserId
     * @param langKey
     * @param request
     * @return
     * @throws BadCredentialsException, UsernameNotFoundException
     */
    public AuthUserDTO signIn(Connection<?> connection, Long mergeWithUserId, String location, String langKey, NativeWebRequest request) {
        List<String> userIds = usersConnectionRepository.findUserIdsWithConnection(connection);
        if (userIds.size() > 1) {
            throw new BadCredentialsException("Multiple users");
        }

        String login;
        User user;
        if (userIds.isEmpty()) {
            user = createSocialUser(connection, location, StringUtils.replace(langKey, "\"", ""), mergeWithUserId);
            login = user.getLogin();
        }
        else {
            login = userIds.get(0);
            Optional<User> result = userRepository.findOneByLogin(login);
            if (!result.isPresent()) {
                throw new UsernameNotFoundException("User " + login + " not exist");
            }
            user = result.get();
        }

        String token = tokenProviderService.createTokenNoThrow(login, true);
        AuthUserDTO authUserDTO = userMapper.userToAuthUserDTO(user);
        authUserDTO.setAccessToken(token);

        // Get user settings
        String authHeaderValue = "Bearer " + token;
        Map<String, Object> settings = userSettingValueServiceClient.getAllBcUserSettingValues(user.getId(), null, authHeaderValue);
        authUserDTO.setSettings(settings);

        return authUserDTO;
    }

    public User createSocialUser(Connection<?> connection, String location, String langKey, Long mergeWithUserId) {
        if (connection == null) {
            log.error("Cannot create social user because connection is null");
            throw new IllegalArgumentException("Connection cannot be null");
        }
        SocialUserProfile userProfile = SocialUserUtil.fetchUserProfile(connection);
        User user = createUserIfNotExist(userProfile, location, langKey, mergeWithUserId);
        createSocialConnection(user.getLogin(), connection);
        String email = userProfile.getEmail();
        if (email != null) {
        	mailService.sendSocialRegistrationValidationEmail(user, userProfile.getProviderId(), email);
        }
        return user;
    }

    private User createUserIfNotExist(SocialUserProfile userProfile, String location, String langKey, Long mergeWithUserId) {
        String login = SocialUserUtil.getBcLoginName(userProfile);
        Optional<User> userOptional = userRepository.findOneByLogin(login);
        if (userOptional.isPresent()) {
        	return userOptional.get();
        }

        String encryptedPassword = passwordEncoder.encode(RandomStringUtils.random(10));
        Set<Authority> authorities = new HashSet<>(1);
        authorities.add(authorityRepository.findOne("ROLE_USER"));

        // Upload avatar to S3
        MediaFileDTO mediaFileDTO = mediaFileServiceClient.clone(userProfile.getImageUrl(), null); // TODO consider to user "user" as domain
        String avatarUrl = null;
        if (mediaFileDTO != null) {
            avatarUrl = mediaFileDTO.getUrlPrefix() + "/" + mediaFileDTO.getId();
        }

        User newUser = null;
        // If merge with guest is required, merge given social account with given user ID
        if (mergeWithUserId != null) {
            User curUser = userRepository.findOne(mergeWithUserId);
            if (curUser != null && UserUtil.isGuestAndNotBeecowUser(curUser)) {
                newUser = curUser;
                newUser.getAuthorities().remove(Authority.GUEST);
            }
        }
        if (newUser == null) {
            newUser = new User();
        }
        newUser.setLogin(login);
        newUser.setPassword(encryptedPassword);
        String provideId = userProfile.getProviderId();
        AccountType accountType = getAccountTypeByProviderId(provideId);
        newUser.setAccountType(accountType);
        UserUtil.setFirstNameAndLastName(newUser, userProfile.getFirstName(), userProfile.getLastName(), userProfile.getDisplayName());
        newUser.setGender(userProfile.getGender());
        UserUtil.setDateOfBirth(newUser, userProfile.getDateOfBirth());
        newUser.setAvatarUrl(avatarUrl);
        newUser.setActivated(true);
        newUser.setAuthorities(authorities);
        if (StringUtils.isNotBlank(langKey)) {
            newUser.setLangKey(langKey);
        }
        if (StringUtils.isNotBlank(location)) {
            newUser.setLocationCode(StringUtils.upperCase(location));
        }

        newUser = userRepository.save(newUser);
        userSearchRepository.save(newUser);

        return newUser;
    }

    private AccountType getAccountTypeByProviderId(String providerId) {
        switch (providerId) {
            case PROVIDER_FACEBOOK:
                return AccountType.FACEBOOK;
            case PROVIDER_GOOGLE:
                return AccountType.GOOGLE;
            case PROVIDER_TWITTER:
                return AccountType.TWITTER;
            default:
                throw new BeecowException("Cannot determine user-type for social login provider '" + providerId + "'");
        }
    }

    private void createSocialConnection(String login, Connection<?> connection) {
        ConnectionRepository connectionRepository = usersConnectionRepository.createConnectionRepository(login);
        connectionRepository.addConnection(connection);
    }
}
