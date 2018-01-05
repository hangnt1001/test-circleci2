/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.service.mapper;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.mediastep.beecow.user.dto.AuthUserDTO;
import com.mediastep.beecow.common.dto.UserDTO;
import com.mediastep.beecow.common.dto.mapper.ImageDTOMapper;
import com.mediastep.beecow.common.security.TokenUserAuthority;
import com.mediastep.beecow.common.security.TokenUserDetails;
import com.mediastep.beecow.common.util.ZonedDateTimeUtil;
import com.mediastep.beecow.gateway.domain.Authority;
import com.mediastep.beecow.gateway.domain.User;
import com.mediastep.beecow.gateway.web.rest.vm.ManagedUserVM;

/**
 * Mapper for the entity User and its DTO UserDTO.
 */
@Mapper(componentModel = "spring", uses = {ImageDTOMapper.class, UserMapperProcessor.class})
public interface UserMapper {

    @Mapping(source = "mobileObject", target = "mobile")
    UserDTO userToUserDTO(User user);

    @Mapping(source = "mobileObject", target = "mobile")
    ManagedUserVM userToManagedUserVM(User user);

    @Mapping(source = "login", target = "username")
    TokenUserDetails userToTokenUserDetails(User user);

    @Mapping(source = "mobileObject", target = "mobile")
    @Mapping(target = "accessToken", ignore = true)
    @Mapping(target = "refreshToken", ignore = true)
    @Mapping(target = "settings", ignore = true)
    AuthUserDTO userToAuthUserDTO(User user);

    default List<UserDTO> usersToUserDTOs(List<User> users) {
        List<UserDTO> userDTOs = new ArrayList<>();
        for (User user : users) {
            UserDTO userDTO = userToUserDTO(user);
            userDTOs.add(userDTO);
        }
        return userDTOs;
    }

    @Mapping(source = "mobile", target = "mobileObject")
    @Mapping(target = "mobile", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activationKey", ignore = true)
    @Mapping(target = "resetKey", ignore = true)
    @Mapping(target = "resetDate", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "activationExpiredDate", ignore = true)
    User userDTOToUser(UserDTO userDTO);

    List<User> userDTOsToUsers(List<UserDTO> userDTOs);

    default User userFromId(Long id) {
        if (id == null) {
            return null;
        }
        User user = new User();
        user.setId(id);
        return user;
    }

    default LocalDate map(ZonedDateTime date) {
        return ZonedDateTimeUtil.toLocalDate(date);
    }

    default ZonedDateTime map(LocalDate date) {
        return ZonedDateTimeUtil.toZonedDateTime(date);
    }

    default String zoneIdToString(ZoneId timeZone) {
        return ZonedDateTimeUtil.toString(timeZone);
    }

    default ZoneId zoneIdToString(String timeZone) {
        return ZonedDateTimeUtil.toZoneId(timeZone);
    }

    default Set<String> stringsFromAuthorities(Set<Authority> authorities) {
        return authorities.stream().map(Authority::getName)
            .collect(Collectors.toSet());
    }

    default Set<Authority> authoritiesFromStrings(Set<String> strings) {
        if (strings == null) {
            return Collections.emptySet();
        }
        return strings.stream().map(string -> {
            Authority auth = new Authority();
            auth.setName(string);
            return auth;
        }).collect(Collectors.toSet());
    }

    TokenUserAuthority tokenUserAuthorityFromAuthority(Authority authority);

    Set<TokenUserAuthority> tokenUserAuthoritiesFromAuthorities(Set<Authority> authorities);
}
