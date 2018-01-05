/*******************************************************************************
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 09 Sep, 2017
 * Author: Anh Le <email:anh.le@mediastep.com>
 *  
 *******************************************************************************/
package com.mediastep.beecow.gateway.config.event;

import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import com.mediastep.beecow.common.dto.BeecowCommandNotifyDTO;

@MessagingGateway
public interface PushNotifyEventGateway {
    @Gateway(requestChannel = Event.PUSH_BEECOW_COMMAND_NOTIFY_CHANNEL_NAME)
    void pushBeecowCommandNotify(BeecowCommandNotifyDTO  beecowCommandNotifyDTO);

}
