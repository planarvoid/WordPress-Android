package com.soundcloud.android.configuration.experiments;

import com.soundcloud.android.R;

import android.content.Context;
import android.support.annotation.StringRes;

import javax.inject.Inject;

public class ChangeLikeToSaveExperimentStringHelper {

    public enum ExperimentString {

        LIKE(R.string.btn_like, R.string.btn_add_to_collection),
        UNLIKE(R.string.btn_unlike, R.string.btn_remove_from_collection),
        ACCESSIBILITY_STATS_USER_LIKED(R.string.accessibility_stats_user_liked, R.string.accessibility_stats_user_added),
        ACCESSIBILITY_LIKE_ACTION(R.string.accessibility_like_action, R.string.accessibility_add_action),
        ACCESSIBILITY_STATS_LIKES(R.plurals.accessibility_stats_likes, R.plurals.accessibility_stats_adds),
        SHORTCUT_PLAY_LIKES(R.string.shortcut_play_likes, R.string.shortcut_play_tracks),
        LIST_EMPTY_YOU_LIKES_MESSAGE(R.string.list_empty_you_likes_message, R.string.list_empty_you_tracks_message),
        LIKE_TOAST_OVERFLOW_ACTION(R.string.like_toast_overflow_action, R.string.add_snackbar_overflow_action),
        UNLIKE_TOAST_OVERFLOW_ACTION(R.string.unlike_toast_overflow_action, R.string.remove_snackbar_overflow_action),
        LIKE_ERROR_TOAST_OVERFLOW_ACTION(R.string.like_error_toast_overflow_action, R.string.add_error_snackbar_overflow_action),
        TAB_TITLE_USER_LIKES(R.string.tab_title_user_likes, R.string.tab_title_user_tracks),
        RECOMMENDATION_REASON_BECAUSE_YOU_LIKED_TRACKTITLE(R.string.recommendation_reason_because_you_liked_tracktitle, R.string.recommendation_reason_because_you_added_tracktitle),
        PREF_SYNC_WIFI_ONLY_DESCRIPTION(R.string.pref_sync_wifi_only_description, R.string.pref_sync_wifi_only_description_tracks),
        LIKED_STATIONS_EMPTY_VIEW_MESSAGE(R.string.liked_stations_empty_view_message, R.string.added_stations_empty_view_message),
        COLLECTIONS_YOUR_LIKED_TRACKS(R.string.collections_your_liked_tracks, R.string.collections_tracks_header),
        TRACK_LIKES_TITLE(R.string.track_likes_title, R.string.track_added_title),
        COLLECTIONS_OPTIONS_TOGGLE_LIKES(R.string.collections_options_toggle_likes, R.string.collections_options_toggle_added),
        COLLECTIONS_EMPTY_PLAYLISTS(R.string.collections_empty_playlists, R.string.collections_empty_playlists_added),
        COLLECTIONS_ONBOARDING_TITLE(R.string.collections_onboarding_title, R.string.collections_onboarding_title_tracks),
        COLLECTIONS_UPSELL_BODY(R.string.collections_upsell_body, R.string.collections_upsell_body_tracks),
        NOTIFICATION_USERNAME_LIKED_TRACKTITLE(R.string.notification_username_liked_tracktitle, R.string.notification_username_added_tracktitle),
        OFFLINE_LIKES_DIALOG_TITLE(R.string.offline_likes_dialog_title, R.string.offline_tracks_dialog_title),
        OFFLINE_LIKES_DIALOG_MESSAGE(R.string.offline_likes_dialog_message, R.string.offline_tracks_dialog_message),
        GO_ONBOARDING_OFFLINE_SETTINGS_SUBTEXT(R.string.go_onboarding_offline_settings_subtext, R.string.go_onboarding_offline_settings_subtext_tracks),
        PREF_OFFLINE_OFFLINE_COLLECTION_DESCRIPTION_OFF(R.string.pref_offline_offline_collection_description_off, R.string.pref_offline_offline_collection_description_off_tracks),
        DISABLE_OFFLINE_COLLECTION_BODY(R.string.disable_offline_collection_body, R.string.disable_offline_collection_body_tracks),
        EMPTY_YOU_SOUNDS_MESSAGE_SECONDARY(R.string.empty_you_sounds_message_secondary, R.string.empty_you_sounds_message_secondary_add),
        USER_PROFILE_SOUNDS_HEADER_LIKES(R.string.user_profile_sounds_header_likes, R.string.user_profile_sounds_header_tracks_in_collection),
        USER_PROFILE_SOUNDS_VIEW_ALL_LIKES(R.string.user_profile_sounds_view_all_likes, R.string.user_profile_sounds_view_all_tracks_in_collection),
        USER_PROFILE_SOUNDS_LIKES_EMPTY(R.string.user_profile_sounds_likes_empty, R.string.user_profile_sounds_tracks_in_collection_empty),
        PUSH_NOTIFICATIONS_LIKE(R.string.push_notifications_like, R.string.push_notifications_add),
        EMAIL_NOTIFICATIONS_LIKE(R.string.email_notifications_like, R.string.email_notifications_add),
        PLAY_QUEUE_HEADER_LIKES(R.string.play_queue_header_likes, R.string.play_queue_header_tracks),
        SUGGESTED_CREATORS_RELATION_LIKED(R.string.suggested_creators_relation_liked, R.string.suggested_creators_relation_added);

        @StringRes private final int likeStringResId;
        @StringRes private final int saveStringResId;

        ExperimentString(int likeStringResId, int saveStringResId) {
            this.likeStringResId = likeStringResId;
            this.saveStringResId = saveStringResId;
        }
    }

    private final ChangeLikeToSaveExperiment changeLikeToSaveExperiment;
    private final Context context;

    @Inject
    ChangeLikeToSaveExperimentStringHelper(ChangeLikeToSaveExperiment changeLikeToSaveExperiment, Context context) {
        this.changeLikeToSaveExperiment = changeLikeToSaveExperiment;
        this.context = context;
    }

    @StringRes
    public int getStringResId(ExperimentString experimentString) {
        return changeLikeToSaveExperiment.isEnabled()
               ? experimentString.saveStringResId
               : experimentString.likeStringResId;
    }

    public String getString(ExperimentString experimentString) {
        return changeLikeToSaveExperiment.isEnabled()
               ? context.getString(experimentString.saveStringResId)
               : context.getString(experimentString.likeStringResId);
    }
}
