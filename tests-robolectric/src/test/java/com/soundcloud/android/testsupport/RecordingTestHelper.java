package com.soundcloud.android.testsupport;

import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.creators.record.WavHeader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;

// Just moved a bunch of old test helpers from TestApplication here
public class RecordingTestHelper {

    public static Recording getValidRecording() throws IOException {
        Recording r = new Recording(createEmptyWavFile());
        if (!r.getEncodedFile().exists() &&
                !r.getEncodedFile().createNewFile()) {
            throw new RuntimeException("could not build encoded file");
        }
        fill(r.getEncodedFile());
        return r;
    }

    public static File createEmptyWavFile() throws IOException {
        return createWavFile(0);
    }

    public static File createWavFile(int length) throws IOException {
        File tmp = File.createTempFile("temp", ".wav");
        WavHeader.writeHeader(tmp, length);
        if (length > 0) {
            RandomAccessFile rf = new RandomAccessFile(tmp, "rw");
            rf.setLength(length);
            rf.seek(length - 1);
            rf.write(42);
            rf.close();
        }
        return tmp;
    }

    private static void fill(File f) throws FileNotFoundException {
        PrintWriter pw = new PrintWriter(new FileOutputStream(f));
        pw.print("123");
        pw.close();
    }

}
