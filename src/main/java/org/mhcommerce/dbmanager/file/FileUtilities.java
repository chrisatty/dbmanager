package org.mhcommerce.dbmanager.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FileUtilities {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtilities.class);

    private List<FileCompressor> fileCompressors;

    @Value("${dbmanager.compression.default:gz}")
    private String defaultCompression;

    @Autowired
    public FileUtilities(List<FileCompressor> fileCompressors) {
        this.fileCompressors = fileCompressors;
    }

    public Set<String> getFileTypes() {
        return fileCompressors.stream().map(fc -> fc.getExtension()).collect(Collectors.toSet());
    }

    // in case we just want to manually give the extension, but we won't actually check the file
    public File decompress(File file) throws IOException {
        Optional<FileCompressor> compressor = findCompressor(file);
        if (!compressor.isPresent()) {
            throw new RuntimeException("Could not find compressor for extension for file " + file.getName());
        }
        String newFilename = file.getAbsolutePath().replace("." + compressor.get().getExtension(), "");
        return compressor.get().decompress(file, Paths.get(newFilename));
    }


    public File compress(File file, String extension) throws IOException {
        Optional<FileCompressor> compressor = findCompressor(extension);
        if (!compressor.isPresent()) {
            throw new RuntimeException("Could not find compressor for extension " + file.getName());
        }
        String newFilename = file.getAbsolutePath() + "." + compressor.get().getExtension();
        return compressor.get().compress(file, Paths.get(newFilename));
    }

    // will use the default compression set in config
    public File compress(File file) throws IOException {
        Optional<FileCompressor> compressor = findCompressor(defaultCompression);
        if (!compressor.isPresent()) {
            throw new RuntimeException("Could not find compressor");
        }
        String newFilename = file.getAbsolutePath() + "." + compressor.get().getExtension();
        return compressor.get().compress(file, Paths.get(newFilename));
    }

    private Optional<FileCompressor> findCompressor(File file) {
        if (!file.getName().contains(".")) {
            return Optional.empty();
        }
        String extension = file.getName().substring(file.getName().lastIndexOf(".") + 1);
        return findCompressor(extension);
    }

    private Optional<FileCompressor> findCompressor(String extension) {
        return fileCompressors.stream()
                              .filter(fc -> fc.getExtension().equalsIgnoreCase(extension))
                              .findAny();
    }

    public boolean canDecompress(File file) {
        return findCompressor(file).isPresent();
    }

    public void delete(File... files) {
        Arrays.asList(files).forEach(file -> {
            try {
                if (file != null && file.exists()) {
                    LOGGER.debug("Deleting file " + file.getAbsolutePath());
                    Files.deleteIfExists(file.toPath());
                }
            } catch (Exception e) {
                LOGGER.warn("Could not delete file " + file.getAbsolutePath(), e);
            }
        });
    }

}