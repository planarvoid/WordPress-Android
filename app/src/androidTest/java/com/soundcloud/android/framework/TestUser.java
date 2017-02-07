package com.soundcloud.android.framework;

import com.soundcloud.android.api.oauth.Token;

/* *********************************************************************************************************************

NOTE!

If you are adding a new test user, the user id will most likely need to be added to the SUBGENIE_EXEMPT_USERS list in
api-mobile to avoid hitting the device limit during test runs.

To do this, either reach out to someone working on api-mobile or create a PR for the appropriate change.

Also be sure to set the subgenieExempt field to the appropriate value if the used id has been made exempt.

********************************************************************************************************************* */

public class TestUser {
    private final String permalink, email, password;
    private final boolean subgenieExempt;
    Token token;

    public static String generateEmail() {
        return "someemail-" + System.currentTimeMillis() + "@tests.soundcloud";
    }

    public TestUser(String permalink,
                    String email,
                    String password,
                    String accessToken,
                    int id,
                    boolean subgenieExempt) {
        this.permalink = permalink;
        this.email = email;
        this.password = password;
        this.token = new Token(accessToken, null, Token.SCOPE_NON_EXPIRING);
        this.subgenieExempt = subgenieExempt;
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

    public static final TestUser defaultUser = new TestUser(
            "android-testing",
            "jan+android-testing@soundcloud.com",
            "android-testing",
            "1-21686-18173653-33b6f1f47717dd4",
            18173653,
            true);
    public static final TestUser scAccount = new TestUser(
            "steven-testowy",
            "soundcloudtestuser@gmail.com",
            "s0undcl0ud",
            "1-21686-41264271-0e4734601e0683b",
            41264271,
            true);
    public static final TestUser noGPlusAccount = new TestUser(
            "Steven Testowy",
            "soundcloudtestuser@gmail.com",
            "s0undcl0ud",
            "1-21686-41264271-abeff5a737dd38f",
            41264271,
            true);
    public static final TestUser GPlusAccount = new TestUser(
            "scandroidtest",
            "sccloudandroid@gmail.com",
            "s0undcl0ud",
            "1-21686-42283903-00deb8337ad2630",
            42283903,
            true);
    public static final TestUser Facebook = new TestUser(
            "Mike Smiechowy",
            "ssmiechowy@gmail.com",
            "passwordyeah3",
            "1-21686-50749473-34aed0136a61fda",
            50749473,
            true);
    public static final TestUser playlistUser = new TestUser(
            "mike-smiechowy",
            "ssmiechowy@gmail.com",
            "passwordyeah3",
            "1-21686-50749473-d1757f763ba4bf6",
            50749473,
            true);
    public static final TestUser playerUser = new TestUser(
            "android-test-player",
            "android.test.player22@gmail.com",
            "S0undCl0ud",
            "1-21686-107904111-2c69858c2bde2a",
            107904111,
            true);
    public static final TestUser deletePlaylistUser = new TestUser(
            "delete-playlist",
            "delete-playlist@gmail.com",
            "S0undCl0ud",
            "1-21686-190901728-67e61c3565bfc2",
            190901728,
            true);
    public static final TestUser testUser = new TestUser(
            "slawomir-smiechowy-2",
            "test26-82@wp.pl",
            "password",
            "1-21686-50670381-cb53f08e0d86252",
            50670381,
            true);
    public static final TestUser emptyUser = new TestUser(
            "scEmpty",
            "scemptyuser@gmail.com",
            "s0undcl0ud",
            "1-21686-67429938-aefdb8a213de5a6",
            67429938,
            true);
    public static final TestUser streamUser = new TestUser(
            "sofia-tester",
            "scstreamuser@gmail.com",
            "s0undcl0ud",
            "1-21686-102628335-431120cd3ef187",
            102628335,
            true);
    public static final TestUser privateUser = new TestUser(
            "privateTrackUser",
            "privatetracksuser@gmail.com",
            "S0undCl0ud",
            "1-21686-106946074-3efbea3a6a7612",
            106946074,
            true);
    public static final TestUser subscribeUser = new TestUser(
            "scandsubscribe",
            "scandsubscribe@gmail.com",
            "s0undcl0ud",
            "1-21686-122411702-cccdf2735af1f8",
            122411702,
            true);
    public static final TestUser likesUser = new TestUser(
            "sctestlike",
            "soundcloudtestlike@gmail.com",
            "passwordyeah77",
            "1-21686-135116976-bec8398cf46615",
            135116976,
            true);
    public static final TestUser likesActionUser = new TestUser(
            "scLikesActionUser",
            "sclikesactionuser@gmail.com",
            "s0undcl0ud",
            "1-21686-149021931-eca5c50b2dbb28",
            149021931,
            true);
    public static final TestUser engagementsUser = new TestUser(
            "engagementsUser",
            "soundcloud.Android.engagementsUser@tests.soundcloud",
            "o89sSQItSUR4sak",
            "1-21686-287403532-e27b6d761563ae",
            287403532,
            true);
    public static final TestUser offlineUser = new TestUser(
            "sctestoffline",
            "sctestoffline@gmail.com",
            "som3aw3som3n3wpassword",
            "1-21686-136770909-bd6c319b73449b",
            136770909,
            true);
    public static final TestUser offlineUserMT = new TestUser(
            "sctestoffline-mt",
            "soundcloud.Android.sctestoffline-mt@tests.soundcloud",
            "SoundCloudTest2016",
            "1-8742-286124072-e9a876583339bf8",
            286124072,
            true);
    public static final TestUser upsellUser = new TestUser(
            "sctestupsell",
            "sctestupsell@gmail.com",
            "passwordyeah88",
            "1-21686-147986827-d5b5d968d6bee3",
            147986827,
            true);
    public static final TestUser offlineEmptyUser = new TestUser(
            "sctestoffline_empty",
            "sctestoffline_empty@gmail.com",
            "passwordyeah88",
            "1-21686-140533558-75da2202affa18",
            140533558,
            true);
    public static final TestUser over21user = new TestUser(
            "over21userblah",
            "over21user@soundcloud.com",
            "#s0undcl0ud",
            "1-21686-149060192-ce66fbe75d9fd3",
            149060192,
            true);
    public static final TestUser childUser = new TestUser(
            "childuserblah",
            "childuserblah@soundcloud.com",
            "passwordyeah88",
            "1-21686-150380114-b3af250571e838",
            150380114,
            true); // 13 years in 2015
    public static final TestUser playlistLikesUser = new TestUser(
            "playlist-likes-user",
            "playlist-likes-user@soundcloud.com",
            "passwordyeah",
            "1-21686-151205360-cbe46d4f5819e0",
            151205360,
            true);
    public static final TestUser addToPlaylistUser = new TestUser(
            "onePlaylistUser",
            "onePlaylistuser@gmail.com",
            "passwordyeah88",
            "1-21686-151356674-b019d504fbb087",
            151356674,
            true);
    public static final TestUser profileEntryUser = new TestUser(
            "sc-profile-entry-user",
            "sc-profile-entry-user@gmail.com",
            "passwordyeah",
            "1-21686-151499536-b8eff81a39117e",
            151499536,
            true);
    public static final TestUser recordUser = new TestUser(
            "sctestrecord",
            "sctestrecord@gmail.com",
            "passwordyeah88",
            "1-21686-147979595-a38022f8d6e0c0",
            147979595,
            true);
    public static final TestUser profileUser = new TestUser(
            "android-profile-user",
            "sc-android-profile-user@soundcloud.com",
            "s0undcl0ud",
            "1-21686-156343632-5069edfa24dfc8",
            156343632,
            true);
    public static final TestUser otherProfileUser = new TestUser(
            "other-profile-user",
            "other-profile-user@soundcloud.com",
            "s0undcl0ud",
            "1-21686-159443075-5b17c15565f269",
            159443075,
            true);
    public static final TestUser stationsUser = new TestUser(
            "evilstations",
            "stations+test@soundcloud.com",
            "stations123",
            "1-21686-161646357-100b22e7b6ccd2",
            161646357,
            true);
    public static final TestUser collectionUser = new TestUser(
            "evilcollections",
            "collections+test@soundcloud.com",
            "collections123",
            "1-21686-173627179-fc9f4c61bcba8b",
            173627179,
            true);
    public static final TestUser freeNonMonetizedUser = new TestUser(
            "unmonetizeable",
            "sc.ht.android.nonmonetizd.free@gmail.com",
            "s0undcl0ud_HT2016",
            "1-21686-190276054-5b745361a448ac",
            190276054,
            true);
    public static final TestUser playlistExplosionUser = new TestUser(
            "user-986959733-404206267",
            "androidtest+playlist+explode@soundcloud.com",
            "s0undcl0ud",
            "1-21686-196444133-fbc606e15e1b6c",
            196444133,
            true);
    public static final TestUser followingOneTrackOnePlaylistUser = new TestUser(
            "user-329253335",
            "followingOneTrackOnePlaylistUser@gmail.com",
            "passwordForTheTrackPlaylistFollowerUser",
            "1-21686-204010094-472bb0a263263f",
            204010094,
            true);
    public static final TestUser chartsTestUser = new TestUser(
            "ChartsTests",
            "soundcloud.Android.ChartsTests@tests.soundcloud",
            "SoundCloudTest2016",
            "1-21686-248182220-43bc24e62ef77d",
            248182220,
            false);

    // not used directly in a test, but user info is kept here for documentation
    public static final TestUser adOwnerUser = new TestUser(
            "scandroidad1",
            "scandroidtestad1@gmail.com",
            "scandtest",
            "",
            107640680,
            true);

    public static final TestUser adTestUser = new TestUser(
            "android_ad_user",
            "guillaume+android+ad+user@soundcloud.com",
            "android_ad_user",
            "1-21686-276289453-03b99d74abe1f2",
            276289453,
            true);

    public static final TestUser androidTestUser = new TestUser(
            "andtestpl",
            "sc.test.user.pl1@gmail.com",
            "addtest",
            "",
            66116218,
            true);
    public static final TestUser androidTestTrackMaker = new TestUser(
            "ScAndTrackMaker",
            "ScAndTrackMaker@gmail.com",
            "passwordyeah88",
            "",
            200690360,
            true);
    public static final TestUser oneTrackOnePlaylistUser = new TestUser(
            "oneTrackOnePlaylistUser",
            "onetrackoneplaylistuser@gmail.com",
            "passwordForTheTrackPlaylistUser",
            "1-21686-204008734-7419586a6a5b2e",
            204008734,
            true);
    public static final TestUser htCreator = new TestUser(
            "ht-creator",
            "sc.ht.android.creator@gmail.com",
            "s0undcl0ud_HT2016",
            "1-21686-190502894-422edfa8ddf43c",
            190502894,
            true);
    public static final TestUser profileTestUser = new TestUser(
            "super-cute-hyper-profile",
            "creators-team+new-profile@soundcloud.com",
            "creators-team",
            "1-21686-218682740-522313271be9c6",
            218682740,
            true);
    public static final TestUser goTestUser = new TestUser(
            "scandroidtestgo",
            "soundcloud.Android.ScAndroidTestGo@tests.soundcloud",
            "SoundCloudTest2016",
            "1-21686-273344775-9b75d0c91573f2",
            273344775,
            false);
    public static final TestUser autocompleteTestUser = new TestUser(
            "user-17994038",
            "discovery-android+test1231231231@soundcloud.com",
            "youdontwantnoproblemwantoproblemwithme",
            "1-21686-285710974-d3a65d5c99a130",
            285710974,
            false);
}
