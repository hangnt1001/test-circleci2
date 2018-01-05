/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 08/06/2017
 * Author: Quang Huynh <email:quang.huynh@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.client;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.mediastep.beecow.user.dto.UserStoreDTO;

@FeignClient(name = "storeService", fallback = UserStoreServiceClientFallBack.class)
public interface UserStoreServiceClient {
    @RequestMapping(value = "/api/user/{id}/store", method = RequestMethod.GET)
    UserStoreDTO findStoreByUser(@RequestParam("id") Long id, @RequestHeader("Authorization") String jwtToken);
}
