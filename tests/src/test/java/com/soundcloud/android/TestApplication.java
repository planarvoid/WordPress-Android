package com.soundcloud.android;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.audio.WavHeader;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.User;
import com.soundcloud.android.tracking.Event;
import com.soundcloud.api.Env;
import com.soundcloud.api.Token;

import android.accounts.Account;
import android.content.Intent;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestApplication extends SoundCloudApplication {
    public Account account;
    public final Map<String, String> accountData = new HashMap<String, String>();
    public final Token token;
    public final List<Event> trackedEvents = new ArrayList<Event>();
    public final List<Intent> broadcasts = new ArrayList<Intent>();

    public TestApplication() {
        this(new Token("access", null, Token.SCOPE_NON_EXPIRING));
    }

    public TestApplication(Token token) {
        this.token = token;
        mCloudApi = new Wrapper(null, "id", "secret", null, token, Env.LIVE);
    }

    @Override
    public Account getAccount() {
        return account;
    }

    @Override
    public Token useAccount(Account account) {
        this.account = account;
        return token;
    }

    @Override
    public boolean setAccountData(String key, String value) {
        return accountData.put(key, value) != null;
    }

    @Override
    public String getAccountData(String key) {
        return accountData.get(key);
    }

    public void setCurrentUserId(long id) {
        setAccountData(User.DataKeys.USER_ID, id);
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

            @Override
            public BindResult bind(BaseExpandableListAdapter adapter, ImageView view, String url, Options options) {
                return BindResult.LOADING;
            }
        };
    }

    // object mother
    public static Recording getValidRecording() throws IOException {
        Recording r = new Recording(getTestFile());
        if (!r.getEncodedFile().exists() &&
            !r.getEncodedFile().createNewFile()) throw new RuntimeException("could not create encoded file");
        fill(r.getEncodedFile());
        return r;
    }

    public static File getTestFile() throws IOException {
        File tmp = File.createTempFile("temp", ".wav");
        WavHeader.writeHeader(tmp);
        return tmp;
    }

    private static void fill(File f) throws FileNotFoundException {
        PrintWriter pw = new PrintWriter(new FileOutputStream(f));
        pw.print("123");
        pw.close();
    }
}
