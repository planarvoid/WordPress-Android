package com.soundcloud.android.storage;

import android.app.SearchManager;
import android.provider.BaseColumns;

public final class TableColumns {

    public static class Connections implements BaseColumns {
        public static final String USER_ID = "user_id";
        public static final String SERVICE = "service";
        public static final String TYPE = "type";
        public static final String CREATED_AT = "created_at";
        public static final String DISPLAY_NAME = "display_name";
        public static final String ACTIVE = "active";
        public static final String POST_PUBLISH = "post_publish";
        public static final String POST_LIKE = "post_like";
        public static final String URI = "uri";
    }

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

    public static final class Likes implements BaseColumns {
        public static final String _TYPE = "_type";
        public static final String CREATED_AT = "created_at";
        public static final String ADDED_AT = "added_at"; // local additions
        public static final String REMOVED_AT = "removed_at"; // local removals
    }

    public static final class Posts {
        public static final String TYPE = "type";
        public static final String TARGET_TYPE = "target_type";
        public static final String TARGET_ID = "target_id";
        public static final String CREATED_AT = "created_at";

        /* not used (yet) */
        public static final String ADDED_AT = "added_at"; // local additions
        public static final String REMOVED_AT = "removed_at"; // local removals

        public static final String TYPE_POST = "post";
        public static final String TYPE_REPOST = "repost";
    }

    public static class PromotedTracks implements BaseColumns {
        public static final String AD_URN = "ad_urn";
        public static final String PROMOTER_ID = "promoter_id";
        public static final String PROMOTER_NAME = "promoter_name";
        public static final String TRACKING_TRACK_CLICKED_URLS = "tracking_track_clicked_urls";
        public static final String TRACKING_PROFILE_CLICKED_URLS = "tracking_profile_clicked_urls";
        public static final String TRACKING_PROMOTER_CLICKED_URLS = "tracking_promoter_clicked_urls";
        public static final String TRACKING_TRACK_PLAYED_URLS = "tracking_track_played_urls";
        public static final String TRACKING_TRACK_IMPRESSION_URLS = "tracking_track_impression_urls";
    }

    /**
     * {@link com.soundcloud.android.storage.DatabaseSchema#DATABASE_CREATE_SOUNDS}
     */
    public static class Sounds extends ResourceTable {
        public static final String ORIGINAL_CONTENT_SIZE = "original_content_size";
        public static final String GENRE = "genre";
        public static final String DURATION = "duration";
        public static final String TAG_LIST = "tag_list";
        public static final String TRACK_TYPE = "track_type";
        public static final String TITLE = "title";
        public static final String PERMALINK_URL = "permalink_url";
        public static final String ARTWORK_URL = "artwork_url";
        public static final String WAVEFORM_URL = "waveform_url";
        public static final String DOWNLOADABLE = "downloadable";
        public static final String DOWNLOAD_URL = "download_url";
        public static final String STREAM_URL = "stream_url";
        public static final String STREAMABLE = "streamable";
        public static final String COMMENTABLE = "commentable";
        public static final String SHARING = "sharing";
        public static final String LICENSE = "license";
        public static final String PURCHASE_URL = "purchase_url";
        public static final String PLAYBACK_COUNT = "playback_count";
        public static final String DOWNLOAD_COUNT = "download_count";
        public static final String COMMENT_COUNT = "comment_count";
        public static final String LIKES_COUNT = "favoritings_count";
        public static final String REPOSTS_COUNT = "reposts_count";
        public static final String SHARED_TO_COUNT = "shared_to_count";
        public static final String USER_ID = "user_id";
        public static final String STATE = "state";
        public static final String TRACKS_URI = "tracks_uri";
        public static final String TRACK_COUNT = "track_count";
        public static final String PLAYLIST_TYPE = "playlist_type";
        public static final String DESCRIPTION = "description";

        public static final int TYPE_TRACK    = 0;
        public static final int TYPE_PLAYLIST = 1;

        public static final String[] ALL_FIELDS = {
                _ID, _TYPE, ORIGINAL_CONTENT_SIZE, DURATION, GENRE, TAG_LIST, TRACK_TYPE, TITLE, PERMALINK_URL,
                ARTWORK_URL, WAVEFORM_URL, DOWNLOADABLE, DOWNLOAD_URL, STREAM_URL, STREAMABLE,
                COMMENTABLE, SHARING, LICENSE, PURCHASE_URL, PLAYBACK_COUNT, DOWNLOAD_COUNT,
                COMMENT_COUNT, LIKES_COUNT, REPOSTS_COUNT, SHARED_TO_COUNT,
                USER_ID, STATE, CREATED_AT, PERMALINK, LAST_UPDATED,
                TRACKS_URI, TRACK_COUNT, PLAYLIST_TYPE, DESCRIPTION
        };
    }

