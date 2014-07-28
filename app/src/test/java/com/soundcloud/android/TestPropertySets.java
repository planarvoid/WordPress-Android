package com.soundcloud.android;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;

public class TestPropertySets {

    public static PropertySet forWidgetTrack() {
        return PropertySet.from(
                TrackProperty.URN.bind(Urn.forTrack(123L)),
                PlayableProperty.TITLE.bind("someone's favorite song"),
                PlayableProperty.CREATOR_NAME.bind("someone's favorite band"),
                PlayableProperty.CREATOR_URN.bind(Urn.forUser(123L)),
                PlayableProperty.IS_LIKED.bind(false)
        );
    }

    public static PropertySet forPlayerTrack() {
        return PropertySet.from(
                TrackProperty.URN.bind(Urn.forTrack(123L)),
                TrackProperty.WAVEFORM_URL.bind("http://waveform.url"),
                PlayableProperty.TITLE.bind("dubstep anthem"),
                PlayableProperty.CREATOR_NAME.bind("squirlex"),
                PlayableProperty.DURATION.bind(123456),
                PlayableProperty.IS_LIKED.bind(true),
                PlayableProperty.LIKES_COUNT.bind(1),
                PlayableProperty.PERMALINK_URL.bind("http://permalink.url"),
                PlayableProperty.IS_PRIVATE.bind(false)
        );
    }

}
