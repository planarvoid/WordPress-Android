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
    NEW_Record_start                    ("Record", "Record_start::%s",                      Type.action,     Level2.Record),
    NEW_Record_stop                     ("Record", "Record_stop",                           Type.action,     Level2.Record),
    NEW_Record_play                     ("Record", "Record_play",                           Type.action,     Level2.Record),
    NEW_Record_play_pause               ("Record", "Record_play_pause",                     Type.action,     Level2.Record),
    NEW_Record_reset                    ("Record", "Record_reset",                          Type.navigation, Level2.Record),
    NEW_Record_save                     ("Record", "Record_save",                           Type.navigation, Level2.Record),
    NEW_Record_start_recording_did_fail ("Record", "Start_Recording_Did_Fail",              Type.action,     Level2.Record),

    NEW_Record_recording_was_interrupted("Record", "Recording_Was_Interrupted",             Type.action,     Level2.Record),
    NEW_Record_dedicated_recording_was_interrupted("Dedicated_Record", "Dedicated_Recording_Was_Interrupted", Type.action, Level2.Record),

    NEW_Record_record_pause             ("Record", "Record_pause::%d",                      Type.action,     Level2.Record),
    NEW_Record_rec_more                 ("Record", "Rec_more",                              Type.action,     Level2.Record),
    NEW_Record_close_main               ("Record", "close::main::%s",                       Type.action,     Level2.Record),
    NEW_Record_paused_play              ("Record", "paused::play::%d",                      Type.action,     Level2.Record),
    NEW_Record_paused_discard           ("Record", "paused::discard",                       Type.action,     Level2.Record),
    NEW_Record_paused_save              ("Record", "paused::save",                          Type.action,     Level2.Record),
    NEW_Record_paused_edit              ("Record", "paused::edit",                          Type.action,     Level2.Record),

    NEW_Record_edit_revert_to_original  ("Record", "edit::revert_to_original",              Type.action,     Level2.Record),
    NEW_Record_edit_interaction         ("Record", "edit::interaction",                     Type.action,     Level2.Record),
    NEW_Record_edit_save_trimmed        ("Record", "edit::save::trimmed",                   Type.action,     Level2.Record),
    NEW_Record_edit_save_not_trimmed    ("Record", "edit::save::not_trimmed",               Type.action,     Level2.Record),
    NEW_Record_old_rec_import_error     ("Record", "old_rec_import_error",                  Type.action,     Level2.Record),
    NEW_Record_old_rec_import           ("Record", "old_rec_import::%i",                    Type.action,     Level2.Record),

    NEW_Record_details_record_upload_share   ("Record_details", "Record_upload_share::%s::%s", Type.action,     Level2.Record),
    NEW_Record_details_record_another_sound  ("Record_details", "Record_another_sound",        Type.action,     Level2.Record),
    NEW_Record_details_close                 ("Record_details", "Close",                       Type.action,     Level2.Record),
    NEW_Record_details_record_edit_delete    ("Record_details", "Record_edit_delete",          Type.action,     Level2.Record),

    // Not used on iOS, also spelling mistake in the key
    NEW_Record_details_record_rehearsal_play  ("Record_details", "Record_Rehersal_play",        Type.action,     Level2.Record),
    NEW_Record_details_record_rehearsal_pause ("Record_details", "Record_Rehersal_pause",       Type.action,     Level2.Record),
    NEW_Record_details_record_rehearsal_save  ("Record_details", "Record_Rehersal_save",        Type.navigation, Level2.Record),
    NEW_Record_details_record_rehearsal_delete("Record_details", "Record_Rehersal_delete",      Type.navigation, Level2.Record),
    NEW_Record_tip_impressions                ("Record_tip_impressions", "%s",                  Type.action,     Level2.Record),

    // Old recording keys
    Record_rec           ("Record", "rec",            Type.action, Level2.Record),
    Record_rec_stop      ("Record", "rec_stop",       Type.action, Level2.Record),
    Record_discard       ("Record", "discard",        Type.action, Level2.Record),
    Record_discard__ok   ("Record", "discard::ok",    Type.action, Level2.Record),
    Record_discard_cancel("Record", "discard:cancel", Type.action, Level2.Record),
    Record_delete        ("Record", "delete",         Type.action, Level2.Record),

    Record_revert("Record", "revert", Type.action, Level2.Record),
    Record_revert__ok("Record", "revert::ok", Type.action, Level2.Record),
    Record_revert_cancel("Record", "revert:cancel", Type.action, Level2.Record),

    Record_play          ("Record", "play",           Type.action, Level2.Record),
    Record_play_stop     ("Record", "play_stop",      Type.action, Level2.Record),
    Record_next          ("Record", "next",           Type.navigation, Level2.Record), // record another sound
    Record_save          ("Record", "save",           Type.navigation, Level2.Record), // TODO

    Record_edit          ("Record", "edit",         Type.navigation, Level2.Record),

    Record_details_add_image       ("Record_details", "add_image",        Type.action,     Level2.Record),
    Record_details_new_image       ("Record_details", "new_image",        Type.action,     Level2.Record),
    Record_details_existing_image  ("Record_details", "existing_image",   Type.action,     Level2.Record),
    Record_details_record_another  ("Record_details", "record_another",   Type.navigation, Level2.Record),
    Record_details_Upload_and_share("Record_details", "Upload_and_share", Type.action,     Level2.Record),

    Dedicated_recording_details_back("Dedicated_recording_details", "back",   Type.navigation, Level2.Record),
    Dedicated_recording_details_send("Dedicated_recording_details", "send",   Type.action,     Level2.Record),

    Follow  ("Follow",   "level2::user_permalink", Type.action, null /* read from args */),
    Unfollow("Unfollow", "level2::user_permalink", Type.action, null /* read from args */),

    Log_out_log_out   ("Log_out",     "log_out",    Type.action, Level2.Settings),
    Log_out_box_ok    ("Log_out_box", "ok",         Type.action, Level2.Settings),
    Log_out_box_cancel("Log_out_box", "cancel",     Type.action, Level2.Settings),

    Like      ("Like",       "user_permalink::track_permalink", Type.action, Level2.Sounds),
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
        for (Object o : args) {
            result = Page.expandVariables(result, o);
        }
        return result;
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