    public static class TrackPolicies implements BaseColumns {
        public static final String TRACK_ID = "track_id";
        public static final String MONETIZABLE = "monetizable";
        public static final String POLICY = "policy";
        public static final String SYNCABLE = "syncable";
        public static final String LAST_UPDATED = "last_updated";

        public static final String[] ALL_FIELDS = new String[] {
                TRACK_ID, MONETIZABLE, POLICY, SYNCABLE, LAST_UPDATED
        };
    }

    /**
     * {@link com.soundcloud.android.storage.DatabaseSchema#DATABASE_CREATE_TRACK_METADATA}
     */
    public static final class TrackMetadata implements BaseColumns {
        public static final String USER_ID = "user_id";
        public static final String PLAY_COUNT = "play_count";
        public static final String CACHED = "cached";
        public static final String TYPE = "type";
        public static final String SIZE = "size";
        public static final String URL_HASH = "url_hash";
        public static final String ETAG = "etag";
        public static final String BITRATE = "bitrate";

        public static final String[] ALL_FIELDS = new String[]{
                _ID, USER_ID, PLAY_COUNT, CACHED, TYPE, ETAG, BITRATE, URL_HASH, SIZE
        };
    }

    /**
     * {@link com.soundcloud.android.storage.DatabaseSchema#DATABASE_CREATE_COLLECTION_ITEMS}
     */
    public static final class CollectionItems {
        public static final String ITEM_ID = "item_id";
        public static final String USER_ID = "user_id";     // "owner" of the item
        public static final String COLLECTION_TYPE = "collection_type"; // the association
        public static final String RESOURCE_TYPE = "resource_type";  // used by sounds to determine playlist or track
        public static final String POSITION = "position";
        public static final String CREATED_AT = "created_at";
    }

    /**
     * {@link com.soundcloud.android.storage.DatabaseSchema#DATABASE_CREATE_USER_ASSOCIATIONS}
     */
    public static final class UserAssociations {
        public static final String OWNER_ID = "owner_id"; // the source user of the association
        public static final String TARGET_ID = "target_id";// the target user of the association
        public static final String ASSOCIATION_TYPE = "association_type"; // the type of association (e.g. Following, Follower)
        public static final String RESOURCE_TYPE = "resource_type";  // currently unused, but if we add groups...
        public static final String POSITION = "position"; // as returned from the api
        public static final String CREATED_AT = "created_at"; // indicates when this was created on the api
        public static final String ADDED_AT = "added_at"; // when was this added locally (pre-api sync)
        public static final String REMOVED_AT = "removed_at"; // when was this removed locally (pre-api sync)
        public static final String TOKEN = "token"; // when was this removed locally (pre-api sync)
    }

    /**
     * {@link com.soundcloud.android.storage.DatabaseSchema#DATABASE_CREATE_USERS}
     */
    public static class Users extends ResourceTable {
        public static final String USERNAME = "username";
        public static final String AVATAR_URL = "avatar_url";
        public static final String CITY = "city";
        public static final String COUNTRY = "country";
        public static final String DISCOGS_NAME = "discogs_name";
        public static final String FOLLOWERS_COUNT = "followers_count";
        public static final String FOLLOWINGS_COUNT = "followings_count";
        public static final String FULL_NAME = "full_name";
        public static final String MYSPACE_NAME = "myspace_name";
        public static final String TRACK_COUNT = "track_count";
        public static final String WEBSITE = "website";
        public static final String WEBSITE_TITLE = "website_title";
        public static final String DESCRIPTION = "description";
        public static final String USER_FOLLOWING = "user_following";
        public static final String USER_FOLLOWER = "user_follower";
        public static final String PERMALINK_URL = "permalink_url";

        public static final String PRIMARY_EMAIL_CONFIRMED = "primary_email_confirmed";
        public static final String PUBLIC_LIKES_COUNT = "public_favorites_count";
        public static final String PRIVATE_TRACKS_COUNT = "private_tracks_count";

        public static final String PLAN = "plan";

