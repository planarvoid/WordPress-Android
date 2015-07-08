package com.soundcloud.android.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import org.junit.Test;

public class ApiTrackTest extends AndroidUnitTest {

    @Test
    public void shouldDefineEqualityBasedOnUrn() {
        ApiTrack apiTrack1 = ModelFixtures.create(ApiTrack.class);
        ApiTrack apiTrack2 = ModelFixtures.create(ApiTrack.class);
        apiTrack2.setUrn(apiTrack1.getUrn());

        assertThat(apiTrack1).isEqualTo(apiTrack2);
    }

    @Test
    public void shouldDefineHashCodeBasedOnUrn() {
        ApiTrack apiTrack1 = ModelFixtures.create(ApiTrack.class);
        ApiTrack apiTrack2 = ModelFixtures.create(ApiTrack.class);
        apiTrack2.setUrn(apiTrack1.getUrn());

        assertThat(apiTrack1.hashCode()).isEqualTo(apiTrack2.hashCode());
    }

    @Test
    public void shouldConvertToPropertySet() {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);

        PropertySet propertySet = track.toPropertySet();

        assertThat(propertySet.get(TrackProperty.URN)).isEqualTo(track.getUrn());
        assertThat(propertySet.get(TrackProperty.TITLE)).isEqualTo(track.getTitle());
        assertThat(propertySet.get(TrackProperty.CREATED_AT)).isEqualTo(track.getCreatedAt());
        assertThat(propertySet.get(TrackProperty.DURATION)).isEqualTo(track.getDuration());
        assertThat(propertySet.get(TrackProperty.IS_PRIVATE)).isEqualTo(track.isPrivate());
        assertThat(propertySet.get(TrackProperty.WAVEFORM_URL)).isEqualTo(track.getWaveformUrl());
        assertThat(propertySet.get(TrackProperty.PERMALINK_URL)).isEqualTo(track.getPermalinkUrl());
        assertThat(propertySet.get(TrackProperty.MONETIZABLE)).isEqualTo(track.isMonetizable());
        assertThat(propertySet.get(TrackProperty.POLICY)).isEqualTo(track.getPolicy());
        assertThat(propertySet.get(TrackProperty.PLAY_COUNT)).isEqualTo(track.getStats().getPlaybackCount());
        assertThat(propertySet.get(TrackProperty.COMMENTS_COUNT)).isEqualTo(track.getStats().getCommentsCount());
        assertThat(propertySet.get(TrackProperty.LIKES_COUNT)).isEqualTo(track.getStats().getLikesCount());
        assertThat(propertySet.get(TrackProperty.REPOSTS_COUNT)).isEqualTo(track.getStats().getRepostsCount());
        assertThat(propertySet.get(TrackProperty.SUB_MID_TIER)).isEqualTo(track.isSubMidTier().get());
        assertThat(propertySet.get(TrackProperty.MONETIZATION_MODEL)).isEqualTo(track.getMonetizationModel().get());

        assertThat(propertySet.get(TrackProperty.CREATOR_NAME)).isEqualTo(track.getUserName());
        assertThat(propertySet.get(TrackProperty.CREATOR_URN)).isEqualTo(track.getUser().getUrn());
    }

}
