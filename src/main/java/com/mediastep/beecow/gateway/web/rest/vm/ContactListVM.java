/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.web.rest.vm;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import com.mediastep.beecow.common.dto.PhoneDTO;
import com.mediastep.beecow.common.util.PhoneDtoUtil;

/**
 * View Model for email.
 */
public class ContactListVM {

    private List<String> emails = null;

    private List<String> mobiles = null;

    @JsonIgnore
    private List<PhoneDTO> mobileDTOs = null;

    private static List<String> get(List<String> list) {
        if (list != null && !list.isEmpty()) {
            return list;
        }
        else {
            return null;
        }
    }

    /**
     * @return the emails
     */
    public List<String> getEmails() {
        return emails;
    }

    /**
     * @param emails the emails to set
     */
    public void setEmails(List<String> emails) {
        this.emails = get(emails);
    }

    /**
     * @return the mobiles
     */
    public List<String> getMobiles() {
        return mobiles;
    }

    /**
     * @param mobiles the mobiles to set
     */
    public void setMobiles(List<String> mobiles) {
        this.mobiles = get(mobiles);
    }

    /**
     * @return the mobiles
     */
    @JsonIgnore
    public List<String> getMobilesWithZero() {
        mobileDTOs = getMobileDTOs();
        if (mobileDTOs != null && !mobileDTOs.isEmpty()) {
            return Lists.transform(mobileDTOs, PhoneDTO::toStringFromPhoneWithZero);
        }
        else {
            return null;
        }
    }

    /**
     * @return the mobiles
     */
    @JsonIgnore
    public List<String> getMobilesWithoutZero() {
        mobileDTOs = getMobileDTOs();
        if (mobileDTOs != null && !mobileDTOs.isEmpty()) {
            return Lists.transform(mobileDTOs, PhoneDTO::toStringFromPhoneWithoutZero);
        }
        else {
            return null;
        }
    }

    /**
     * @return the mobiles
     */
    private List<PhoneDTO> getMobileDTOs() {
        if (mobiles == null || mobiles.isEmpty()) {
            return null;
        }
        mobileDTOs = new ArrayList<>();
        for (String mobile : mobiles) {
            PhoneDTO mobileDTO = PhoneDtoUtil.stringToPhoneDTO(mobile);
            mobileDTOs.add(mobileDTO);
        }
        return mobileDTOs;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ContactListVM {emails=" + emails + ", mobiles=" + mobiles + "}";
    }
}
