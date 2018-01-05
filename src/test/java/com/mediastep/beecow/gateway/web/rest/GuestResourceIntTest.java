/*******************************************************************************
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 01/01/2017
 * Author: Huyen Lam <email:huyen.lam@mediastep.com>
 *  
 *******************************************************************************/
package com.mediastep.beecow.gateway.web.rest;

import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.DEFAULT_AVATAR;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import com.mediastep.beecow.common.domain.enumeration.AccountType;
import com.mediastep.beecow.common.security.AuthoritiesConstants;
import com.mediastep.beecow.gateway.BeecowGatewayApp;
import com.mediastep.beecow.gateway.security.jwt.TokenProviderService;
import com.mediastep.beecow.gateway.service.UserService;
import com.mediastep.beecow.gateway.service.mapper.UserMapper;

/**
 * Test class for the GuestResource REST controller.
 *
 * @see UserResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = BeecowGatewayApp.class)
public class GuestResourceIntTest {

    @Inject
    private UserService userService;

    @Inject
    private UserMapper userMapper;

    @Inject
    private TokenProviderService tokenProviderService;

    private MockMvc restUserMockMvc;

    @Before
    public void setup() {
        GuestResource guestResource = new GuestResource();
        ReflectionTestUtils.setField(guestResource, "userService", userService);
        ReflectionTestUtils.setField(guestResource, "userMapper", userMapper);
        ReflectionTestUtils.setField(guestResource, "tokenProviderService", tokenProviderService);
        this.restUserMockMvc = MockMvcBuilders.standaloneSetup(guestResource).build();
    }

    @Test
    @Transactional
    public void testCreateGuest() throws Exception {
        String location = "vn-sg";
        String langKey = "en";
        restUserMockMvc.perform(post("/api/guest/{location}/{langKey}", location, langKey))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$.login").value(startsWith(UserService.LOGIN_NAME_PREFIX)))
                .andExpect(jsonPath("$.accountType").value(AccountType.PRE_ACTIVATE.toString()))
                .andExpect(jsonPath("$.locationCode").value(location.toUpperCase()))
                .andExpect(jsonPath("$.langKey").value(langKey))
                .andExpect(jsonPath("$.avatarUrl.urlPrefix").value(DEFAULT_AVATAR))
                .andExpect(jsonPath("$.authorities[*]").value(hasItem(AuthoritiesConstants.GUEST)))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @Transactional
    public void testCreateGuestLangKeyMissing() throws Exception {
        String location = "vn-sg";
        restUserMockMvc.perform(post("/api/guest/{location}", location))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void testCreateGuestLangKeyEmpty() throws Exception {
        String location = "vn-sg";
        String langKey = "";
        restUserMockMvc.perform(post("/api/guest/{location}/{langKey}", location, langKey))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void testCreateGuestLangKeyBlank() throws Exception {
        String location = "vn-sg";
        String langKey = "    ";
        restUserMockMvc.perform(post("/api/guest/{location}/{langKey}", location, langKey))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void testCreateGuestLangKeyTooLong() throws Exception {
        String location = "vn-sg";
        String langKey = "enenenenenenenenenenenenenenen";
        restUserMockMvc.perform(post("/api/guest/{location}/{langKey}", location, langKey))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    @Transactional
    public void testCreateGuestLocationCode() throws Exception {
        restUserMockMvc.perform(post("/api/guest"))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void testCreateGuestLocationCodeEmpty() throws Exception {
        String location = "";
        String langKey = "en";
        restUserMockMvc.perform(post("/api/guest/{location}/{langKey}", location, langKey))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void testCreateGuestLocationCodeBlank() throws Exception {
        String location = "    ";
        String langKey = "en";
        restUserMockMvc.perform(post("/api/guest/{location}/{langKey}", location, langKey))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void testCreateGuestLocationCodeTooLong() throws Exception {
        String location = "vn-sgsgsgsgsgsgsgsgsgsgsgsgsgsgsgsgsgsgsgsg";
        String langKey = "en";
        restUserMockMvc.perform(post("/api/guest/{location}/{langKey}", location, langKey))
                .andDo(MockMvcResultHandlers.print(System.err))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
    }
}
