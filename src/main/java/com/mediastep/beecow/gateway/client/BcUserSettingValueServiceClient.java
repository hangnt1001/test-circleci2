/*******************************************************************************
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 01/01/2017
 * Author: Huyen Lam <email:huyen.lam@mediastep.com>
 *  
 *******************************************************************************/
package com.mediastep.beecow.gateway.client;

import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.Map;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "userServices", fallback = BcUserSettingValueServiceClientFallback.class)
public interface BcUserSettingValueServiceClient {

    /**
     * GET  /bc-user-setting-values/user?fromDate : get all the bcUserSettingValues.
     *
     * @param userId user ID which settings belong to, may be null
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of bcUserSettingValues in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @RequestMapping(value = "/api/bc-user-setting-values/users/{userId}", method = RequestMethod.GET)
    Map<String, Object> getAllBcUserSettingValues(@PathVariable("userId") Long userId, @RequestParam("fromDate") @DateTimeFormat(iso = ISO.DATE_TIME) ZonedDateTime fromDate, @RequestHeader("Authorization") String jwtToken);
}
