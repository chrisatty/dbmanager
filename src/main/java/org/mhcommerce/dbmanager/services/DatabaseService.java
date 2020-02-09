package org.mhcommerce.dbmanager.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateSecretResponse;
import com.github.dockerjava.api.model.SecretSpec;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.mhcommerce.dbmanager.Database;
import org.mhcommerce.dbmanager.DbManager;
import org.mhcommerce.dbmanager.exceptions.ExecutionException;
import org.mhcommerce.dbmanager.exceptions.InvalidRequestException;
import org.mhcommerce.dbmanager.exceptions.NotFoundException;
import org.mhcommerce.dbmanager.file.FileUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DatabaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseService.class);

    private final DbManager dbManager;
    private final FileUtilities fileUtilities;
    private final DockerClient dockerClient;
    private final boolean deleteScripts;

    @Autowired
    public DatabaseService(DbManager dbManager, FileUtilities fileUtilities, DockerClient dockerClient,
                            @Value("${dbmanager.script.delete}") boolean deleteScripts) {
        this.dbManager = dbManager;
        this.fileUtilities = fileUtilities;
        this.dockerClient = dockerClient;
        this.deleteScripts = deleteScripts;
    }

    public Database create(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new InvalidRequestException("Database name cannot be empty");
        } else if (!name.matches("[a-zA-Z0-9_/-]*")) {
            throw new InvalidRequestException(name + " is an invalid database name");
        } else if (dbManager.getAllDatabases().contains(name)) {
            throw new InvalidRequestException("Database with name " + name + " already exists");
        }

        dbManager.create(name);
        String password = RandomStringUtils.randomAlphabetic(10);
        LOGGER.info("Creating database with name " + name);
        dbManager.createUser(name, name, password);

        SecretSpec spec = new SecretSpec().withName((name + "_password")).withData(password);
        CreateSecretResponse response = dockerClient.createSecretCmd(spec).exec();

        return new Database(dbManager.getHost(), dbManager.getPort(), name, name, response.getId());
    }

    public void execute(String dbName, File scriptFile) {
        validateExecute(dbName, scriptFile);

        if (fileUtilities.canDecompress(scriptFile)) {
            LOGGER.info("Attempting to decompress " + scriptFile.getAbsolutePath());
            File decompressedFile;
            try {
                decompressedFile = fileUtilities.decompress(scriptFile);
            } catch (IOException e) {
                LOGGER.error("Could not decompress file " + scriptFile.getName());
                throw new ExecutionException("Could not decompress script file " + scriptFile.getName(), e);
            }
            try {
                execute(dbName, decompressedFile);
            } finally {
                fileUtilities.delete(decompressedFile);
            }
            
        } else {
            LOGGER.info("Running " + scriptFile.getName() + " against database " + dbName);
            dbManager.runScript(dbName, scriptFile);
        }
        if (deleteScripts) {
            fileUtilities.delete(scriptFile);
        }
        
    }

    // this allows us to validate before sending if for async execution
    public void validateExecute(String dbName, File scriptFile) {
        if (!dbManager.getAllDatabases().contains(dbName)) {
            throw new NotFoundException("Database with name " + dbName + " does not exist");
        }
        if (!scriptFile.exists()) {
            throw new InvalidRequestException("File " + scriptFile.getName() + " does not exist");
        }
        if (!scriptFile.canRead()) {
            throw new ExecutionException("Wrong permissions on file " + scriptFile.getName());
        }
    }

    public Database fork(String dbName, String newDbName) {
        LOGGER.info("Forking database " + dbName + " to " + newDbName);
        File backup = null;
        Database database;
        try {
            LOGGER.debug("Taking backup for " + dbName);
            backup = dbManager.backup(dbName, Paths.get(System.getProperty("java.io.tmpdir")), "fork_for_" + newDbName);

            LOGGER.debug("Creating database " + newDbName);
            database = create(newDbName);

            LOGGER.debug("Populating " + newDbName + " with backup from " + dbName);
            dbManager.runScript(newDbName, backup);
        } catch (Exception e) {
            // if exception thrown, we'll delete the DB if it got created
            if (dbManager.getAllDatabases().contains(newDbName)) {
                dbManager.delete(newDbName);
            }
            throw e;
        } finally {
            fileUtilities.delete(backup);
        }
        return database;
    }

    public void validateFork(String dbName, String newDbName) {
        List<String> existingDbs = dbManager.getAllDatabases();
        if (!existingDbs.contains(dbName)) {
            throw new NotFoundException("Database with name " + dbName + " does not exist");
        }
        if (StringUtils.isEmpty(newDbName)) {
            throw new InvalidRequestException("New database name cannot be empty");
        }
        if (!newDbName.matches("[a-zA-Z0-9_/-]*")) {
            throw new InvalidRequestException(newDbName + " is an invalid database name");
        }
        if (existingDbs.contains(newDbName)) {
            throw new InvalidRequestException("Database with name " + dbName + " already exists");
        }
    }

    public void delete(String name) throws NotFoundException {
        if (!dbManager.getAllDatabases().contains(name)) {
            throw new NotFoundException("Database with name " + name + " does not exist");
        }
        LOGGER.info("Deleting database with name " + name);
        dbManager.delete(name);
        try {
            dockerClient.removeSecretCmd(name + "_password").exec();
        } catch (Exception e) {
            LOGGER.warn("Could not remove docker secret for database " + name);
        }
    }

    public List<String> getAll() {
        return dbManager.getAllDatabases();
    }

}