/*******************************************************************************
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 01/01/2017
 * Author: Huyen Lam <email:huyen.lam@mediastep.com>
 *  
 *******************************************************************************/
package com.mediastep.beecow.gateway.config.event;


import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;

/**
 * The class enable spring stream that allows sending user entity events to subscriber.
 */
@Configuration
@EnableBinding(Event.class)
@IntegrationComponentScan(basePackageClasses = {PushNotifyEventGateway.class})
public class PushNotifyEventGatewayConfiguration {
}