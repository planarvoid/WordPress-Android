package com.soundcloud.android;

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
    public static final Uri FORSS_PLAYLIST_URI = Uri.parse("http://soundcloud.com/forss/playlists/ecclesia-inspiration");
    public static final Uri FORSS_PLAYLIST_M_URI = Uri.parse("http://m.soundcloud.com/forss/playlists/ecclesia-inspiration");
    public static final Uri FORSS_PLAYLIST_SC_URI = Uri.parse("soundcloud:playlists:2050462");

    public static final Uri FACEBOOK_SOUND_URI = Uri.parse("https://soundcloud.com/soundcloud/5-years?utm_source=soundcloud&utm_campaign=share&utm_medium=facebook");
    public static final Uri FACEBOOK_USER_URI = Uri.parse("https://soundcloud.com/steveangello?utm_source=soundcloud&utm_campaign=share&utm_medium=facebook");
    public static final Uri FACEBOOK_SOUND_DEEP_LINK = Uri.parse("soundcloud://sounds:274334/?target_url=https://soundcloud.com/manchesterorchestra/cope?utm_source=soundcloud&utm_campaign=share&utm_medium=facebook");

    public static final Uri STEVE_ANGELLO_URI = Uri.parse("http://soundcloud.com/steveangello");
    public static final Uri STEVE_ANGELLO_SC_URI = Uri.parse("soundcloud:users:118312");

    public static final Uri UNRESOLVABLE_SC_TRACK_URI = Uri.parse("soundcloud:tracks:99999999999");
    public static final Uri UNRESOLVABLE_SC_USER_URI = Uri.parse("soundcloud:users:99999999999");

    public static final Uri JOBS_PAGE = Uri.parse("http://soundcloud.com/jobs");

    public static final Uri BROKEN_LINK = Uri.parse("soundcloud:ounds:274334> Track</a></td>");
}
