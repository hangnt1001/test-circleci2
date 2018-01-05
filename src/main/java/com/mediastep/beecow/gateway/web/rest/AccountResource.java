/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.web.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;
import com.mediastep.beecow.common.domain.enumeration.AccountType;
import com.mediastep.beecow.common.dto.PhoneDTO;
import com.mediastep.beecow.common.dto.UserDTO;
import com.mediastep.beecow.common.security.AuthoritiesConstants;
import com.mediastep.beecow.common.security.SecurityUtils;
import com.mediastep.beecow.common.security.TokenUserDetails;
import com.mediastep.beecow.common.util.PhoneDtoUtil;
import com.mediastep.beecow.gateway.client.BcUserSettingValueServiceClient;
import com.mediastep.beecow.gateway.domain.User;
import com.mediastep.beecow.gateway.domain.UserField;
import com.mediastep.beecow.gateway.security.jwt.TokenProviderService;
import com.mediastep.beecow.gateway.service.MailService;
import com.mediastep.beecow.gateway.service.SMSService;
import com.mediastep.beecow.gateway.service.UserService;
import com.mediastep.beecow.gateway.service.mapper.UserMapper;
import com.mediastep.beecow.gateway.service.util.UserUtil;
import com.mediastep.beecow.gateway.web.rest.util.HeaderUtil;
import com.mediastep.beecow.gateway.web.rest.vm.CurrentAndNewPasswordVM;
import com.mediastep.beecow.gateway.web.rest.vm.EmailVM;
import com.mediastep.beecow.gateway.web.rest.vm.KeyAndPasswordVM;
import com.mediastep.beecow.gateway.web.rest.vm.ManagedUserVM;
import com.mediastep.beecow.user.dto.AuthUserDTO;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/api")
public class AccountResource {

    private final Logger log = LoggerFactory.getLogger(AccountResource.class);

    @Inject
    private UserService userService;

    @Inject
    private UserMapper userMapper;

    @Inject
    private MailService mailService;

    @Inject
    private SMSService smsService;

    @Inject
    private TokenProviderService tokenProviderService;

    @Inject
    private BcUserSettingValueServiceClient userSettingValueServiceClient;

    /**
     * POST  /register : register the user.
     *
     * @param managedUserVM the managed user View Model
     * @return the ResponseEntity with status 201 (Created) if the user is registered or 400 (Bad Request) if the login or e-mail is already in use
     */
    @PostMapping(path = "/register", produces={MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE})
    @Timed
    @Deprecated
    public ResponseEntity<UserDTO> registerAccount(@Valid @RequestBody ManagedUserVM managedUserVM) {
        log.debug("REST request to register: {}", managedUserVM);

        HttpHeaders textPlainHeaders = new HttpHeaders();
        textPlainHeaders.setContentType(MediaType.TEXT_PLAIN);

        String email = StringUtils.lowerCase(managedUserVM.getEmail());
        if (!userService.isEmailAvailableForRegistration(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).headers(HeaderUtil.createFailureAlert("user-account", "emailexists", "Email already in use")).body(null);
        }
        String login = StringUtils.lowerCase(managedUserVM.getLogin());
        if (!userService.isLoginAvailableForRegistration(login)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).headers(HeaderUtil.createFailureAlert("user-account", "loginexists", "Login already in use")).body(null);
        }

        User user = userService.createUser(managedUserVM.getLogin(), AccountType.EMAIL, managedUserVM.getPassword(),
            managedUserVM.getFirstName(), managedUserVM.getLastName(), managedUserVM.getDateOfBirth(), managedUserVM.getGender(),
            managedUserVM.getEmail().toLowerCase(),
            managedUserVM.getLocationCode(), managedUserVM.getLangKey());

        sendActivationCode(user);

        AuthUserDTO userDTO = userMapper.userToAuthUserDTO(user);
        String token = tokenProviderService.createToken(user, true);
        userDTO.setAccessToken(token);

        try {
			return ResponseEntity.created(new URI("/api/account/"))
			    .headers(HeaderUtil.createEntityCreationAlert("user", user.getId().toString()))
			    .body(userDTO);
		} catch (URISyntaxException exc) {
			log.error("Register failed: " + managedUserVM, exc);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
    }

