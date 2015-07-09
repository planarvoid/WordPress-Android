package com.soundcloud.android.tests;

import com.soundcloud.android.framework.TestUser;

import android.net.Uri;

public final class TestConsts {
    private TestConsts() {}


    public static final Uri CHE_FLUTE_URI = Uri.parse("http://soundcloud.com/steveangello/steve-angello-che-flute");
    public static final Uri CHE_FLUTE_M_URI = Uri.parse("http://m.soundcloud.com/steveangello/steve-angello-che-flute");
    public static final Uri CHE_FLUTE_SC_URI = Uri.parse("soundcloud:tracks:274334");
    public static final Uri FORSS_SOUND_URI = Uri.parse("soundcloud:sounds:274334");

    public static final Uri TWITTER_SOUND_URI = Uri.parse("soundcloud://sounds:119017847/");

    public static final Uri FORSS_SET_URI = Uri.parse("http://soundcloud.com/forss/sets/ecclesia-inspiration");
    public static final Uri FORSS_SET_M_URI = Uri.parse("http://m.soundcloud.com/forss/sets/ecclesia-inspiration");
    public static final Uri FORSS_PLAYLIST_SC_URI = Uri.parse("soundcloud:playlists:2050462");

    public static final Uri FACEBOOK_SOUND_URI = Uri.parse("http://soundcloud.com/steveangello/steve-angello-che-flute?utm_source=soundcloud&utm_campaign=share&utm_medium=facebook");
    public static final Uri FACEBOOK_USER_URI = Uri.parse("https://soundcloud.com/steveangello?utm_source=soundcloud&utm_campaign=share&utm_medium=facebook");
    public static final Uri FACEBOOK_SOUND_DEEP_LINK = Uri.parse("soundcloud://sounds:274334/?target_url=https://soundcloud.com/manchesterorchestra/cope?utm_source=soundcloud&utm_campaign=share&utm_medium=facebook");

    public static final Uri STEVE_ANGELLO_URI = Uri.parse("http://soundcloud.com/steveangello");
    public static final Uri STEVE_ANGELLO_SC_URI = Uri.parse("soundcloud:users:118312");

    public static final Uri UNRESOLVABLE_SC_TRACK_URI = Uri.parse("soundcloud:tracks:99999999999");
    public static final Uri UNRESOLVABLE_SC_USER_URI = Uri.parse("soundcloud:users:99999999999");

    public static final Uri JOBS_PAGE = Uri.parse("https://soundcloud.com/jobs");
    public static final Uri RESET_PASSWORD_LINK_WITH_TRACKING = Uri.parse("http://soundcloud.com/-/t/click/postman-email-account_lifecycle-password_reset_request?url=http%3A%2F%2Fsoundcloud.com%2Flogin%2Freset%2F123456789abcdef1234567");

    public static final Uri BROKEN_LINK = Uri.parse("soundcloud:ounds:274334> Track</a></td>");

    public static final Uri AUDIO_AD_AND_LEAVE_BEHIND_PLAYLIST_URI = Uri.parse("https://soundcloud.com/scandroidad1/sets/monetizable-playlist");
    public static final Uri INTERSTITIAL_PLAYLIST_URI = Uri.parse("https://soundcloud.com/scandroidad1/sets/monetizable2");

    public static final Uri HOME_URI = Uri.parse("soundcloud://home");

    public static final Uri OTHER_PROFILE_USER_URI = Uri.parse("https://soundcloud.com/"+ TestUser.otherProfileUser.getPermalink());
}
