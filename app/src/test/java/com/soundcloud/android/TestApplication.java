package com.soundcloud.android;

import com.soundcloud.android.api.legacy.PublicApiWrapper;
import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.creators.record.WavHeader;
import com.soundcloud.api.Token;
import dagger.ObjectGraph;

import android.content.Intent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class TestApplication extends SoundCloudApplication {
    public final Token token;
    public final List<Intent> broadcasts = new ArrayList<Intent>();
    private PublicApiWrapper mCloudApi;

    public TestApplication() {
        this(new Token("access", null, Token.SCOPE_NON_EXPIRING));
    }

    private TestApplication(Token token) {
        objectGraph = ObjectGraph.create(new TestApplicationModule(this));
        this.token = token;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mCloudApi = PublicApiWrapper.getInstance(this);
        mCloudApi.setToken(token);
    }

    public PublicCloudAPI getCloudAPI() {
        return mCloudApi;
    }

    @Override
    public void sendBroadcast(Intent intent) {
        broadcasts.add(intent);
        super.sendBroadcast(intent);
    }

    // object mother
    public static Recording getValidRecording() throws IOException {
        Recording r = new Recording(createEmptyWavFile());
        if (!r.getEncodedFile().exists() &&
                !r.getEncodedFile().createNewFile()) {
            throw new RuntimeException("could not build encoded file");
        }
        fill(r.getEncodedFile());
        return r;
    }

    public static File createJpegFile() throws IOException {
        return File.createTempFile("temp", ".jpg");
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
