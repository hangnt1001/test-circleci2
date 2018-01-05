/*******************************************************************************
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 01/01/2017
 * Author: Huyen Lam <email:huyen.lam@mediastep.com>
 *  
 *******************************************************************************/
package com.mediastep.beecow.gateway.service;

import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;

import com.mediastep.beecow.common.events.LoginEvent;
import com.mediastep.beecow.common.events.UserEvent;
import com.mediastep.beecow.gateway.config.UserOutputChannel;

@MessagingGateway
public interface UserEventGateway {
     @Gateway(requestChannel = UserOutputChannel.USER_CHANNEL_NAME)
     void send(UserEvent event);

     @Gateway(requestChannel = UserOutputChannel.USER_LOGIN_CHANNEL_NAME)
     void sendLoginEvent(LoginEvent loginEvent);
}