        public static final String[] ALL_FIELDS = {
                _ID, USERNAME, AVATAR_URL, CITY, COUNTRY, DISCOGS_NAME,
                FOLLOWERS_COUNT, FOLLOWINGS_COUNT, FULL_NAME, MYSPACE_NAME,
                TRACK_COUNT, WEBSITE, WEBSITE_TITLE, DESCRIPTION, PERMALINK,
                LAST_UPDATED, PERMALINK_URL, PRIMARY_EMAIL_CONFIRMED, PUBLIC_LIKES_COUNT,
                PRIVATE_TRACKS_COUNT, PLAN
        };
    }

    public static final class Comments extends ResourceTable {
        public static final String BODY = "body";
        public static final String TIMESTAMP = "timestamp";
        public static final String USER_ID = "user_id";
        public static final String TRACK_ID = "track_id";
    }

    /**
     * {@link com.soundcloud.android.storage.DatabaseSchema#DATABASE_CREATE_RECORDINGS}
     */
    public static final class Recordings implements BaseColumns {
        public static final String USER_ID = "user_id";
        public static final String TIMESTAMP = "timestamp";
        public static final String LONGITUDE = "longitude";
        public static final String LATITUDE = "latitude";
        public static final String WHAT_TEXT = "what_text";
        public static final String WHERE_TEXT = "where_text";
        public static final String AUDIO_PATH = "audio_path";
        public static final String DURATION = "duration";
        public static final String DESCRIPTION = "description";
        public static final String ARTWORK_PATH = "artwork_path";
        public static final String SHARED_EMAILS = "shared_emails";
        public static final String SHARED_IDS = "shared_ids";
        public static final String PRIVATE_USER_ID = "private_user_id";
        public static final String TIP_KEY = "tip_key";
        public static final String SERVICE_IDS = "service_ids";
        public static final String IS_PRIVATE = "is_private";
        public static final String EXTERNAL_UPLOAD = "external_upload";
        public static final String UPLOAD_STATUS = "upload_status";
        public static final String FOUR_SQUARE_VENUE_ID = "four_square_venue_id";
        public static final String TRIM_LEFT = "trim_left";
        public static final String TRIM_RIGHT = "trim_right";
        public static final String FILTERS = "filters";
        public static final String OPTIMIZE = "optimize";
        public static final String FADING = "fading";

        public static final String[] ALL_FIELDS = {
                _ID, USER_ID, TIMESTAMP, LONGITUDE, LATITUDE, WHAT_TEXT,
                WHERE_TEXT, AUDIO_PATH, DURATION, DESCRIPTION, ARTWORK_PATH,
                SHARED_EMAILS, SHARED_IDS, PRIVATE_USER_ID, TIP_KEY, SERVICE_IDS, IS_PRIVATE,
                EXTERNAL_UPLOAD, UPLOAD_STATUS, FOUR_SQUARE_VENUE_ID,
                TRIM_LEFT, TRIM_RIGHT, FILTERS, OPTIMIZE, FADING
        };
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
        public static final String SIZE = "size";
        public static final String SYNC_STATE = "sync_state";        // are we currently syncing?
        public static final String EXTRA = "extra";                  // general purpose field
    }

    /**
     * {@link com.soundcloud.android.storage.DatabaseSchema#DATABASE_CREATE_COLLECTION_PAGES}
     */
    public static final class CollectionPages implements BaseColumns {
        public static final String COLLECTION_ID = "collection_id";
        public static final String ETAG = "etag";
        public static final String SIZE = "size";
        public static final String PAGE_INDEX = "page_index";
    }

    /**
     * {@link com.soundcloud.android.storage.DatabaseSchema#DATABASE_CREATE_PLAY_QUEUE}
     */
    public final static class PlayQueue implements BaseColumns {
        public static final String TRACK_ID = "track_id";
        public static final String SOURCE = "source";
        public static final String SOURCE_VERSION = "source_version";
    }

    /**
     * {@link com.soundcloud.android.storage.DatabaseSchema#DATABASE_CREATE_TRACK_DOWNLOADS}
     */
    public final static class TrackDownloads implements BaseColumns {
        public static final String REQUESTED_AT = "requested_at";
        public static final String DOWNLOADED_AT = "downloaded_at";
        public static final String REMOVED_AT = "removed_at";
        public static final String UNAVAILABLE_AT = "unavailable_at";
    }

    public final static class OfflineContent implements BaseColumns {
        public static final String _TYPE = "_type";

