/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.service;

import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.ENCRYPTED_DEFAUT_PASSWORD;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.createUser;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;
import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import com.mediastep.beecow.common.security.AuthoritiesConstants;
import com.mediastep.beecow.gateway.BeecowGatewayApp;
import com.mediastep.beecow.gateway.domain.User;
import com.mediastep.beecow.gateway.repository.UserRepository;
import com.mediastep.beecow.gateway.service.util.RandomUtil;

/**
 * Test class for the UserResource REST controller.
 *
 * @see UserService
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = BeecowGatewayApp.class)
@Transactional
public class UserServiceIntTest {

    @Inject
    private UserRepository userRepository;

    @Inject
    private UserService userService;

    @Inject
    private EntityManager em;

    @Test
    @Transactional
    public void assertThatUserMustExistToResetPassword() {
        Optional<User> maybeUser = userService.requestPasswordReset("john.doe@localhost");
        assertThat(maybeUser.isPresent()).isFalse();

        maybeUser = userService.requestPasswordReset("admin@localhost");
        assertThat(maybeUser.isPresent()).isTrue();

        assertThat(maybeUser.get().getEmail()).isEqualTo("admin@localhost");
        assertThat(maybeUser.get().getResetDate()).isNotNull();
        assertThat(maybeUser.get().getResetKey()).isNotNull();
    }

    @Test
    @Transactional
    public void assertThatOnlyActivatedUserCanRequestPasswordReset() {
        User user = createUser(em, "johndoe", ENCRYPTED_DEFAUT_PASSWORD, "john.doe@localhost", "John", "Doe", AuthoritiesConstants.USER, true);
        user.setActivated(false);
        userRepository.saveAndFlush(user);
        Optional<User> maybeUser = userService.requestPasswordReset("john.doe@localhost");
        assertThat(maybeUser.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void assertThatResetKeyMustNotBeOlderThan24Hours() {
        User user = createUser(em, "johndoe", ENCRYPTED_DEFAUT_PASSWORD, "john.doe@localhost", "John", "Doe", AuthoritiesConstants.USER, true);

        ZonedDateTime daysAgo = ZonedDateTime.now().minusHours(25);
        String resetKey = RandomUtil.generateResetKey();
        user.setActivated(true);
        user.setResetDate(daysAgo);
        user.setResetKey(resetKey);

        userRepository.saveAndFlush(user);

        Optional<User> maybeUser = userService.completePasswordReset("johndoe2", user.getResetKey());

        assertThat(maybeUser.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void assertThatResetKeyMustBeValid() {
        User user = createUser(em, "johndoe", ENCRYPTED_DEFAUT_PASSWORD, "john.doe@localhost", "John", "Doe", AuthoritiesConstants.USER, true);

        ZonedDateTime daysAgo = ZonedDateTime.now().minusHours(25);
        user.setActivated(true);
        user.setResetDate(daysAgo);
        user.setResetKey("1234");
        userRepository.save(user);
        Optional<User> maybeUser = userService.completePasswordReset("johndoe2", user.getResetKey());
        assertThat(maybeUser.isPresent()).isFalse();
    }

    @Test
    @Transactional
    public void assertThatUserCanResetPassword() {
        User user = createUser(em, "johndoe", ENCRYPTED_DEFAUT_PASSWORD, "john.doe@localhost", "John", "Doe", AuthoritiesConstants.USER, true);
        String oldPassword = user.getPassword();
        ZonedDateTime daysAgo = ZonedDateTime.now().minusHours(2);
        String resetKey = RandomUtil.generateResetKey();
        user.setActivated(true);
        user.setResetDate(daysAgo);
        user.setResetKey(resetKey);
        userRepository.save(user);
        Optional<User> maybeUser = userService.completePasswordReset("johndoe2", user.getResetKey());
        assertThat(maybeUser.isPresent()).isTrue();
        assertThat(maybeUser.get().getResetDate()).isNull();
        assertThat(maybeUser.get().getResetKey()).isNull();
        assertThat(maybeUser.get().getPassword()).isNotEqualTo(oldPassword);
    }
}
