/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.service.mapper;

import javax.inject.Inject;

import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;

import com.mediastep.beecow.common.config.UserServiceConfig;
import com.mediastep.beecow.common.dto.ImageDTO;
import com.mediastep.beecow.common.dto.UserDTO;
import com.mediastep.beecow.common.dto.UserStatus;
import com.mediastep.beecow.common.util.ImageDtoUtil;
import com.mediastep.beecow.gateway.domain.User;
import com.mediastep.beecow.gateway.service.util.UserUtil;

/**
 * Mapper for the entity User and its DTO UserDTO.
 */
@Component
public class UserMapperProcessor {

    @Inject
    private UserServiceConfig userServiceConfig;

    @AfterMapping
    public void setDefaultAvatar(User source, @MappingTarget UserDTO target) {
        if (target.getAvatarUrl() == null) {
            String defaultAvatarUrl = userServiceConfig.getDefaultAvatarUrl(source.getGender());
            ImageDTO avatarImageDTO = ImageDtoUtil.stringToImageDTO(defaultAvatarUrl);
            target.setAvatarUrl(avatarImageDTO);
        }
    }

    @AfterMapping
    public void setStatus(User source, @MappingTarget UserDTO target) {
        UserStatus status = UserUtil.getUserStatus(source);
        target.setStatus(status);
    }
}
