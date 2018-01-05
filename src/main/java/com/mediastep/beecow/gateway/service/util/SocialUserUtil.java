/*******************************************************************************
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 01/01/2017
 * Author: Huyen Lam <email:huyen.lam@mediastep.com>
 *  
 *******************************************************************************/
package com.mediastep.beecow.gateway.service.util;

import org.apache.commons.lang3.StringUtils;
import org.mapstruct.factory.Mappers;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.UserProfile;
import org.springframework.social.facebook.api.Facebook;

import com.mediastep.beecow.gateway.domain.SocialUserProfile;
import com.mediastep.beecow.gateway.service.SocialService;
import com.mediastep.beecow.gateway.service.mapper.SocialUserProfileMapper;

/**
 * A Email.
 */
public class SocialUserUtil {

	private static final SocialUserProfileMapper socialUserProfileMapper = Mappers.getMapper(SocialUserProfileMapper.class);

	public static String getBcLoginName(SocialUserProfile profile) {
		String username = profile.getUsername();
		if (StringUtils.isBlank(username)) {
			throw new IllegalArgumentException("User name missing: " + profile);
		}
        String login = profile.getProviderId() + SocialService.SOCIAL_LOGIN_SEP + StringUtils.lowerCase(username);
        return login;
	}

    public static SocialUserProfile fetchUserProfile(Connection<?> connection) {
    	SocialUserProfile profile;
    	Object api = connection.getApi();
        if (api instanceof Facebook) {
        	profile = fetchFacebookUserProfile(connection, (Facebook) api);
        }
        else {
        	profile = fetchUserProfileDefault(connection);
        }
        return profile;
    }

    private static SocialUserProfile fetchFacebookUserProfile(Connection<?> connection, Facebook api) {
		org.springframework.social.facebook.api.User socialUser = api.userOperations().getUserProfile();
		SocialUserProfile user = socialUserProfileMapper.facebookToUserProfile(socialUser);
		setProviderId(user, connection);
		resolveUserName(user);
		resolveDisplayName(user, connection);
		setUserImageUrl(user, connection);
		return user;
    }

    private static void resolveUserName(SocialUserProfile user) {
		String userName = user.getUsername();
		if (StringUtils.isBlank(userName)) {
			String email = user.getEmail();
			if (email != null) {
				userName = email;
			}
			else {
				userName = user.getId();
			}
			user.setUsername(userName);
		}
    }

    private static void resolveDisplayName(SocialUserProfile user, Connection<?> connection) {
		if (StringUtils.isBlank(user.getDisplayName())) {
			String displayName = connection.getDisplayName();
			user.setDisplayName(displayName);
		}
    }

    private static void setProviderId(SocialUserProfile user, Connection<?> connection) {
        String providerId = connection.getKey().getProviderId();
		user.setProviderId(providerId);
    }

    private static void setUserImageUrl(SocialUserProfile user, Connection<?> connection) {
        String imageUrl = getUserImageUrl(connection, user.getProviderId());
        user.setImageUrl(imageUrl);
    }

    private static SocialUserProfile fetchUserProfileDefault(Connection<?> connection) {
		UserProfile socialUser = connection.fetchUserProfile();
		SocialUserProfile user = socialUserProfileMapper.userProfileToUserProfile(socialUser);
		setProviderId(user, connection);
		resolveUserName(user);
		resolveDisplayName(user, connection);
		setUserImageUrl(user, connection);
		return user;
    }

    private static String getUserImageUrl(Connection<?> connection, String providerId) {
        String imageUrl = connection.getImageUrl();
        if (SocialService.PROVIDER_FACEBOOK.equals(providerId)) {
            imageUrl = imageUrl + "?type=large";
        }
        return imageUrl;
    }
}
