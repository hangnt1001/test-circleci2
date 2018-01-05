/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.service;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mediastep.beecow.common.config.UserServiceConfig;
import com.mediastep.beecow.common.domain.enumeration.AccountType;
import com.mediastep.beecow.common.domain.enumeration.BeecowCommandTypeEnum;
import com.mediastep.beecow.common.domain.enumeration.Gender;
import com.mediastep.beecow.common.dto.BeecowCommandNotifyDTO;
import com.mediastep.beecow.common.dto.ImageDTO;
import com.mediastep.beecow.common.dto.PhoneDTO;
import com.mediastep.beecow.common.dto.UserDTO;
import com.mediastep.beecow.common.errors.EntityExistException;
import com.mediastep.beecow.common.errors.InvalidInputException;
import com.mediastep.beecow.common.security.AuthoritiesConstants;
import com.mediastep.beecow.common.security.SecurityUtils;
import com.mediastep.beecow.common.util.ImageDtoUtil;
import com.mediastep.beecow.gateway.config.event.PushNotifyEventGateway;
import com.mediastep.beecow.gateway.domain.Authority;
import com.mediastep.beecow.gateway.domain.User;
import com.mediastep.beecow.gateway.repository.AuthorityRepository;
import com.mediastep.beecow.gateway.repository.UserRepository;
import com.mediastep.beecow.gateway.repository.search.UserSearchRepository;
import com.mediastep.beecow.gateway.service.mapper.UserMapper;
import com.mediastep.beecow.gateway.service.mapper.UserPropertiesMapper;
import com.mediastep.beecow.gateway.service.util.RandomUtil;
import com.mediastep.beecow.gateway.service.util.UserUtil;
import com.mediastep.beecow.gateway.service.validation.UserValidator;
import com.mediastep.beecow.gateway.web.rest.errors.ErrorConstants;
import com.mediastep.beecow.gateway.web.rest.vm.ContactListVM;
import com.mediastep.beecow.gateway.web.rest.vm.ManagedUserVM;

/**
 * Service class for managing users.
 */
@Service
@Transactional
public class UserService {

    public static final String LOGIN_NAME_PREFIX = "guest_";

    private final Logger log = LoggerFactory.getLogger(UserService.class);

    @Inject
    private EntityManager entityManager;

    @Inject
    private UserServiceConfig userServiceConfig;

    @Inject
    private SocialService socialService;

    @Inject
    private PasswordEncoder passwordEncoder;

    @Inject
    private UserRepository userRepository;

    @Inject
    private UserSearchRepository userSearchRepository;

    @Inject
    private AuthorityRepository authorityRepository;

    @Inject
    private UserValidator userValidator;

    @Inject
    private UserMapper userMapper;

    @Inject
    private UserPropertiesMapper userPropertiesMapper;

    @Inject
    private AuthenticationManager authenticationManager;


    public Optional<User> activateRegistration(long userId, String activationKey) {
        log.debug("Activating user ID {} with activation key {}", userId, activationKey);
        return userRepository.findOneByIdAndActivationKey(userId, activationKey)
            .filter(user -> {
                return UserUtil.isWaitingForActivate(user);
            })
            .map(user -> {
                // activate given user for the registration key.
                unsetActivationKey(user);
                Set<Authority> authorities = user.getAuthorities();
                authorities.remove(Authority.GUEST);
                authorities.add(Authority.USER);
                userSearchRepository.save(user);
                log.debug("Activated user: {}", user);
                return user;
            });
    }

    public Optional<User> findOneByResetKey(String key) {
       return userRepository.findOneByResetKeyOrderByResetDateDesc(key);
    }

    public Optional<User> completePasswordReset(String newPassword, String key) {
       log.debug("Reset user password for reset key {}", key);
       return userRepository.findOneByResetKeyOrderByResetDateDesc(key)
            .filter(user -> {
                return isWaitingForResetPassword(user);
           })
           .map(user -> {
                user.setPassword(passwordEncoder.encode(newPassword));
                user.setResetKey(null);
                user.setResetDate(null);
                return user;
           });
    }

    /**
     * Check if user has activation key and still valid
     * @param user
     * @return
     */
    public boolean isWaitingForResetPassword(User user) {
        assert(user != null);
        ZonedDateTime expectedResetDate = ZonedDateTime.now().minus(userServiceConfig.getResetKeyValidTime(), userServiceConfig.getResetKeyValidTimeUnit());
        ZonedDateTime resetDate = user.getResetDate();
        return user.getResetKey() != null && resetDate != null && expectedResetDate.isBefore(resetDate);
    }

