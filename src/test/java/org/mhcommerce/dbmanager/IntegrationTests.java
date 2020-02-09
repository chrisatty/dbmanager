package org.mhcommerce.dbmanager;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import com.github.dockerjava.api.DockerClient;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mhcommerce.dbmanager.file.FileArchiver;
import org.mhcommerce.dbmanager.services.BackupService;
import org.mhcommerce.dbmanager.services.DatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles(profiles = "mysql") // dont want AWS
@TestPropertySource("classpath:application-test.properties")
@Disabled("Should only be run with a MySQL connection and access to a docker socket")
class IntegrationTests {

    @TempDir
    public static Path sharedTempDir;

    @TestConfiguration
    static class TestConfig {

        @Bean
        /* Don't upload to S3 in tests - just move it to a new files in tmp */
        public FileArchiver fileArchiver() {
            return new FileArchiver() {
                @Override
                public URI archive(File file) throws IOException {
                    return Files.move(file.toPath(), sharedTempDir.getFileName()).toUri();
                }
            };
        }
    }

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private BackupService backupService;

    @Autowired
    private DockerClient dockerClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setup() {
        jdbcTemplate.execute("DROP database if exists dbManagerTest");
        jdbcTemplate.execute("CREATE database if not exists dbManagerTestExisting");
    }

    @AfterEach
    public void after() {
        jdbcTemplate.execute("DROP database if exists dbManagerTest");
        jdbcTemplate.execute("DROP user if exists dbManagerTest");
        jdbcTemplate.execute("DROP database if exists dbManagerTestExisting");
    }

	@Test
	public void testDatabaseCreation() {
        try {
            Database database = databaseService.create("dbManagerTest");
            assertEquals(database.getHost(), "127.0.0.1");
            assertEquals(3306, database.getPort());
            assertEquals(database.getUsername(), "dbManagerTest");
            assertTrue(dbExists("dbManagerTest"));
            assertTrue(userExists("dbManagerTest"));
            assertTrue(
                dockerClient.listSecretsCmd().exec().stream()
                        .filter(s -> s.getId().equals(database.getSecret())).findAny().isPresent()
            );
        } finally {
            dockerClient.removeSecretCmd("dbManagerTest_password").exec();
        }
    }

    @Test
    public void testDatabaseDeletion() {
        databaseService.delete("dbManagerTestExisting");
        assertFalse(dbExists("dbManagerTestExisting"));
    }

    @Test
	public void testExecuteScript() throws Exception {
        File script = new File("src/test/resources/exampleScript.sql");
        databaseService.execute("dbManagerTestExisting", script);
        
        assertEquals("test",
         jdbcTemplate.queryForObject("show tables from dbManagerTestExisting like 'test'", String.class)
        );
    }

    @Test
	public void testExecuteCompressedScript() throws Exception {
        File script = new File("src/test/resources/exampleScript2.sql.gz");
        databaseService.execute("dbManagerTestExisting", script);
        
        assertEquals("compressedtest",
         jdbcTemplate.queryForObject("show tables from dbManagerTestExisting like 'compressedtest'", String.class)
        );
    }

    @Test
	public void testFork() throws Exception {
        jdbcTemplate.execute("CREATE TABLE dbManagerTestExisting.fork (name VARCHAR(20));");

        Database database = databaseService.fork("dbManagerTestExisting", "dbManagerTest");
        assertTrue(dbExists("dbManagerTest"));
        assertTrue(userExists("dbManagerTest"));
        assertTrue(
            dockerClient.listSecretsCmd().exec().stream()
                    .filter(s -> s.getId().equals(database.getSecret())).findAny().isPresent()
        );
        assertEquals("fork",
         jdbcTemplate.queryForObject("show tables from dbManagerTestExisting like 'fork'", String.class)
        );

        dockerClient.removeSecretCmd("dbManagerTest_password").exec();
    }

    @Test
	public void testBackup() throws Exception {
        URI uri = backupService.backup("dbManagerTestExisting");
        File backup = new File(uri);
        assertTrue(backup.exists());
    }

    private boolean userExists(String user) {
        return jdbcTemplate.queryForList("select User from mysql.user", String.class).contains(user);
    }

    private boolean dbExists(String name) {
        return jdbcTemplate.queryForList("show databases", String.class).contains(name);
    }
}
