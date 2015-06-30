package com.soundcloud.android;

import com.soundcloud.android.api.legacy.PublicApiWrapper;
import com.soundcloud.android.api.oauth.Token;
import dagger.ObjectGraph;

import android.content.Intent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestApplication extends SoundCloudApplication {
    public final Token token;
    public final List<Intent> broadcasts = new ArrayList<>();
    private PublicApiWrapper oldCloudApi;

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
        oldCloudApi = PublicApiWrapper.getInstance(this);
    }

    public PublicApiWrapper getCloudAPI() {
        return oldCloudApi;
    }

    @Override
    public void sendBroadcast(Intent intent) {
        broadcasts.add(intent);
        super.sendBroadcast(intent);
    }

    public static File createJpegFile() throws IOException {
        return File.createTempFile("temp", ".jpg");
    }
}
