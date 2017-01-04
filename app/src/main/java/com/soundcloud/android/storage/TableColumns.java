package com.soundcloud.android.storage;

import android.provider.BaseColumns;

@Deprecated // use the new `Tables` structure
public final class TableColumns {

    public static class ResourceTable implements BaseColumns {
        public static final String _TYPE = "_type";
        public static final String CREATED_AT = "created_at";
        public static final String LAST_UPDATED = "last_updated";
        public static final String PERMALINK = "permalink";
    }

    public static class SoundStream implements BaseColumns {
        public static final String SOUND_ID = "sound_id";
        public static final String SOUND_TYPE = "sound_type";
        public static final String REPOSTER_ID = "reposter_id";
        public static final String PROMOTED_ID = "promoted_id";
        public static final String CREATED_AT = "created_at";
    }

    public static class PromotedTracks implements BaseColumns {
        public static final String AD_URN = "ad_urn";
        public static final String CREATED_AT = "created_at";
        public static final String PROMOTER_ID = "promoter_id";
        public static final String PROMOTER_NAME = "promoter_name";
        public static final String TRACKING_TRACK_CLICKED_URLS = "tracking_track_clicked_urls";
        public static final String TRACKING_PROFILE_CLICKED_URLS = "tracking_profile_clicked_urls";
        public static final String TRACKING_PROMOTER_CLICKED_URLS = "tracking_promoter_clicked_urls";
        public static final String TRACKING_TRACK_PLAYED_URLS = "tracking_track_played_urls";
        public static final String TRACKING_TRACK_IMPRESSION_URLS = "tracking_track_impression_urls";
    }

    public static final class Waveforms implements BaseColumns {
        public static final String TRACK_ID = "track_id";
        public static final String MAX_AMPLITUDE = "max_amplitude";
        public static final String SAMPLES = "samples";
        public static final String CREATED_AT = "created_at";
    }

    public static final class Comments implements BaseColumns {
        public static final String CREATED_AT = "created_at";
        public static final String BODY = "body";
        public static final String TIMESTAMP = "timestamp";
        public static final String USER_ID = "user_id";
        public static final String TRACK_ID = "track_id";
    }

    /**
     * {@link com.soundcloud.android.storage.DatabaseSchema#DATABASE_CREATE_ACTIVITIES}
     */
    public static class Activities implements BaseColumns {
        public static final String UUID = "uuid";
        public static final String TYPE = "type";
        public static final String TAGS = "tags";
        public static final String USER_ID = "user_id";
        public static final String SOUND_ID = "sound_id";
        public static final String SOUND_TYPE = "sound_type";
        public static final String COMMENT_ID = "comment_id";
        public static final String CREATED_AT = "created_at";
        public static final String CONTENT_ID = "content_id";
        public static final String SHARING_NOTE_TEXT = "sharing_note_text";
        public static final String SHARING_NOTE_CREATED_AT = "sharing_note_created_at";

        public static final String[] ALL_FIELDS = {
                _ID, UUID, TYPE, TAGS, USER_ID, SOUND_ID, SOUND_TYPE, COMMENT_ID, CREATED_AT,
                CONTENT_ID, SHARING_NOTE_TEXT, SHARING_NOTE_CREATED_AT
        };
    }

    /**
     * {@link com.soundcloud.android.storage.DatabaseSchema#DATABASE_CREATE_COLLECTIONS}
     */
    public static final class Collections implements BaseColumns {
        public static final String URI = "uri";                      // local content provider uri
        public static final String LAST_SYNC = "last_sync";          // timestamp of last sync
        public static final String LAST_SYNC_ATTEMPT = "last_sync_attempt";          // timestamp of last sync
        public static final String EXTRA = "extra";                  // general purpose field
    }

