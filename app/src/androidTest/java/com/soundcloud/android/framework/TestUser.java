package com.soundcloud.android.framework;

import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.oauth.Token;

import android.content.Context;

import java.io.IOException;

public class TestUser {
    private final String permalink, email, password;

    private Token token;
    private PublicApiUser user;

    public static String generateEmail() {
        return "someemail-" + System.currentTimeMillis() + "@tests.soundcloud";
    }

    public TestUser(String permalink, String email, String password) {
        this.permalink = permalink;
        this.email = email;
        this.password = password;
    }

    public TestUser(String permalink, String email, String password, String accessToken) {
        this.permalink = permalink;
        this.email = email;
        this.password = password;
        this.token = new Token(accessToken, null, Token.SCOPE_NON_EXPIRING);
    }

    public TestUser(String permalink, String email, String password, String accessToken, int id) {
        this.permalink = permalink;
        this.email = email;
        this.password = password;
        this.token = new Token(accessToken, null, Token.SCOPE_NON_EXPIRING);
        this.user = new PublicApiUser(id, permalink);
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
        PublicApi apiWrapper = AccountAssistant.createApiWrapper(context);
        int maxRetries = 3;
        int tryCount = 0;
        boolean result = false;
        boolean shouldRetry = true;
        while (shouldRetry) {
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
                if (!result) {
                    Thread.sleep(5000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    protected PublicApiUser getUser(PublicApi apiWrapper) throws IOException {
        if (user == null) {
            user = AccountAssistant.getLoggedInUser(apiWrapper);
        }
        return user;
    }

    protected Token getToken(Context context, PublicApi apiWrapper) throws IOException {
        if (token == null) {
            token = apiWrapper.login(email, password);
        }
        AccountAssistant.setToken(context, token);
        return token;
    }

    public static final TestUser defaultUser = new TestUser(
            "android-testing",
            "jan+android-testing@soundcloud.com",
            "android-testing",
            "1-21686-18173653-33b6f1f47717dd4",
            18173653);
    public static final TestUser scAccount = new TestUser(
            "steven-testowy",
            "soundcloudtestuser@gmail.com",
            "s0undcl0ud",
            "1-21686-41264271-0e4734601e0683b",
            41264271);
    public static final TestUser noGPlusAccount = new TestUser(
            "Steven Testowy",
            "soundcloudtestuser@gmail.com",
            "s0undcl0ud",
            "1-21686-41264271-abeff5a737dd38f",
            41264271);
    public static final TestUser GPlusAccount = new TestUser(
            "scandroidtest",
            "sccloudandroid@gmail.com",
            "s0undcl0ud",
            "1-21686-42283903-00deb8337ad2630",
            42283903);
    public static final TestUser Facebook = new TestUser(
            "Mike Smiechowy",
            "ssmiechowy@gmail.com",
            "passwordyeah3",
            "1-21686-50749473-34aed0136a61fda",
            50749473);
    public static final TestUser playlistUser = new TestUser(
            "mike-smiechowy",
            "ssmiechowy@gmail.com",
            "passwordyeah3",
            "1-21686-50749473-d1757f763ba4bf6",
            50749473);
    public static final TestUser playerUser = new TestUser(
            "android-test-player",
            "android.test.player22@gmail.com",
            "S0undCl0ud",
            "1-21686-107904111-2c69858c2bde2a",
            107904111);
    public static final TestUser deletePlaylistUser = new TestUser(
            "delete-playlist",
            "delete-playlist@gmail.com",
            "S0undCl0ud",
            "1-21686-190901728-67e61c3565bfc2",
            190901728);
    public static final TestUser testUser = new TestUser(
            "slawomir-smiechowy-2",
            "test26-82@wp.pl",
            "password",
            "1-21686-50670381-cb53f08e0d86252",
            50670381);
    public static final TestUser emptyUser = new TestUser(
            "scEmpty",
            "scemptyuser@gmail.com",
            "s0undcl0ud",
            "1-21686-67429938-aefdb8a213de5a6",
            67429938);
    public static final TestUser streamUser = new TestUser(
            "sofia-tester",
            "scstreamuser@gmail.com",
            "s0undcl0ud",
            "1-21686-102628335-431120cd3ef187",
            102628335);
    public static final TestUser privateUser = new TestUser(
            "privateTrackUser",
            "privatetracksuser@gmail.com",
            "S0undCl0ud",
            "1-21686-106946074-3efbea3a6a7612",
            106946074);
    public static final TestUser subscribeUser = new TestUser(
            "scandsubscribe",
            "scandsubscribe@gmail.com",
            "s0undcl0ud",
            "1-21686-122411702-cccdf2735af1f8",
            122411702);
    public static final TestUser likesUser = new TestUser(
            "sctestlike",
            "soundcloudtestlike@gmail.com",
            "passwordyeah77",
            "1-21686-135116976-bec8398cf46615",
            135116976);
    public static final TestUser likesActionUser = new TestUser(
            "scLikesActionUser",
            "sclikesactionuser@gmail.com",
            "s0undcl0ud",
            "1-21686-149021931-eca5c50b2dbb28",
            149021931);
    public static final TestUser offlineUser = new TestUser(
            "sctestoffline",
            "sctestoffline@gmail.com",
            "passwordyeah88",
            "1-21686-136770909-d1e6cfc089fb1f",
            136770909);
    public static final TestUser upsellUser = new TestUser(
            "sctestupsell",
            "sctestupsell@gmail.com",
            "passwordyeah88",
            "1-21686-147986827-d5b5d968d6bee3",
            147986827);
    public static final TestUser offlineEmptyUser = new TestUser(
            "sctestoffline_empty",
            "sctestoffline_empty@gmail.com",
            "passwordyeah88",
            "1-21686-140533558-75da2202affa18",
            140533558);
    public static final TestUser over21user = new TestUser(
            "over21userblah",
            "over21user@soundcloud.com",
            "#s0undcl0ud",
            "1-21686-149060192-ce66fbe75d9fd3",
            149060192);
    public static final TestUser childUser = new TestUser(
            "childuserblah",
            "childuserblah@soundcloud.com",
            "passwordyeah88",
            "1-21686-150380114-b3af250571e838",
            150380114); // 13 years in 2015
    public static final TestUser playlistLikesUser = new TestUser(
            "playlist-likes-user",
            "playlist-likes-user@soundcloud.com",
            "passwordyeah",
            "1-21686-151205360-cbe46d4f5819e0",
            151205360);
    public static final TestUser addToPlaylistUser = new TestUser(
            "onePlaylistUser",
            "onePlaylistuser@gmail.com",
            "passwordyeah88",
            "1-21686-151356674-b019d504fbb087",
            151356674);
    public static final TestUser profileEntryUser = new TestUser(
            "sc-profile-entry-user",
            "sc-profile-entry-user@gmail.com",
            "passwordyeah",
            "1-21686-151499536-b8eff81a39117e",
            151499536);
    public static final TestUser recordUser = new TestUser(
            "sctestrecord",
            "sctestrecord@gmail.com",
            "passwordyeah88",
            "1-21686-147979595-a38022f8d6e0c0",
            147979595);
    public static final TestUser profileUser = new TestUser(
            "android-profile-user",
            "sc-android-profile-user@soundcloud.com",
            "s0undcl0ud",
            "1-21686-156343632-5069edfa24dfc8",
            156343632);
    public static final TestUser otherProfileUser = new TestUser(
            "other-profile-user",
            "other-profile-user@soundcloud.com",
            "s0undcl0ud",
            "1-21686-159443075-5b17c15565f269",
            159443075);
    public static final TestUser stationsUser = new TestUser(
            "evilstations",
            "stations+test@soundcloud.com",
            "stations123",
            "1-21686-161646357-100b22e7b6ccd2",
            161646357);
    public static final TestUser collectionsUser = new TestUser(
            "evilcollections",
            "collections+test@soundcloud.com",
            "collections123",
            "1-21686-173627179-fc9f4c61bcba8b",
            173627179);
    public static final TestUser freeNonMonetizedUser = new TestUser(
            "unmonetizeable",
            "sc.ht.android.nonmonetizd.free@gmail.com",
            "s0undcl0ud_HT2016",
            "1-21686-190276054-5b745361a448ac",
            190276054);
    public static final TestUser freeMonetizedUser = new TestUser(
            "Monetizeable",
            "sc.ht.android.monetized.free@gmail.com",
            "s0undcl0ud_HT2016",
            "1-21686-190275390-0cf52de1bed061",
            190275390);

    // not used directly in a test, but user info is kept here for documentation
    public static final TestUser adUser = new TestUser("scandroidad1", "scandroidtestad1@gmail.com", "scandtest", "");
    public static final TestUser androidTestUser = new TestUser("andtestpl", "sc.test.user.pl1@gmail.com", "addtest", "");
}
