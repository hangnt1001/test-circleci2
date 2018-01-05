/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.security;

import java.io.Serializable;

import com.mediastep.beecow.common.dto.UserStatus;
import com.mediastep.beecow.gateway.web.rest.errors.ErrorConstants;

/**
 * This exception is throw in case of a not activated user trying to authenticate.
 */
public class UserNotActivatedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorVM errorVM;

    public UserNotActivatedException(String message, UserStatus status) {
        super(message);
        this.errorVM = new ErrorVM(status);
    }

	/**
	 * @return the errorVM
	 */
	public ErrorVM getErrorVM() {
		return errorVM;
	}

	public static class ErrorVM implements Serializable {

		private static final long serialVersionUID = 1L;

		private static final String message = ErrorConstants.ERR_USER_NOT_ACTIVATED;

		private final UserStatus status;

	    public ErrorVM(UserStatus status) {
	        this.status = status;
	    }

		/**
		 * @return the message
		 */
		public static String getMessage() {
			return message;
		}

		/**
		 * @return the status
		 */
		public UserStatus getStatus() {
			return status;
		}
	}
}