        public static final int TYPE_PLAYLIST = Sounds.TYPE_PLAYLIST;
    }

    public static class SoundView extends ResourceTable implements BaseColumns {
        public static final String LAST_UPDATED = Sounds.LAST_UPDATED;
        public static final String PERMALINK = Sounds.PERMALINK;
        public static final String CREATED_AT = Sounds.CREATED_AT;
        public static final String DURATION = Sounds.DURATION;
        public static final String ORIGINAL_CONTENT_SIZE = Sounds.ORIGINAL_CONTENT_SIZE;
        public static final String STATE = Sounds.STATE;
        public static final String GENRE = Sounds.GENRE;
        public static final String TAG_LIST = Sounds.TAG_LIST;
        public static final String TRACK_TYPE = Sounds.TRACK_TYPE;
        public static final String TITLE = Sounds.TITLE;
        public static final String PERMALINK_URL = Sounds.PERMALINK_URL;
        public static final String ARTWORK_URL = Sounds.ARTWORK_URL;
        public static final String WAVEFORM_URL = Sounds.WAVEFORM_URL;
        public static final String DOWNLOADABLE = Sounds.DOWNLOADABLE;
        public static final String DOWNLOAD_URL = Sounds.DOWNLOAD_URL;
        public static final String STREAM_URL = Sounds.STREAM_URL;
        public static final String STREAMABLE = Sounds.STREAMABLE;
        public static final String COMMENTABLE = Sounds.COMMENTABLE;
        public static final String SHARING = Sounds.SHARING;
        public static final String LICENSE = Sounds.LICENSE;
        public static final String PURCHASE_URL = Sounds.PURCHASE_URL;
        public static final String PLAYBACK_COUNT = Sounds.PLAYBACK_COUNT;
        public static final String DOWNLOAD_COUNT = Sounds.DOWNLOAD_COUNT;
        public static final String COMMENT_COUNT = Sounds.COMMENT_COUNT;
        public static final String LIKES_COUNT = Sounds.LIKES_COUNT;
        public static final String REPOSTS_COUNT = Sounds.REPOSTS_COUNT;
        public static final String SHARED_TO_COUNT = Sounds.SHARED_TO_COUNT;
        public static final String TRACKS_URI = Sounds.TRACKS_URI;
        public static final String TRACK_COUNT = Sounds.TRACK_COUNT;
        public static final String DESCRIPTION = Sounds.DESCRIPTION;

        public static final String POLICIES_MONETIZABLE = "sound_policies_monetizable";
        public static final String POLICIES_POLICY = "sound_policies_policy";
        public static final String POLICIES_SYNCABLE = "sound_policies_syncable";

        public static final String USER_ID = "sound_user_id";
        public static final String USERNAME = "sound_user_username";
        public static final String USER_PERMALINK = "sound_user_permalink";

        public static final String USER_AVATAR_URL = "sound_user_avatar_url";
        public static final String USER_LIKE = "sound_user_like";
        public static final String USER_REPOST = "sound_user_repost";
        public static final String USER_PLAY_COUNT = "sound_user_play_count";

        public static final String OFFLINE_DOWNLOADED_AT = "sound_offline_downloaded_at";
        public static final String OFFLINE_REMOVED_AT = "sound_offline_removed_at";

        @Deprecated
        public static final String CACHED = "sound_cached";

        public static final String[] ALL_VIEW_FIELDS = {
                POLICIES_MONETIZABLE, POLICIES_POLICY, POLICIES_SYNCABLE,
                USER_ID, USERNAME, USER_PERMALINK, USER_AVATAR_URL, USER_LIKE, USER_REPOST, USER_PLAY_COUNT,
                OFFLINE_DOWNLOADED_AT, OFFLINE_REMOVED_AT, CACHED
        };
        public static final String[] ALL_FIELDS;

        static {
            ALL_FIELDS = new String[Sounds.ALL_FIELDS.length + ALL_VIEW_FIELDS.length];
            System.arraycopy(Sounds.ALL_FIELDS, 0, ALL_FIELDS, 0, Sounds.ALL_FIELDS.length);

            System.arraycopy(ALL_VIEW_FIELDS, 0, ALL_FIELDS, Sounds.ALL_FIELDS.length, ALL_VIEW_FIELDS.length);
        }

    }

    public final static class SoundStreamView extends SoundStream {
        public static final String REPOSTER_USERNAME = "reposter_username";
        public static final String REPOSTER_PERMALINK = "reposter_permalink";
        public static final String REPOSTER_AVATAR_URL = "reposter_avatar_url";
    }

