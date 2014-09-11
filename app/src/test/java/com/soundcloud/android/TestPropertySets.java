package com.soundcloud.android;

import static com.google.common.collect.Lists.newArrayList;

import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.propeller.PropertySet;

import android.net.Uri;

import java.util.Date;

public abstract class TestPropertySets {
    public static PropertySet audioAdProperties(TrackUrn monetizedTrack) {
        return PropertySet.from(
                AdProperty.AD_URN.bind("advertisement:123"),
                AdProperty.MONETIZABLE_TRACK_URN.bind(monetizedTrack),
                AdProperty.ARTWORK.bind(Uri.parse("http:a//d.artwork.url")),
                AdProperty.CLICK_THROUGH_LINK.bind(Uri.parse("http://ad.click.through.url")),
                AdProperty.DEFAULT_TEXT_COLOR.bind("#000000"),
                AdProperty.DEFAULT_BACKGROUND_COLOR.bind("#FFFFF"),
                AdProperty.PRESSED_TEXT_COLOR.bind("#111111"),
                AdProperty.PRESSED_BACKGROUND_COLOR.bind("#222222"),
                AdProperty.FOCUSED_TEXT_COLOR.bind("#333333"),
                AdProperty.FOCUSED_BACKGROUND_COLOR.bind("#444444"),
                AdProperty.AUDIO_AD_IMPRESSION_URLS.bind(newArrayList("adswizzUrl", "advertiserUrl")),
                AdProperty.AUDIO_AD_FINISH_URLS.bind(newArrayList("finish1", "finish2")),
                AdProperty.AUDIO_AD_CLICKTHROUGH_URLS.bind(newArrayList("click1", "click2")),
                AdProperty.AUDIO_AD_SKIP_URLS.bind(newArrayList("skip1", "skip2")),
                AdProperty.AUDIO_AD_COMPANION_DISPLAY_IMPRESSION_URLS.bind(newArrayList("visual1", "visual2"))
        );
    }

    public static PropertySet expectedTrackForWidget() {
        return PropertySet.from(
                TrackProperty.URN.bind(Urn.forTrack(123L)),
                PlayableProperty.TITLE.bind("someone's favorite song"),
                PlayableProperty.CREATOR_NAME.bind("someone's favorite band"),
                PlayableProperty.CREATOR_URN.bind(Urn.forUser(123L)),
                PlayableProperty.IS_LIKED.bind(false)
        );
    }

    public static PropertySet expectedTrackForPlayer() {
        return PropertySet.from(
                TrackProperty.URN.bind(Urn.forTrack(123L)),
                TrackProperty.WAVEFORM_URL.bind("http://waveform.url"),
                TrackProperty.PLAY_COUNT.bind(1),
                TrackProperty.COMMENTS_COUNT.bind(1),
                TrackProperty.STREAM_URL.bind("http://stream.url"),
                PlayableProperty.TITLE.bind("dubstep anthem"),
                PlayableProperty.CREATOR_NAME.bind("squirlex"),
                PlayableProperty.CREATOR_URN.bind(Urn.forUser(456L)),
                PlayableProperty.DURATION.bind(123456),
                PlayableProperty.IS_LIKED.bind(true),
                PlayableProperty.LIKES_COUNT.bind(1),
                PlayableProperty.REPOSTS_COUNT.bind(1),
                PlayableProperty.PERMALINK_URL.bind("http://permalink.url"),
                PlayableProperty.IS_PRIVATE.bind(false),
                PlayableProperty.IS_REPOSTED.bind(false),
                PlayableProperty.CREATED_AT.bind(new Date())
        );
    }

    public static PropertySet expectedPrivateTrackForPlayer() {
        return PropertySet.from(
                TrackProperty.URN.bind(Urn.forTrack(123L)),
                PlayableProperty.IS_PRIVATE.bind(true),
                PlayableProperty.TITLE.bind("dubstep anthem"),
                PlayableProperty.CREATOR_NAME.bind(""),
                PlayableProperty.PERMALINK_URL.bind("http://permalink.url"),
                PlayableProperty.IS_REPOSTED.bind(true));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Analytics / Tracking
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static PropertySet expectedTrackForAnalytics(TrackUrn trackUrn, String policy, int duration) {
        return PropertySet.from(
                TrackProperty.URN.bind(trackUrn),
                TrackProperty.POLICY.bind(policy),
                PlayableProperty.DURATION.bind(duration)
        );
    }

    public static PropertySet expectedTrackForAnalytics(TrackUrn trackUrn) {
        return expectedTrackForAnalytics(trackUrn, "allow", 1000);
    }

}
