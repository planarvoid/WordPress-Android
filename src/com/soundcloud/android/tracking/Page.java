package com.soundcloud.android.tracking;

import com.at.ATParams;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;

public enum Page {
    Entry_main("main", Level2.Entry),
    Entry_login__main("login::main", Level2.Entry),
    Entry_login__recover_password("login::recover_password", Level2.Entry),

    Entry_signup__main   ("signup::main", Level2.Entry),
    Entry_signup__details("signup::details", Level2.Entry),

    Entry_tour__main   ("tour::main",    Level2.Entry),
    Entry_tour__record ("tour::record",  Level2.Entry),
    Entry_tour__share  ("tour::share",   Level2.Entry),
    Entry_tour__people ("tour::people",  Level2.Entry),
    Entry_tour__comment("tour::comment", Level2.Entry),
    Entry_tour__done   ("tour::done",    Level2.Entry),

    Entry_signup__find_friends("signup::find_friends", Level2.Entry),
    Entry_confirm_your_email  ("confirm_your_email",   Level2.Entry),

    // stream tab
    Stream_main          ("main::user_permalink", Level2.Stream),
    Stream_stream_setting("stream_setting::user_permalink", Level2.Stream),

    // activity tab
    Activity_activity("main::user_permalink", Level2.Activity),

    // record tab
    Record_main   ("main::user_permalink", Level2.Record),
    Record_details("details::user_permalink", Level2.Record),

    // search
    Search_main("main", Level2.Search),
    Search_results__sounds__keyword("results::sounds::%s", Level2.Search),
    Search_results__people__keyword("results::people::%s", Level2.Search),

    // settings
    Settings_main         ("main",          Level2.Settings),
    Settings_account_sync ("account_sync",  Level2.Settings),
    Settings_notifications("notifications", Level2.Settings),
    Settings_change_log   ("change_log",    Level2.Settings),
    Settings_about        ("about",         Level2.Settings),

    // user browser
    Users_dedicated_rec("user_permalink::dedicated_rec", Level2.Users),
    Users_sounds       ("user_permalink::sounds",    Level2.Users),
    Users_likes        ("user_permalink::likes",     Level2.Users),
    Users_following    ("user_permalink::following", Level2.Users),
    Users_followers    ("user_permalink::followers", Level2.Users),
    Users_info         ("user_permalink::info",      Level2.Users),

    // user browser (you)
    You_find_friends("find_friends::user_permalink", Level2.You),
    You_sounds      ("sounds::user_permalink",    Level2.You),
    You_likes       ("likes::user_permalink",     Level2.You),
    You_following   ("following::user_permalink", Level2.You),
    You_followers   ("followers::user_permalink", Level2.You),
    You_info        ("info::user_permalink",      Level2.You),

    // player
    Sounds_main             ("user_permalink::track_permalink::main", Level2.Sounds),
    Sounds_add_comment      ("user_permalink::track_permalink::add_comment", Level2.Sounds),
    Sounds_share            ("user_permalink::track_permalink::share", Level2.Sounds),
    Sounds_info__main       ("user_permalink::track_permalink::info_main", Level2.Sounds),
    Sounds_info__people_like("user_permalink::track_permalink::info_people_like", Level2.Sounds),
    Sounds_info__comment    ("user_permalink::track_permalink::info_comment", Level2.Sounds),

    UNKNOWN(null, null);

    public final String name;
    public final Level2 level2;

    static final String user_permalink  = "user_permalink";
    static final String track_permalink = "track_permalink";

    Page(String name, Level2 level2) {
        this.name = name;
        this.level2 = level2;
    }

    public ATParams atParams(Object... args) {
        ATParams atp = new ATParams();
        if (level2 != null) atp.setLevel2(String.valueOf(level2.id));
        atp.setPage(expandPage(args));
        return atp;
    }

    /* package */ String expandPage(Object... args) {
        String result = name;
        for (Object o : args) {
            result = expandVariables(result, o);
        }
        return result;
    }

    static String expandVariables(String template, Object o) {
        if (o instanceof Track) {
            return expandVariables(template, (Track)o);
        } else if (o instanceof User) {
            return expandVariables(template, (User)o);
        } else {
            return String.format(template, o);
        }
    }

    private static String expandVariables(String template, Track track) {
        if (template.contains(track_permalink)) {
            template = template.replace(track_permalink, track.permalink);
        }
        if (track.user != null) {
            template = expandVariables(template, track.user);
        }
        return template;
    }

    private static String expandVariables(String template, User user) {
        if (template.contains(user_permalink) && user.permalink != null) {
            template = template.replace(user_permalink, user.permalink);
        }
        return template;
    }
}
