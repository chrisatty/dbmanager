package org.mhcommerce.dbmanager.file.compressors;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.mhcommerce.dbmanager.file.FileCompressor;
import org.springframework.stereotype.Component;

@Component
public class GzipCompressor implements FileCompressor {

    @Override
    public File compress(File file, Path newPath) throws IOException  {
        File compressedFile = newPath.toFile();
        try (GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(compressedFile))) {
            try (FileInputStream in = new FileInputStream(file)){
                byte[] buffer = new byte[1024];
                int len;
                while((len=in.read(buffer)) != -1){
                    out.write(buffer, 0, len);
                }
            }
        }
        return compressedFile;

    }

    @Override
    public File decompress(File compressedFile, Path newPath) throws IOException {
        File newFile = newPath.toFile();
        try (FileOutputStream out = new FileOutputStream(newFile)) {
            try (GZIPInputStream in = new GZIPInputStream(new FileInputStream(compressedFile))){
                byte[] buffer = new byte[1024];
                int len;
                while((len=in.read(buffer)) != -1){
                    out.write(buffer, 0, len);
                }
            }
        }
        return newFile;
    }

    @Override
    public String getExtension() {
        return "gz";
    }
}