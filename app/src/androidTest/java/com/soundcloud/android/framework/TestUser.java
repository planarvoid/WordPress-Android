package com.soundcloud.android.framework;

import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.api.ApiWrapper;

import android.content.Context;

import java.io.IOException;

public class TestUser {
    private final String permalink, email, password;

    private Token token;
    private PublicApiUser user;

    public static String generateEmail() {
        return "someemail-"+System.currentTimeMillis()+"@tests.soundcloud";
    }

    public TestUser(String permalink, String email, String password) {
        this.permalink = permalink;
        this.email = email;
        this.password = password;
    }

    public String getPermalink() {
        return this.permalink;
    }

    public String getEmail() {
        return this.email;
    }

    public String getPassword() {
        return this.password;
    }

    public boolean logIn(Context context) {
        ApiWrapper apiWrapper = AccountAssistant.createApiWrapper(context);
        int maxRetries = 3;
        int tryCount = 0;
        boolean result = false;
        boolean shouldRetry = true;
        while(shouldRetry){
            try {
                tryCount++;
                result = AccountAssistant.addAccountAndEnableSync(context, getToken(context, apiWrapper), getUser(apiWrapper));
            } catch (IOException e) {

                if (tryCount > maxRetries) {
                    throw new AssertionError("error logging in: " + e.getMessage());
                }
            }
            try {
                shouldRetry = (result == false);
                if(!result) {
                    Thread.sleep(5000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    protected PublicApiUser getUser(ApiWrapper apiWrapper) throws IOException {
        if (user == null){
            user = AccountAssistant.getLoggedInUser(apiWrapper);
        }
        return user;
    }

    protected Token getToken(Context context, ApiWrapper apiWrapper) throws IOException {
        if (token == null){
            token = AccountAssistant.getToken(context, apiWrapper, email, password);
        }
        return token;
    }

    public static final TestUser defaultUser        = new TestUser("android-testing",       "jan+android-testing@soundcloud.com",   "android-testing");
    public static final TestUser scAccount          = new TestUser("steven-testowy",        "soundcloudtestuser@gmail.com",         "s0undcl0ud");
    public static final TestUser noGPlusAccount     = new TestUser("Steven Testowy",        "soundcloudtestuser@gmail.com",         "s0undcl0ud");
    public static final TestUser GPlusAccount       = new TestUser("scandroidtest",         "sccloudandroid@gmail.com",             "s0undcl0ud");
    public static final TestUser Facebook           = new TestUser("Mike Smiechowy",        "ssmiechowy@gmail.com",                 "passwordyeah3");
    public static final TestUser playlistUser       = new TestUser("mike-smiechowy",        "ssmiechowy@gmail.com",                 "passwordyeah");
    public static final TestUser playerUser         = new TestUser("android-test-player",   "android.test.player22@gmail.com",      "S0undCl0ud");
    public static final TestUser testUser           = new TestUser("slawomir-smiechowy-2",  "test26-82@wp.pl",                      "password");
    public static final TestUser emptyUser          = new TestUser("scEmpty",               "scemptyuser@gmail.com",                "s0undcl0ud");
    public static final TestUser followedUser       = new TestUser("android-followed",      "sctestfollowed@gmail.com",             "followed");
    public static final TestUser streamUser         = new TestUser("sofia-tester",          "scstreamuser@gmail.com",               "s0undcl0ud");
    public static final TestUser privateUser        = new TestUser("privateTrackUser",      "privatetracksuser@gmail.com",           "S0undCl0ud");
    public static final TestUser subscribeUser      = new TestUser("scandsubscribe",        "scandsubscribe@gmail.com",             "s0undcl0ud");
    public static final TestUser likesUser          = new TestUser("sctestlike",            "soundcloudtestlike@gmail.com",         "passwordyeah77");
    public static final TestUser likesActionUser    = new TestUser("scLikesActionUser",     "sclikesactionuser@gmail.com",          "s0undcl0ud");
    public static final TestUser offlineUser        = new TestUser("sctestoffline",         "sctestoffline@gmail.com",              "passwordyeah88");
    public static final TestUser offlineUpsellUser  = new TestUser("sctestupsell",          "sctestupsell@gmail.com",               "passwordyeah88");
    public static final TestUser offlineEmptyUser   = new TestUser("sctestoffline_empty",   "sctestoffline_empty@gmail.com",        "passwordyeah88");
    public static final TestUser over21user         = new TestUser("over21userblah",        "over21user@soundcloud.com",            "#s0undcl0ud");
    public static final TestUser childUser          = new TestUser("childuserblah",         "childuserblah@soundcloud.com",         "passwordyeah88"); // 13 years in 2015
    public static final TestUser playlistLikesUser  = new TestUser("playlist-likes-user",   "playlist-likes-user@soundcloud.com",   "passwordyeah");
    public static final TestUser addToPlaylistUser  = new TestUser("onePlaylistUser",       "onePlaylistuser@gmail.com",            "passwordyeah88");
    public static final TestUser profileEntryUser   = new TestUser("sc-profile-entry-user", "sc-profile-entry-user@gmail.com",      "passwordyeah");
    public static final TestUser recordUser         = new TestUser("sctestrecord",          "sctestrecord@gmail.com",               "passwordyeah88");
    public static final TestUser profileUser        = new TestUser("android-profile-user",  "sc-android-profile-user@soundcloud.com","s0undcl0ud");
    public static final TestUser otherProfileUser   = new TestUser("other-profile-user",    "other-profile-user@soundcloud.com",    "s0undcl0ud");

    // not used directly in a test, but user info is kept here for documentation
    public static final TestUser adUser             = new TestUser("scandroidad1",          "scandroidtestad1@gmail.com",           "scandtest");
    public static final TestUser androidTestUser    = new TestUser("andtestpl",             "sc.test.user.pl1@gmail.com",           "addtest");
}
