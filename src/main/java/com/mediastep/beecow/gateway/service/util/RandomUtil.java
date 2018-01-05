/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.service.util;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for generating random Strings.
 */
public final class RandomUtil {

    private static final int DEF_COUNT = 20;
    private static final int SHORT_DEF_COUNT = 6;

    private RandomUtil() {
    }

    /**
     * Generates a password.
     *
     * @return the generated password
     */
    public static String generatePassword() {
        return RandomStringUtils.randomAlphanumeric(DEF_COUNT);
    }

    /**
     * Generates an activation key.
     *
     * @return the generated activation key
     */
    public static String generateActivationKey() {
        return RandomStringUtils.randomNumeric(DEF_COUNT);
    }
    
    /**
     * Generates an short activation key.
     *
     * @return the generated activation key
     */
    public static String generateShortActivationKey() {
        return RandomStringUtils.randomNumeric(SHORT_DEF_COUNT);
    }

    /**
    * Generates a reset key.
    *
    * @return the generated reset key
    */
    public static String generateResetKey() {
        return RandomStringUtils.randomNumeric(SHORT_DEF_COUNT);
    }

    /**
    * Generates a guest login subfix.
    *
    * @return the generated reset key
    */
    public static String generateGuestSubfix(String prefix) {
        String randomString = RandomStringUtils.randomNumeric(DEF_COUNT);
        if (StringUtils.isBlank(prefix)) {
            return randomString;
        }
        else {
            return prefix + randomString;
        }
    }
}