    public Optional<User> requestPasswordReset(String mail) {
        return userRepository.findOneByEmail(mail)
            .filter(User::getActivated)
            .map(user -> {
                user.setResetKey(RandomUtil.generateResetKey());
                user.setResetDate(ZonedDateTime.now());
                return user;
            });
    }

    public Optional<User> requestPasswordReset(PhoneDTO mobile) {
        return userRepository.findOneByMobile(mobile)
            .filter(User::getActivated)
            .map(user -> {
                setPasswordReset(user);
                return user;
            });
    }

    private void setPasswordReset(User user) {
        user.setResetKey(RandomUtil.generateResetKey());
        user.setResetDate(ZonedDateTime.now());
    }

    /**
     * Create pre-activate user account for login
     * @param location location where user live, accepted input is defined in ISO_3166-2
     * @param language language key
     * @return
     */
    public User createPreavtivateUser(String location, String language) {
        // Try to create user with random username
        User user = null;
        int retry = userServiceConfig.getRetry();
        for (int i = 0; i < retry; i++) {
            try {
                user = new User();
                setGuestProperties(user);
                user.setLocationCode(location);
                user.setLangKey(language);
                user = doSave(user);
                if (user != null && user.getId() != null) {
                    return user;
                }
            }
            catch (RuntimeException exc) {
                log.warn("Failed to create pre-activate user {}, retry times {}", user, i);
                log.warn("", exc);
            }
        }

        return null;
    }

    private void setGuestProperties(User user) {
        user.setLogin(RandomUtil.generateGuestSubfix(LOGIN_NAME_PREFIX));
        user.setEmail(null);
        user.setMobile(null);
        user.setPassword(passwordEncoder.encode(""));
        user.setAccountType(AccountType.PRE_ACTIVATE);
        UserUtil.setDefaultDisplayName(user);
        user.setActivated(true);
        Set<Authority> authorities = new HashSet<>();
        authorities.add(Authority.GUEST);
        user.setAuthorities(authorities);
    }

    public User createUser(String login, AccountType accountType, String password, String firstName, String lastName, ZonedDateTime dateOfBirth, Gender gender,
        String email, String locationCode, String langKey) {

        User newUser = new User();
        Authority authority = authorityRepository.findOne(AuthoritiesConstants.GUEST);
        Set<Authority> authorities = new HashSet<>();
        String encryptedPassword = passwordEncoder.encode(password);
        if (StringUtils.isBlank(login)) {
            login = email;
        }
        newUser.setLogin(login);
        // new user gets initially a generated password
        newUser.setPassword(encryptedPassword);
        newUser.setAccountType(accountType);
        newUser.setEmail(email);
        UserUtil.setFirstNameAndLastName(newUser, firstName, lastName, null);
        UserUtil.setDateOfBirth(newUser, dateOfBirth);
        if (gender == null) {
            gender = Gender.MALE;
        }
        newUser.setGender(gender);
        newUser.setLocationCode(locationCode);
        newUser.setLangKey(langKey);
        // new user gets registration key
        setActivationKey(newUser);
        authorities.add(authority);
        newUser.setAuthorities(authorities);
        newUser = validateAndSave(newUser);
        log.debug("Created Information for User: {}", newUser);
        return newUser;
    }

    private void setActivationKey(User user) {
        user.setActivated(false);
        user.setActivationKey(RandomUtil.generateShortActivationKey());
        ZonedDateTime activationExpiredDate = ZonedDateTime.now().plus(userServiceConfig.getActivationKeyValidTime(), userServiceConfig.getActivationKeyValidTimeUnit());
        user.setActivationExpiredDate(activationExpiredDate);
    }

    private void unsetActivationKey(User user) {
        user.setActivated(true);
        user.setActivationKey(null);
        user.setActivationExpiredDate(null);
    }

    private User validateAndSave(User user) {
        userValidator.validate(user);
        return doSave(user);
    }

    private User doSave(User user) {
        // Set default language if not provided
        if (StringUtils.isBlank(user.getLangKey())) {
            user.setLangKey(userServiceConfig.getDefaultLanguage());
        }
        // Remove out-dated user with same login
    	revertNotActivatedUsers(user.getId(), user.getLogin(), user.getEmail(), user.getMobile());
        // Save user
        user = userRepository.save(user);
        userSearchRepository.save(user);
        return user;
    }

