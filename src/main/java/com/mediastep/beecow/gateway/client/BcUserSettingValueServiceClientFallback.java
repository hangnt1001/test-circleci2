/*******************************************************************************
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 01/01/2017
 * Author: Huyen Lam <email:huyen.lam@mediastep.com>
 *  
 *******************************************************************************/
package com.mediastep.beecow.gateway.client;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class BcUserSettingValueServiceClientFallback implements BcUserSettingValueServiceClient {

    public Map<String, Object> getAllBcUserSettingValues(Long userId, ZonedDateTime fromDate, String jwtToken) {
        return new HashMap<String, Object>();
    }
}
