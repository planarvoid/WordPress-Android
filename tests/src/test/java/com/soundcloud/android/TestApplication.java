package com.soundcloud.android;

import com.soundcloud.android.audio.WavHeader;
import com.soundcloud.android.imageloader.ImageLoader;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.tracking.Event;
import com.soundcloud.api.Token;

import android.accounts.Account;
import android.content.Intent;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class TestApplication extends SoundCloudApplication {
    public Account account;
    public final Token token;
    public final List<Event> trackedEvents = new ArrayList<Event>();
    public final List<Intent> broadcasts = new ArrayList<Intent>();

    public TestApplication() {
        this(new Token("access", null, Token.SCOPE_NON_EXPIRING));
    }

    public TestApplication(Token token) {
        this.token = token;
    }

    @Override
    public Token useAccount(Account account) {
        this.account = account;
        return token;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mCloudApi = new Wrapper(this, token);
    }

    @Override
    public void track(Event page, Object... args) {
        trackedEvents.add(page);
    }

    @Override
    public void sendBroadcast(Intent intent) {
        broadcasts.add(intent);
        super.sendBroadcast(intent);
    }

    @Override
    protected ImageLoader createImageLoader() {
        return new ImageLoader() {
            @Override
            public BindResult bind(BaseAdapter adapter, ImageView view, String url, Options options) {
                return BindResult.LOADING;
            }
        };
    }

    // object mother
    public static Recording getValidRecording() throws IOException {
        Recording r = new Recording(createEmptyWavFile());
        if (!r.getEncodedFile().exists() &&
            !r.getEncodedFile().createNewFile()) throw new RuntimeException("could not create encoded file");
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
            rf.seek(length-1);
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
