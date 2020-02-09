package org.mhcommerce.dbmanager.controllers;

import org.junit.After;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateSecretCmd;
import com.github.dockerjava.api.command.CreateSecretResponse;
import com.github.dockerjava.api.command.RemoveSecretCmd;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc@TestPropertySource("classpath:application-test.properties")
public class DatabaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DbManager dbManager;

    @MockBean
    private AsyncJobRunner asyncJobRunner;

    @MockBean
    private DockerClient dockerClient;

    @Before
    public void setup() throws Exception {
        doNothing().when(dbManager).create(eq("my_db"));
        doNothing().when(dbManager).createUser(eq("my_db"), any(), any());
        doNothing().when(dbManager).runScript(any(), any());

        when(dbManager.getAllDatabases()).thenReturn(Collections.singletonList("existing_db"));
        when(dbManager.getHost()).thenReturn("localhost");
        when(dbManager.getPort()).thenReturn(3306);

        // mock docker client result
        CreateSecretCmd cmd = mock(CreateSecretCmd.class);
        CreateSecretResponse response = mock(CreateSecretResponse.class);
        when(response.getId()).thenReturn("secretid");
        when(cmd.exec()).thenReturn(response);
        when(dockerClient.createSecretCmd(any())).thenReturn(cmd);
        RemoveSecretCmd removeCmd = mock(RemoveSecretCmd.class);
        when(dockerClient.removeSecretCmd(any())).thenReturn(removeCmd);

        // create a sample script file per test
        Paths.get("/tmp/test_script").toFile().createNewFile();
    }

    @After
    public void clean() throws Exception {
        Files.deleteIfExists(Paths.get("/tmp/test_script"));
    }

    @Test
    public void testCreate() throws Exception {
        String body = "{ \"name\": \"my_test-db\"}";
        this.mockMvc.perform(post("/database").content(body).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.host").value("localhost"))
                .andExpect(jsonPath("$.port").value(3306))
                .andExpect(jsonPath("$.name").value("my_test-db"))
                .andExpect(jsonPath("$.username").value("my_test-db"))
                .andExpect(jsonPath("$.secret").value("secretid"));
    }

    @Test
    public void testInvalidDbName() throws Exception {
        String body = "{ \"name\": \"invalid'chars\"}";
        this.mockMvc.perform(post("/database").content(body).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }


    @Test
    public void testCreateWithExistingDbName() throws Exception {
        String body = "{ \"name\": \"existing_db\"}";
        this.mockMvc.perform(post("/database").content(body).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testExecute() throws Exception {
        String body =  "{ \"file\": \"test_script\"}";
        this.mockMvc.perform(post("/database/existing_db/execute").content(body).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());
    }

    @Test
    public void testExecuteWithCallbackUrl() throws Exception {
        String body =  "{ \"file\": \"test_script\", \"callbackUrl\" : \"http://myurl.com\"}";
        this.mockMvc.perform(post("/database/existing_db/execute").content(body).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());
    }

    @Test
    public void testExecuteWithInvalidCallbackUrl() throws Exception {
        String body =  "{ \"file\": \"test_script\", \"callbackUrl\" : \"invalid_url\"}";
        this.mockMvc.perform(post("/database/existing_db/execute").content(body).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testExecuteNonExistentFile() throws Exception {
        String body =  "{ \"file\": \"non_existent_file\"}";
        this.mockMvc.perform(post("/database/existing_db/execute").content(body).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testFork() throws Exception {
        String body =  "{ \"forkedName\": \"my_new_name\"}";
        this.mockMvc.perform(post("/database/existing_db/fork").content(body).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());
    }

    @Test
    public void testDelete() throws Exception {
        this.mockMvc.perform(delete("/database/existing_db"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testDeleteOnInvalidDb() throws Exception {
        this.mockMvc.perform(delete("/database/my_db"))
                .andExpect(status().isNotFound());
    }
}
