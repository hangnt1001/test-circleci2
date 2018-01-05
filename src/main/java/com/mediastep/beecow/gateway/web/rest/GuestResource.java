/*******************************************************************************
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 01/01/2017
 * Author: Huyen Lam <email:huyen.lam@mediastep.com>
 *
 *******************************************************************************/
package com.mediastep.beecow.gateway.web.rest;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;
import com.mediastep.beecow.user.dto.AuthUserDTO;
import com.mediastep.beecow.common.dto.UserDTO;
import com.mediastep.beecow.gateway.domain.User;
import com.mediastep.beecow.gateway.security.jwt.TokenProviderService;
import com.mediastep.beecow.gateway.service.UserService;
import com.mediastep.beecow.gateway.service.mapper.UserMapper;
import com.mediastep.beecow.gateway.web.rest.util.HeaderUtil;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 * REST controller for managing users.
 *
 * <p>This class accesses the User entity, and needs to fetch its collection of authorities.</p>
 * <p>
 * For a normal use-case, it would be better to have an eager relationship between User and Authority,
 * and send everything to the client side: there would be no View Model and DTO, a lot less code, and an outer-join
 * which would be good for performance.
 * </p>
 * <p>
 * We use a View Model and a DTO for 3 reasons:
 * <ul>
 * <li>We want to keep a lazy association between the user and the authorities, because people will
 * quite often do relationships with the user, and we don't want them to get the authorities all
 * the time for nothing (for performance reasons). This is the #1 goal: we should not impact our users'
 * application because of this use-case.</li>
 * <li> Not having an outer join causes n+1 requests to the database. This is not a real issue as
 * we have by default a second-level cache. This means on the first HTTP call we do the n+1 requests,
 * but then all authorities come from the cache, so in fact it's much better than doing an outer join
 * (which will get lots of data from the database, for each HTTP call).</li>
 * <li> As this manages users, for security reasons, we'd rather have a DTO layer.</li>
 * </ul>
 * <p>Another option would be to have a specific JPA entity graph to handle this case.</p>
 */
@RestController
@RequestMapping("/api")
public class GuestResource {

    private final Logger log = LoggerFactory.getLogger(GuestResource.class);

    @Inject
    private UserMapper userMapper;

    @Inject
    private UserService userService;

    @Inject
    private TokenProviderService tokenProviderService;

    /**
     * POST  /guest  : Creates a guest user.
     * <p>
     * Creates a new guest user to see some information from the system.
     * </p>
     *
     * @param langKey language key
     * @return the ResponseEntity with status 201 (Created) and with body the new user, or with status 400 (Bad Request) if the login or email is already in use
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @ApiOperation(value = "Create a pre-activate user")
    @PostMapping("/guest/{location}/{langKey}")
    @Timed
    public ResponseEntity<AuthUserDTO> createGuest(
        @ApiParam(value = "Location code (ISO_3166-2)")
        @PathVariable(required = false) String location,
        @ApiParam(value = "Language code (ISO 639-1)")
        @PathVariable(required = false) String langKey) throws URISyntaxException {

        log.debug("REST request to create guest");
        if (StringUtils.isBlank(location) || location.length() < UserDTO.LOCATION_CODE_MIN_LENGTH || location.length() > UserDTO.LOCATION_CODE_MAX_LENGTH) {
            return ResponseEntity.badRequest()
                .headers(HeaderUtil.createFailureAlert("guest-account", "locationblank", "Location must be provided"))
                .body(null);
        }
        if (StringUtils.isBlank(langKey) || langKey.length() < UserDTO.LANG_KEY_MIN_LENGTH || langKey.length() > UserDTO.LANG_KEY_MAX_LENGTH) {
            return ResponseEntity.badRequest()
                .headers(HeaderUtil.createFailureAlert("guest-account", "locationblank", "Language must be provided"))
                .body(null);
        }

        User newUser = userService.createPreavtivateUser(location, langKey);
        if (newUser == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .headers(HeaderUtil.createFailureAlert("guest-account", "cannotcreateguest", "Cannot create guest account at the moment, please try again later or contact admin fro support"))
                .body(null);
        }

        String token = tokenProviderService.createToken(newUser.getLogin(), true);
        AuthUserDTO newUserDTO = userMapper.userToAuthUserDTO(newUser);
        newUserDTO.setAccessToken(token);
        return ResponseEntity.created(new URI("/api/users/" + newUserDTO.getLogin()))
            .headers(HeaderUtil.createAlert("guest-account.created", newUserDTO.getLogin()))
            .body(newUserDTO);
    }
}
