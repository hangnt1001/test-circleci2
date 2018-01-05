/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.web.rest;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.support.OAuth2ConnectionFactory;
import org.springframework.social.oauth2.AccessGrant;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import com.codahale.metrics.annotation.Timed;
import com.mediastep.beecow.user.dto.AuthUserDTO;
import com.mediastep.beecow.gateway.service.SocialService;
import com.mediastep.beecow.gateway.web.rest.util.HeaderUtil;
import com.mediastep.beecow.gateway.web.rest.vm.SocialLoginVM;

@RestController
@RequestMapping("/api")
public class SocialResource {

    private final Logger log = LoggerFactory.getLogger(SocialResource.class);

    @Inject
    private SocialService socialService;

    @Inject
    private ConnectionFactoryLocator connectionFactoryLocator;

    @GetMapping("/social/signin/{providerId}")
    @Timed
    @Deprecated
    public ResponseEntity<AuthUserDTO> sigin(@PathVariable String providerId, @RequestParam String socialAccessToken, @RequestParam(required = false) Long mergeWithUserId,
        @CookieValue(name = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "\"en\"") String langKey, NativeWebRequest request) {
        try {
            OAuth2ConnectionFactory<?> connectionFactory = (OAuth2ConnectionFactory<?>) connectionFactoryLocator.getConnectionFactory(providerId);
            AccessGrant accessGrant = new AccessGrant(socialAccessToken);
            Connection<?> connection = connectionFactory.createConnection(accessGrant);
            AuthUserDTO userDTO = socialService.signIn(connection, mergeWithUserId, null, langKey, request);
            return ResponseEntity.ok().body(userDTO);
        }
        catch (Exception exc) {
            log.error("Exception while completing OAuth 2 connection: ", exc);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).headers(HeaderUtil.createFailureAlert("error", "social-signin", "Cannot sign-in with given social access token")).body(null);
        }
    }

    @PostMapping("/social/signin/{providerId}")
    @Timed
    public ResponseEntity<AuthUserDTO> sigin2(@PathVariable String providerId, @Valid @RequestBody SocialLoginVM loginVM, NativeWebRequest request, HttpServletResponse response) {
        try {
            OAuth2ConnectionFactory<?> connectionFactory = (OAuth2ConnectionFactory<?>) connectionFactoryLocator.getConnectionFactory(providerId);
            AccessGrant accessGrant = new AccessGrant(loginVM.getToken());
            Connection<?> connection = connectionFactory.createConnection(accessGrant);
            AuthUserDTO userDTO = socialService.signIn(connection, loginVM.getMergeWithUserId(), loginVM.getLocation(), loginVM.getLangKey(), request);
            return ResponseEntity.ok().body(userDTO);
        }
        catch (Exception exc) {
            log.error("Exception while completing OAuth 2 connection: ", exc);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).headers(HeaderUtil.createFailureAlert("error", "social-signin", "Cannot sign-in with given social access token")).body(null);
        }
    }
}
