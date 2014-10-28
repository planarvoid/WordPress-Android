package com.soundcloud.android.analytics.eventlogger;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Strings;
import com.soundcloud.android.R;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.AdTrackingKeys;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.VisualAdImpressionEvent;
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
    public static final String MONETIZATION_TYPE_AUDIO_AD = "audio_ad";

    private final String appId;
    private final DeviceHelper deviceHelper;
    private final ExperimentOperations experimentOperations;

    @Inject
    public EventLoggerUrlBuilder(Resources resources, ExperimentOperations experimentOperations, DeviceHelper deviceHelper) {
        this.appId = resources.getString(R.string.app_id);
        this.experimentOperations = experimentOperations;
        this.deviceHelper = deviceHelper;
    }

    public String build(UIEvent event) {
        final Uri.Builder builder;

        switch (event.getKind()) {
            case UIEvent.KIND_AUDIO_AD_CLICK:
                builder = audioAddClickThrough(event)
                        .appendQueryParameter(MONETIZATION_TYPE, MONETIZATION_TYPE_AUDIO_AD)
                        .appendQueryParameter(CLICK_NAME, "clickthrough::companion_display");
                break;
            case UIEvent.KIND_SKIP_AUDIO_AD_CLICK:
                builder = audioAddClick(event)
                        .appendQueryParameter(MONETIZATION_TYPE, MONETIZATION_TYPE_AUDIO_AD)
                        .appendQueryParameter(CLICK_NAME, "ad::skip");
                break;
            default:
                builder = click(event);
                break;
        }

        return builder.toString();
    }

    public String build(VisualAdImpressionEvent event) {
        return audioAdImpression(event)
                .appendQueryParameter(EXTERNAL_MEDIA, event.get(AdTrackingKeys.KEY_AD_ARTWORK_URL))
                .appendQueryParameter(IMPRESSION_NAME, "companion_display")
                .appendQueryParameter(MONETIZATION_TYPE, MONETIZATION_TYPE_AUDIO_AD)
                .toString();
    }

    public String build(AdOverlayTrackingEvent event) {
        checkArgument(
                event.getKind().equals(AdOverlayTrackingEvent.KIND_CLICK) || event.getKind().equals(AdOverlayTrackingEvent.KIND_IMPRESSION),
                "Unknown LeaveBehindTrackingEvent kind: " + event.getKind());

        if (event.getKind().equals(AdOverlayTrackingEvent.KIND_CLICK)) {
            return audioAddClickThrough(event)
                    .appendQueryParameter(CLICK_NAME, "clickthrough::" + event.get(AdTrackingKeys.KEY_AD_TYPE))
                    .appendQueryParameter(MONETIZATION_TYPE, event.get(AdTrackingKeys.KEY_MONETIZATION_TYPE))
                    .toString();
        } else {
            return audioAdImpression(event)
                    .appendQueryParameter(EXTERNAL_MEDIA, event.get(AdTrackingKeys.KEY_AD_ARTWORK_URL))
                    .appendQueryParameter(IMPRESSION_NAME, event.get(AdTrackingKeys.KEY_AD_TYPE))
                    .appendQueryParameter(MONETIZATION_TYPE, event.get(AdTrackingKeys.KEY_MONETIZATION_TYPE))
                    .toString();
        }
    }

    public String buildForAudioAdImpression(PlaybackSessionEvent event) {
        return audioAdImpression(event)
                .appendQueryParameter(IMPRESSION_NAME, "audio_ad_impression")
                .appendQueryParameter(MONETIZATION_TYPE, MONETIZATION_TYPE_AUDIO_AD)
                .toString();
    }

    // This variant generates a click event from a playback event, as the spec requires this in some cases
    public String buildForAdFinished(PlaybackSessionEvent event) {
        return audioAddClick(event)
                .appendQueryParameter(CLICK_NAME, "ad::finish")
                .appendQueryParameter(MONETIZATION_TYPE, MONETIZATION_TYPE_AUDIO_AD)
                .toString();
    }

    private Uri.Builder audioAddClickThrough(TrackingEvent event) {
        return audioAddClick(event)
                .appendQueryParameter(CLICK_TARGET, event.get(AdTrackingKeys.KEY_CLICK_THROUGH_URL));
    }

    private Uri.Builder audioAddClick(TrackingEvent event) {
        final Uri.Builder builder = addCommonAudioAdParams(click(event), event)
                .appendQueryParameter(EXTERNAL_MEDIA, event.get(AdTrackingKeys.KEY_AD_ARTWORK_URL));
        addClickObjectIfIncluded(event, builder);
        return builder;
    }

    private void addClickObjectIfIncluded(TrackingEvent event, Uri.Builder builder) {
        final String clickObjectUrn = event.get(AdTrackingKeys.KEY_CLICK_OBJECT_URN);
        if (!Strings.isNullOrEmpty(clickObjectUrn)) {
            builder.appendQueryParameter(CLICK_OBJECT, clickObjectUrn);
        }
    }

    private Uri.Builder audioAdImpression(TrackingEvent event) {
        return addCommonAudioAdParams(impression(event), event)
                .appendQueryParameter(IMPRESSION_OBJECT, event.get(AdTrackingKeys.KEY_AD_TRACK_URN));
    }

    private Uri.Builder impression(TrackingEvent event) {
        return buildUriForPath("impression", event);
    }

    private Uri.Builder click(TrackingEvent event) {
        return buildUriForPath("click", event);
    }

    private Uri.Builder addCommonAudioAdParams(Uri.Builder builder, TrackingEvent event) {
        return builder
                .appendQueryParameter(AD_URN, event.get(AdTrackingKeys.KEY_AD_URN))
                .appendQueryParameter(MONETIZED_OBJECT, event.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN));
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
            builder.appendQueryParameter(MONETIZATION_TYPE, MONETIZATION_TYPE_AUDIO_AD);
            builder.appendQueryParameter(AD_URN, event.get(AdTrackingKeys.KEY_AD_URN));
            builder.appendQueryParameter(MONETIZED_OBJECT, event.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN));
        }

        return builder.build().toString();
    }

    public String build(PlaybackPerformanceEvent event) {
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

    public String build(PlaybackErrorEvent event) {
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

    private Uri.Builder buildUriForPath(String path, long timestamp) {
        return Uri.parse(ENDPOINT).buildUpon().appendPath(path)
                .appendQueryParameter(CLIENT_ID, appId)
                .appendQueryParameter(ANONYMOUS_ID, deviceHelper.getUniqueDeviceID())
                .appendQueryParameter(TIMESTAMP, String.valueOf(timestamp));
    }

    private Uri.Builder buildUriForPath(String path, TrackingEvent event) {
        return buildUriForPath(path, event.getTimeStamp())
                .appendQueryParameter(USER, formatOriginUrl(event.get(AdTrackingKeys.KEY_USER_URN)))
                .appendQueryParameter(PAGE_NAME, formatOriginUrl(event.get(AdTrackingKeys.KEY_ORIGIN_SCREEN)));
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
