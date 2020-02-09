package org.mhcommerce.dbmanager.file.archivers;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.mhcommerce.dbmanager.file.FileArchiver;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("filesystem")
public class FileSystemArchiver implements FileArchiver {

    @Override
    public URI archive(File file) throws IOException {
        String newFile = System.getProperty("user.home") + File.separator + "dbbackups" + File.separator + file.getName();
        return Files.move(file.toPath(), Paths.get(newFile)).toUri();
    }
}
