package com.soundcloud.android.analytics.eventlogger;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Strings;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.AdTrackingKeys;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.VisualAdImpressionEvent;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.utils.DeviceHelper;

import android.content.res.Resources;
import android.net.Uri;

import javax.inject.Inject;
import java.util.Locale;
import java.util.Map;

public class EventLoggerUrlDataBuilder extends EventLoggerDataBuilder {

    private final String endpoint;

    @Inject
    public EventLoggerUrlDataBuilder(Resources resources,
                                     ExperimentOperations experimentOperations,
                                     DeviceHelper deviceHelper,
                                     AccountOperations accountOperations) {
        super(resources, experimentOperations, deviceHelper, accountOperations);
        this.endpoint = resources.getString(R.string.event_logger_base_url);

    }

    public String build(UIEvent event) {
        final Uri.Builder builder;

        switch (event.getKind()) {
            case UIEvent.KIND_AUDIO_AD_CLICK:
                builder = audioAddClickThrough(event)
                        .appendQueryParameter(EventLoggerParam.MONETIZATION_TYPE, MONETIZATION_TYPE_AUDIO_AD)
                        .appendQueryParameter(EventLoggerParam.CLICK_NAME, "clickthrough::companion_display");
                break;
            case UIEvent.KIND_SKIP_AUDIO_AD_CLICK:
                builder = audioAddClick(event)
                        .appendQueryParameter(EventLoggerParam.MONETIZATION_TYPE, MONETIZATION_TYPE_AUDIO_AD)
                        .appendQueryParameter(EventLoggerParam.CLICK_NAME, "ad::skip");
                break;
            default:
                builder = click(event);
                break;
        }
        return builder.toString();
    }

    public String build(ScreenEvent event) {
        final Uri.Builder builder = buildUriForPath(PAGEVIEW_EVENT, event.getTimeStamp())
                .appendQueryParameter(EventLoggerParam.USER, accountOperations.getLoggedInUserUrn().toString())
                .appendQueryParameter(EventLoggerParam.PAGE_NAME, event.get(ScreenEvent.KEY_SCREEN));

        addExperimentParams(builder);
        return builder.toString();
    }

    public String build(VisualAdImpressionEvent event) {
        return audioAdImpression(event)
                .appendQueryParameter(EventLoggerParam.EXTERNAL_MEDIA, event.get(AdTrackingKeys.KEY_AD_ARTWORK_URL))
                .appendQueryParameter(EventLoggerParam.IMPRESSION_NAME, "companion_display")
                .appendQueryParameter(EventLoggerParam.MONETIZATION_TYPE, MONETIZATION_TYPE_AUDIO_AD)
                .toString();
    }

    public String build(AdOverlayTrackingEvent event) {
        checkArgument(
                event.getKind().equals(AdOverlayTrackingEvent.KIND_CLICK) || event.getKind().equals(AdOverlayTrackingEvent.KIND_IMPRESSION),
                "Unknown LeaveBehindTrackingEvent kind: " + event.getKind());

        if (event.getKind().equals(AdOverlayTrackingEvent.KIND_CLICK)) {
            return audioAddClickThrough(event)
                    .appendQueryParameter(EventLoggerParam.CLICK_NAME, "clickthrough::" + event.get(AdTrackingKeys.KEY_AD_TYPE))
                    .appendQueryParameter(EventLoggerParam.MONETIZATION_TYPE, event.get(AdTrackingKeys.KEY_MONETIZATION_TYPE))
                    .toString();
        } else {
            return audioAdImpression(event)
                    .appendQueryParameter(EventLoggerParam.EXTERNAL_MEDIA, event.get(AdTrackingKeys.KEY_AD_ARTWORK_URL))
                    .appendQueryParameter(EventLoggerParam.IMPRESSION_NAME, event.get(AdTrackingKeys.KEY_AD_TYPE))
                    .appendQueryParameter(EventLoggerParam.MONETIZATION_TYPE, event.get(AdTrackingKeys.KEY_MONETIZATION_TYPE))
                    .toString();
        }
    }

    public String buildForAudioAdImpression(PlaybackSessionEvent event) {
        return audioAdImpression(event)
                .appendQueryParameter(EventLoggerParam.IMPRESSION_NAME, "audio_ad_impression")
                .appendQueryParameter(EventLoggerParam.MONETIZATION_TYPE, MONETIZATION_TYPE_AUDIO_AD)
                .toString();
    }

    // This variant generates a click event from a playback event, as the spec requires this in some cases
    public String buildForAdFinished(PlaybackSessionEvent event) {
        return audioAddClick(event)
                .appendQueryParameter(EventLoggerParam.CLICK_NAME, "ad::finish")
                .appendQueryParameter(EventLoggerParam.MONETIZATION_TYPE, MONETIZATION_TYPE_AUDIO_AD)
                .toString();
    }

