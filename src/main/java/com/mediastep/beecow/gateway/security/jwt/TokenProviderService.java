/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.security.jwt;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import com.mediastep.beecow.common.domain.enumeration.AuthorTypeEnum;
import com.mediastep.beecow.common.security.AuthoritiesConstants;
import com.mediastep.beecow.common.security.SecurityUtils;
import com.mediastep.beecow.common.security.TokenPageDetails;
import com.mediastep.beecow.common.security.TokenProvider;
import com.mediastep.beecow.common.security.TokenStoreDetails;
import com.mediastep.beecow.common.security.TokenUserAuthority;
import com.mediastep.beecow.common.security.TokenUserDetails;
import com.mediastep.beecow.gateway.client.JobServiceClient;
import com.mediastep.beecow.gateway.domain.User;
import com.mediastep.beecow.gateway.service.mapper.UserMapper;
import com.mediastep.beecow.job.dto.CompanyDTO;
import com.mediastep.beecow.store.client.StoreServiceClient;
import com.mediastep.beecow.store.service.dto.StoreDTO;

/**
 * The service query data from database and generate token using TokenProvider
 */
@Service
public class TokenProviderService {

    private final Logger log = LoggerFactory.getLogger(TokenProvider.class);

    @Inject
    private UserDetailsService userDetailsService;

    @Inject
    private UserMapper userMapper;

    @Inject
    private TokenProvider tokenProvider;

    @Inject
    StoreServiceClient storeServiceClient;

    @Inject
    JobServiceClient jobServiceClient;

    public String createToken(String login, boolean rememberMe) {
        return doCreateToken(login, rememberMe);
    }

    private String doCreateToken(String login, boolean rememberMe) {
        UserDetails user = userDetailsService.loadUserByUsername(login);
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
            user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        return tokenProvider.createToken(authenticationToken, rememberMe);
    }

    public String createToken(User user, boolean rememberMe) {
        TokenUserDetails tokenUser = userMapper.userToTokenUserDetails(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(tokenUser, null, tokenUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return tokenProvider.createToken(authentication, rememberMe);
    }

    public String createTokenNoThrow(String login, boolean rememberMe) {
        try {
            return doCreateToken(login, rememberMe);
        } catch (AuthenticationException exc) {
            log.warn("Authentication error", exc);
        }
        return null;
    }

    public String createToken(Authentication authentication, Boolean rememberMe) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return tokenProvider.createToken(authentication, rememberMe, AuthorTypeEnum.USER);
    }

    public String createStoreToken(Long storeId, boolean rememberMe, String jwt) {
        StoreDTO store = storeServiceClient.getStoreById(storeId, jwt);
        if (store == null) {
        	return "";
        }
        String token = createStoreToken(store, rememberMe);
        return token;
    }

    private String createStoreToken(StoreDTO store, boolean rememberMe) {
    	assert(store != null);
        Long storeId = store.getId();
        TokenUserDetails user = SecurityUtils.getUserDetails(TokenUserDetails.class);
        Set<TokenUserAuthority> authorities = user.getAuthorities();
        TokenStoreDetails tokenStoreDetails = new TokenStoreDetails(user);
        tokenStoreDetails.setStoreId(storeId);
        authorities.add(new TokenUserAuthority(AuthoritiesConstants.STORE));
        Authentication authentication = new UsernamePasswordAuthenticationToken(tokenStoreDetails, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.createToken(authentication, rememberMe, AuthorTypeEnum.STORE);
        return token;
    }

    public String createStoreTokenByUrl(String url, String jwt) {
    	StoreDTO store = storeServiceClient.getStoreByURL(url, jwt);
    	if (store == null) {
    		return null;
    	}
        String token = createStoreToken(store, false);
        return token;
    }

    private String createCompanyToken(CompanyDTO company, boolean rememberMe) {
    	assert(company != null);
        Long companyId = company.getId();
        TokenUserDetails user = SecurityUtils.getUserDetails(TokenUserDetails.class);
        Set<TokenUserAuthority> authorities = user.getAuthorities();
        TokenPageDetails tokenPageDetails = new TokenPageDetails(user);
        tokenPageDetails.setPageId(companyId);
        authorities.add(new TokenUserAuthority(AuthoritiesConstants.COMPANY));
        Authentication authentication = new UsernamePasswordAuthenticationToken(tokenPageDetails, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.createToken(authentication, rememberMe, AuthorTypeEnum.COMPANY);
        return token;
    }

    public String createCompanyTokenByUrl(String url, String jwt) {
    	CompanyDTO company = jobServiceClient.getCompanyByURL(url, jwt);
    	if (company == null) {
    		return null;
    	}
        String token = createCompanyToken(company, false);
        return token;
    }

    public String createCompanyToken(boolean rememberMe, String jwt) {
        String token = "";
        try {
            TokenUserDetails user = (TokenUserDetails)SecurityUtils.getUserDetails();
            Long companyId = jobServiceClient.getCurrentCompanyId(jwt).getBody();

            if(companyId != null) {
                TokenPageDetails tokenPageDetails = new TokenPageDetails(user);
                tokenPageDetails.setPageId(companyId);
                user.getAuthorities().remove(new TokenUserAuthority(AuthoritiesConstants.STORE));
                user.getAuthorities().add(new TokenUserAuthority(AuthoritiesConstants.COMPANY));

                Authentication authentication = new UsernamePasswordAuthenticationToken(tokenPageDetails, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                token = tokenProvider.createToken(authentication, rememberMe, AuthorTypeEnum.COMPANY);
            }
        } catch (Exception e){
            log.info("Exception throw in createCompanyToken method " + e.getMessage());
        }
        return token;
    }
}
