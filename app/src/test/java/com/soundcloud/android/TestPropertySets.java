package com.soundcloud.android;

import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.propeller.PropertySet;

import java.util.Date;

public abstract class TestPropertySets {

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

    public static PropertySet expectedAudioAdForAnalytics(TrackUrn audioAdTrack) {
        return PropertySet.from(
                AdProperty.AD_URN.bind("adswizz:ads:456"),
                AdProperty.MONETIZABLE_TRACK_URN.bind(audioAdTrack)
        );
    }
}
