package org.mhcommerce.dbmanager.services;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mhcommerce.dbmanager.DbManager;
import org.mhcommerce.dbmanager.file.FileArchiver;
import org.mhcommerce.dbmanager.file.FileUtilities;
import org.mhcommerce.dbmanager.exceptions.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.util.Arrays;

@RunWith(MockitoJUnitRunner.class)
public class BackupServiceTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private DbManager dbManager;

    @Mock
    private FileUtilities fileUtilities;

    @Mock
    private FileArchiver fileArchiver;

    @InjectMocks
    private BackupService backupService;

    @Before
    public void setup() throws Exception {
        when(dbManager.getAllDatabases()).thenReturn(Arrays.asList("existing_db"));
    }


    @Test(expected = NotFoundException.class)
    public void testBackupInvalidDatabase() {
        backupService.backup("db_doesnt_exist");
    }

    @Test
    public void testBackup() throws Exception {
        File backupFile = temporaryFolder.newFile();
        File compressedFile = temporaryFolder.newFile();
        when(dbManager.backup(eq("existing_db"), any() ,any())).thenReturn(backupFile);
        when(fileUtilities.compress(backupFile)).thenReturn(compressedFile);
        when(fileArchiver.archive(compressedFile)).thenReturn(new URI("http://www.test.com/file"));
        
        assertEquals("http://www.test.com/file", backupService.backup("existing_db").toString());
    }
}