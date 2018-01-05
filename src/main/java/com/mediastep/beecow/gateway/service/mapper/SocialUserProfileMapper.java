/*******************************************************************************
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 01/01/2017
 * Author: Huyen Lam <email:huyen.lam@mediastep.com>
 *  
 *******************************************************************************/
package com.mediastep.beecow.gateway.service.mapper;

import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.social.connect.UserProfile;

import com.mediastep.beecow.common.domain.enumeration.Gender;
import com.mediastep.beecow.gateway.domain.SocialUserProfile;
import com.mediastep.beecow.gateway.service.SocialService;

/**
 * Mapper for the entity Email and its DTO EmailDTO.
 */
@Mapper(componentModel = "spring", uses = {})
public interface SocialUserProfileMapper {

	@Mapping(target = "providerId", constant = SocialService.PROVIDER_FACEBOOK)
	@Mapping(source = "id", target = "id")
	@Mapping(source = "name", target = "displayName")
	@Mapping(source = "firstName", target = "firstName")
	@Mapping(source = "lastName", target = "lastName")
	@Mapping(source = "email", target = "email")
	@Mapping(source = "gender", target = "gender")
	@Mapping(target = "username", ignore = true)
	@Mapping(target = "imageUrl", ignore = true)
	@Mapping(target = "dateOfBirth", ignore = true)
	SocialUserProfile facebookToUserProfile(org.springframework.social.facebook.api.User profile);

	@Mapping(target = "providerId", ignore = true)
	@Mapping(target = "displayName", ignore = true)
	@Mapping(target = "gender", ignore = true)
	@Mapping(target = "imageUrl", ignore = true)
	@Mapping(target = "dateOfBirth", ignore = true)
	SocialUserProfile userProfileToUserProfile(UserProfile profile);

	default Gender map(String gender) {
		if (StringUtils.isBlank(gender)) {
			return null;
		}
		return Gender.valueOf(StringUtils.upperCase(gender));
	}
}