    private Uri.Builder audioAddClickThrough(TrackingEvent event) {
        return audioAddClick(event)
                .appendQueryParameter(EventLoggerParam.CLICK_TARGET, event.get(AdTrackingKeys.KEY_CLICK_THROUGH_URL));
    }

    private Uri.Builder audioAddClick(TrackingEvent event) {
        final Uri.Builder builder = addCommonAudioAdParams(click(event), event)
                .appendQueryParameter(EventLoggerParam.EXTERNAL_MEDIA, event.get(AdTrackingKeys.KEY_AD_ARTWORK_URL));
        addClickObjectIfIncluded(event, builder);
        return builder;
    }

    private void addClickObjectIfIncluded(TrackingEvent event, Uri.Builder builder) {
        final String clickObjectUrn = event.get(AdTrackingKeys.KEY_CLICK_OBJECT_URN);
        if (!Strings.isNullOrEmpty(clickObjectUrn)) {
            builder.appendQueryParameter(EventLoggerParam.CLICK_OBJECT, clickObjectUrn);
        }
    }

    private Uri.Builder audioAdImpression(TrackingEvent event) {
        return addCommonAudioAdParams(impression(event), event)
                .appendQueryParameter(EventLoggerParam.IMPRESSION_OBJECT, event.get(AdTrackingKeys.KEY_AD_TRACK_URN));
    }

    private Uri.Builder impression(TrackingEvent event) {
        return buildUriForPath("impression", event);
    }

    private Uri.Builder click(TrackingEvent event) {
        return buildUriForPath("click", event);
    }

