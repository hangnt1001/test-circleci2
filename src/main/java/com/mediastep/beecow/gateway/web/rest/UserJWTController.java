/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.web.rest;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;
import com.mediastep.beecow.common.config.UserServiceConfig;
import com.mediastep.beecow.common.dto.SwitchProfileDTO;
import com.mediastep.beecow.common.dto.TokenDTO;
import com.mediastep.beecow.common.errors.EntityNotFoundException;
import com.mediastep.beecow.common.errors.UnauthorizedException;
import com.mediastep.beecow.common.security.AuthoritiesConstants;
import com.mediastep.beecow.common.security.SecurityUtils;
import com.mediastep.beecow.common.security.TokenUserAuthority;
import com.mediastep.beecow.common.security.TokenUserDetails;
import com.mediastep.beecow.common.util.LocationCodeHelper;
import com.mediastep.beecow.gateway.client.BcUserSettingValueServiceClient;
import com.mediastep.beecow.gateway.client.UserStoreServiceClient;
import com.mediastep.beecow.gateway.domain.Authority;
import com.mediastep.beecow.gateway.domain.User;
import com.mediastep.beecow.gateway.security.UserNotActivatedException;
import com.mediastep.beecow.gateway.security.jwt.JWTConfigurer;
import com.mediastep.beecow.gateway.security.jwt.TokenProviderService;
import com.mediastep.beecow.gateway.service.UserService;
import com.mediastep.beecow.gateway.service.mapper.UserMapper;
import com.mediastep.beecow.gateway.web.rest.errors.ErrorConstants;
import com.mediastep.beecow.gateway.web.rest.vm.LoginVM;
import com.mediastep.beecow.gateway.web.rest.vm.PhoneLoginVM;
import com.mediastep.beecow.user.dto.AuthUserDTO;
import com.mediastep.beecow.user.dto.UserStoreDTO;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@RequestMapping("/api")
public class UserJWTController {

    private final String BEECOW_LOGIN = "beecowuser";

    private final String BEECOW_LOGIN_SEP = "_";

    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(UserJWTController.class);

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

    @Inject
    private BcUserSettingValueServiceClient userSettingValueServiceClient;

    @Inject
    private UserStoreServiceClient userStoreServiceClient;

