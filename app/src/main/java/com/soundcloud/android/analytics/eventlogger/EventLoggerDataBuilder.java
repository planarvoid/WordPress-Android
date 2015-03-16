package com.soundcloud.android.analytics.eventlogger;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.VisualAdImpressionEvent;
import com.soundcloud.android.utils.DeviceHelper;

import android.content.res.Resources;

public abstract class EventLoggerDataBuilder {

    static final String CLIENT_ID = "client_id";
    static final String ANONYMOUS_ID = "anonymous_id";
    static final String TIMESTAMP = "ts";
    static final String USER = "user";
    // audio event params
    static final String ACTION = "action";
    static final String DURATION = "duration";
    static final String SOUND = "sound";
    static final String PAGE_NAME = "page_name";
    static final String TRIGGER = "trigger";
    static final String SOURCE = "source";
    static final String POLICY = "policy";
    static final String SOURCE_VERSION = "source_version";
    static final String PLAYLIST_ID = "set_id";
    static final String PLAYLIST_POSITION = "set_position";
    // ad specific params
    static final String AD_URN = "ad_urn";
    static final String EXTERNAL_MEDIA = "external_media";
    static final String MONETIZATION_TYPE = "monetization_type";
    static final String MONETIZED_OBJECT = "monetized_object";
    static final String IMPRESSION_NAME = "impression_name";
    static final String IMPRESSION_OBJECT = "impression_object";
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
    // click specific params
    static final String CLICK_NAME = "click_name";
    static final String CLICK_OBJECT = "click_object";
    static final String CLICK_TARGET = "click_target";
    public static final String MONETIZATION_TYPE_AUDIO_AD = "audio_ad";
    static final String REASON = "reason";

    protected final String appId;
    protected final String endpoint;
    protected final DeviceHelper deviceHelper;
    protected final ExperimentOperations experimentOperations;
    protected final AccountOperations accountOperations;

    public EventLoggerDataBuilder(Resources resources,
                                     ExperimentOperations experimentOperations,
                                     DeviceHelper deviceHelper,
                                     AccountOperations accountOperations) {
        this.accountOperations = accountOperations;
        this.appId = resources.getString(R.string.app_id);
        this.endpoint = resources.getString(R.string.event_logger_base_url);
        this.experimentOperations = experimentOperations;
        this.deviceHelper = deviceHelper;
    }

    public abstract String build(AdOverlayTrackingEvent event);

    public abstract String build(ScreenEvent event);

    public abstract String build(VisualAdImpressionEvent event);

    public abstract String build(UIEvent event);

    public abstract String build(PlaybackPerformanceEvent eventData);

    public abstract String build(PlaybackErrorEvent eventData);

    public abstract String buildForAudioAdImpression(PlaybackSessionEvent eventData);

    public abstract String buildForAdFinished(PlaybackSessionEvent eventData);

    public abstract String buildForAudioEvent(PlaybackSessionEvent eventData);
}
