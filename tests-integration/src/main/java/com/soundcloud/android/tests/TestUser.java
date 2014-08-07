package com.soundcloud.android.tests;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.Token;

import android.content.Context;

import java.io.IOException;

public class TestUser {
    private final String username, email, password;

    private Token token;
    private PublicApiUser user;

    public static String generateEmail() {
        return "someemail-"+System.currentTimeMillis()+"@test.com";
    }

    public TestUser(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public String getUsername() {
        return this.username;
    }

    public String getEmail() {
        return this.email;
    }

    public String getPassword() {
        return this.password;
    }

    public boolean logIn(Context context) {
        ApiWrapper apiWrapper = AccountAssistant.createApiWrapper(context);
        try {
            Token token = getToken(apiWrapper);
            PublicApiUser user = getUser(apiWrapper);
            return SoundCloudApplication.fromContext(context).addUserAccountAndEnableSync(user, token, SignupVia.NONE);

        } catch (IOException e) {
            throw new AssertionError("error logging in: " + e.getMessage());
        }
    }

    protected PublicApiUser getUser(ApiWrapper apiWrapper) throws IOException {
        if (user == null){
            user = AccountAssistant.getLoggedInUser(apiWrapper);
        }
        return user;
    }

    protected Token getToken(ApiWrapper apiWrapper) throws IOException {
        if (token == null){
            token = AccountAssistant.getToken(apiWrapper, username, password);
        }
        return token;
    }

    public static final TestUser defaultUser    = new TestUser("android-testing",       "",                             "android-testing");
    public static final TestUser adUser         = new TestUser("scandroidad1",          "scandroidtestad1@gmail.com",   "scandtest");
    public static final TestUser scAccount      = new TestUser("Steven Testowy",        "soundcloudtestuser@gmail.com", "s0undcl0ud");
    public static final TestUser scTestAccount  = new TestUser("android-testing",       "",                             "android-testing");
    public static final TestUser noGPlusAccount = new TestUser("Steven Testowy",        "soundcloudtestuser@gmail.com", "s0undcl0ud");
    public static final TestUser GPlusAccount   = new TestUser("scandroidtest",         "sccloudandroid@gmail.com",     "s0undcl0ud");
    public static final TestUser Facebook       = new TestUser("Mike Smiechowy",        "ssmiechowy@gmail.com",         "passwordyeah3");
    public static final TestUser playlistUser   = new TestUser("mike-smiechowy",        "ssmiechowy@gmail.com",         "passwordyeah");
    public static final TestUser playerUser     = new TestUser("android-test-player",   "android-test-player@gmail.com","S0undCl0ud");
    public static final TestUser testUser       = new TestUser("Slawomir Smiechowy 2",  "test26-82@wp.pl",              "password");
    public static final TestUser emptyUser      = new TestUser("scEmpty",               "scemptyuser@gmail.com",        "s0undcl0ud");
    public static final TestUser followedUser   = new TestUser("android-followed",      "sctestfollowed@gmail.com",     "followed");
    public static final TestUser streamUser     = new TestUser("sofia-tester",          "scstreamuser@gmail.com",       "s0undcl0ud");
    public static final TestUser privateUser    = new TestUser("privateTrackUser",      "privatetrackuser@gmail.com",   "S0undCl0ud");
}
