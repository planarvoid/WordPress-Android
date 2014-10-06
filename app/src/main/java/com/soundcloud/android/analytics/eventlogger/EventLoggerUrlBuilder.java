package com.soundcloud.android.analytics.eventlogger;

import com.soundcloud.android.R;
import com.soundcloud.android.events.AudioAdCompanionImpressionEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.experiments.ExperimentOperations;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.utils.DeviceHelper;

import android.content.res.Resources;
import android.net.Uri;

import javax.inject.Inject;
import java.util.Locale;
import java.util.Map;

public class EventLoggerUrlBuilder {

    private static final String ENDPOINT = "http://eventlogger.soundcloud.com";

    private static final String CLIENT_ID = "client_id";
    private static final String ANONYMOUS_ID = "anonymous_id";
    private static final String TIMESTAMP = "ts";
    private static final String USER = "user";
    // audio event params
    private static final String ACTION = "action";
    private static final String DURATION = "duration";
    private static final String SOUND = "sound";
    private static final String PAGE_NAME = "page_name";
    private static final String TRIGGER = "trigger";
    private static final String SOURCE = "source";
    private static final String POLICY = "policy";
    private static final String SOURCE_VERSION = "source_version";
    private static final String PLAYLIST_ID = "set_id";
    private static final String PLAYLIST_POSITION = "set_position";
    // ad specific params
    private static final String AD_URN = "ad_urn";
    private static final String EXTERNAL_MEDIA = "external_media";
    private static final String MONETIZATION_TYPE = "monetization_type";
    private static final String MONETIZED_OBJECT = "monetized_object";
    private static final String IMPRESSION_NAME = "impression_name";
    private static final String IMPRESSION_OBJECT = "impression_object";
    // performance & error event params
    private static final String LATENCY = "latency";
    private static final String PROTOCOL = "protocol";
    private static final String PLAYER_TYPE = "player_type";
    private static final String TYPE = "type";
    private static final String HOST = "host";
    private static final String CONNECTION_TYPE = "connection_type";
    private static final String OS = "os";
    private static final String BITRATE = "bitrate";
    private static final String FORMAT = "format";
    private static final String URL = "url";
    private static final String ERROR_CODE = "errorCode";
    // click specific params
    private static final String CLICK_NAME = "click_name";
    private static final String CLICK_OBJECT = "click_object";
    private static final String CLICK_TARGET = "click_target";

    private final String appId;
    private final DeviceHelper deviceHelper;
    private final ExperimentOperations experimentOperations;

    @Inject
    public EventLoggerUrlBuilder(Resources resources, ExperimentOperations experimentOperations, DeviceHelper deviceHelper) {
        this.appId = resources.getString(R.string.app_id);
        this.experimentOperations = experimentOperations;
        this.deviceHelper = deviceHelper;
    }

    public String buildForVisualAdImpression(AudioAdCompanionImpressionEvent event) {
        return buildUriForPath("impression", event.getTimeStamp())
                .appendQueryParameter(USER, event.getUserUrn().toString())
                .appendQueryParameter(AD_URN, event.getAdsWizzUrn())
                .appendQueryParameter(IMPRESSION_NAME, "companion_display")
                .appendQueryParameter(IMPRESSION_OBJECT, event.getTrackUrn().toString())
                .appendQueryParameter(MONETIZATION_TYPE, "audio_ad")
                .appendQueryParameter(MONETIZED_OBJECT, event.getMonetizedTrackUrn().toString())
                .appendQueryParameter(EXTERNAL_MEDIA, event.getExternalMediaUrn().toString())
                .toString();
    }

    public String buildForAdImpression(PlaybackSessionEvent event) {
        final Uri.Builder builder = buildUriForPath("impression", event.getTimeStamp());
        builder.appendQueryParameter(USER, event.get(PlaybackSessionEvent.KEY_USER_URN));
        builder.appendQueryParameter(AD_URN, event.get(PlaybackSessionEvent.KEY_AD_URN));
        builder.appendQueryParameter(IMPRESSION_OBJECT, event.get(PlaybackSessionEvent.KEY_TRACK_URN));
        builder.appendQueryParameter(MONETIZED_OBJECT, event.get(PlaybackSessionEvent.KEY_MONETIZED_URN));
        builder.appendQueryParameter(IMPRESSION_NAME, "audio_ad_impression");
        builder.appendQueryParameter(MONETIZATION_TYPE, "audio_ad");
        return builder.toString();
    }

