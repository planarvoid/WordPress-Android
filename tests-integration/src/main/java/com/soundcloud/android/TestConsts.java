package com.soundcloud.android;

import android.net.Uri;

public final class TestConsts {
    private TestConsts() {}


    public static final Uri CHE_FLUTE_URI = Uri.parse("http://soundcloud.com/steveangello/steve-angello-che-flute");
    public static final Uri CHE_FLUTE_M_URI = Uri.parse("http://m.soundcloud.com/steveangello/steve-angello-che-flute");
    public static final Uri CHE_FLUTE_SC_URI = Uri.parse("soundcloud:tracks:274334");

    public static final Uri FORSS_SET_URI = Uri.parse("http://soundcloud.com/forss/sets/ecclesia-inspiration");
    public static final Uri FORSS_SET_M_URI = Uri.parse("http://m.soundcloud.com/forss/sets/ecclesia-inspiration");
    public static final Uri FORSS_SET_SC_URI = Uri.parse("soundcloud:playlists:2050462");


    public static final Uri STEVE_ANGELLO_URI = Uri.parse("http://soundcloud.com/steveangello");
    public static final Uri STEVE_ANGELLO_SC_URI = Uri.parse("soundcloud:users:118312");

    public static final Uri UNRESOLVABLE_SC_TRACK_URI = Uri.parse("soundcloud:tracks:99999999999");
    public static final Uri UNRESOLVABLE_SC_USER_URI = Uri.parse("soundcloud:users:99999999999");

    public static final Uri JOBS_PAGE = Uri.parse("http://soundcloud.com/jobs");
}
