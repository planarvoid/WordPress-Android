package com.soundcloud.android.offline;

import com.soundcloud.android.Consts;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.IOUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SecureFileStorage {

    private static final String DIRECTORY_NAME = "offline";
    protected final File OFFLINE_DIR;

    @Inject
    public SecureFileStorage() {
        OFFLINE_DIR = new File(Consts.FILES_PATH, DIRECTORY_NAME);
        createDirectoryIfNeeded();
    }

    // TODO: Encrypt before storage. Separate story.
    public void storeTrack(Urn urn, InputStream input) throws IOException {
        if (!createDirectoryIfNeeded()) {
            throw new IOException("Failed to create directory for " + OFFLINE_DIR.getAbsolutePath());
        }

        final File trackFile = new File(OFFLINE_DIR, generateFileName(urn));
        OutputStream output = null;

        try {
            output = new FileOutputStream(trackFile);
            int count;
            byte[] data = new byte[1024];
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }
        } finally {
            IOUtils.close(input);
            IOUtils.close(output);
        }
    }

    protected String generateFileName(Urn urn) {
        // Todo: this is part of encryption story
        return "track" + urn.getNumericId() + ".mp3";
    }

    private boolean createDirectoryIfNeeded() {
        return OFFLINE_DIR.exists() || IOUtils.mkdirs(OFFLINE_DIR);
    }
}
