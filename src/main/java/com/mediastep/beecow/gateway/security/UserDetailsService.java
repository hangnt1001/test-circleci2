/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.security;

import java.util.Locale;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mediastep.beecow.common.errors.EntityNotFoundException;
import com.mediastep.beecow.gateway.domain.User;
import com.mediastep.beecow.gateway.repository.UserRepository;
import com.mediastep.beecow.gateway.service.mapper.UserMapper;
import com.mediastep.beecow.gateway.service.util.UserUtil;
import com.mediastep.beecow.gateway.web.rest.errors.ErrorConstants;

/**
 * Authenticate a user from the database.
 */
@Component("userDetailsService")
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {

    private final Logger log = LoggerFactory.getLogger(UserDetailsService.class);

    @Inject
    private UserRepository userRepository;

    @Inject
    private UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(final String login) {
        log.debug("Authenticating {}", login);
        String lowercaseLogin = login.toLowerCase(Locale.ENGLISH);
        Optional<User> userFromDatabase = userRepository.findOneByLogin(lowercaseLogin);
        return userFromDatabase.map(user -> {
            if (!user.getActivated()) {
                throw new UserNotActivatedException(ErrorConstants.ERR_USER_NOT_FOUND, UserUtil.getUserStatus(user));
            }
            return userMapper.userToTokenUserDetails(user);
        }).orElseThrow(() -> new EntityNotFoundException(User.class, ErrorConstants.ERR_USER_NOT_FOUND));
    }
}
