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
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.validator.constraints.Email;
import org.springframework.data.elasticsearch.annotations.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mediastep.beecow.common.domain.enumeration.AccountType;
import com.mediastep.beecow.common.domain.enumeration.Gender;
import com.mediastep.beecow.common.dto.PhoneDTO;
import com.mediastep.beecow.common.util.PhoneDtoUtil;
import com.mediastep.beecow.gateway.config.Constants;
import com.mediastep.beecow.gateway.config.audit.UserEntityEventListener;

/**
 * A user.
 */
@Entity
@Table(name = "jhi_user")
@DynamicUpdate
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Document(indexName = "user")
@EntityListeners(UserEntityEventListener.class)
public class User extends AbstractAuditingEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Pattern(regexp = Constants.LOGIN_REGEX)
    @Size(min = 1, max = 100)
    @Column(length = 100, unique = true, nullable = false)
    private String login;

    @JsonIgnore
    @NotNull
    @Size(min = 60, max = 60)
    @Column(name = "password_hash",length = 60)
    private String password;

    @Size(max = 50)
    @Column(name = "first_name", length = 50)
    private String firstName;

    @Size(max = 50)
    @Column(name = "last_name", length = 50)
    private String lastName;

    @NotNull
    @Size(max = 100)
    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "avatar_url")
    private String avatarUrl;
    

    @Column(name = "cover_photo_url")
    private String coverPhotoUrl;

    @Email
    @Size(max = 100)
    @Column(length = 100, unique = true)
    private String email;

    @Pattern(regexp = Constants.FULL_PHONE_NUMBER_REGEX)
    @Size(max = 30)
    @Column(name = "mobile", length = 30, unique = true)
    private String mobile;

    @Transient
    private PhoneDTO mobileObject;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type")
    private AccountType accountType;

    @Column(name = "time_zone")
    private String timeZone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @NotNull
    @Column(nullable = false)
    private boolean activated = false;

    @Size(min = 2, max = 10)
    @Column(name = "location_code", length = 10)
    private String locationCode;

    @Size(min = 2, max = 10)
    @Column(name = "lang_key", length = 10)
    private String langKey;

    @Size(max = 20)
    @Column(name = "activation_key", length = 20)
    @JsonIgnore
    private String activationKey;

    @Column(name = "activation_exp_date", nullable = true)
    private ZonedDateTime activationExpiredDate;

    @Size(max = 20)
    @Column(name = "reset_key", length = 20)
    private String resetKey;

    @Column(name = "reset_date", nullable = true)
    private ZonedDateTime resetDate = null;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "jhi_user_authority",
        joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "id")},
        inverseJoinColumns = {@JoinColumn(name = "authority_name", referencedColumnName = "name")})
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<Authority> authorities = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    //Lowercase the login before saving it in database
    public void setLogin(String login) {
        this.login = StringUtils.lowerCase(login);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * @return the displayName
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @param displayName the displayName to set
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * @return the avatarUrl
     */
    public String getAvatarUrl() {
        return avatarUrl;
    }

    /**
     * @param avatarUrl the avatarUrl to set
     */
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    /**
     * @return the coverPhotoUrl
     */
    public String getCoverPhotoUrl() {
        return coverPhotoUrl;
    }

    /**
     * @param coverPhotoUrl the coverPhotoUrl to set
     */
    public void setCoverPhotoUrl(String coverPhotoUrl) {
        this.coverPhotoUrl = coverPhotoUrl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = StringUtils.lowerCase(email);
    }

    /**
     * @return the mobile
     */
    public String getMobile() {
        if (mobile == null) {
            mobile = PhoneDtoUtil.phoneDTOToString(mobileObject);
        }
        return mobile;
    }

    /**
     * @param mobile the mobile to set
     */
    public void setMobile(String mobile) {
        this.mobile = mobile;
        this.mobileObject = PhoneDtoUtil.stringToPhoneDTO(mobile);
    }

    /**
     * @return the mobileObject
     */
    public PhoneDTO getMobileObject() {
        if (mobileObject == null) {
            mobileObject = PhoneDtoUtil.stringToPhoneDTO(mobile);
        }
        return mobileObject;
    }

    /**
     * @param mobileObject the mobileObject to set
     */
    public void setMobileObject(PhoneDTO mobileObject) {
        this.mobileObject = mobileObject;
        this.mobile = PhoneDtoUtil.phoneDTOToString(mobileObject);
    }

    /**
     * @return the accountType
     */
    public AccountType getAccountType() {
        return accountType;
    }

    /**
     * @param accountType the accountType to set
     */
    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    /**
     * @return the timeZone
     */
    public String getTimeZone() {
        return timeZone;
    }

    /**
     * @param timeZone the timeZone to set
     */
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public boolean getActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public String getActivationKey() {
        return activationKey;
    }

    public void setActivationKey(String activationKey) {
        this.activationKey = activationKey;
    }

    public String getResetKey() {
        return resetKey;
    }

    public void setResetKey(String resetKey) {
        this.resetKey = resetKey;
    }

    public ZonedDateTime getResetDate() {
       return resetDate;
    }

    public void setResetDate(ZonedDateTime resetDate) {
       this.resetDate = resetDate;
    }

    public String getLocationCode() {
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        if (locationCode != null) {
            locationCode = locationCode.toUpperCase();
        }
        this.locationCode = locationCode;
    }

    public String getLangKey() {
        return langKey;
    }

    public void setLangKey(String langKey) {
        if (langKey != null) {
            langKey = langKey.toLowerCase();
        }
        this.langKey = langKey;
    }

    public Set<Authority> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Set<Authority> authorities) {
        this.authorities = authorities;
    }

    public ZonedDateTime getActivationExpiredDate() {
        return activationExpiredDate;
    }

    public void setActivationExpiredDate(ZonedDateTime activationExpiredDate) {
        this.activationExpiredDate = activationExpiredDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        User user = (User) o;

        if (!login.equals(user.login)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return login.hashCode();
    }

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "User {"
				+ "id=" + id
				+ ", login=" + login
				+ ", firstName=" + firstName
				+ ", lastName=" + lastName
				+ ", displayName=" + displayName
				+ ", avatarUrl=" + avatarUrl
				+ ", coverPhotoUrl=" + coverPhotoUrl
				+ ", email=" + email
				+ ", mobile=" + mobile
				+ ", mobileObject=" + mobileObject
				+ ", accountType=" + accountType
				+ ", timeZone=" + timeZone
				+ ", dateOfBirth=" + dateOfBirth
				+ ", gender=" + gender
				+ ", activated=" + activated
				+ ", locationCode=" + locationCode
				+ ", langKey=" + langKey
				+ ", activationExpiredDate=" + activationExpiredDate
				+ ", resetDate=" + resetDate
				+ ", authorities=" + authorities
			+ "}";
	}
}
