/*******************************************************************************
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 01/01/2017
 * Author: Huyen Lam <email:huyen.lam@mediastep.com>
 *  
 *******************************************************************************/
package com.mediastep.beecow.gateway.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediastep.beecow.gateway.domain.Email;

/**
 * Spring Data JPA repository for the Email entity.
 */
public interface EmailRepository extends JpaRepository<Email,Long> {
    Email findOneByEmail(String email);
}