    /**
     * Revert registered user but not activated to guest.
     */
    private void revertNotActivatedUsers(Long expUserId, String login, String email, String mobile) {
        List<User> users = userRepository.findAllRegisteredButNotActivated(login, email, mobile, ZonedDateTime.now());
        for (User user : users) {
        	if (expUserId == null || user.getId() != expUserId) {
	        	log.debug("Revert not activated user to guest: {}", user);
	        	setGuestProperties(user);
	        	doSave(user);
        	}
        }
        entityManager.flush();
    }

    public User createUser(ManagedUserVM managedUserVM) {
        User user = new User();
        user.setAccountType(managedUserVM.getAccountType());
        user.setLogin(managedUserVM.getLogin());
        user.setEmail(managedUserVM.getEmail());
        UserUtil.setFirstNameAndLastName(user, managedUserVM.getFirstName(), managedUserVM.getLastName(), managedUserVM.getDisplayName());
        if (managedUserVM.getLangKey() == null) {
            user.setLangKey("en"); // default language
        } else {
            user.setLangKey(managedUserVM.getLangKey());
        }
        if (managedUserVM.getAuthorities() != null) {
            Set<Authority> authorities = new HashSet<>();
            managedUserVM.getAuthorities().forEach(
                authority -> authorities.add(authorityRepository.findOne(authority))
            );
            user.setAuthorities(authorities);
        }
        String encryptedPassword = passwordEncoder.encode(RandomUtil.generatePassword());
        user.setPassword(encryptedPassword);
        user.setResetKey(RandomUtil.generateResetKey());
        user.setResetDate(ZonedDateTime.now());
        user.setActivated(true);
        user = doSave(user);
        log.debug("Created Information for User: {}", user);
        return user;
    }

    public User createUser(Map<String, Object> userProps, boolean fromMobileApp) {
        User user = new User();
        Set<Authority> authorities = new HashSet<>();
        authorities.add(Authority.GUEST);
        user.setAuthorities(authorities);
        setRegistrationProperties(user, userProps, fromMobileApp);
        validateContactInfo(user); // Validate email, mobile and login name
        setLoginIfRequired(user, fromMobileApp);
        user = validateAndSave(user);
        log.debug("Created Information for User: {}", user);
        return user;
    }

    /**
     * Upgrade activate a pre-activate user
     * @return
     */
    public User upgradePreActivateAccount(User user, Map<String, Object> userProps, boolean fromMobileApp) {
        // Reset user information
        entityManager.detach(user);
        user.setLogin(null);
        // Upgrade user
        setRegistrationProperties(user, userProps, fromMobileApp);
        refineContactInfo(user);
        validateContactInfo(user); // Validate email, mobile and login name
        user = validateAndSave(user);
        log.debug("Upgraded pre-activate user: {}", user);
        return user;
    }

    private void setRegistrationProperties(User user, Map<String, Object> userProps, boolean fromMobileApp) {
        // Validate password
        String password = userPropertiesMapper.getPassword(userProps);
        userValidator.validatePassword(password);
        // Check if login and email is already used by other user.
        userPropertiesMapper.setSystemProperties(user, userProps);
        // Set Login name if not set
        setLoginIfRequired(user, fromMobileApp);
        // Set other properties and activation code
        userPropertiesMapper.setProperties(user, userProps);
        if (user.getGender() == null) {
            user.setGender(Gender.MALE);
        }
        // Set activation code and authorities
        setActivationKey(user);
    }

    private void refineContactInfo(User user) {
        if (user.getAccountType() == AccountType.EMAIL) {
            user.setMobile(null);
        }
        else {
            user.setEmail(null);
        }
    }

    /**
     * Validate login name (assigned from email or mobile phone number)
     * @param user
     * @throws EntityExistException if is not available for registration
     */
    private void validateContactInfo(User user) {
        if (user.getAccountType() == AccountType.EMAIL) {
            validateEmail(user);
        }
        else {
            validateMobile(user);
        }
    }

    /**
     * Validate email
     * @param user
     * @return true if valid, false if blank
     * @throws EntityExistException if is not available for registration
     */
    private boolean validateEmail(User user) {
        String email = user.getEmail();
        if (StringUtils.isBlank(email)) {
            return false;
        }
        if (isEmailAvailableForMobileRegistration(email)) {
            return true;
        }
        else {
            throw new EntityExistException(User.class, ErrorConstants.ERR_USER_EMAIL_EXISTS, email);
        }
    }