    public final static class ActivityView extends Activities {
        public static final String COMMENT_BODY = "comment_body";
        public static final String COMMENT_TIMESTAMP = "comment_timestamp";
        public static final String COMMENT_CREATED_AT = "comment_created_at";

        public static final String USER_USERNAME = "activity_user_username";
        public static final String USER_PERMALINK = "activity_user_permalink";
        public static final String USER_AVATAR_URL = "activity_user_avatar_url";

        public static final String[] ALL_VIEW_FIELDS = {
                COMMENT_BODY, COMMENT_TIMESTAMP, COMMENT_CREATED_AT,
                USER_USERNAME, USER_PERMALINK, USER_AVATAR_URL
        };

        public static final String[] ALL_FIELDS;

        static {
            // sometimes java feels like C all over again
            ALL_FIELDS = new String[Activities.ALL_FIELDS.length + ALL_VIEW_FIELDS.length + SoundView.ALL_FIELDS.length];
            System.arraycopy(Activities.ALL_FIELDS, 0, ALL_FIELDS, 0, Activities.ALL_FIELDS.length);
            System.arraycopy(ALL_VIEW_FIELDS, 0, ALL_FIELDS, Activities.ALL_FIELDS.length, ALL_VIEW_FIELDS.length);
            System.arraycopy(SoundView.ALL_FIELDS, 0, ALL_FIELDS, Activities.ALL_FIELDS.length + ALL_VIEW_FIELDS.length, SoundView.ALL_FIELDS.length);
        }
    }

    public final static class AssociationView {
        public static final String ASSOCIATION_TIMESTAMP = "association_timestamp";
        public static final String ASSOCIATION_TYPE = "association_type";
        public static final String ASSOCIATION_OWNER_ID = "association_owner_id";
    }

    public final static class UserAssociationView extends Users {
        public static final String USER_ASSOCIATION_TIMESTAMP = AssociationView.ASSOCIATION_TIMESTAMP;
        public static final String USER_ASSOCIATION_TYPE = AssociationView.ASSOCIATION_TYPE;
        public static final String USER_ASSOCIATION_OWNER_ID = AssociationView.ASSOCIATION_OWNER_ID;
        public static final String USER_ASSOCIATION_POSITION = "user_association_position";
        public static final String USER_ASSOCIATION_ADDED_AT = "user_association_added_at";
        public static final String USER_ASSOCIATION_REMOVED_AT = "user_association_removed_at";
        public static final String USER_ASSOCIATION_TOKEN = "user_association_token";
    }

    public final static class SoundAssociationView extends SoundView {
        public static final String SOUND_ASSOCIATION_TIMESTAMP = AssociationView.ASSOCIATION_TIMESTAMP;
        public static final String SOUND_ASSOCIATION_TYPE = AssociationView.ASSOCIATION_TYPE;
        public static final String SOUND_ASSOCIATION_OWNER_ID = AssociationView.ASSOCIATION_OWNER_ID;
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

    /**
     * @see <a href="http://developer.android.com/guide/topics/search/adding-custom-suggestions.html#SuggestionTable">
     * Building a suggestion table</a>
     */
    public final static class Suggestions implements BaseColumns {
        public static final String ID = "id";

        // following | like | group
        public static final String KIND = "kind";

        // used as an index to search
        public static final String TEXT = "text";

        // avatar_url | artwork_url
        @Deprecated // we can remove this column, it's not used anymore since we've moved to the image resolver
        public static final String ICON_URL = "icon_url";

        // permalink_url
        public static final String PERMALINK_URL = "permalink_url";

        // use search manager compatible mappings
        public static final String COLUMN_TEXT1 = SearchManager.SUGGEST_COLUMN_TEXT_1;
        public static final String COLUMN_TEXT2 = SearchManager.SUGGEST_COLUMN_TEXT_2;
        public static final String COLUMN_ICON = SearchManager.SUGGEST_COLUMN_ICON_1;

        // soundcloud:tracks:XXXX | soundcloud:users:XXXX
        public static final String INTENT_DATA = SearchManager.SUGGEST_COLUMN_INTENT_DATA;

        public static final String[] ALL_FIELDS = {
                ID, KIND, TEXT, ICON_URL, PERMALINK_URL, COLUMN_TEXT1, COLUMN_TEXT2, COLUMN_ICON, INTENT_DATA
        };
    }
}
