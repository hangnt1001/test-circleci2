/*******************************************************************************
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 01/01/2017
 * Author: Huyen Lam <email:huyen.lam@mediastep.com>
 *  
 *******************************************************************************/
package com.mediastep.beecow.gateway.web.rest;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;
import com.mediastep.beecow.gateway.config.Constants;
import com.mediastep.beecow.gateway.service.EmailService;
import com.mediastep.beecow.gateway.service.MailService;
import com.mediastep.beecow.gateway.service.dto.EmailDTO;
import com.mediastep.beecow.gateway.web.rest.util.HeaderUtil;
import com.mediastep.beecow.gateway.web.rest.util.PaginationUtil;
import com.mediastep.beecow.gateway.web.rest.vm.ContactUsVM;

import io.swagger.annotations.ApiParam;

/**
 * REST controller for managing Email.
 */
@RestController
@RequestMapping("/api")
public class EmailResource {

    private final Logger log = LoggerFactory.getLogger(EmailResource.class);

    @Inject
    private EmailService emailService;

    @Inject
    private MailService mailService;

    /**
     * POST  /emails : Create a new email.
     *
     * @param emailDTO the emailDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new emailDTO, or with status 400 (Bad Request) if the email has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/emails")
    @Timed
    public ResponseEntity<EmailDTO> createEmail(@Valid @RequestBody EmailDTO emailDTO) throws URISyntaxException {
        log.debug("REST request to save Email : {}", emailDTO);
        EmailDTO result = emailService.save(emailDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityCreationAlert("email", result.getId().toString()))
            .body(result);
    }

    /**
     * POST  /emails : Create a new email.
     *
     * @param emailDTO the emailDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new emailDTO, or with status 400 (Bad Request) if the email has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/emails/contact-us")
    @Timed
    public ResponseEntity<Void> contactUs(@Valid @RequestBody ContactUsVM contactUs) throws URISyntaxException {
        log.debug("REST request to send Email to web-admin : {}", contactUs);
        if (StringUtils.isBlank(contactUs.getContent())) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert("contact-us", "contentblank", "Content was not provided")).build();
        }
        mailService.contactUs(contactUs);
        emailService.saveIfNotFound(contactUs.getFrom());
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityCreationAlert("email", contactUs.getFrom().getEmail()))
            .build();
    }

    /**
     * GET  /emails : get all the emails.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of emails in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping("/emails")
    @Timed
    public ResponseEntity<List<EmailDTO>> getAllEmails(@ApiParam Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of Emails");
        Page<EmailDTO> page = emailService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/emails");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /emails/:email : find the email.
     *
     * @param email the id of the emailDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the emailDTO, or with status 404 (Not Found)
     */
    @GetMapping("/emails/{email:" + Constants.EMAIL_REGEX + "}")
    @Timed
    public ResponseEntity<EmailDTO> getEmail(@PathVariable String email) {
        log.debug("REST request to get Email : {}", email);
        EmailDTO emailDTO = emailService.findOneByEmail(email);
        return Optional.ofNullable(emailDTO)
            .map(result -> new ResponseEntity<>(
                result,
                HttpStatus.OK))
            .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * DELETE  /emails/:email : delete the email.
     *
     * @param email the email of the emailDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/emails/{email:" + Constants.EMAIL_REGEX + "}")
    @Timed
    public ResponseEntity<Void> deleteEmail(@PathVariable String email) {
        log.debug("REST request to delete Email : {}", email);
        emailService.deleteEmail(email);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert("email", email.toString())).build();
    }

    /**
     * SEARCH  /_search/emails?query=:query : search for the email corresponding
     * to the query.
     *
     * @param query the query of the email search 
     * @param pageable the pagination information
     * @return the result of the search
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping("/_search/emails")
    @Timed
    public ResponseEntity<List<EmailDTO>> searchEmails(@RequestParam String query, @ApiParam Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to search for a page of Emails for query {}", query);
        Page<EmailDTO> page = emailService.search(query, pageable);
        HttpHeaders headers = PaginationUtil.generateSearchPaginationHttpHeaders(query, page, "/api/_search/emails");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }


}
