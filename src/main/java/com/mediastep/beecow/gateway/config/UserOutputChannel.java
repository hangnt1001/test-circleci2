/*******************************************************************************
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 01/01/2017
 * Author: Huyen Lam <email:huyen.lam@mediastep.com>
 *  
 *******************************************************************************/
package com.mediastep.beecow.gateway.config;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

public interface UserOutputChannel {

    String USER_CHANNEL_NAME = "userChannel";
    String USER_LOGIN_CHANNEL_NAME = "userLoginChannel";

    @Output
    MessageChannel userChannel();

    @Output
    MessageChannel userLoginChannel();
}