    @PostMapping("/authenticate/admin")
    @Timed
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Login/password mismatch"),
            @ApiResponse(code = 403, message = "Forbidden, user could be deactivated or not activated yet, check responded entity for more information"),
            @ApiResponse(code = 404, message = "User not found")})
    public ResponseEntity<?> authorizeAdmin(@Valid @RequestBody LoginVM loginVM, HttpServletResponse response) {

        UsernamePasswordAuthenticationToken authenticationToken =
            new UsernamePasswordAuthenticationToken(loginVM.getUsername(), loginVM.getPassword());

        try {
        	String login = loginVM.getUsername();
            Optional<User> userOptional = userService.getUserWithAuthoritiesByLogin(login);
            if (!userOptional.isPresent()) {
            	throw new EntityNotFoundException(User.class, ErrorConstants.ERR_USER_NOT_FOUND, login);
            }
            User user = userOptional.get();
            if (!user.getAuthorities().contains(Authority.ADMIN)) {
            	throw new UnauthorizedException("Only admin can login");
            }
            // Generate token
            Authentication authentication = this.authenticationManager.authenticate(authenticationToken);
            boolean rememberMe = (loginVM.isRememberMe() == null) ? false : loginVM.isRememberMe();
            String jwt = tokenProviderService.createToken(authentication, rememberMe);
            String authHeaderValue = "Bearer " + jwt;
            response.addHeader(JWTConfigurer.AUTHORIZATION_HEADER, authHeaderValue);
            AuthUserDTO userDTO = userMapper.userToAuthUserDTO(user);
            userDTO.setAccessToken(jwt);
            // Get user settings
            Map<String, Object> settings = userSettingValueServiceClient.getAllBcUserSettingValues(user.getId(), null, authHeaderValue);
            userDTO.setSettings(settings);
            // Get store information
            UserStoreDTO userStore = userStoreServiceClient.findStoreByUser(user.getId(), authHeaderValue);
            if(userStore != null) {
                userDTO.setStore(userStore);
            }
            return ResponseEntity.ok(userDTO);
        }
        catch (AuthenticationException exception) {
        	return toResponseEntity(exception);
        }
    }

    @PostMapping("/authenticate")
    @Timed
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Login/password mismatch"),
            @ApiResponse(code = 403, message = "Forbidden, user could be deactivated or not activated yet, check responded entity for more information"),
            @ApiResponse(code = 404, message = "User not found")})
    public ResponseEntity<?> authorize(@Valid @RequestBody LoginVM loginVM, HttpServletResponse response) {

        UsernamePasswordAuthenticationToken authenticationToken =
            new UsernamePasswordAuthenticationToken(loginVM.getUsername(), loginVM.getPassword());

        try {
            Authentication authentication = this.authenticationManager.authenticate(authenticationToken);
            boolean rememberMe = (loginVM.isRememberMe() == null) ? false : loginVM.isRememberMe();
            String jwt = tokenProviderService.createToken(authentication, rememberMe);
            response.addHeader(JWTConfigurer.AUTHORIZATION_HEADER, "Bearer " + jwt);
            return ResponseEntity.ok(new TokenDTO(jwt, null));
        } catch (AuthenticationException exception) {
        	return toResponseEntity(exception);
        }
    }

    private ResponseEntity<?> toResponseEntity(Throwable exc) {
    	Throwable rootCause = ExceptionUtils.getRootCause(exc);
    	if (rootCause == null) {
    		rootCause = exc;
    	}
    	if (rootCause instanceof EntityNotFoundException) {
    		return new ResponseEntity<>(((EntityNotFoundException) rootCause).getErrorVM(), HttpStatus.NOT_FOUND);
    	}
    	else if (rootCause instanceof UserNotActivatedException) {
    		return new ResponseEntity<>(((UserNotActivatedException) rootCause).getErrorVM(), HttpStatus.FORBIDDEN);
    	}
    	else {
    		return new ResponseEntity<>(Collections.singletonMap("AuthenticationException", rootCause.getLocalizedMessage()), HttpStatus.UNAUTHORIZED);
    	}
    }

    @PostMapping("/authenticate/mobile")
    @Timed
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Login/password mismatch"),
            @ApiResponse(code = 403, message = "Forbidden, user could be deactivated or not activated yet, check responded entity for more information"),
            @ApiResponse(code = 404, message = "User not found")})
    public ResponseEntity<?> authorizeMobile(@Valid @RequestBody LoginVM loginVM, HttpServletResponse response) {

        UsernamePasswordAuthenticationToken authenticationToken =
            new UsernamePasswordAuthenticationToken(loginVM.getUsername(), loginVM.getPassword());

        try {
        	String login = loginVM.getUsername();
            Optional<User> userOptional = userService.getUserWithAuthoritiesByLogin(login);
            if (!userOptional.isPresent()) {
            	throw new EntityNotFoundException(User.class, ErrorConstants.ERR_USER_NOT_FOUND, login);
            }
            // Generate token
            Authentication authentication = this.authenticationManager.authenticate(authenticationToken);
            boolean rememberMe = (loginVM.isRememberMe() == null) ? false : loginVM.isRememberMe();
            String jwt = tokenProviderService.createToken(authentication, rememberMe);
            String authHeaderValue = "Bearer " + jwt;
            response.addHeader(JWTConfigurer.AUTHORIZATION_HEADER, authHeaderValue);
            User user = userOptional.get();
            AuthUserDTO userDTO = userMapper.userToAuthUserDTO(user);
            userDTO.setAccessToken(jwt);
            // Get user settings
            Map<String, Object> settings = userSettingValueServiceClient.getAllBcUserSettingValues(user.getId(), null, authHeaderValue);
            userDTO.setSettings(settings);
            // Get store information
            UserStoreDTO userStore = userStoreServiceClient.findStoreByUser(user.getId(), authHeaderValue);
            if(userStore != null) {
                userDTO.setStore(userStore);
            }
            return ResponseEntity.ok(userDTO);
        }
        catch (AuthenticationException exception) {
        	return toResponseEntity(exception);
        }
    }

    @PostMapping("/authenticate/mobile/phone")
    @Timed
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Login/password mismatch"),
            @ApiResponse(code = 403, message = "Forbidden, user could be deactivated or not activated yet, check responded entity for more information"),
            @ApiResponse(code = 404, message = "User not found")})
    public ResponseEntity<?> authorizeMobileByPhone(@Valid @RequestBody PhoneLoginVM loginVM, HttpServletResponse response) {
        String login1 = loginVM.getMobile().toStringFromPhoneWithZero();
        String login2 = loginVM.getMobile().toStringFromPhoneWithoutZero();
        try {
            Optional<User> userOptional = userService.getUserWithAuthoritiesByLogin(login1, login2);
            if (!userOptional.isPresent()) {
            	throw new EntityNotFoundException(User.class, ErrorConstants.ERR_USER_NOT_FOUND, login1, login2);
            }
            User user = userOptional.get();
            UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(user.getLogin(), loginVM.getPassword());
            AuthUserDTO userDTO = userMapper.userToAuthUserDTO(user);
            // Generate token
            Authentication authentication = this.authenticationManager.authenticate(authenticationToken);
            boolean rememberMe = (loginVM.isRememberMe() == null) ? false : loginVM.isRememberMe();
            String jwt = tokenProviderService.createToken(authentication, rememberMe);
            String authHeaderValue = "Bearer " + jwt;
            response.addHeader(JWTConfigurer.AUTHORIZATION_HEADER, authHeaderValue);
            userDTO.setAccessToken(jwt);
            // Get user settings
            Map<String, Object> settings = userSettingValueServiceClient.getAllBcUserSettingValues(user.getId(), null, authHeaderValue);
            userDTO.setSettings(settings);
            //get store information
            UserStoreDTO userStore = userStoreServiceClient.findStoreByUser(user.getId(), authHeaderValue);
            if(userStore != null) {
                if(!ObjectUtils.isEmpty(userStore)) {
                    userDTO.setStore(userStore);
                }
            }
            return ResponseEntity.ok(userDTO);
        }
        catch (AuthenticationException exception) {
        	return toResponseEntity(exception);
        }
    }

    @ApiOperation("switch profile between USER,STORE and COMPANY")
    @PostMapping("/authenticate/switch-profile")
    @Timed
    public ResponseEntity<?> authorizeSwitchProfile(@Valid @RequestBody @ApiParam(value = "object keep profile type and storeID") SwitchProfileDTO switchProfileDTO, HttpServletRequest request, HttpServletResponse response) {
        try {
            String jwt = "";
            switch (switchProfileDTO.getAuthorTypeEnum()) {
                case STORE:
                    if(switchProfileDTO.getPageId() != null && switchProfileDTO.getPageId() > 0) {
                        jwt = this.tokenProviderService.createStoreToken(switchProfileDTO.getPageId(), true, request.getHeader("Authorization"));
                    }
                    break;
                case USER:
                    TokenUserDetails userDetials = (TokenUserDetails)SecurityUtils.getUserDetails();
                    userDetials.getAuthorities().remove(new TokenUserAuthority(AuthoritiesConstants.STORE));
                    userDetials.getAuthorities().remove(new TokenUserAuthority(AuthoritiesConstants.COMPANY));
                    Authentication authentication = new UsernamePasswordAuthenticationToken(userDetials, null, userDetials.getAuthorities());
                    jwt = this.tokenProviderService.createToken(authentication, true);
                    break;
                case COMPANY:
                    jwt = this.tokenProviderService.createCompanyToken(true, request.getHeader("Authorization"));
                    break;
                default:
                    break;
            }

            response.addHeader(JWTConfigurer.AUTHORIZATION_HEADER, "Bearer " + jwt);
            User user = userService.getUserWithAuthoritiesByLogin(SecurityUtils.getCurrentUserLogin()).get();
            AuthUserDTO userDTO = userMapper.userToAuthUserDTO(user);
            userDTO.setAccessToken(jwt);
            return ResponseEntity.ok(userDTO);
        } catch (AuthenticationException exception) {
            return new ResponseEntity<>(Collections.singletonMap("AuthenticationException",exception.getLocalizedMessage()), HttpStatus.UNAUTHORIZED);
        }
    }

    @ApiOperation("Get to STORE token by URL")
    @ApiResponses(value = {
        @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 404, message = "Store not found")
    })
    @GetMapping("/authenticate/store/{url}")
    @Timed
    @Secured({AuthoritiesConstants.EDITOR, AuthoritiesConstants.ADMIN})
    public ResponseEntity<TokenDTO> authorizeSwitchToStoreByURL(@Valid @PathVariable String url, HttpServletRequest request, HttpServletResponse response) {
    	if (StringUtils.isBlank(url)) {
    		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    	}
        String jwt = tokenProviderService.createStoreTokenByUrl(url, request.getHeader("Authorization"));
        if (jwt == null) {
        	return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(new TokenDTO(jwt, null));
    }

    @ApiOperation("Get to STORE token by URL")
    @ApiResponses(value = {
        @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 404, message = "Store not found")
    })
    @GetMapping("/authenticate/company/{url}")
    @Timed
    @Secured({AuthoritiesConstants.EDITOR, AuthoritiesConstants.ADMIN})
    public ResponseEntity<TokenDTO> authorizeSwitchToCompanyByURL(@Valid @PathVariable String url, HttpServletRequest request, HttpServletResponse response) {
    	if (StringUtils.isBlank(url)) {
    		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    	}
        String jwt = tokenProviderService.createCompanyTokenByUrl(url, request.getHeader("Authorization"));
        if (jwt == null) {
        	return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(new TokenDTO(jwt, null));
    }

    @GetMapping("/logout")
    @Timed
    @ApiOperation(value = "Logout current user",
        notes = "The service return Beecow User information and access token. " +
                "The Beecow User locationCode equals to current user locationCode. " +
                "If current user locationCode is empty or no token is provided, the Global BeecowUser will be returned")
    public ResponseEntity<AuthUserDTO> logout(HttpServletResponse response) {
        // Find Beecow account based on current user location and language
        User curUser = userService.getUserWithAuthorities();
        String beecowLogin = getBeecowUserLogin(curUser);
        Optional<User> beecowUserOpt = userService.getUserWithAuthoritiesByLogin(beecowLogin);
        // Find global Beecow account if not found
        if (!beecowUserOpt.isPresent()) {
            beecowUserOpt = userService.getUserWithAuthoritiesByLogin(BEECOW_LOGIN);
        }
        // Respond OK, user information and token
        return beecowUserOpt
             .map(beecowUser -> {
                AuthUserDTO authUserDTO = buildLogoutResponse(beecowUser, response);
                /**************************************************
                 * NOTE: for front-end convenience
                 */
                authUserDTO.setLocationCode(null);
                authUserDTO.setLangKey(null);
                /**************************************************/
                return ResponseEntity.ok(authUserDTO);
            })
            .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body((AuthUserDTO) null));
    }

    private AuthUserDTO buildLogoutResponse(User beecowUser, HttpServletResponse response) {
        AuthUserDTO beecowUserDTO = userMapper.userToAuthUserDTO(beecowUser);
        // Generate token
        String jwt = tokenProviderService.createToken(beecowUser, true);
        String authHeaderValue = "Bearer " + jwt;
        response.addHeader(JWTConfigurer.AUTHORIZATION_HEADER, authHeaderValue);
        beecowUserDTO.setAccessToken(jwt);
        return beecowUserDTO;
    }

    /**
     * Get Beecow login-name which country code equals to country code of input user.
     * <pre>
     * For example:
     * * Input user = {locationCode = "MM-05", langKey = "my"}, return "beecow_mm" (input langKey is not special value)
     * * Input user = {locationCode = "MM-05", langKey = "my-zawgyi"}, return "beecow_mm_my-zawgyi" (input langKey is special that may has a Beecow account associate with)
     * </pre>
     * @param user
     * @return
     */
    private String getBeecowUserLogin(User user) {
        StringBuilder beecowLogin = new StringBuilder(BEECOW_LOGIN);
        if (user == null) {
            return beecowLogin.toString();
        }
        // Get input user country code and append to Beecow login-name
        String countryCode = LocationCodeHelper.getCountryCode(user.getLocationCode());
        if (StringUtils.isNotBlank(countryCode)) {
            beecowLogin.append(BEECOW_LOGIN_SEP).append(countryCode);
        }
        // Get input user language-key and append to Beecow login-name if required
        String langKey = user.getLangKey();
        if (userServiceConfig.getSpecialBeecowUserLangKeys().contains(langKey)) {
            beecowLogin.append(BEECOW_LOGIN_SEP).append(langKey);
        }
        return beecowLogin.toString().toLowerCase();
    }
}
