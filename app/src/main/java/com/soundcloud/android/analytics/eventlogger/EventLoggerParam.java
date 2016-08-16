package com.soundcloud.android.analytics.eventlogger;

class EventLoggerParam {
    static final String CLIENT_ID = "client_id";
    static final String ANONYMOUS_ID = "anonymous_id";
    static final String TIMESTAMP = "ts";
    static final String USER = "user";

    // audio event params
    static final String ACTION = "action";
    static final String TRACK = "track";
    static final String TRACK_OWNER = "track_owner";
    static final String REPOSTER = "reposted_by";
    static final String TRACK_LENGTH = "track_length";
    static final String PAGE_NAME = "page_name";
    static final String TRIGGER = "trigger";
    static final String SOURCE = "source";
    static final String SOURCE_URN = "source_urn";
    static final String SOURCE_VERSION = "source_version";
    static final String IN_PLAYLIST = "in_playlist";
    static final String PLAYLIST_POSITION = "playlist_position";
    static final String PLAYHEAD_POSITION = "playhead_position";
    static final String POLICY = "policy";

    // subs
    static final String LOCAL_STORAGE_PLAYBACK = "local_storage_playback";
    static final String CONSUMER_SUBS_PLAN = "consumer_subs_plan";
    static final String OFFLINE_EVENT_STAGE = "event_stage";
    static final String APP_VERSION = "app_version";
    static final String IN_LIKES = "in_likes";

    // monetization params
    static final String AD_URN = "ad_urn";
    static final String EXTERNAL_MEDIA = "external_media";
    static final String MONETIZATION_TYPE = "monetization_type";
    static final String MONETIZED_OBJECT = "monetized_object";
    static final String MONETIZATION_MODEL = "monetization_model";
    static final String IMPRESSION_NAME = "impression_name";
    static final String IMPRESSION_OBJECT = "impression_object";
    static final String IMPRESSION_CATEGORY = "impression_category";
    static final String PROMOTED_BY = "promoted_by";
    static final String IN_FOREGROUND = "in_foreground";
    static final String AD_REQUEST_MADE = "request_made";
    static final String AD_REQUEST_SUCCESS = "request_success";
    static final String AD_REQUEST_ENDPOINT = "request_endpoint";
    static final String AD_SELECTION_OPTIMIZED = "selection_optimized";
    static final String ADS_RECEIVED = "ads_received";
    static final String PLAYER_VISIBLE = "is_player_visible";

    // performance & error event params
    static final String LATENCY = "latency";
    static final String PROTOCOL = "protocol";
    static final String PLAYER_TYPE = "player_type";
    static final String TYPE = "type";
    static final String HOST = "host";
    static final String CONNECTION_TYPE = "connection_type";
    static final String OS = "os";
    static final String BITRATE = "bitrate";
    static final String FORMAT = "format";
    static final String URL = "url";
    static final String ERROR_CODE = "errorCode";
    static final String ERROR_NAME = "error_code";

    // rich media specific performance & error event params
    static final String METRIC_NAME = "metric";
    static final String METRIC_VALUE = "value";
    static final String MEDIA_TYPE = "media_type";

    // click specific params
    static final String CLICK_NAME = "click_name";
    static final String CLICK_OBJECT = "click_object";
    static final String CLICK_TARGET = "click_target";
    static final String CLICK_CATEGORY = "click_category";
    static final String CLICK_ATTRIBUTES = "click_attributes";
    static final String PAUSE_REASON = "pause_reason";
    static final String OVERFLOW_MENU = "overflow_menu";

    // HTI Params
    static final String UUID = "client_event_id";
    static final String REFERRING_EVENT = "referring_event";
    static final String REFERRING_EVENT_KIND = "event_type";

    // search tracking
    static final String QUERY_URN = "query_urn";
    static final String QUERY_POSITION = "query_position";

    // foreground
    static final String PAGE_URN = "page_urn";
    static final String REFERRER = "referrer";

    // audio action
    static final String AUDIO_ACTION_PLAY = "play";
    static final String AUDIO_ACTION_PAUSE = "pause";
    static final String AUDIO_ACTION_CHECKPOINT = "checkpoint";


    //audio v0
    @Deprecated
    static final String PLAYLIST_ID = "set_id";
    @Deprecated
    static final String PLAYLIST_POSITION_v0 = "set_position";
    @Deprecated
    static final String DURATION = "duration";
    @Deprecated
    static final String SOUND = "sound";
}
