package com.soundcloud.android.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
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
    public void shouldConvertToTrackItem() {
        ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);

        TrackItem trackItem = TrackItem.from(apiTrack);

        assertThat(trackItem.getUrn()).isEqualTo(apiTrack.getUrn());
        assertThat(trackItem.getTitle()).isEqualTo(apiTrack.getTitle());
        assertThat(trackItem.getCreatedAt()).isEqualTo(apiTrack.getCreatedAt());
        assertThat(trackItem.getSnippetDuration()).isEqualTo(apiTrack.getSnippetDuration());
        assertThat(trackItem.getFullDuration()).isEqualTo(apiTrack.getFullDuration());
        assertThat(trackItem.isPrivate()).isEqualTo(apiTrack.isPrivate());
        assertThat(trackItem.getWaveformUrl()).isEqualTo(apiTrack.getWaveformUrl());
        assertThat(trackItem.getPermalinkUrl()).isEqualTo(apiTrack.getPermalinkUrl());
        assertThat(trackItem.isMonetizable()).isEqualTo(apiTrack.isMonetizable());
        assertThat(trackItem.getPolicy()).isEqualTo(apiTrack.getPolicy());
        assertThat(trackItem.getPlayCount()).isEqualTo(apiTrack.getStats().getPlaybackCount());
        assertThat(trackItem.getCommentsCount()).isEqualTo(apiTrack.getStats().getCommentsCount());
        assertThat(trackItem.getLikesCount()).isEqualTo(apiTrack.getStats().getLikesCount());
        assertThat(trackItem.getRepostCount()).isEqualTo(apiTrack.getStats().getRepostsCount());
        assertThat(trackItem.isSubMidTier()).isEqualTo(apiTrack.isSubMidTier().get());
        assertThat(trackItem.isSubHighTier()).isEqualTo(apiTrack.isSubHighTier().get());
        assertThat(trackItem.getMonetizationModel()).isEqualTo(apiTrack.getMonetizationModel().get());
        assertThat(trackItem.getImageUrlTemplate()).isEqualTo(apiTrack.getImageUrlTemplate());
        assertThat(trackItem.getCreatorName()).isEqualTo(apiTrack.getUserName());
        assertThat(trackItem.getCreatorUrn()).isEqualTo(apiTrack.getUser().getUrn());
    }

}
