/*******************************************************************************
 * Copyright 2017 (C) Mediastep Software Inc.
 *
 * Created on : 01/01/2017
 * Author: Huyen Lam <email:huyen.lam@mediastep.com>
 *  
 *******************************************************************************/
package com.mediastep.beecow.gateway.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import com.mediastep.beecow.gateway.BeecowGatewayApp;
import com.mediastep.beecow.gateway.domain.Email;
import com.mediastep.beecow.gateway.repository.EmailRepository;
import com.mediastep.beecow.gateway.repository.search.EmailSearchRepository;
import com.mediastep.beecow.gateway.service.EmailService;
import com.mediastep.beecow.gateway.service.MailService;
import com.mediastep.beecow.gateway.service.dto.EmailDTO;
import com.mediastep.beecow.gateway.service.mapper.EmailMapper;
import com.mediastep.beecow.gateway.web.rest.vm.ContactUsVM;

/**
 * Test class for the EmailResource REST controller.
 *
 * @see EmailResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = BeecowGatewayApp.class)
public class EmailResourceIntTest {

    private static final String DEFAULT_EMAIL = "test@example.com";
    private static final String UPDATED_EMAIL = "test2@example.com";
    private static final String INVALID_EMAIL = "test";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_COMPANY = "AAAAAAAAAA";
    private static final String UPDATED_COMPANY = "BBBBBBBBBB";

    private static final String DEFAULT_PHONE = "AAAAAAAAAA";
    private static final String UPDATED_PHONE = "BBBBBBBBBB";

    private static final String DEFAULT_EMAIL_CONTENT = "AAAAAAAAAAAAAAAAAAAA";

    @Inject
    private EmailRepository emailRepository;

    @Inject
    private EmailMapper emailMapper;

    @Inject
    private EmailService emailService;

    @Inject
    private MailService mailService;

    @Inject
    private EmailSearchRepository emailSearchRepository;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Inject
    private EntityManager em;

    private MockMvc restEmailMockMvc;

    private Email email;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        EmailResource emailResource = new EmailResource();
        ReflectionTestUtils.setField(emailResource, "emailService", emailService);
        ReflectionTestUtils.setField(emailResource, "mailService", mailService);
        this.restEmailMockMvc = MockMvcBuilders.standaloneSetup(emailResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Email createEntity(EntityManager em) {
        Email email = new Email()
                .email(DEFAULT_EMAIL)
                .name(DEFAULT_NAME)
                .company(DEFAULT_COMPANY)
                .phone(DEFAULT_PHONE);
        return email;
    }

    @Before
    public void initTest() {
        emailSearchRepository.deleteAll();
        email = createEntity(em);
    }

    @Test
    @Transactional
    public void createEmail() throws Exception {
        int databaseSizeBeforeCreate = emailRepository.findAll().size();

        // Create the Email
        EmailDTO emailDTO = emailMapper.emailToEmailDTO(email);

        restEmailMockMvc.perform(post("/api/emails")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(emailDTO)))
            .andExpect(status().isOk());

        // Validate the Email in the database
        List<Email> emailList = emailRepository.findAll();
        assertThat(emailList).hasSize(databaseSizeBeforeCreate + 1);
        Email testEmail = emailList.get(emailList.size() - 1);
        assertThat(testEmail.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(testEmail.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testEmail.getCompany()).isEqualTo(DEFAULT_COMPANY);
        assertThat(testEmail.getPhone()).isEqualTo(DEFAULT_PHONE);

        // Validate the Email in ElasticSearch
        Email emailEs = emailSearchRepository.findOne(testEmail.getId());
        assertThat(emailEs.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(emailEs.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(emailEs.getCompany()).isEqualTo(DEFAULT_COMPANY);
        assertThat(emailEs.getPhone()).isEqualTo(DEFAULT_PHONE);
    }

    @Test
    @Transactional
    public void createEmailAlreadyExist() throws Exception {
        // Initialize the database
        emailRepository.saveAndFlush(email);
        int databaseSizeBeforeCreate = emailRepository.findAll().size();

        // Create the Email
        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setId(null);
        emailDTO.setEmail(DEFAULT_EMAIL);
        emailDTO.setName(UPDATED_NAME);
        emailDTO.setCompany(UPDATED_COMPANY);
        emailDTO.setPhone(UPDATED_PHONE);

        restEmailMockMvc.perform(post("/api/emails")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(emailDTO)))
            .andExpect(status().isOk());

        // Validate the Email in the database
        List<Email> emailList = emailRepository.findAll();
        assertThat(emailList).hasSize(databaseSizeBeforeCreate);
        Email testEmail = emailRepository.findOne(email.getId());
        assertThat(testEmail.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(testEmail.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testEmail.getCompany()).isEqualTo(UPDATED_COMPANY);
        assertThat(testEmail.getPhone()).isEqualTo(UPDATED_PHONE);

        // Validate the Email in ElasticSearch
        Email emailEs = emailSearchRepository.findOne(testEmail.getId());
        assertThat(emailEs.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(emailEs.getName()).isEqualTo(UPDATED_NAME);
        assertThat(emailEs.getCompany()).isEqualTo(UPDATED_COMPANY);
        assertThat(emailEs.getPhone()).isEqualTo(UPDATED_PHONE);
    }

    @Test
    @Transactional
    public void createEmailChangeEmail() throws Exception {
        // Initialize the database
        emailRepository.saveAndFlush(email);
        int databaseSizeBeforeCreate = emailRepository.findAll().size();

        // Create the Email
        EmailDTO emailDTO = emailMapper.emailToEmailDTO(email);
        emailDTO.setEmail(UPDATED_EMAIL);
        emailDTO.setName(UPDATED_NAME);
        emailDTO.setCompany(UPDATED_COMPANY);
        emailDTO.setPhone(UPDATED_PHONE);

        restEmailMockMvc.perform(post("/api/emails")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(emailDTO)))
            .andExpect(status().isOk());

        // Validate the Email in the database
        List<Email> emailList = emailRepository.findAll();
        assertThat(emailList).hasSize(databaseSizeBeforeCreate + 1);
        Email testEmail = emailList.get(emailList.size() - 1);
        assertThat(testEmail.getEmail()).isEqualTo(UPDATED_EMAIL);
        assertThat(testEmail.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testEmail.getCompany()).isEqualTo(UPDATED_COMPANY);
        assertThat(testEmail.getPhone()).isEqualTo(UPDATED_PHONE);

        // Validate the Email in ElasticSearch
        Email emailEs = emailSearchRepository.findOne(testEmail.getId());
        assertThat(emailEs.getEmail()).isEqualTo(UPDATED_EMAIL);
        assertThat(emailEs.getName()).isEqualTo(UPDATED_NAME);
        assertThat(emailEs.getCompany()).isEqualTo(UPDATED_COMPANY);
        assertThat(emailEs.getPhone()).isEqualTo(UPDATED_PHONE);
    }

    @Test
    @Transactional
    public void createEmailInvalid() throws Exception {
        int databaseSizeBeforeCreate = emailRepository.findAll().size();

        // Create the Email
        email.setEmail(INVALID_EMAIL);
        EmailDTO emailDTO = emailMapper.emailToEmailDTO(email);

        restEmailMockMvc.perform(post("/api/emails")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(emailDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Email in the database
        List<Email> emailList = emailRepository.findAll();
        assertThat(emailList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkEmailIsRequired() throws Exception {
        int databaseSizeBeforeTest = emailRepository.findAll().size();
        // set the field null
        email.setEmail(null);

        // Create the Email, which fails.
        EmailDTO emailDTO = emailMapper.emailToEmailDTO(email);

        restEmailMockMvc.perform(post("/api/emails")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(emailDTO)))
            .andExpect(status().isBadRequest());

        List<Email> emailList = emailRepository.findAll();
        assertThat(emailList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllEmails() throws Exception {
        // Initialize the database
        emailRepository.saveAndFlush(email);

        // Get all the emailList
        restEmailMockMvc.perform(get("/api/emails?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(email.getId().intValue())))
            .andExpect(jsonPath("$.[*].email").value(hasItem(DEFAULT_EMAIL.toString())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())))
            .andExpect(jsonPath("$.[*].company").value(hasItem(DEFAULT_COMPANY.toString())))
            .andExpect(jsonPath("$.[*].phone").value(hasItem(DEFAULT_PHONE.toString())));
    }

    @Test
    @Transactional
    public void getEmail() throws Exception {
        // Initialize the database
        emailRepository.saveAndFlush(email);

        // Get the email
        restEmailMockMvc.perform(get("/api/emails/{email}", email.getEmail()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(email.getId().intValue()))
            .andExpect(jsonPath("$.email").value(DEFAULT_EMAIL.toString()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME.toString()))
            .andExpect(jsonPath("$.company").value(DEFAULT_COMPANY.toString()))
            .andExpect(jsonPath("$.phone").value(DEFAULT_PHONE.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingEmail() throws Exception {
        // Get the email
        restEmailMockMvc.perform(get("/api/emails/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void deleteEmail() throws Exception {
        // Initialize the database
        emailRepository.saveAndFlush(email);
        emailSearchRepository.save(email);
        int databaseSizeBeforeDelete = emailRepository.findAll().size();

        // Get the email
        restEmailMockMvc.perform(delete("/api/emails/{email}", email.getEmail())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate ElasticSearch is empty
        boolean emailExistsInEs = emailSearchRepository.exists(email.getId());
        assertThat(emailExistsInEs).isFalse();

        // Validate the database is empty
        List<Email> emailList = emailRepository.findAll();
        assertThat(emailList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void searchEmail() throws Exception {
        // Initialize the database
        emailRepository.saveAndFlush(email);
        emailSearchRepository.save(email);

        // Search the email
        restEmailMockMvc.perform(get("/api/_search/emails?query=id:" + email.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(email.getId().intValue())))
            .andExpect(jsonPath("$.[*].email").value(hasItem(DEFAULT_EMAIL.toString())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())))
            .andExpect(jsonPath("$.[*].company").value(hasItem(DEFAULT_COMPANY.toString())))
            .andExpect(jsonPath("$.[*].phone").value(hasItem(DEFAULT_PHONE.toString())));
    }

    @Test
    @Transactional
    public void contactUs() throws Exception {
        int databaseSizeBeforeContact = emailRepository.findAll().size();

        // Create the Email
        EmailDTO from = emailMapper.emailToEmailDTO(email);
        ContactUsVM contactUs = new ContactUsVM();
        contactUs.setFrom(from);
        contactUs.setContent(DEFAULT_EMAIL_CONTENT);

        restEmailMockMvc.perform(post("/api/emails/contact-us")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(contactUs)))
            .andExpect(status().isOk());

        // Validate the Email in the database
        List<Email> emailList = emailRepository.findAll();
        assertThat(emailList).hasSize(databaseSizeBeforeContact + 1);
        Email testEmail = emailList.get(emailList.size() - 1);
        assertThat(testEmail.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(testEmail.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testEmail.getCompany()).isEqualTo(DEFAULT_COMPANY);
        assertThat(testEmail.getPhone()).isEqualTo(DEFAULT_PHONE);

        // Validate the Email in ElasticSearch
        Email emailEs = emailSearchRepository.findOne(testEmail.getId());
        assertThat(emailEs.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(emailEs.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(emailEs.getCompany()).isEqualTo(DEFAULT_COMPANY);
        assertThat(emailEs.getPhone()).isEqualTo(DEFAULT_PHONE);
    }

    @Test
    @Transactional
    public void contactUsTwice() throws Exception {
        int databaseSizeBeforeContact = emailRepository.findAll().size();

        // Create the Email
        EmailDTO from = emailMapper.emailToEmailDTO(email);
        ContactUsVM contactUs = new ContactUsVM();
        contactUs.setFrom(from);
        contactUs.setContent(DEFAULT_EMAIL_CONTENT);

        restEmailMockMvc.perform(post("/api/emails/contact-us")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(contactUs)))
            .andExpect(status().isOk());

        // Validate the Email in the database
        List<Email> emailList = emailRepository.findAll();
        assertThat(emailList).hasSize(databaseSizeBeforeContact + 1);
        Email testEmail = emailList.get(emailList.size() - 1);
        assertThat(testEmail.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(testEmail.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testEmail.getCompany()).isEqualTo(DEFAULT_COMPANY);
        assertThat(testEmail.getPhone()).isEqualTo(DEFAULT_PHONE);

        // Validate the Email in ElasticSearch
        Email emailEs = emailSearchRepository.findOne(testEmail.getId());
        assertThat(emailEs.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(emailEs.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(emailEs.getCompany()).isEqualTo(DEFAULT_COMPANY);
        assertThat(emailEs.getPhone()).isEqualTo(DEFAULT_PHONE);

        databaseSizeBeforeContact++;
        // Call the second time
        restEmailMockMvc.perform(post("/api/emails/contact-us")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(contactUs)))
            .andExpect(status().isOk());

        emailList = emailRepository.findAll();
        assertThat(emailList).hasSize(databaseSizeBeforeContact);
    }

    @Test
    @Transactional
    public void contactUsFromEmailInvalid() throws Exception {
        // Create the Email
        EmailDTO from = new EmailDTO();
        from.setEmail(INVALID_EMAIL);
        ContactUsVM contactUs = new ContactUsVM();
        contactUs.setFrom(from);
        contactUs.setContent(DEFAULT_EMAIL_CONTENT);

        restEmailMockMvc.perform(post("/api/emails/contact-us")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(contactUs)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void contactUsFromEmailNull() throws Exception {
        // Create the Email
        EmailDTO from = new EmailDTO();
        from.setEmail(null);
        ContactUsVM contactUs = new ContactUsVM();
        contactUs.setFrom(from);
        contactUs.setContent(DEFAULT_EMAIL_CONTENT);

        restEmailMockMvc.perform(post("/api/emails/contact-us")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(contactUs)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void contactUsFromNull() throws Exception {
        // Create the Email
        ContactUsVM contactUs = new ContactUsVM();
        contactUs.setFrom(null);
        contactUs.setContent(DEFAULT_EMAIL_CONTENT);

        restEmailMockMvc.perform(post("/api/emails/contact-us")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(contactUs)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void contactUsContentBlank() throws Exception {
        // Create the Email
        EmailDTO from = new EmailDTO();
        from.setEmail(DEFAULT_EMAIL);
        ContactUsVM contactUs = new ContactUsVM();
        contactUs.setFrom(from);
        contactUs.setContent("                    ");

        restEmailMockMvc.perform(post("/api/emails/contact-us")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(contactUs)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    public void contactUsContentNull() throws Exception {
        // Create the Email
        EmailDTO from = new EmailDTO();
        from.setEmail(DEFAULT_EMAIL);
        ContactUsVM contactUs = new ContactUsVM();
        contactUs.setFrom(from);
        contactUs.setContent(null);

        restEmailMockMvc.perform(post("/api/emails/contact-us")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(contactUs)))
            .andExpect(status().isBadRequest());
    }
}
