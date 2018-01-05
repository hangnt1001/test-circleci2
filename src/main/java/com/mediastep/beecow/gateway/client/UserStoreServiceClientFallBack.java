/*
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 11/7/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 */

package com.mediastep.beecow.gateway.client;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.mediastep.beecow.user.dto.UserStoreDTO;

@Component
public class UserStoreServiceClientFallBack implements UserStoreServiceClient {
    @Override
    public UserStoreDTO findStoreByUser(@RequestParam("id") Long id, @RequestHeader("Authorization") String jwtToken) {
        return null;
    }

}
