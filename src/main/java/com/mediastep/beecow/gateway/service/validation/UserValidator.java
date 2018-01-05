/*******************************************************************************
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 01/01/2017
 * Author: Huyen Lam <email:huyen.lam@mediastep.com>
 *  
 *******************************************************************************/
package com.mediastep.beecow.gateway.service.validation;

import java.util.Set;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.mediastep.beecow.common.domain.enumeration.AccountType;
import com.mediastep.beecow.common.errors.InvalidInputException;
import com.mediastep.beecow.gateway.domain.User;
import com.mediastep.beecow.gateway.service.util.UserUtil;
import com.mediastep.beecow.gateway.web.rest.errors.ErrorConstants;
import com.mediastep.beecow.gateway.web.rest.vm.ManagedUserVM;

/**
 * Service class for managing users.
 */
@Component
public class UserValidator {

    @Inject
    private Validator validator;

    public void validatePassword(String password) {
        if (password == null || password.length() < ManagedUserVM.PASSWORD_MIN_LENGTH || password.length() > ManagedUserVM.PASSWORD_MAX_LENGTH) {
            throw new InvalidInputException(ErrorConstants.ERR_USER_PASSWORD_INVALID);
        }
    }

    public void validate(User user) {
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        for (ConstraintViolation<User> violation : violations) {
            throw new InvalidInputException(ErrorConstants.ERR_USER_PREFIX + violation.getPropertyPath() + ErrorConstants.ERR_USER_INVALID_SUBFIX);
        }

        validateContactInfo(user);
    }

    private void validateContactInfo(User user) {
        if (UserUtil.isGuest(user) || UserUtil.isSocialAccount(user)) {
            return;
        }
        if (user.getAccountType() == AccountType.EMAIL && StringUtils.isBlank(user.getEmail())) {
            throw new InvalidInputException(ErrorConstants.ERR_USER_EMAIL_MISSING);
        }
        else if (user.getAccountType() == AccountType.MOBILE && StringUtils.isBlank(user.getMobile())) {
            throw new InvalidInputException(ErrorConstants.ERR_USER_MOBILE_MISSING);
        }
    }
}