    /**
     * Validate mobile
     * @param user
     * @return true if valid, false if blank
     * @throws EntityExistException if is not available for registration
     */
    private boolean validateMobile(User user) {
        PhoneDTO mobileDTO = user.getMobileObject();
        if (mobileDTO == null) {
            return false;
        }
        if (isMobileAvailableForMobileRegistration(mobileDTO)) {
            return true;
        }
        else {
            throw new EntityExistException(User.class, ErrorConstants.ERR_USER_MOBILE_EXISTS, mobileDTO);
        }
    }

    /**
     * The method check if login name exists, it will set mobile phone or email as login name.
     * @param user
     * @throws EntityExistException if login is not available for registration
     */
    private void setLoginIfRequired(User user, boolean fromMobileApp) {
        String login = user.getLogin();
        if (StringUtils.isBlank(login)) {
            AccountType accountType = user.getAccountType();
            if (accountType == AccountType.EMAIL) {
                login = user.getEmail();
            }
            else if (accountType == AccountType.MOBILE) {
                login = user.getMobile();
            }
            else {
                throw new InvalidInputException(ErrorConstants.ERR_USER_LOGIN_MISSING);
            }
        }
        if ((fromMobileApp && isLoginAvailableForMobileRegistration(login)) ||
            (!fromMobileApp && isLoginAvailableForRegistration(login))) {
            user.setLogin(login);
        }
        else {
            throw new EntityExistException(User.class, ErrorConstants.ERR_USER_LOGIN_EXISTS, login);
        }
    }

