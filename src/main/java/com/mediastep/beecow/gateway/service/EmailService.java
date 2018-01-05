/*******************************************************************************
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 01/01/2017
 * Author: Huyen Lam <email:huyen.lam@mediastep.com>
 *  
 *******************************************************************************/
package com.mediastep.beecow.gateway.service;

import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mediastep.beecow.gateway.domain.Email;
import com.mediastep.beecow.gateway.repository.EmailRepository;
import com.mediastep.beecow.gateway.repository.search.EmailSearchRepository;
import com.mediastep.beecow.gateway.service.dto.EmailDTO;
import com.mediastep.beecow.gateway.service.mapper.EmailMapper;

/**
 * Service Implementation for managing Email.
 */
@Service
@Transactional
public class EmailService {

    private final Logger log = LoggerFactory.getLogger(EmailService.class);
    
    @Inject
    private EmailRepository emailRepository;

    @Inject
    private EmailMapper emailMapper;

    @Inject
    private EmailSearchRepository emailSearchRepository;

    /**
     * Save a email.
     *
     * @param emailDTO the entity to save
     * @return the persisted entity
     */
    public EmailDTO save(EmailDTO emailDTO) {
        log.debug("Request to save Email : {}", emailDTO);
        Email email = emailMapper.emailDTOToEmail(emailDTO);
        Email existingEmail = emailRepository.findOneByEmail(emailDTO.getEmail());
        if (existingEmail != null) {
            email.setId(existingEmail.getId());
        }
        else {
            email.setId(null);
        }
        email = emailRepository.save(email);
        emailSearchRepository.save(email);
        EmailDTO result = emailMapper.emailToEmailDTO(email);
        return result;
    }

    /**
     * Save a email.
     *
     * @param emailDTO the entity to save
     * @return the persisted entity
     */
    public EmailDTO saveIfNotFound(EmailDTO emailDTO) {
        log.debug("Request to save Email if not found : {}", emailDTO);
        Email email = emailMapper.emailDTOToEmail(emailDTO);
        Email existingEmail = emailRepository.findOneByEmail(emailDTO.getEmail());
        if (existingEmail != null) {
            return emailMapper.emailToEmailDTO(existingEmail);
        }
        email.setId(null);
        email = emailRepository.save(email);
        emailSearchRepository.save(email);
        EmailDTO result = emailMapper.emailToEmailDTO(email);
        return result;
    }

    /**
     *  Get all the emails.
     *  
     *  @param pageable the pagination information
     *  @return the list of entities
     */
    @Transactional(readOnly = true) 
    public Page<EmailDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Emails");
        Page<Email> result = emailRepository.findAll(pageable);
        return result.map(email -> emailMapper.emailToEmailDTO(email));
    }

    /**
     *  Get one email by id.
     *
     *  @param id the id of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true) 
    public EmailDTO findOne(Long id) {
        log.debug("Request to get Email : {}", id);
        Email email = emailRepository.findOne(id);
        EmailDTO emailDTO = emailMapper.emailToEmailDTO(email);
        return emailDTO;
    }

    /**
     *  Get one email.
     *
     *  @param email the email of the entity
     *  @return the entity
     */
    @Transactional(readOnly = true) 
    public EmailDTO findOneByEmail(String email) {
        log.debug("Request to get Email : {}", email);
        Email emailEnity = emailRepository.findOneByEmail(email);
        EmailDTO emailDTO = emailMapper.emailToEmailDTO(emailEnity);
        return emailDTO;
    }

    /**
     *  Delete the  email by id.
     *
     *  @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete Email : {}", id);
        doDelete(id);
    }

    /**
     *  Delete the  email by id.
     *
     *  @param id the id of the entity
     */
    private void doDelete(Long id) {
        emailRepository.delete(id);
        emailSearchRepository.delete(id);
    }

    /**
     *  Delete the  email by id.
     *
     *  @param id the id of the entity
     */
    public void deleteEmail(String email) {
        log.debug("Request to delete Email : {}", email);
        Email emailEnity = emailRepository.findOneByEmail(email);
        if (emailEnity != null) {
            doDelete(emailEnity.getId());
        }
    }

    /**
     * Search for the email corresponding to the query.
     *
     *  @param query the query of the search
     *  @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<EmailDTO> search(String query, Pageable pageable) {
        log.debug("Request to search for a page of Emails for query {}", query);
        Page<Email> result = emailSearchRepository.search(queryStringQuery(query), pageable);
        return result.map(email -> emailMapper.emailToEmailDTO(email));
    }
}