    private Uri.Builder addCommonAudioAdParams(Uri.Builder builder, TrackingEvent event) {
        return builder
                .appendQueryParameter(EventLoggerParam.AD_URN, event.get(AdTrackingKeys.KEY_AD_URN))
                .appendQueryParameter(EventLoggerParam.MONETIZED_OBJECT, event.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN));
    }

    public String buildForAudioEvent(PlaybackSessionEvent event) {
        // add basic UI event params
        // cf. https://github.com/soundcloud/eventgateway-schemas/blob/v0/doc/base-ui-event.md#android
        final Uri.Builder builder = buildUriForPath("audio", event.getTimeStamp());
        addExperimentParams(builder);
        builder.appendQueryParameter(EventLoggerParam.USER, event.get(PlaybackSessionEvent.KEY_USER_URN));

        // audio event specific params
        // cf. https://github.com/soundcloud/eventgateway-schemas/blob/v0/doc/audio.md#android
        if (event.isPlayEvent()) {
            builder.appendQueryParameter(EventLoggerParam.ACTION, "play");
        } else {
            builder.appendQueryParameter(EventLoggerParam.ACTION, "stop");
            builder.appendQueryParameter(EventLoggerParam.REASON, getStopReason(event));
        }
        builder.appendQueryParameter(EventLoggerParam.DURATION, String.valueOf(event.getDuration()));
        builder.appendQueryParameter(EventLoggerParam.SOUND, getLegacyTrackUrn(event.get(PlaybackSessionEvent.KEY_TRACK_URN)));

        final String trackPolicy = event.get(PlaybackSessionEvent.KEY_POLICY);
        if (trackPolicy != null && !event.isAd()) {
            builder.appendQueryParameter(EventLoggerParam.POLICY, trackPolicy);
        }

        TrackSourceInfo trackSourceInfo = event.getTrackSourceInfo();

        builder.appendQueryParameter(EventLoggerParam.TRIGGER, getTrigger(trackSourceInfo));
        builder.appendQueryParameter(EventLoggerParam.PAGE_NAME, formatOriginUrl(trackSourceInfo.getOriginScreen()));
        builder.appendQueryParameter(EventLoggerParam.PROTOCOL, event.get(PlaybackSessionEvent.KEY_PROTOCOL));
        builder.appendQueryParameter(EventLoggerParam.PLAYER_TYPE, event.get(PlaybackSessionEvent.PLAYER_TYPE));
        builder.appendQueryParameter(EventLoggerParam.CONNECTION_TYPE, event.get(PlaybackSessionEvent.CONNECTION_TYPE));

        if (trackSourceInfo.hasSource()) {
            builder.appendQueryParameter(EventLoggerParam.SOURCE, trackSourceInfo.getSource());
            builder.appendQueryParameter(EventLoggerParam.SOURCE_VERSION, trackSourceInfo.getSourceVersion());
        }

        if (trackSourceInfo.isFromPlaylist()) {
            builder.appendQueryParameter(EventLoggerParam.PLAYLIST_ID, String.valueOf(trackSourceInfo.getPlaylistUrn().getNumericId()));
            builder.appendQueryParameter(EventLoggerParam.PLAYLIST_POSITION, String.valueOf(trackSourceInfo.getPlaylistPosition()));
        }

        // audio ad specific params
        // cf. https://github.com/soundcloud/eventgateway-schemas/blob/v0/doc/audio-ads-tracking.md#audio
        if (event.isAd()) {
            builder.appendQueryParameter(EventLoggerParam.MONETIZATION_TYPE, MONETIZATION_TYPE_AUDIO_AD);
            builder.appendQueryParameter(EventLoggerParam.AD_URN, event.get(AdTrackingKeys.KEY_AD_URN));
            builder.appendQueryParameter(EventLoggerParam.MONETIZED_OBJECT, event.get(AdTrackingKeys.KEY_MONETIZABLE_TRACK_URN));
        }

        return builder.build().toString();
    }

    private void addExperimentParams(Uri.Builder builder) {
        // add a/b experiment params
        for (Map.Entry<String, Integer> entry : experimentOperations.getTrackingParams().entrySet()) {
            builder.appendQueryParameter(entry.getKey(), String.valueOf(entry.getValue()));
        }
    }

    private String getStopReason(PlaybackSessionEvent eventData) {
        switch (eventData.getStopReason()) {
            case PlaybackSessionEvent.STOP_REASON_PAUSE:
                return "pause";
            case PlaybackSessionEvent.STOP_REASON_BUFFERING:
                return "buffering";
            case PlaybackSessionEvent.STOP_REASON_SKIP:
                return "skip";
            case PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED:
                return "track_finished";
            case PlaybackSessionEvent.STOP_REASON_END_OF_QUEUE:
                return "end_of_content";
            case PlaybackSessionEvent.STOP_REASON_NEW_QUEUE:
                return "context_change";
            case PlaybackSessionEvent.STOP_REASON_ERROR:
                return "playback_error";
            default:
                throw new IllegalArgumentException("Unexpected stop reason : " + eventData.getStopReason());
        }
    }

    public String build(PlaybackPerformanceEvent event) {
        final Uri.Builder builder = buildUriForPath("audio_performance", event.getTimeStamp());
        return builder
                .appendQueryParameter(EventLoggerParam.LATENCY, String.valueOf(event.getMetricValue()))
                .appendQueryParameter(EventLoggerParam.PROTOCOL, event.getProtocol().getValue())
                .appendQueryParameter(EventLoggerParam.PLAYER_TYPE, event.getPlayerType().getValue())
                .appendQueryParameter(EventLoggerParam.TYPE, getPerformanceEventType(event.getMetric()))
                .appendQueryParameter(EventLoggerParam.HOST, event.getCdnHost())
                .appendQueryParameter(EventLoggerParam.USER, event.getUserUrn().toString())
                .appendQueryParameter(EventLoggerParam.CONNECTION_TYPE, event.getConnectionType().getValue())
                .build().toString();
    }

    public String build(PlaybackErrorEvent event) {
        final Uri.Builder builder = buildUriForPath("audio_error", event.getTimestamp());
        return builder
                .appendQueryParameter(EventLoggerParam.PROTOCOL, event.getProtocol().getValue())
                .appendQueryParameter(EventLoggerParam.OS, deviceHelper.getUserAgent())
                .appendQueryParameter(EventLoggerParam.BITRATE, event.getBitrate())
                .appendQueryParameter(EventLoggerParam.FORMAT, event.getFormat())
                .appendQueryParameter(EventLoggerParam.URL, event.getCdnHost())
                .appendQueryParameter(EventLoggerParam.ERROR_CODE, event.getCategory())
                .appendQueryParameter(EventLoggerParam.CONNECTION_TYPE, event.getConnectionType().getValue())
                .build().toString();
    }

    @Override
    public String build(SearchEvent event) {
        // not implementing
        return null;
    }

    private Uri.Builder buildUriForPath(String path, long timestamp) {
        return Uri.parse(endpoint).buildUpon().appendPath(path)
                .appendQueryParameter(EventLoggerParam.CLIENT_ID, appId)
                .appendQueryParameter(EventLoggerParam.ANONYMOUS_ID, deviceHelper.getUDID())
                .appendQueryParameter(EventLoggerParam.TIMESTAMP, String.valueOf(timestamp));
    }

    private Uri.Builder buildUriForPath(String path, TrackingEvent event) {
        return buildUriForPath(path, event.getTimeStamp())
                .appendQueryParameter(EventLoggerParam.USER, formatOriginUrl(event.get(AdTrackingKeys.KEY_USER_URN)))
                .appendQueryParameter(EventLoggerParam.PAGE_NAME, formatOriginUrl(event.get(AdTrackingKeys.KEY_ORIGIN_SCREEN)));
    }

    private String formatOriginUrl(String originUrl) {
        return originUrl.toLowerCase(Locale.ENGLISH).replace(" ", "_");
    }

}
