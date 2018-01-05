/*******************************************************************************
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 01/01/2017
 * Author: Huyen Lam <email:huyen.lam@mediastep.com>
 *  
 *******************************************************************************/
package com.mediastep.beecow.gateway.service.mapper;

import com.mediastep.beecow.gateway.domain.*;
import com.mediastep.beecow.gateway.service.dto.EmailDTO;

import org.mapstruct.*;
import java.util.List;

/**
 * Mapper for the entity Email and its DTO EmailDTO.
 */
@Mapper(componentModel = "spring", uses = {})
public interface EmailMapper {

    EmailDTO emailToEmailDTO(Email email);

    List<EmailDTO> emailsToEmailDTOs(List<Email> emails);

    Email emailDTOToEmail(EmailDTO emailDTO);

    List<Email> emailDTOsToEmails(List<EmailDTO> emailDTOs);
}
