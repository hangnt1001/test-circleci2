/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.domain;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.mediastep.beecow.common.security.AuthoritiesConstants;

/**
 * An authority (a security role) used by Spring Security.
 */
@Entity
@Table(name = "jhi_authority")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Authority implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final Authority ADMIN = new Authority(AuthoritiesConstants.ADMIN);

    public static final Authority USER = new Authority(AuthoritiesConstants.USER);

    public static final Authority ANONYMOUS = new Authority(AuthoritiesConstants.ANONYMOUS);

    public static final Authority GUEST = new Authority(AuthoritiesConstants.GUEST);

    public static final Authority BEECOW = new Authority(AuthoritiesConstants.BEECOW);

    public static final Authority STORE = new Authority(AuthoritiesConstants.STORE);

    public static final Authority COMPANY = new Authority(AuthoritiesConstants.COMPANY);

    public static final Authority PARTNER = new Authority(AuthoritiesConstants.PARTNER);

    public static final Authority EDITOR = new Authority(AuthoritiesConstants.EDITOR);

    @NotNull
    @Size(min = 0, max = 50)
    @Id
    @Column(length = 50)
    private String name;

    public static Set<String> authoritiesToStrings(Set<Authority> authorities) {
        if (authorities == null) {
            return null;
        }
        return authorities.stream().map(Authority::getName)
                .collect(Collectors.toSet());
    }

    public Authority() {
    }

    public Authority(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || o instanceof String || getClass() != o.getClass()) {
            return false;
        }

        String oname;
        if (o instanceof String) {
            oname = (String) o;
        }
        else {
            Authority authority = (Authority) o;
            oname = authority.getName();
        }

        if (name != null ? !name.equals(oname) : oname != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Authority{" +
            "name='" + name + '\'' +
            "}";
    }
}
