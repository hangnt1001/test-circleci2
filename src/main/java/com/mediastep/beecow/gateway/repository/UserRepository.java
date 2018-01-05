/*
 *
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 12/1/2017
 * Author: Loi Tran <email:loi.tran@mediastep.com>
 *
 */

package com.mediastep.beecow.gateway.repository;

import com.mediastep.beecow.common.dto.PhoneDTO;
import com.mediastep.beecow.gateway.domain.User;

import java.time.ZonedDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the User entity.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    static final String QUERY_FIND_USER_BY_CONTACT_WHERE_CLAUSE =
        "where "
            + "(((:emails) is not null) and (user.accountType = 'EMAIL') and (user.email in :emails)) "
            + "or "
            + "(((:mobileListWithZero) is not null) and (user.accountType = 'MOBILE') and (user.mobile in :mobileListWithZero)) "
            + "or "
            + "(((:mobileListWithoutZero) is not null) and (user.accountType = 'MOBILE') and (user.mobile in :mobileListWithoutZero)) ";

    Optional<User> findOneByIdAndActivationKey(Long userId, String activationKey);

    Optional<User> findOneByLoginAndActivationKey(String login, String activationKey);

    List<User> findAllByActivatedIsFalseAndCreatedDateBefore(ZonedDateTime dateTime);

    Optional<User> findOneByResetKeyOrderByResetDateDesc(String resetKey);

    @Query("select user from User user where user.email = ?1 and user.accountType = 'EMAIL'")
    Optional<User> findOneByEmail(String email);

    @Query("select user from User user where (user.mobile = ?1 or user.mobile = ?2) and user.accountType = 'MOBILE'")
    Optional<User> findOneByMobile(String phoneNumberWithFrontZero, String phoneNumberWithoutFrontZero);

    default Optional<User> findOneByMobile(PhoneDTO mobileDTO) {
        return findOneByMobile(mobileDTO.toStringFromPhoneWithZero(), mobileDTO.toStringFromPhoneWithoutZero());
    }

    Optional<User> findOneByLogin(String login);

    Optional<User> findOneByLoginIn(List<String> logins);

    @Query(value = "select user "
		+ "from User user "
        + "where ((:login is not null and user.login = :login) "
        		+ "or (:email is not null and user.email = :email and user.accountType = 'EMAIL') "
        		+ "or (:mobile is not null and user.mobile = :mobile and user.accountType = 'MOBILE')) "
        	+ "and (user.activated != true) "
            + "and (user.accountType != 'PRE_ACTIVATE') "
            + "and (user.activationKey is not null and user.activationKey != '') "
            + "and (user.activationExpiredDate < :now) ")
    List<User> findAllRegisteredButNotActivated(@Param("login") String login, @Param("email") String email, @Param("mobile") String mobile, @Param("now") ZonedDateTime now);

    @Query(value = "select user from User user "
        + "where user.activated != true "
            + "and user.accountType != 'PRE_ACTIVATE' "
            + "and user.activationKey is not null and user.activationKey != '' "
            + "and user.activationExpiredDate < :now ")
    List<User> findAllRegisteredButNotActivated(@Param("now") ZonedDateTime now);

    @Query(value = "select distinct user from User user left join fetch user.authorities",
        countQuery = "select count(user) from User user")
    Page<User> findAllWithAuthorities(Pageable pageable);

    @Query(value = "select distinct user from User user left join user.authorities where 'ROLE_GUEST' NOT MEMBER OF user.authorities",
        countQuery = "select count(distinct user) from User user left join user.authorities where 'ROLE_GUEST' NOT MEMBER OF user.authorities")
    Page<User> findAllActivatedWithAuthorities(Pageable pageable);

    @Query(value = "select user from User user " + QUERY_FIND_USER_BY_CONTACT_WHERE_CLAUSE)
    List<User> findAll(@Param("emails") List<String> emails, @Param("mobileListWithZero") List<String> mobileListWithZero, @Param("mobileListWithoutZero") List<String> mobileListWithoutZero, Sort orderBy);
}