    public String buildForAudioEvent(PlaybackSessionEvent event) {
        // add basic UI event params
        // cf. https://github.com/soundcloud/eventgateway-schemas/blob/v0/doc/base-ui-event.md#android
        final Uri.Builder builder = buildUriForPath("audio", event.getTimeStamp());
        builder.appendQueryParameter(USER, event.get(PlaybackSessionEvent.KEY_USER_URN));
        // add a/b experiment params
        for (Map.Entry<String, Integer> entry : experimentOperations.getTrackingParams().entrySet()) {
            builder.appendQueryParameter(entry.getKey(), String.valueOf(entry.getValue()));
        }

        // audio event specific params
        // cf. https://github.com/soundcloud/eventgateway-schemas/blob/v0/doc/audio.md#android
        builder.appendQueryParameter(ACTION, event.isPlayEvent() ? "play" : "stop");
        builder.appendQueryParameter(DURATION, String.valueOf(event.getDuration()));
        // EventLogger v0 requires us to pass URNs in the legacy format
        builder.appendQueryParameter(SOUND, event.get(PlaybackSessionEvent.KEY_TRACK_URN).replaceFirst(":tracks:", ":sounds:"));

        final String trackPolicy = event.get(PlaybackSessionEvent.KEY_POLICY);
        if (trackPolicy != null && !event.isAd()) {
            builder.appendQueryParameter(POLICY, trackPolicy);
        }

        TrackSourceInfo trackSourceInfo = event.getTrackSourceInfo();

        if (trackSourceInfo.getIsUserTriggered()) {
            builder.appendQueryParameter(TRIGGER, "manual");
        } else {
            builder.appendQueryParameter(TRIGGER, "auto");
        }
        builder.appendQueryParameter(PAGE_NAME, formatOriginUrl(trackSourceInfo.getOriginScreen()));
        builder.appendQueryParameter(PROTOCOL, event.get(PlaybackSessionEvent.KEY_PROTOCOL));

        if (trackSourceInfo.hasSource()) {
            builder.appendQueryParameter(SOURCE, trackSourceInfo.getSource());
            builder.appendQueryParameter(SOURCE_VERSION, trackSourceInfo.getSourceVersion());
        }

        if (trackSourceInfo.isFromPlaylist()) {
            builder.appendQueryParameter(PLAYLIST_ID, String.valueOf(trackSourceInfo.getPlaylistUrn().getNumericId()));
            builder.appendQueryParameter(PLAYLIST_POSITION, String.valueOf(trackSourceInfo.getPlaylistPosition()));
        }

        // audio ad specific params
        // cf. https://github.com/soundcloud/eventgateway-schemas/blob/v0/doc/audio-ads-tracking.md#audio
        if (event.isAd()) {
            builder.appendQueryParameter(MONETIZATION_TYPE, "audio_ad");
            builder.appendQueryParameter(AD_URN, event.get(PlaybackSessionEvent.KEY_AD_URN));
            builder.appendQueryParameter(MONETIZED_OBJECT, event.get(PlaybackSessionEvent.KEY_MONETIZED_URN));
        }

        return builder.build().toString();
    }

    public String buildForAudioPerformanceEvent(PlaybackPerformanceEvent event) {
        final Uri.Builder builder = buildUriForPath("audio_performance", event.getTimeStamp());
        return builder
                .appendQueryParameter(LATENCY, String.valueOf(event.getMetricValue()))
                .appendQueryParameter(PROTOCOL, event.getProtocol().getValue())
                .appendQueryParameter(PLAYER_TYPE, event.getPlayerType().getValue())
                .appendQueryParameter(TYPE, getPerformanceEventType(event.getMetric()))
                .appendQueryParameter(HOST, event.getCdnHost())
                .appendQueryParameter(USER, event.getUserUrn().toString())
                .appendQueryParameter(CONNECTION_TYPE, event.getConnectionType().getValue())
                .build().toString();
    }

