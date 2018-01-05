/*******************************************************************************
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 01/01/2017
 * Author: Huyen Lam <email:huyen.lam@mediastep.com>
 *  
 *******************************************************************************/
package com.mediastep.beecow.gateway.config.audit;

import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mediastep.beecow.common.config.audit.AbstractEntityEventListener;
import com.mediastep.beecow.common.dto.UserDTO;
import com.mediastep.beecow.common.events.EntityEventType;
import com.mediastep.beecow.common.events.UserEvent;
import com.mediastep.beecow.gateway.domain.User;
import com.mediastep.beecow.gateway.service.UserEventGateway;
import com.mediastep.beecow.gateway.service.mapper.UserMapper;

public class UserEntityEventListener extends AbstractEntityEventListener<User> {

    private final Logger log = LoggerFactory.getLogger(UserEntityEventListener.class);

    @Override
    protected void process(User user, EntityEventType entityEventType) {
        log.debug("Fire {} event with entity {}", entityEventType, user);
        UserEventGateway userEventGateway = beanFactory.getBean(UserEventGateway.class);
        UserMapper userMapper = beanFactory.getBean(UserMapper.class);
        UserDTO userDTO = userMapper.userToUserDTO(user);
        UserEvent event = (UserEvent) new UserEvent().type(entityEventType).entity(userDTO);
        userEventGateway.send(event);
    }

    @PostPersist
    public void onPostCreate(Object target) {
        log.debug("Entity created: {}", target);
        super.onChanged(target, EntityEventType.CREATED);
    }

    @PostUpdate
    public void onPostUpdate(Object target) {
        log.debug("Entity updated: {}", target);
        super.onChanged(target, EntityEventType.UPDATED);
    }

    @PostRemove
    public void onPostRemove(Object target) {
        log.debug("Entity deleted: {}", target);
        super.onChanged(target, EntityEventType.DELETED);
    }
}
