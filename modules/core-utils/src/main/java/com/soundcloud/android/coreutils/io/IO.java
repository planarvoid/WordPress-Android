package com.soundcloud.android.coreutils.io;

import static com.google.common.base.Preconditions.checkNotNull;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Helper methods which also make testing a bit easier
 */
public class IO {

    private static final int DEFAULT_BUFFER_SIZE = 4096;
    private final Context context;

    public IO(Context context) {
        this.context = context.getApplicationContext();
    }

    public OutputStream outputStreamForFile(File file) throws FileNotFoundException {
        return new FileOutputStream(file);
    }

    public FileInputStream fileInputStream(File file) throws FileNotFoundException {
        return new FileInputStream(file);
    }

    public InputStream inputStreamFromAsset(String filename) throws IOException {
        return context.getAssets().open(filename);
    }

    public InputStream inputStreamFromPrivateDirectory(String filePath) throws IOException {
        return fileInputStream(createFileInPrivateDirectory(filePath));
    }

    public boolean fileExistsInPrivateDirectory(String filePath) {
        File file = new File(context.getFilesDir(), filePath);
        return file.exists();
    }

    public File createFileInPrivateDirectory(String filePath) throws IOException {
        File file = new File(context.getFilesDir(), filePath);

        if (file.exists()) {
            return file;
        }

        if (!file.createNewFile()) {
            throw new IOException("Could not create migration file for copying");
        }
        return file;
    }

    public long copy(InputStream in, OutputStream out) throws IOException {
        checkNotNull(in, "InputStream cannot be null");
        checkNotNull(out, "OutputStream cannot be null");
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        long count = 0;
        int n = 0;
        while (-1 != (n = in.read(buffer))) {
            out.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    public void closeQuietly(Closeable... closeables) {
        checkNotNull(closeables);
        for(Closeable closeable : closeables) {
            try {
                if (closeable != null) {
                    closeable.close();
                }
            } catch (IOException io) {
                // ignore

            }
        }
    }

    public String toString(InputStream in) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);
        copy(in, out);
        //No need to close out stream as it does nothing on ByteArrayOutputStream
        return out.toString();
    }


}