    public String buildForAudioErrorEvent(PlaybackErrorEvent event) {
        final Uri.Builder builder = buildUriForPath("audio_error", event.getTimestamp());
        return builder
                .appendQueryParameter(PROTOCOL, event.getProtocol().getValue())
                .appendQueryParameter(OS, deviceHelper.getUserAgent())
                .appendQueryParameter(BITRATE, event.getBitrate())
                .appendQueryParameter(FORMAT, event.getFormat())
                .appendQueryParameter(URL, event.getCdnHost())
                .appendQueryParameter(ERROR_CODE, event.getCategory())
                .build().toString();
    }

    public String buildForClick(UIEvent event) {
        final Uri.Builder builder = buildUriForPath("click", event.getTimestamp());

        final Map<String, String> eventAttributes = event.getAttributes();
        switch (event.getKind()) {
            case AUDIO_AD_CLICK:
                addCommonAudioAdParams(builder, eventAttributes);
                builder.appendQueryParameter(CLICK_NAME, "clickthrough::companion_display");
                builder.appendQueryParameter(CLICK_TARGET, eventAttributes.get("ad_click_url"));
                break;
            case SKIP_AUDIO_AD_CLICK:
                addCommonAudioAdParams(builder, eventAttributes);
                builder.appendQueryParameter(CLICK_NAME, "ad::skip");
                break;
            default:
                break;
        }

        return builder.toString();
    }

    private void addCommonAudioAdParams(Uri.Builder builder, Map<String, String> eventAttributes) {
        builder.appendQueryParameter(AD_URN, eventAttributes.get("ad_urn"));
        builder.appendQueryParameter(MONETIZATION_TYPE, "audio_ad");
        builder.appendQueryParameter(MONETIZED_OBJECT, eventAttributes.get("ad_monetized_urn"));
        builder.appendQueryParameter(CLICK_OBJECT, eventAttributes.get("ad_track_urn"));
        builder.appendQueryParameter(EXTERNAL_MEDIA, eventAttributes.get("ad_image_url"));
    }

    // This variant generates a click event from a playback event, as the spec requires this in some cases
    public String buildForAdFinished(PlaybackSessionEvent event) {
        final Uri.Builder builder = buildUriForPath("click", event.getTimeStamp());

        builder.appendQueryParameter(AD_URN, event.get(PlaybackSessionEvent.KEY_AD_URN));
        builder.appendQueryParameter(MONETIZATION_TYPE, "audio_ad");
        builder.appendQueryParameter(MONETIZED_OBJECT, event.get(PlaybackSessionEvent.KEY_MONETIZED_URN));
        builder.appendQueryParameter(CLICK_OBJECT, event.get(PlaybackSessionEvent.KEY_TRACK_URN));
        builder.appendQueryParameter(EXTERNAL_MEDIA, event.get(PlaybackSessionEvent.KEY_AD_ARTWORK));
        builder.appendQueryParameter(CLICK_NAME, "ad::finish");

        return builder.toString();
    }

    private Uri.Builder buildUriForPath(String path, long timestamp) {
        final Uri.Builder builder = Uri.parse(ENDPOINT).buildUpon().appendPath(path);
        builder.appendQueryParameter(CLIENT_ID, appId);
        builder.appendQueryParameter(ANONYMOUS_ID, deviceHelper.getUniqueDeviceID());
        builder.appendQueryParameter(TIMESTAMP, String.valueOf(timestamp));
        return builder;
    }

    private String formatOriginUrl(String originUrl) {
        return originUrl.toLowerCase(Locale.ENGLISH).replace(" ", "_");
    }

    private String getPerformanceEventType(int type) {
        switch (type) {
            case PlaybackPerformanceEvent.METRIC_TIME_TO_PLAY:
                return "play";
            case PlaybackPerformanceEvent.METRIC_TIME_TO_BUFFER:
                return "buffer";
            case PlaybackPerformanceEvent.METRIC_TIME_TO_PLAYLIST:
                return "playlist";
            case PlaybackPerformanceEvent.METRIC_TIME_TO_SEEK:
                return "seek";
            case PlaybackPerformanceEvent.METRIC_FRAGMENT_DOWNLOAD_RATE:
                return "fragmentRate";
            default:
                throw new IllegalArgumentException("Unexpected metric type " + type);
        }
    }

}
