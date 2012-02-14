package com.soundcloud.android.tracking;

import com.at.ATParams;

/**
 * See
 * <a href="https://docs.google.com/a/soundcloud.com/spreadsheet/ccc?key=0AkJmFQ2aH2kTdHM2MXQzMjZ4blNHT00wNl9vMFMxNmc#gid=1">
 * master document</a> for more information.
 */
public enum Click {

    Login_with_facebook("Login_main", "Login_with_facebook", Type.action,     Level2.Entry),
    Login              ("Login_main", "Login",               Type.navigation, Level2.Entry),
    Login_Login_done   ("Login",      "Login_done",          Type.navigation, Level2.Entry),

    Signup_Signup             ("Signup", "Signup",              Type.navigation, Level2.Entry),
    Signup_Signup_terms       ("Signup", "Signup_terms",        Type.navigation, Level2.Entry),
    Signup_Signup_done        ("Signup", "Signup_done",         Type.navigation, Level2.Entry),
    Signup_Signup_details_next("Signup", "Signup_details_next", Type.navigation, Level2.Entry),
    Signup_Signup_details_skip("Signup", "Signup_details_skip", Type.navigation, Level2.Entry),

    Tour_Tour_skip("Tour", "Tour_skip", Type.navigation, Level2.Entry),
    Tour_Tour_done("Tour", "Tour_done", Type.navigation, Level2.Entry),

    Stream_main_stream_setting      ("Stream_main", "stream_setting",        Type.action, Level2.Stream),
    Stream_box_stream_all_tracks    ("Stream_box",  "stream_all_tracks",     Type.action, Level2.Stream),
    Stream_box_stream_only_Exclusive("Stream_box",  "stream_only_exclusive", Type.action, Level2.Stream),
    Stream_box_stream_cancel        ("Stream_box",  "stream_cancel",         Type.action, Level2.Stream),

    Record_rec           ("Record", "rec",            Type.action, Level2.Record),
    Record_rec_stop      ("Record", "rec_stop",       Type.action, Level2.Record),
    Record_discard       ("Record", "discard",        Type.action, Level2.Record),
    Record_discard__ok   ("Record", "discard::ok",    Type.action, Level2.Record),
    Record_discard_cancel("Record", "discard:cancel", Type.action, Level2.Record),
    Record_play          ("Record", "play",           Type.action, Level2.Record),
    Record_play_stop     ("Record", "play_stop",      Type.action, Level2.Record),
    Record_next          ("Record", "next",           Type.navigation, Level2.Record),

    Record_details_add_image       ("Record_details", "add_image",        Type.action,     Level2.Record),
    Record_details_new_image       ("Record_details", "new_image",        Type.action,     Level2.Record),
    Record_details_existing_image  ("Record_details", "existing_image",   Type.action,     Level2.Record),
    Record_details_record_another  ("Record_details", "record_another",   Type.navigation, Level2.Record),
    Record_details_Upload_and_share("Record_details", "Upload_and_share", Type.navigation, Level2.Record),

    /* NOT USED */ Follow_page_url  ("Follow",   "page_url", Type.action, null),
    /* NOT USED */ Unfollow_page_url("Unfollow", "page_url", Type.action, null),

    Log_out_log_out   ("Log_out",     "log_out",    Type.action, Level2.Settings),
    Log_out_box_ok    ("Log_out_box", "ok",         Type.action, Level2.Settings),
    Log_out_box_cancel("Log_out_box", "cancel",     Type.action, Level2.Settings),

    Like      ("Like",       "user_permalink::track_permalink", Type.action, Level2.Sounds),
    Comment   ("Comment",    "user_permalink::track_permalink", Type.action, Level2.Sounds),
    Share_main("Share_main", "user_permalink::track_permalink", Type.action, Level2.Sounds),
    Share_fb  ("Share_fb",   "user_permalink::track_permalink", Type.action, Level2.Sounds),

    UNKNOWN(null, null, null, null);


    public final String chapter, name;
    public final Type type;
    public final Level2 level2;

    Click(String chapter, String name, Type type, Level2 level2) {
        this.chapter = chapter;
        this.name = name;
        this.type = type;
        this.level2 = level2;
    }

    public ATParams atParams(Object... args) {
        ATParams atp = new ATParams();
        atp.xt_click(level2 == null ? null : String.valueOf(level2.id), expandClick(args), type.atType);
        return atp;
    }

    /* package */ String expandClick(Object... args) {
        String result = chapter+"::"+name;
        for (Object o : args) {
            result = Page.expandVariables(result, o);
        }
        return result;
    }

    enum Type {
        action(ATParams.clicType.action),
        exitPage(ATParams.clicType.exitPage),
        download(ATParams.clicType.download),
        navigation(ATParams.clicType.navigation);

        public final ATParams.clicType atType;

        Type(ATParams.clicType type) {
            this.atType = type;
        }
    }
}
