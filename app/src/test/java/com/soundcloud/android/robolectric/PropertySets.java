package com.soundcloud.android.robolectric;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.propeller.PropertySet;

public abstract class PropertySets {

    public static PropertySet expectedTrackDataForAnalytics(TrackUrn trackUrn, String policy, int duration) {
        return PropertySet.from(
                TrackProperty.URN.bind(trackUrn),
                TrackProperty.POLICY.bind(policy),
                PlayableProperty.DURATION.bind(duration)
        );
    }

    public static PropertySet expectedTrackDataForAnalytics(TrackUrn trackUrn) {
        return expectedTrackDataForAnalytics(trackUrn, "allow", 1000);
    }

}
