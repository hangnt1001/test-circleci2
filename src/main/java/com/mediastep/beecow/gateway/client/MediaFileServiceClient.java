/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.client;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.mediastep.beecow.common.dto.MediaFileDTO;

/**
 * REST controller for managing MediaFile.
 */
@FeignClient(name = "mediaServices", fallback = MediaFileServiceClientFallback.class)
public interface MediaFileServiceClient {

    @RequestMapping(value = "/api/clone", method = RequestMethod.POST)
    MediaFileDTO clone(@RequestParam("url") String url, @RequestParam(name = "domain", defaultValue = "general") String domain);
}
