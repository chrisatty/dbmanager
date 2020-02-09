package org.mhcommerce.dbmanager;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public interface DbManager {

    void create(String name);
    List<String> getAllDatabases();
    void createUser(String dbName, String username, String password);
    void runScript(String name, File script);
    void delete(String name);
    File backup(String dbName, Path path, String filename);
    String getHost();
    int getPort();
}