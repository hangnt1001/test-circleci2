/*******************************************************************************
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 01/01/2017
 * Author: Huyen Lam <email:huyen.lam@mediastep.com>
 *  
 *******************************************************************************/
package com.mediastep.beecow.gateway.config.audit;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EntityAuditEventConfig implements BeanFactoryAware {

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        EntityAuditEventListener.setBeanFactory(beanFactory);
    }
}
