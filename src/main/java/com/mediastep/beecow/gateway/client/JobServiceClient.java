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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.mediastep.beecow.job.dto.CompanyDTO;

@FeignClient(name = "jobServices")
public interface JobServiceClient {
    @RequestMapping(value = "/api/companies/getCurrentCompanyId", method = RequestMethod.GET)
    ResponseEntity<Long> getCurrentCompanyId(@RequestHeader("Authorization") String jwtToken);

    @RequestMapping(value = "/api/companies/getByPageUrl", method = RequestMethod.GET)
    CompanyDTO getCompanyByURL(@RequestParam("pageUrl") String pageUrl, @RequestHeader("Authorization") String jwtToken);
}