    public static class SoundView extends ResourceTable implements BaseColumns {
        public static final String LAST_UPDATED = Tables.Sounds.LAST_UPDATED.name();
        public static final String PERMALINK = Tables.Sounds.PERMALINK.name();
        public static final String CREATED_AT = Tables.Sounds.CREATED_AT.name();
        public static final String DURATION = Tables.Sounds.DURATION.name();
        public static final String SNIPPET_DURATION = Tables.Sounds.SNIPPET_DURATION.name();
        public static final String FULL_DURATION = Tables.Sounds.FULL_DURATION.name();
        public static final String ORIGINAL_CONTENT_SIZE = Tables.Sounds.ORIGINAL_CONTENT_SIZE.name();
        public static final String STATE = Tables.Sounds.STATE.name();
        public static final String GENRE = Tables.Sounds.GENRE.name();
        public static final String TAG_LIST = Tables.Sounds.TAG_LIST.name();
        public static final String TRACK_TYPE = Tables.Sounds.TRACK_TYPE.name();
        public static final String TITLE = Tables.Sounds.TITLE.name();
        public static final String PERMALINK_URL = Tables.Sounds.PERMALINK_URL.name();
        public static final String ARTWORK_URL = Tables.Sounds.ARTWORK_URL.name();
        public static final String WAVEFORM_URL = Tables.Sounds.WAVEFORM_URL.name();
        public static final String DOWNLOADABLE = Tables.Sounds.DOWNLOADABLE.name();
        public static final String DOWNLOAD_URL = Tables.Sounds.DOWNLOAD_URL.name();
        public static final String STREAM_URL = Tables.Sounds.STREAM_URL.name();
        public static final String STREAMABLE = Tables.Sounds.STREAMABLE.name();
        public static final String COMMENTABLE = Tables.Sounds.COMMENTABLE.name();
        public static final String SHARING = Tables.Sounds.SHARING.name();
        public static final String LICENSE = Tables.Sounds.LICENSE.name();
        public static final String PURCHASE_URL = Tables.Sounds.PURCHASE_URL.name();
        public static final String PLAYBACK_COUNT = Tables.Sounds.PLAYBACK_COUNT.name();
        public static final String DOWNLOAD_COUNT = Tables.Sounds.DOWNLOAD_COUNT.name();
        public static final String COMMENT_COUNT = Tables.Sounds.COMMENT_COUNT.name();
        public static final String LIKES_COUNT = Tables.Sounds.LIKES_COUNT.name();
        public static final String REPOSTS_COUNT = Tables.Sounds.REPOSTS_COUNT.name();
        public static final String SHARED_TO_COUNT = Tables.Sounds.SHARED_TO_COUNT.name();
        public static final String TRACKS_URI = Tables.Sounds.TRACKS_URI.name();
        public static final String TRACK_COUNT = Tables.Sounds.TRACK_COUNT.name();
        public static final String DESCRIPTION = Tables.Sounds.DESCRIPTION.name();
        public static final String IS_ALBUM = Tables.Sounds.IS_ALBUM.name();
        public static final String SET_TYPE = Tables.Sounds.SET_TYPE.name();
        public static final String RELEASE_DATE = Tables.Sounds.RELEASE_DATE.name();

        public static final String POLICIES_MONETIZABLE = "sound_policies_monetizable";
        public static final String POLICIES_BLOCKED = "sound_policies_blocked";
        public static final String POLICIES_SNIPPED = "sound_policies_snipped";
        public static final String POLICIES_POLICY = "sound_policies_policy";
        public static final String POLICIES_SYNCABLE = "sound_policies_syncable";
        public static final String POLICIES_MONETIZATION_MODEL = "sound_policies_monetization_model";
        public static final String POLICIES_SUB_MID_TIER = "sound_policies_sub_mid_tier";
        public static final String POLICIES_SUB_HIGH_TIER = "sound_policies_sub_high_tier";

        public static final String USER_ID = "sound_user_id";
        public static final String USERNAME = "sound_user_username";
        public static final String USER_PERMALINK = "sound_user_permalink";

        public static final String USER_AVATAR_URL = "sound_user_avatar_url";
        public static final String USER_LIKE = "sound_user_like";
        public static final String USER_REPOST = "sound_user_repost";
        public static final String USER_PLAY_COUNT = "sound_user_play_count";

        public static final String OFFLINE_DOWNLOADED_AT = "sound_offline_downloaded_at";
        public static final String OFFLINE_REMOVED_AT = "sound_offline_removed_at";
        public static final String OFFLINE_REQUESTED_AT = "sound_offline_requested_at";
        public static final String OFFLINE_UNAVAILABLE_AT = "sound_offline_unavailable_at";
    }

    public final static class SoundStreamView extends SoundStream {
        public static final String REPOSTER_USERNAME = "reposter_username";
        public static final String REPOSTER_PERMALINK = "reposter_permalink";
        public static final String REPOSTER_AVATAR_URL = "reposter_avatar_url";
        public static final String PROMOTER_AVATAR_URL = "promoter_avatar_url";
        public static final String SOUND_PERMALINK_URL = "sound_permalink_url";
    }

    public final static class ActivityView extends Activities {
        public static final String COMMENT_BODY = "comment_body";
        public static final String COMMENT_TIMESTAMP = "comment_timestamp";
        public static final String COMMENT_CREATED_AT = "comment_created_at";

        public static final String USER_USERNAME = "activity_user_username";
        public static final String USER_PERMALINK = "activity_user_permalink";
        public static final String USER_AVATAR_URL = "activity_user_avatar_url";
    }

    public final static class PlaylistTracksView extends SoundView {
        public static final String PLAYLIST_ID = PlaylistTracks.PLAYLIST_ID;
        public static final String PLAYLIST_POSITION = "playlist_position";
        public static final String PLAYLIST_ADDED_AT = "playlist_added_at";
    }

    public final static class PlaylistTracks implements BaseColumns {
        public static final String PLAYLIST_ID = "playlist_id";
        public static final String TRACK_ID = "track_id";
        public static final String POSITION = "position";
        public static final String ADDED_AT = "added_at";
        public static final String REMOVED_AT = "removed_at";
    }
}
