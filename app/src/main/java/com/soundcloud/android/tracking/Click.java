package com.soundcloud.android.tracking;

import com.at.ATParams;

/**
 * See
 * <a href="https://docs.google.com/a/soundcloud.com/spreadsheet/ccc?key=0AkJmFQ2aH2kTdHM2MXQzMjZ4blNHT00wNl9vMFMxNmc#gid=1">
 * master document</a> for more information.
 */
public enum Click implements Event {

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

    // New recording keys
    Record_Main_Record_Start       ("record_main",  "record_start::%s",   Type.action, Level2.Record),
    Record_Main_Record_Pause       ("record_main",  "record_pause::%s",   Type.action, Level2.Record),
    Record_Pause_Record_More       ("record_pause", "record_more",        Type.action, Level2.Record),
    Record_Pause_Play              ("record_pause", "play",               Type.action, Level2.Record),
    Record_Pause_Delete            ("record_pause", "delete::%s",         Type.action, Level2.Record),
    Record_Pause_Publish           ("record_pause", "publish::%s::%s",    Type.action, Level2.Record),
    Record_Edit_Play               ("record_edit",  "play",               Type.action, Level2.Record),
    Record_Edit_Interact           ("record_edit",  "interact",           Type.action, Level2.Record),
    Record_Edit_Apply              ("record_edit",  "apply",              Type.action, Level2.Record),
    Record_Edit_Revert_To_Original ("record_edit",  "revert_to_original", Type.action, Level2.Record),
    Record_Edit_Fade_In_Interact   ("record_edit",  "fade_in_interact",   Type.action, Level2.Record),
    Record_Share_Post              ("record_share", "post::%s::%s::%s",   Type.action, Level2.Record),
    Record_Share_Record_Another    ("record_share", "record_another",     Type.action, Level2.Record),

    Follow  ("Follow",   "level2::user_permalink", Type.action, null /* read from args */),
    Unfollow("Unfollow", "level2::user_permalink", Type.action, null /* read from args */),

    Log_out_log_out   ("Log_out",     "log_out",    Type.action, Level2.Settings),
    Log_out_box_ok    ("Log_out_box", "ok",         Type.action, Level2.Settings),
    Log_out_box_cancel("Log_out_box", "cancel",     Type.action, Level2.Settings),

    Like      ("Like",       "user_permalink::track_permalink", Type.action, Level2.Sounds),
    Repost    ("Repost",     "user_permalink::track_permalink", Type.action, Level2.Sounds),
    Comment   ("Comment",    "user_permalink::track_permalink", Type.action, Level2.Sounds),
    Share_main("Share_main", "user_permalink::track_permalink", Type.action, Level2.Sounds),
    /* NOT USED */ Share_fb  ("Share_fb",   "user_permalink::track_permalink", Type.action, Level2.Sounds),

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
        atp.xt_click(getLevel2Id(args), expandClick(args), type.atType);
        return atp;
    }

    private String getLevel2Id(Object... args) {
        Level2 l2 = level2;
        if (l2 == null && args != null) {
            for (Object arg : args) {
                if (arg instanceof Level2) {
                    l2 = (Level2) arg;
                    break;
                }
            }
        }
        return l2 == null ? null : String.valueOf(l2.id);
    }

    @Override
    public Level2 level2() {
        return level2;
    }

    /* package */ String expandClick(Object... args) {
        String result = chapter+"::"+name;
        return Page.expandVariables(result, args);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+":"+super.toString();
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
