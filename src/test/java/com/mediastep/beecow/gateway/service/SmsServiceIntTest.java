/*******************************************************************************
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 01/01/2017
 * Author: Huyen Lam <email:huyen.lam@mediastep.com>
 *
 *******************************************************************************/
package com.mediastep.beecow.gateway.service;

import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.DEFAULT_ACTIVATION_CODE;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.DEFAULT_PASSWORD_RESET_KEY;
import static com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil.ENCRYPTED_DEFAUT_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doAnswer;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import com.mediastep.beecow.common.dto.PhoneDTO;
import com.mediastep.beecow.common.security.AuthoritiesConstants;
import com.mediastep.beecow.common.util.CommonTestUtil;
import com.mediastep.beecow.common.util.PhoneDtoUtil;
import com.mediastep.beecow.gateway.BeecowGatewayApp;
import com.mediastep.beecow.gateway.domain.User;
import com.mediastep.beecow.gateway.web.rest.UserResourceIntTestUtil;
import com.mediastep.beecow.sms.service.SMSRequestGateway;
import com.mediastep.beecow.sms.service.dto.MessageDTO;

/**
 * Test class for the BcOrderResource REST controller.
 *
 * @see BcOrderResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = BeecowGatewayApp.class)
public class SmsServiceIntTest {

    @Autowired
    private SMSService smsService;

    @Autowired
    private EntityManager em;

    @Mock
    private SMSRequestGateway smsGateway;

    private User user;

    private MessageDTO smsResult;

    private Boolean resultReady = Boolean.FALSE;

    @Before
    public void setup() {
        resultReady = Boolean.FALSE;
        smsResult = null;
        MockitoAnnotations.initMocks(this);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                synchronized (resultReady) {
                    smsResult = (MessageDTO) invocation.getArguments()[0];
                    resultReady = Boolean.TRUE;
                    return null;
                }
            }
        }).when(smsGateway).send(anyObject());
        ReflectionTestUtils.setField(smsService, "smsGateway", smsGateway);
    }

    @Before
    public void initTest() {
    	PhoneDTO mobile = PhoneDtoUtil.stringToPhoneDTO("+84:0123456789");
        user = UserResourceIntTestUtil.createMobileUser(em, mobile.toString(), ENCRYPTED_DEFAUT_PASSWORD, mobile.toString(), AuthoritiesConstants.USER, true);
        user.setActivationKey(DEFAULT_ACTIVATION_CODE);
        user.setResetKey(DEFAULT_PASSWORD_RESET_KEY);
        em.persist(user);
        em.flush();
    }

    public static String createUserToken(SecurityContext securityContext, Long userId) {
        return CommonTestUtil.createUserToken(securityContext, userId, String.valueOf(userId));
    }

    private void waitForResult() throws InterruptedException {
        while (!resultReady) Thread.sleep(100);
    }

    @Test
    @Transactional
    public void sendActivationMessage() throws Exception {
    	smsService.sendActivationMessage(user);
        waitForResult();
        assertThat(smsResult.getContent()).isEqualTo(DEFAULT_ACTIVATION_CODE + " is your Beecow's verification code");
        PhoneDTO mobile = smsResult.getTo().get(0);
        assertThat(mobile.toString()).isEqualTo("+84:0123456789");
    }

    @Test
    @Transactional
    public void sendActivationMessageVi() throws Exception {
    	user.setLangKey("vi");
    	em.persist(user);
    	em.flush();
    	smsService.sendActivationMessage(user);
        waitForResult();
        assertThat(smsResult.getContent()).isEqualTo(DEFAULT_ACTIVATION_CODE + " là mã xác nhận Beecow của bạn");
        PhoneDTO mobile = smsResult.getTo().get(0);
        assertThat(mobile.toString()).isEqualTo("+84:0123456789");
    }

    @Test
    @Transactional
    public void sendPasswordResetMessage() throws Exception {
    	smsService.sendPasswordResetMessage(user);
        waitForResult();
        assertThat(smsResult.getContent()).isEqualTo(DEFAULT_PASSWORD_RESET_KEY + " is your Beecow's reset password code");
        PhoneDTO mobile = smsResult.getTo().get(0);
        assertThat(mobile.toString()).isEqualTo("+84:0123456789");
    }

    @Test
    @Transactional
    public void sendPasswordResetMessageVi() throws Exception {
    	user.setLangKey("vi");
    	em.persist(user);
    	em.flush();
    	smsService.sendPasswordResetMessage(user);
        waitForResult();
        assertThat(smsResult.getContent()).isEqualTo(DEFAULT_PASSWORD_RESET_KEY + " là mã đặt lại mật khẩu Beecow của bạn");
        PhoneDTO mobile = smsResult.getTo().get(0);
        assertThat(mobile.toString()).isEqualTo("+84:0123456789");
    }
}
