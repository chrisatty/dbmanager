package org.mhcommerce.dbmanager.file;

import java.io.File;
import java.io.IOException;
import java.net.URI;

public interface FileArchiver {

    URI archive(File file) throws IOException;
}