package org.mhcommerce.dbmanager.services;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mhcommerce.dbmanager.Database;
import org.mhcommerce.dbmanager.DbManager;
import org.mhcommerce.dbmanager.exceptions.*;
import org.mhcommerce.dbmanager.file.FileUtilities;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

import java.io.File;
import java.util.Arrays;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateSecretCmd;
import com.github.dockerjava.api.command.CreateSecretResponse;
import com.github.dockerjava.api.command.RemoveSecretCmd;

@RunWith(MockitoJUnitRunner.class)
public class DatabaseServiceTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private DbManager dbManager;

    @Mock
    private FileUtilities fileUtilities;

    @Mock
    private DockerClient dockerClient;

    private DatabaseService databaseService;

    @Before
    public void setup() throws Exception {
        databaseService = new DatabaseService(dbManager, fileUtilities, dockerClient, true);

        when(dbManager.getAllDatabases()).thenReturn(Arrays.asList("existing_db"));
        when(dbManager.getHost()).thenReturn("localhost");
        when(dbManager.getPort()).thenReturn(3306);

        // mock docker client result
        CreateSecretCmd cmd = mock(CreateSecretCmd.class);
        CreateSecretResponse response = mock(CreateSecretResponse.class);
        when(response.getId()).thenReturn("docker_secret");
        when(cmd.exec()).thenReturn(response);
        when(dockerClient.createSecretCmd(any())).thenReturn(cmd);
        RemoveSecretCmd removeCmd = mock(RemoveSecretCmd.class);
        when(dockerClient.removeSecretCmd(any())).thenReturn(removeCmd);

    }

    @Test
    public void testCreate() throws Exception {
        Database database = databaseService.create("my_new_db");
        assertEquals("localhost", database.getHost());
        assertEquals(3306, database.getPort());
        assertEquals("my_new_db", database.getName());
        assertEquals("my_new_db", database.getUsername());
        assertEquals("docker_secret", database.getSecret());
        verify(dbManager).create(eq("my_new_db"));
        verify(dbManager).createUser(eq("my_new_db"), any(), any());
    }

    @Test(expected=InvalidRequestException.class)
    public void testInvalidDbName() throws Exception {
        databaseService.create("has!invalid'chars");
    }


    @Test(expected=InvalidRequestException.class)
    public void testCreateWithExistingDbName() throws Exception {
        databaseService.create("existing_db");
    }

    @Test
    public void testExecute() throws Exception {
        File testScript = temporaryFolder.newFile();
        databaseService.execute("existing_db", testScript);
        verify(dbManager).runScript(eq("existing_db"), eq(testScript));
    }

    @Test
    public void testExecuteAndDecompress() throws Exception {
        File testScript = temporaryFolder.newFile();
        File decompressedTestScript = temporaryFolder.newFile();
        when(fileUtilities.canDecompress(testScript)).thenReturn(true);
        when(fileUtilities.decompress(testScript)).thenReturn(decompressedTestScript);

        databaseService.execute("existing_db", testScript);
        verify(dbManager).runScript(eq("existing_db"), eq(decompressedTestScript));
    }


    @Test(expected= InvalidRequestException.class)
    public void testExecuteNonExistentFile() throws Exception {
        File testScript = temporaryFolder.newFile();
        testScript.delete();
        databaseService.execute("existing_db", testScript);
    }

    @Test
    public void testFork() throws Exception {
        File testBackup = temporaryFolder.newFile();
        when(dbManager.backup(eq("existing_db"), any(), any())).thenReturn(testBackup);

        databaseService.fork("existing_db", "new_db");
        verify(dbManager).create(eq("new_db"));
        verify(dbManager).createUser(eq("new_db"), eq("new_db"), any());
        verify(dbManager).runScript(eq("new_db"), eq(testBackup));
    }


    @Test(expected=InvalidRequestException.class)
    public void testForkToExistingDb() {
        databaseService.fork("existing_db", "existing_db");
    }

    @Test
    public void testDelete() throws Exception {
        databaseService.delete("existing_db");
        verify(dbManager).delete(eq("existing_db"));
    }

    @Test(expected=NotFoundException.class)
    public void testDeleteOnInvalidDb() throws Exception {
        databaseService.delete("non_exisintent_db");
    }

}