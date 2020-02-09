package org.mhcommerce.dbmanager.services;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.mhcommerce.dbmanager.DbManager;
import org.mhcommerce.dbmanager.exceptions.*;
import org.mhcommerce.dbmanager.file.FileArchiver;
import org.mhcommerce.dbmanager.file.FileUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;;

@Service
public class BackupService {

    private static final DateTimeFormatter fileNameFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd_hh-mm-ss");
    private static final Logger LOGGER = LoggerFactory.getLogger(BackupService.class);

    private final DbManager dbManager;
    private final FileArchiver fileArchiver;
    private final FileUtilities fileUtilities;

    @Autowired
    public BackupService(DbManager dbManager, FileArchiver fileArchiver, FileUtilities fileUtilities) {
        this.dbManager = dbManager;
        this.fileArchiver = fileArchiver;
        this.fileUtilities = fileUtilities;
    }

    public void validateRequest(String dbName) {
        if (!dbManager.getAllDatabases().contains(dbName)) {
            throw new NotFoundException("Database with name " + dbName + " does not exist");
        }
    }

    public URI backup(String dbName) {
        validateRequest(dbName);
        File compressedFile = null;
        File backupFile = null;
        URI uri;
        try {
            String backupFilename = dbName + "_" + LocalDateTime.now().format(fileNameFormatter);
            LOGGER.info("Backing up database " + dbName);
            backupFile = dbManager.backup(dbName, Paths.get(System.getProperty("java.io.tmpdir")), backupFilename);

            LOGGER.debug("Compressing backup file " + backupFile.getName());
            compressedFile = fileUtilities.compress(backupFile);
            
            LOGGER.debug("Archiving backup file " + compressedFile.getName());
            uri = fileArchiver.archive(compressedFile);
        } catch (IOException e) {
            LOGGER.error("Error creating backup for database " + dbName);
            throw new ExecutionException("Error creating backup", e);
        } finally {
            fileUtilities.delete(compressedFile, backupFile);
        }
        return uri;
    }
}