    @Deprecated
    public User updateUser(String firstName, String lastName, String displayName, String email, ZonedDateTime dateOfBirth,
        Gender gender, ImageDTO avatarUrl, String locationCode, String langKey) {
        return userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin()).map(user -> {
            user.setEmail(email);
            UserUtil.setDateOfBirth(user, dateOfBirth);
            user.setGender(gender);
            user.setAvatarUrl(ImageDtoUtil.imageDTOToString(avatarUrl));
            UserUtil.setFirstNameAndLastName(user, firstName, lastName, displayName);
            user.setLocationCode(locationCode);
            user.setLangKey(langKey);
            userValidator.validate(user); // Validate user for updating
            userSearchRepository.save(user);
            log.debug("Changed Information for User: {}", user);
            return user;
        })
        .orElseGet(() -> {
            return null;
        });
    }

    @Deprecated
    public void updateUser(Long id, String login, String firstName, String lastName, String email,
        boolean activated, String langKey, Set<String> authorities) {

        Optional.of(userRepository
            .findOne(id))
            .ifPresent(user -> {
                user.setLogin(login);
                user.setEmail(email);
                UserUtil.setFirstNameAndLastName(user, firstName, lastName, null);
                user.setActivated(activated);
                user.setLangKey(langKey);
                Set<Authority> managedAuthorities = user.getAuthorities();
                managedAuthorities.clear();
                authorities.forEach(
                    authority -> managedAuthorities.add(authorityRepository.findOne(authority))
                );
                log.debug("Changed Information for User: {}", user);
            });
    }

    /**
     * Update user information (individually)
     * @param userProps User information
     * @return return updated user object
     */
    public User updateUser(Map<String, Object> userProps) {
        return userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin()).map(user -> {
            // If account is not register with email or email empty => allow edit email
            if (user.getAccountType() != AccountType.EMAIL || StringUtils.isBlank(user.getEmail())) {
                userPropertiesMapper.setEmail(user, userProps);
            }
            // If account is not register with mobile phone number or phone number empty => allow edit mobile
            if (user.getAccountType() != AccountType.MOBILE || user.getMobile() == null) {
                userPropertiesMapper.setMobile(user, userProps);
            }
            userPropertiesMapper.setProperties(user, userProps);
            userValidator.validate(user); // Validate user for updating
            userSearchRepository.save(user);
            log.debug("Changed Information for User: {}", user);
            return user;
        })
        .orElseGet(() -> {
            return null;
        });
    }

    /**
     * Upgrade activate a pre-activate user
     * @return
     */
    @Deprecated
    public User upgradePreActivateAccount(User user, AccountType accountType, String login, String password, String firstName, String lastName, String email) {
        if (StringUtils.isBlank(login)) {
            login = email;
        }
        user.setLogin(login);
        // new user gets initially a generated password
        String encryptedPassword = passwordEncoder.encode(password);
        user.setPassword(encryptedPassword);
        user.setAccountType(accountType);
        user.setEmail(email);
        user.setMobile(null);
        UserUtil.setFirstNameAndLastName(user, firstName, lastName, null);
        user.setGender(Gender.MALE);
        // new user is not active
        user.setActivated(true);
        // new user gets registration key
        setActivationKey(user);
        user = validateAndSave(user);
        log.debug("Created Information for User: {}", user);
        return user;
    }

    public void deleteUser(String login) {
    	log.debug("Request to delete user with login name '{}'", login);
        Optional<User> optUser = userRepository.findOneByLogin(login);
        optUser.ifPresent(user -> {
            socialService.deleteUserSocialConnection(user.getLogin());
            doDeleteUser(user.getId());
            log.debug("Deleted User: {}", user);
        });
    }

    private void doDeleteUser(Long userId) {
        userRepository.delete(userId);
        userSearchRepository.delete(userId);
        entityManager.flush();
    }

    /**
     * Change user's password and do not send push to any device.
     *
     * @param password    New password need to be change.
     */
    public void changePassword(String password) {
        userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin()).ifPresent(user -> {
            String encryptedPassword = passwordEncoder.encode(password);
            user.setPassword(encryptedPassword);
            log.debug("Changed password for User: {}", user);
        });
    }


    /**
     * Change user password and send notification to other devices request them logout.
     *
     * @param password        New password need to be change.
     *
     * @param deviceToken    The token's device we will ignore for the push
     *                         Null if push all device that login with this User account.
     */
    public void changePassword(String password, String deviceToken) {
        userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin()).ifPresent(user -> {
            String encryptedPassword = passwordEncoder.encode(password);
            user.setPassword(encryptedPassword);
            pushChangePasswordNeedLogoutNotification(user, deviceToken);
            log.debug("Changed password for User: {}", user);
        });
    }

    @Inject
    PushNotifyEventGateway pushNotifyEventGateway;

    @Inject
    private MessageSource messageSource;

    /**
     * Push notification to tell user know Password has changed to others devices
     * to let them logout.
     *
     * @param user            The Account we need to send command to.
     * @param deviceToken     The token's device we need to ignore push to.
     */
     private void pushChangePasswordNeedLogoutNotification(User user, String deviceToken) {
         if (user != null) {
             log.debug("Push notification to mobile end point for dating user {}", user.getDisplayName());
             BeecowCommandNotifyDTO beecowCommandNotifyDTO = new BeecowCommandNotifyDTO();

             String message = generateChangePasswordNeedLogoutMessageByLang(user.getLangKey());
             byte[] bytesEncoded = Base64.encodeBase64(message.getBytes());
             beecowCommandNotifyDTO.setMessageByte(bytesEncoded);
             beecowCommandNotifyDTO.setUserId(user.getId());

             beecowCommandNotifyDTO.setCommandType(BeecowCommandTypeEnum.COMMAND_TYPE_REQUEST_LOGOUT);
             beecowCommandNotifyDTO.setDeviceToken(deviceToken);

             pushNotifyEventGateway.pushBeecowCommandNotify(beecowCommandNotifyDTO);
         }
     }

     /**
     * Get message by langCode
     *
     * @param langKey
     * @return
     */
    private String generateChangePasswordNeedLogoutMessageByLang(String langKey) {
        Locale locale = Locale.forLanguageTag(langKey);
        log.info(locale.toString());
        String[] args = {};
        String message = messageSource.getMessage("notification.beecow-command.change-password-need-logout", args, locale);

        return message;
    }

    /**
     * Check currentPassword is correct
     *
     * @return true if current password is matched with current password.
     */
    public boolean checkCurrentPassword(String currentPassword) {
            String userName = SecurityUtils.getCurrentUserLogin();
            try {
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userName, currentPassword);
                this.authenticationManager.authenticate(authenticationToken);
                return true;
            } catch (Exception e) {}

            return false;
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserWithAuthoritiesByLogin(String login) {
        return userRepository.findOneByLogin(login).map(user -> {
            user.getAuthorities().size();
            return user;
        });
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserWithAuthoritiesByLogin(String login1, String login2) {
        return userRepository.findOneByLoginIn(Arrays.asList(login1, login2)).map(user -> {
            user.getAuthorities().size();
            return user;
        });
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserWithAuthoritiesByEmail(String email) {
        return userRepository.findOneByEmail(email).map(user -> {
            user.getAuthorities().size();
            return user;
        });
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserWithAuthoritiesByMobile(PhoneDTO mobileDTO) {
        return userRepository.findOneByMobile(mobileDTO).map(user -> {
            user.getAuthorities().size();
            return user;
        });
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserWithAuthorities(Long id) {
        User user = userRepository.findOne(id);
        if (user != null) {
            user.getAuthorities().size(); // eagerly load the association
        }
        return Optional.ofNullable(user);
    }

    @Transactional(readOnly = true)
    public User getUserWithAuthorities() {
        Optional<User> optionalUser = userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin());
        User user = null;
        if (optionalUser.isPresent()) {
          user = optionalUser.get();
            user.getAuthorities().size(); // eagerly load the association
         }
         return user;
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getUsersWithAuthorities(ContactListVM contactListVM, Pageable pageable) {
        List<String> emails = contactListVM.getEmails();
        List<String> mobileListWithZero = contactListVM.getMobilesWithZero();
        List<String> mobileListWithoutZero = contactListVM.getMobilesWithoutZero();
        List<User> users = userRepository.findAll(emails, mobileListWithZero, mobileListWithoutZero, pageable.getSort());
        return userMapper.usersToUserDTOs(users);
    }

    /**
     * Check if login name can be used for new account
     * <pre>
     * + Not exist
     * + Or exists, but not used (not activated and code expired)
     * </pre>
     * @param login
     * @return true if exist
     */
    public boolean isLoginAvailableForRegistration(String login) {
        return getUserWithAuthoritiesByLogin(login)
            .map(user -> {
                return !isActivatedOrWaitingForActivation(user);
            })
            .orElse(true);
    }

    /**
     * Check if email can be used for new account
     * <pre>
     * + Not exist
     * + Or exists, but not used (not activated and code expired)
     * </pre>
     * @param email
     * @return true if exist
     */
    public boolean isEmailAvailableForRegistration(String email) {
        return getUserWithAuthoritiesByEmail(email)
            .map(user -> {
                return !isActivatedOrWaitingForActivation(user) || UserUtil.isSocialAccount(user);
            })
            .orElse(true);
    }

    /**
     * Check if user is not GUEST or is GUEST but waiting for validation
     * @return
     */
    private boolean isActivatedOrWaitingForActivation(User user) {
        return (!UserUtil.isGuest(user) || UserUtil.isWaitingForActivate(user));
    }

    /**
     * Check if login name can be used for new account
     * <pre>
     * + Not exist
     * + Or exists, but not used (not activated and code expired)
     * </pre>
     * @return true if exist
     */
    public boolean isLoginAvailableForMobileRegistration(String login) {
        return getUserWithAuthoritiesByLogin(login)
            .map(user -> {
                return !isConflict(user);
            })
            .orElse(true);
    }

    /**
     * Check if email can be used for new account
     * <pre>
     * + Not exist
     * + Or exists, but not used (not activated and code expired)
     * </pre>
     * @param email
     * @return true if exist
     */
    public boolean isEmailAvailableForMobileRegistration(String email) {
        return getUserWithAuthoritiesByEmail(email)
            .map(user -> {
                return !isConflict(user);
            })
            .orElse(true);
    }

    /**
     * Check if given user conflict with current user which prevent it from activating the current user
     * @param user
     * @return true if conflict
     */
    private boolean isConflict(User user) {
        if (SecurityUtils.isCurrentUser(user.getId())) {
            return false;
        }
        else {
            return isActivatedOrWaitingForActivation(user);
        }
    }

    /**
     * Check if mobile can be used for new account
     * <pre>
     * + Not exist
     * + Or exists, but not used (not activated and code expired)
     * </pre>
     * @param mobileDTO
     * @return true if exist
     */
    public boolean isMobileAvailableForMobileRegistration(PhoneDTO mobileDTO) {
        return getUserWithAuthoritiesByMobile(mobileDTO)
            .map(user -> {
                return !isConflict(user);
            })
            .orElse(true);
    }

    public User updateActivationCode(long userId) {
        User user = userRepository.findOne(userId);
        if (user == null) {
            return null;
        }
        // new user gets registration key
        setActivationKey(user);
        user = doSave(user);
        log.debug("Created Information for User: {}", user);
        return user;
    }

    public User updateUserLang(long userId, String langKey) {
        User user = userRepository.findOne(userId);
        if (user == null) {
            return null;
        }
        user.setLangKey(langKey);
        user = validateAndSave(user);
        log.debug("Update langguage for user: {}", user);
        return user;
    }

    public User updateUserLocation(long userId, String locationCode) {
        User user = userRepository.findOne(userId);
        if (user == null) {
            return null;
        }
        user.setLocationCode(locationCode);
        user = validateAndSave(user);
        log.debug("Update location for user: {}", user);
        return user;
    }
}
