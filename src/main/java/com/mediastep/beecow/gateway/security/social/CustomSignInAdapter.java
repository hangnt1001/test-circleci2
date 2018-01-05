/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.security.social;

import javax.inject.Inject;
import javax.servlet.http.Cookie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.web.SignInAdapter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import com.mediastep.beecow.gateway.config.JHipsterProperties;
import com.mediastep.beecow.gateway.security.jwt.TokenProviderService;

@Component
public class CustomSignInAdapter implements SignInAdapter {

    private final Logger log = LoggerFactory.getLogger(CustomSignInAdapter.class);

    @Inject
    private TokenProviderService tokenProviderService;

    @Inject
    private JHipsterProperties jHipsterProperties;

    @Override
    public String signIn(String userId, Connection<?> connection, NativeWebRequest request) {
        try {
            String jwt = tokenProviderService.createToken(userId, true);
            ServletWebRequest servletWebRequest = (ServletWebRequest) request;
            servletWebRequest.getResponse().addCookie(getSocialAuthenticationCookie(jwt));
        } catch (AuthenticationException exception) {
            log.error("Social authentication error");
        }
        return jHipsterProperties.getSocial().getRedirectAfterSignIn();
    }

    private Cookie getSocialAuthenticationCookie(String token) {
        Cookie socialAuthCookie = new Cookie("social-authentication", token);
        socialAuthCookie.setPath("/");
        socialAuthCookie.setMaxAge(10);
        return socialAuthCookie;
    }
}
