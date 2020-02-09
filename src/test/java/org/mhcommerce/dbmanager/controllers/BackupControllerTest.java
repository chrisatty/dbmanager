package org.mhcommerce.dbmanager.controllers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mhcommerce.dbmanager.AsyncJobRunner;
import org.mhcommerce.dbmanager.DbManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource("classpath:application-test.properties")
public class BackupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DbManager dbManager;

    @MockBean
    private AsyncJobRunner asyncJobRunner;

    @Before
    public void setup() throws Exception {
        when(dbManager.getAllDatabases()).thenReturn(Collections.singletonList("existing_db"));
    }

    @Test
    public void testBackupOfNonExistentDatabase() throws Exception {
        this.mockMvc.perform(post("/database/test_db/backup"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testBackupWithCallbackUrl() throws Exception {
        String body =  "{ \"callbackUrl\" : \"http://myurl.com\" }";
        this.mockMvc.perform(post("/database/existing_db/backup").content(body).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());
    }

    @Test
    public void testBackupWithInvalidCallbackUrl() throws Exception {
        String body =  "{ \"callbackUrl\" : \"invalid_url\" }";
        this.mockMvc.perform(post("/database/existing_db/backup").content(body).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
