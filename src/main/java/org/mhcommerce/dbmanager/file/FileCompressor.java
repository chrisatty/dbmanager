package org.mhcommerce.dbmanager.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public interface FileCompressor {

    File compress(File file, Path compressedFile) throws IOException;

    File decompress(File compressedFile, Path file) throws IOException;

    String getExtension();

}