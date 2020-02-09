package org.mhcommerce.dbmanager.mysql;

import org.mhcommerce.dbmanager.DbManager;
import org.mhcommerce.dbmanager.exceptions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Component
@Profile("mysql")
public class MySqlManager implements DbManager {

    private final JdbcTemplate jdbcTemplate;
    @Autowired
    public MySqlManager(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public File backup(String dbName, Path path, String filename) {
        Path filePath = Paths.get(path.toString(), filename + ".sql");
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("/usr/bin/mysqldump",
                        "-u", getAdminUser(),
                        "-p" + getAdminPassword(),
                        "-h", getHost(),
                        "-P", String.valueOf(getPort()),
                        dbName);
        builder.redirectOutput(filePath.toFile());
        int exitCode = 0;
        try {
            Process process = builder.start();
            exitCode = process.waitFor();
        } catch (Exception e) {
            throw new ExecutionException("Could not execute mysqldump command");
        }
        if (exitCode != 0) {
            throw new ExecutionException("mysqldump returned non zero status (" + exitCode + ")");
        } else if (!filePath.toFile().exists()) {
            throw new ExecutionException("Error creating mysql dump file");
        }
        return filePath.toFile();
    }

    @Override
    public void create(String name) {
        jdbcTemplate.execute("CREATE database " + name);
    }

    @Override
    public void runScript(String dbName, File file) {       
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("/usr/bin/mysql",
                        "-u", getAdminUser(),
                        "-p" + getAdminPassword(),
                        "-h", getHost(),
                        "-P", String.valueOf(getPort()),
                        dbName);
        builder.redirectInput(file);
        int exitCode = 0;
        try {
            Process process = builder.start();
            exitCode = process.waitFor();
        } catch (Exception e) {
            throw new ExecutionException("Could not execute script");
        }
        if (exitCode != 0) {
            throw new ExecutionException("mysql returned non zero status (" + exitCode + ")");
        }
    }

    @Override
    public void createUser(String dbName, String username, String password) {
        String createUser = String.format(
                "CREATE USER '%s'@'%%' IDENTIFIED BY '%s';", username, password);
        jdbcTemplate.execute(createUser);

        String grantPrivs = String.format(
                "GRANT ALL ON `%s`.* TO '%s'@'%%' WITH GRANT OPTION;", dbName, username);
        jdbcTemplate.execute(grantPrivs);
    }

    @Override
    public List<String> getAllDatabases() {
        return jdbcTemplate.queryForList("SHOW DATABASES", String.class);
    }

    @Override
    public void delete(String name) {
        jdbcTemplate.execute("DROP database " + name);
        jdbcTemplate.execute("DROP user if exists " + name);
    }

    @Override
    public String getHost() {
        return ((DriverManagerDataSource) jdbcTemplate.getDataSource())
                    .getUrl().replace("jdbc:mysql://", "").split(":")[0];
    }

    @Override
    public int getPort() {
        return Integer.parseInt(((DriverManagerDataSource) jdbcTemplate.getDataSource())
                    .getUrl().replace("jdbc:mysql://", "").split(":")[1]);
    }

    private String getAdminUser() {
        return ((DriverManagerDataSource) jdbcTemplate.getDataSource()).getUsername();
    }

    private String getAdminPassword() {
        return ((DriverManagerDataSource) jdbcTemplate.getDataSource()).getPassword();
    }
}
