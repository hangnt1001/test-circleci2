/*******************************************************************************
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 09 Sep, 2017
 * Author: Anh Le <email:anh.le@mediastep.com>
 *  
 *******************************************************************************/
package com.mediastep.beecow.gateway.config.event;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

public interface Event {

    String PUSH_BEECOW_COMMAND_NOTIFY_CHANNEL_NAME = "pushBeecowCommandNotifyChannel";
    
    @Output
    MessageChannel pushBeecowCommandNotifyChannel();
}
