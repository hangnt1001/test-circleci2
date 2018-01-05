/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.web.rest;

import com.mediastep.beecow.gateway.domain.User;
import com.mediastep.beecow.gateway.security.social.CustomSignInAdapter;
import com.mediastep.beecow.gateway.service.SocialService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.social.connect.Connection;
import org.springframework.social.connect.web.ProviderSignInUtils;
import org.springframework.social.support.URIBuilder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.view.RedirectView;

import javax.inject.Inject;

@RestController
@RequestMapping("/social")
public class SocialController {
    private final Logger log = LoggerFactory.getLogger(SocialController.class);

    @Inject
    private SocialService socialService;

    @Inject
    private ProviderSignInUtils providerSignInUtils;

    @Inject
    private CustomSignInAdapter customSignInAdapter;

    @GetMapping("/signup")
    public RedirectView signUp(WebRequest webRequest, @CookieValue(name = "NG_TRANSLATE_LANG_KEY", required = false, defaultValue = "\"en\"") String langKey, NativeWebRequest request) {
        try {
            Connection<?> connection = providerSignInUtils.getConnectionFromSession(webRequest);
            User user = socialService.createSocialUser(connection, null, langKey.replace("\"", ""), null);
            return new RedirectView(URIBuilder.fromUri(customSignInAdapter.signIn(user.getLogin(), connection, request))
                .build().toString(), true);
        } catch (Exception e) {
            log.error("Exception creating social user: ", e);
            return new RedirectView(URIBuilder.fromUri("/#/social-register/no-provider")
                .queryParam("success", "false")
                .build().toString(), true);
        }
    }
}
