/*******************************************************************************
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 01/01/2017
 * Author: Huyen Lam <email:huyen.lam@mediastep.com>
 *  
 *******************************************************************************/
package com.mediastep.beecow.gateway.client;

import org.springframework.stereotype.Component;

import com.mediastep.beecow.common.dto.MediaFileDTO;

@Component
public class MediaFileServiceClientFallback implements MediaFileServiceClient {

    @Override
    public MediaFileDTO clone(String url, String domain) {
        return null;
    }
}