    /**
     * POST /register/mobile : register the user.
     *
     * @param managedUserVM
     *            the managed user View Model
     * @return the ResponseEntity with status 201 (Created) if the user is
     *         registered or 400 (Bad Request) if the login or e-mail is already
     *         in use
     */
    @PostMapping(path = "/register/mobile", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE })
    @Timed
    @Secured(AuthoritiesConstants.GUEST)
    @ApiOperation(value = "Register or activate a pre-activate user (for mobile)",
        notes = "The service creates a new user or activates a pre-activate user" + "\n" +
                "<ul>" +
                "<li>" + "If client does not provide token or provided token belongs to beecow-user, the service creates a new user and responds 201 with access-token if success." + "</li>" +
                "<li>" + "If client provides a token which belongs to a pre-activate user, the service activates the user and responds 200 if success." + "</li>" +
                "</ul>")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "User is created"),
        @ApiResponse(code = 200, message = "User is activated"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 403, message = "Forbidden, logged in user is activated"),
        @ApiResponse(code = 409, message = "Email or login name has already exists, or database constraints violated"),})
    @Deprecated
    public ResponseEntity<UserDTO> upgradePreActivateAccount(@Valid @RequestBody ManagedUserVM managedUserVM) {
        log.debug("REST request to upgrade pre-activate account: {}", managedUserVM);
        User user = getCurrentPreactivateUser();
        if (user == null) {
        	return registerAccount(managedUserVM);
        }
        // Only perform activation for guest user
        if (StringUtils.isBlank(managedUserVM.getLogin())) {
            managedUserVM.setLogin(managedUserVM.getEmail());
        }
        // Check if login and email is already used by other user.
        String email = StringUtils.lowerCase(managedUserVM.getEmail());
        if (!userService.isEmailAvailableForMobileRegistration(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).headers(HeaderUtil.createFailureAlert("user-account", "emailexists", "Email already in use")).body(null);
        }
        String login = StringUtils.lowerCase(managedUserVM.getLogin());
        if (!userService.isLoginAvailableForMobileRegistration(login)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).headers(HeaderUtil.createFailureAlert("user-account", "loginexists", "Login already in use")).body(null);
        }
        // Register pre-avtivate user
        user = userService.upgradePreActivateAccount(user, AccountType.EMAIL, managedUserVM.getLogin(), managedUserVM.getPassword(),
            managedUserVM.getFirstName(), managedUserVM.getLastName(), managedUserVM.getEmail());
        sendActivationCode(user);
        UserDTO userDTO = userMapper.userToUserDTO(user);
        return ResponseEntity.ok(userDTO);
    }

    /**
     * POST  /register : register the user.
     *
     * @return the ResponseEntity with status 201 (Created) if the user is registered or 400 (Bad Request) if the login or e-mail is already in use
     */
    @PostMapping(path = "/register2", produces={MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE})
    @Timed
    public ResponseEntity<UserDTO> registerAccount2(@RequestBody Map<String, Object> userProps) {
        log.debug("REST request to register 2: {}", userProps);
        AccountType accountType;
        if (userProps.containsKey(UserField.EMAIL)) {
            accountType = AccountType.EMAIL;
        }
        else {
            accountType = AccountType.MOBILE;
        }
        setAccountTypeIfNotSet(userProps, accountType);
        User user = userService.createUser(userProps, false);
        sendActivationCode(user);
        AuthUserDTO userDTO = userMapper.userToAuthUserDTO(user);
        String token = tokenProviderService.createToken(user, true);
        userDTO.setAccessToken(token);
        try {
			return ResponseEntity.created(new URI("/api/account/"))
			        .headers(HeaderUtil.createEntityCreationAlert("user", user.getId().toString()))
			        .body(userDTO);
		} catch (URISyntaxException exc) {
			log.error("Register failed: " + userProps, exc);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
    }

    private void setAccountTypeIfNotSet(Map<String, Object> userProps, AccountType accountType) {
        if (!userProps.containsKey(UserField.ACCOUNT_TYPE)) {
            userProps.put(UserField.ACCOUNT_TYPE, accountType);
        }
    }

    private void sendActivationCode(User user) {
        if (user.getAccountType() == AccountType.EMAIL) {
            mailService.sendActivationEmail(user);
        }
        else {
            smsService.sendActivationMessage(user);
        }
    }

    /**
     * POST /register/mobile : register the user.
     *
     * @return the ResponseEntity with status 201 (Created) if the user is
     *         registered or 400 (Bad Request) if the login or e-mail is already
     *         in use
     */
    @PostMapping(path = "/register2/mobile", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE })
    @Timed
    @Secured(AuthoritiesConstants.GUEST)
    @ApiOperation(value = "Register or activate a pre-activate user (for mobile by email)",
        notes = "The service creates a new user or activates a pre-activate user" + "\n" +
                "<ul>" +
                "<li>" + "If client does not provide token or provided token belongs to beecow-user, the service creates a new user and responds 201 with access-token if success." + "</li>" +
                "<li>" + "If client provides a token which belongs to a pre-activate user, the service activates the user and responds 200 if success." + "</li>" +
                "</ul>")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "User is created"),
        @ApiResponse(code = 200, message = "User is activated"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 403, message = "Forbidden, logged in user is activated"),
        @ApiResponse(code = 409, message = "Email or login name has already exists, or database constraints violated"),})
    public ResponseEntity<UserDTO> upgradePreActivateAccount2ByEmail(@RequestBody Map<String, Object> userProps) {
        log.debug("REST request to update pre-activate acocunt by email: {}", userProps);
        setAccountTypeIfNotSet(userProps, AccountType.EMAIL);
        return upgradePreActivateAccount2(userProps, true);
    }

    /**
     * POST /register/mobile/phone : register the user.
     *
     * @return the ResponseEntity with status 201 (Created) if the user is
     *         registered or 400 (Bad Request) if the login or e-mail is already
     *         in use
     */
    @PostMapping(path = "/register2/mobile/phone", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE })
    @Timed
    @Secured(AuthoritiesConstants.GUEST)
    @ApiOperation(value = "Register or activate a pre-activate user (for mobile by phone number)",
        notes = "The service creates a new user or activates a pre-activate user" + "\n" +
                "<ul>" +
                "<li>" + "If client does not provide token or provided token belongs to beecow-user, the service creates a new user and responds 201 with access-token if success." + "</li>" +
                "<li>" + "If client provides a token which belongs to a pre-activate user, the service activates the user and responds 200 if success." + "</li>" +
                "</ul>")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "User is created"),
        @ApiResponse(code = 200, message = "User is activated"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 403, message = "Forbidden, logged in user is activated"),
        @ApiResponse(code = 409, message = "Email or login name has already exists, or database constraints violated"),})
    public ResponseEntity<UserDTO> upgradePreActivateAccount2ByPhone(@RequestBody Map<String, Object> userProps) {
        log.debug("REST request to update pre-activate acocunt by phone number: {}", userProps);
        setAccountTypeIfNotSet(userProps, AccountType.MOBILE);
        return upgradePreActivateAccount2(userProps, true);
    }

    /**
     * Upgrade pre-activate user (by email or phone number).
     */
    private ResponseEntity<UserDTO> upgradePreActivateAccount2(final Map<String, Object> userProps, boolean fromMobileApp) {
        User user = getCurrentPreactivateUser();
        if (user == null) {
        	return registerAccount2(userProps);
        }
        // Only perform activation for guest user
        user = userService.upgradePreActivateAccount(user, userProps, fromMobileApp);
        sendActivationCode(user);
        UserDTO userDTO = userMapper.userToUserDTO(user);
        return ResponseEntity.ok(userDTO);
    }

    /**
     * Get current pre-activate user
     * @return current pre-activate user, null if current user is not pre-activate user
     */
    private User getCurrentPreactivateUser() {
    	Long currentUserId = SecurityUtils.getCurrentUserId();
    	if (currentUserId == null) {
    		return null;
    	}
        Optional<User> userOptional = userService.getUserWithAuthorities(currentUserId);
        if (!userOptional.isPresent()) {
        	return null;
        }
        User user = userOptional.get();
        if (UserUtil.isBeecowUser(user) || !UserUtil.isGuest(user)) {
        	return null;
        }
        return user;
    }

    /**
     * GET  /activate : activate the registered user.
     *
     * @param activationKey the activation key
     * @return the ResponseEntity with status 200 (OK) and the activated user in body, or status 500 (Internal Server Error) if the user couldn't be activated
     */
    @GetMapping("/activate")
    @Timed
    public ResponseEntity<AuthUserDTO> activateAccount(@RequestParam Long userId, @RequestParam(value = "key") String activationKey) {
        log.debug("REST request to activate for user ID '{}' with activate key {}", userId, activationKey);
        return userService.getUserWithAuthorities(userId)
            .map(user -> {
                // Activation key still valid and equal to input activation key
                boolean valid = UserUtil.isWaitingForActivate(user);
                boolean match = activationKey.equals(user.getActivationKey());
                if (valid && match) {
                    // Activate user
                    return userService.activateRegistration(userId, activationKey)
                        .map(updatedUser -> {
                            // Create token for user to access the system
                            String token = tokenProviderService.createToken(updatedUser, true);
                            String authHeaderValue = "Bearer " + token;
                            AuthUserDTO userDTO = userMapper.userToAuthUserDTO(updatedUser);
                            userDTO.setAccessToken(token);
                            Map<String, Object> settings = userSettingValueServiceClient.getAllBcUserSettingValues(user.getId(), null, authHeaderValue);
                            userDTO.setSettings(settings);
                            sendWelcomeEmail(user);
                            return new ResponseEntity<>(userDTO, HttpStatus.OK);
                        })
                        .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).headers(HeaderUtil.createFailureAlert("user-account", "activationfailed", "Activation failed and return null")).body((AuthUserDTO) null));
                }
                else if (valid && !match) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).headers(HeaderUtil.createFailureAlert("user-account", "activationinvalid", "Activation code does not match")).body((AuthUserDTO) null);
                }
                else {
                    return ResponseEntity.status(HttpStatus.GONE).headers(HeaderUtil.createFailureAlert("user-account", "activationexpired", "Activation code is not expired")).body((AuthUserDTO) null);
                }
            })
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).headers(HeaderUtil.createFailureAlert("user-account", "usernotfound", "User with given ID was not found")).body((AuthUserDTO) null));
    }

    private void sendWelcomeEmail(User user) {
        if (user.getAccountType() == AccountType.EMAIL) {
            mailService.sendWelcomeEmail(user);
        }
    }

    /**
     * POST  /resend-activation-code : Re-send activation code.
     * @param userId
     * @return
     */
    @ApiOperation("Resend activation code")
    @GetMapping("/resend-activation-code")
    @Timed
    public ResponseEntity<Void> resendActivationCode(@RequestParam long userId) {
        log.debug("REST request to resend activation key for user ID '{}'", userId);
        User updatedUser = userService.updateActivationCode(userId);
        if (updatedUser == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).headers(HeaderUtil.createFailureAlert("user-account", "usernotfound", "User with given ID was not found")).body(null);
        }
        sendActivationCode(updatedUser);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * GET  /authenticate : check if the user is authenticated, and return its login.
     *
     * @param request the HTTP request
     * @return the login if the user is authenticated
     */
    @GetMapping("/authenticate")
    @Timed
    public String isAuthenticated(HttpServletRequest request) {
        log.debug("REST request to check if the current user is authenticated");
        return request.getRemoteUser();
    }

    /**
     * GET  /account : get the current user.
     *
     * @return the ResponseEntity with status 200 (OK) and the current user in body, or status 500 (Internal Server Error) if the user couldn't be returned
     */
    @GetMapping("/account")
    @Timed
    public ResponseEntity<UserDTO> getAccount() {
        return Optional.ofNullable(userService.getUserWithAuthorities())
            .map(user -> new ResponseEntity<>(userMapper.userToUserDTO(user), HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * GET  /users/email?email=:email : get the user.
     *
     * @param email the email of the user to find
     * @return the ResponseEntity with status 200 (OK) and with body the "login" user, or with status 404 (Not Found)
     */
    @GetMapping("/account/email")
    @Timed
    public ResponseEntity<UserDTO> getUserByEmail(@RequestParam String email) {
        log.debug("REST request to get User : {}", email);
        return userService.getUserWithAuthoritiesByEmail(email)
            .map(user -> new ResponseEntity<>(userMapper.userToUserDTO(user), HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * GET  /users/mobile?mobile=:mobile : get the user.
     *
     * @param mobile the mobile of the user to find
     * @return the ResponseEntity with status 200 (OK) and with body the "login" user, or with status 404 (Not Found)
     */
    @GetMapping("/account/mobile")
    @Timed
    public ResponseEntity<UserDTO> getUserByMobile(@RequestParam String mobile) {
        log.debug("REST request to get User : {}", mobile);
        return userService.getUserWithAuthoritiesByMobile(PhoneDtoUtil.stringToPhoneDTO(mobile))
            .map(user -> new ResponseEntity<>(userMapper.userToUserDTO(user), HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * POST  /account : update the current user information.
     *
     * @param userDTO the current user information
     * @return the ResponseEntity with status 200 (OK), or status 400 (Bad Request) or 500 (Internal Server Error) if the user couldn't be updated
     */
    @PostMapping("/account")
    @Timed
    @ApiOperation(value = "Edit user information")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "User is activated"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 404, message = "User not found, user maybe deleted"),
        @ApiResponse(code = 401, message = "Unauthorized, token is required"),
        @ApiResponse(code = 403, message = "Forbidden, guest token is not allowed to edit user information"),
        @ApiResponse(code = 409, message = "Email or login name has already exists, or database constraints violated"),})
    public ResponseEntity<UserDTO> saveAccount(@Valid @RequestBody UserDTO userDTO) {
        TokenUserDetails currentUser = SecurityUtils.getUserDetails(TokenUserDetails.class);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).headers(HeaderUtil.createFailureAlert("user-account", "unauthorized", "User is not authorized")).body(null);
        }
        if (UserUtil.isGuest(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).headers(HeaderUtil.createFailureAlert("user-account", "forbidden", "Guest is not allowed to edit user information")).body(null);
        }
        if (StringUtils.isNotBlank(userDTO.getEmail())) {
            Optional<User> existingUser = userService.getUserWithAuthoritiesByEmail(userDTO.getEmail());
            if (existingUser.isPresent() && (!existingUser.get().getLogin().equalsIgnoreCase(userDTO.getLogin()))) {
                return ResponseEntity.status(HttpStatus.CONFLICT).headers(HeaderUtil.createFailureAlert("user-account", "emailexists", "Email already in use")).body(null);
            }
        }
        return userService.getUserWithAuthorities(currentUser.getId())
            .map(u -> {
                User updatedUser = userService.updateUser(userDTO.getFirstName(), userDTO.getLastName(), userDTO.getDisplayName(), userDTO.getEmail(),
                    userDTO.getDateOfBirth(), userDTO.getGender(), userDTO.getAvatarUrl(), userDTO.getLocationCode(), userDTO.getLangKey());
                UserDTO updatedUserDTO = userMapper.userToUserDTO(updatedUser);
                return ResponseEntity.ok(updatedUserDTO);
            })
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * POST  /account : update the current user information.
     *
     * @return the ResponseEntity with status 200 (OK), or status 400 (Bad Request) or 500 (Internal Server Error) if the user couldn't be updated
     */
    @PostMapping("/account2")
    @Timed
    @ApiOperation(value = "Edit user information (allow edit indevidual property)")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "User is activated"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 404, message = "User not found, user maybe deleted"),
        @ApiResponse(code = 401, message = "Unauthorized, token is required"),
        @ApiResponse(code = 403, message = "Forbidden, guest token is not allowed to edit user information"),
        @ApiResponse(code = 409, message = "Email or login name has already exists, or database constraints violated"),})
    public ResponseEntity<UserDTO> saveAccount2(@RequestBody Map<String, Object> userProps) {
        TokenUserDetails currentUser = SecurityUtils.getUserDetails(TokenUserDetails.class);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).headers(HeaderUtil.createFailureAlert("user-account", "unauthorized", "User is not authorized")).body(null);
        }
        if (UserUtil.isGuest(currentUser)) {
            // Only allow guest to update location-code and language
            Set<String> expectingKeys = new HashSet<>();
            expectingKeys.add(UserField.LOCATION_CODE);
            expectingKeys.add(UserField.LANG_KEY);
            userProps.keySet().retainAll(expectingKeys);
        }
        return userService.getUserWithAuthorities(currentUser.getId())
            .map(u -> {
                User updatedUser = userService.updateUser(userProps);
                UserDTO updatedUserDTO = userMapper.userToUserDTO(updatedUser);
                return ResponseEntity.ok(updatedUserDTO);
            })
            .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * PUT  /account/change_password : Change user's password when current password is correct, just apply for Beecow Account register by Phone or Email only.
     *
     * @param password the OldAndNewPasswordVM model's instance include old and new password.
     */
    @PutMapping(path = "/account/change_password")
    @Timed
    @ApiOperation(value = "Change user's password when current password is correct, just apply for Beecow Account register by Phone or Email only.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Change password successfuly"),
        @ApiResponse(code = 400, message = "Bad Request - New password is wrong format"),
        @ApiResponse(code = 406, message = "Not Acceptable - Current password is Incorrect"),
        @ApiResponse(code = 409, message = "Conflict - New password must be differrent from Current password")})
    public ResponseEntity<?> changePassword(@RequestBody CurrentAndNewPasswordVM password) {
            if (!checkPasswordLength(password.getNewPassword())) {
                return new ResponseEntity<>("New password is wrong format", HttpStatus.BAD_REQUEST);
        }

        if (password.getNewPassword().equals(password.getCurrentPassword())) {
            return new ResponseEntity<>("New password must be differrent from Current password", HttpStatus.CONFLICT);
        }

        // Check current password is OK or not
        if (!userService.checkCurrentPassword(password.getCurrentPassword())) {
                return new ResponseEntity<>("Current password is Incorrect", HttpStatus.NOT_ACCEPTABLE);
        }

        // Now change password
        userService.changePassword(password.getNewPassword(), password.getDeviceToken());
        return new ResponseEntity<>("Change password successfuly", HttpStatus.OK);
    }

    /**
     * PUT  /account/force_change_password : changes the current user's password
     *
     * @param password the new password
     * @return the ResponseEntity with status 200 (OK), or status 400 (Bad Request) if the new password is not strong enough
     */
    @PutMapping(path = "/account/force_change_password")
    @Timed
    @ApiOperation(value = "Force change user's password")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Change password successfuly"),
        @ApiResponse(code = 400, message = "Bad Request - New password is wrong format")})
    public ResponseEntity<?> forceChangePassword(@RequestBody String password) {
        if (!checkPasswordLength(password)) {
            return new ResponseEntity<>("New password is wrong format", HttpStatus.BAD_REQUEST);
        }

        userService.changePassword(password);
        return new ResponseEntity<>("Change password successfuly", HttpStatus.OK);
    }


    /**
     * POST   /account/reset_password/init : Send an e-mail to reset the password of the user
     *
     * @param mail the mail of the user
     * @return the ResponseEntity with status 200 (OK) if the e-mail was sent, or status 400 (Bad Request) if the e-mail address is not registered
     */
    @PostMapping(path = "/account/reset_password/init")
    @Timed
    public ResponseEntity<?> requestPasswordReset(@RequestBody String mail) {
        return doRequestPasswordResetByEmail(mail);
    }

    /**
     * POST   /account/reset_password/init : Send an e-mail to reset the password of the user
     *
     * @param mail the mail of the user
     * @return the ResponseEntity with status 200 (OK) if the e-mail was sent, or status 400 (Bad Request) if the e-mail address is not registered
     */
    @PostMapping(path = "/account/reset_password/email/init")
    @Timed
    public ResponseEntity<?> requestPasswordReset(@Valid @RequestBody EmailVM mail) {
        return doRequestPasswordResetByEmail(mail.getEmail());
    }

    private ResponseEntity<?> doRequestPasswordResetByEmail(String email) {
        return userService.requestPasswordReset(email)
            .map(user -> {
                mailService.sendPasswordResetMail(user);
                return new ResponseEntity<>("e-mail was sent", HttpStatus.OK);
            })
            .orElse(new ResponseEntity<>("e-mail address not found", HttpStatus.NOT_FOUND));
    }

    /**
     * POST   /account/reset_password/init : Send an e-mail to reset the password of the user
     *
     * @return the ResponseEntity with status 200 (OK) if the e-mail was sent, or status 400 (Bad Request) if the e-mail address is not registered
     */
    @PostMapping(path = "/account/reset_password/mobile/init")
    @Timed
    public ResponseEntity<?> requestPasswordReset(@Valid @RequestBody PhoneDTO mobile) {
        return userService.requestPasswordReset(mobile)
            .map(user -> {
                smsService.sendPasswordResetMessage(user);
                return new ResponseEntity<>("SMS was sent", HttpStatus.OK);
            })
            .orElse(new ResponseEntity<>("Phone number not found", HttpStatus.NOT_FOUND));
    }

    /**
     * POST   /account/reset_password/finish : Finish to reset the password of the user
     *
     * @param keyAndPassword the generated key and the new password
     * @return the ResponseEntity with status 200 (OK) if the password has been reset,
     * or status 400 (Bad Request) or 500 (Internal Server Error) if the password could not be reset
     */
    @PostMapping(path = "/account/reset_password/finish")
    @Timed
    public ResponseEntity<AuthUserDTO> finishPasswordReset(@Valid @RequestBody KeyAndPasswordVM keyAndPassword) {
        return doFinishPasswordReset(keyAndPassword);
    }

    private ResponseEntity<AuthUserDTO> doFinishPasswordReset(KeyAndPasswordVM keyAndPassword) {
        String key = keyAndPassword.getKey();
        return userService.findOneByResetKey(key)
            .map(user -> {
                // Reset key still valid and equal to input reset key
                boolean valid = userService.isWaitingForResetPassword(user);
                if (valid) {
                    // Activate user
                    return userService.completePasswordReset(keyAndPassword.getNewPassword(), key)
                        .map(updatedUser -> {
                            // Create token for user to access the system
                            String token = tokenProviderService.createToken(updatedUser, true);
                            String authHeaderValue = "Bearer " + token;
                            AuthUserDTO userDTO = userMapper.userToAuthUserDTO(updatedUser);
                            userDTO.setAccessToken(token);
                            Map<String, Object> settings = userSettingValueServiceClient.getAllBcUserSettingValues(user.getId(), null, authHeaderValue);
                            userDTO.setSettings(settings);
                            sendPasswordChangedEmail(user);
                            return new ResponseEntity<>(userDTO, HttpStatus.OK);
                        })
                        .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).headers(HeaderUtil.createFailureAlert("user-account", "resetfailed", "Reset failed and return null")).body((AuthUserDTO) null));
                }
                else {
                    return ResponseEntity.status(HttpStatus.GONE).headers(HeaderUtil.createFailureAlert("user-account", "resetexpired", "Reset code is not expired")).body((AuthUserDTO) null);
                }
            })
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).headers(HeaderUtil.createFailureAlert("user-account", "usernotfound", "User with given ID was not found")).body((AuthUserDTO) null));
    }

    private void sendPasswordChangedEmail(User user) {
        if (user.getAccountType() == AccountType.EMAIL) {
            mailService.sendPasswordChangedEmail(user);
        }
    }

    private boolean checkPasswordLength(String password) {
        return (!StringUtils.isEmpty(password) &&
            password.length() >= ManagedUserVM.PASSWORD_MIN_LENGTH &&
            password.length() <= ManagedUserVM.PASSWORD_MAX_LENGTH);
    }

}
