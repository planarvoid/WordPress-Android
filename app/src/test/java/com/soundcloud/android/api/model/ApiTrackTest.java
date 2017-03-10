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
        TrackItem trackItem = ModelFixtures.trackItem(apiTrack);

        assertThat(trackItem.getUrn()).isEqualTo(apiTrack.getUrn());
        assertThat(trackItem.title()).isEqualTo(apiTrack.getTitle());
        assertThat(trackItem.getCreatedAt()).isEqualTo(apiTrack.getCreatedAt());
        assertThat(trackItem.snippetDuration()).isEqualTo(apiTrack.getSnippetDuration());
        assertThat(trackItem.fullDuration()).isEqualTo(apiTrack.getFullDuration());
        assertThat(trackItem.isPrivate()).isEqualTo(apiTrack.isPrivate());
        assertThat(trackItem.waveformUrl()).isEqualTo(apiTrack.getWaveformUrl());
        assertThat(trackItem.permalinkUrl()).isEqualTo(apiTrack.getPermalinkUrl());
        assertThat(trackItem.policy()).isEqualTo(apiTrack.getPolicy());
        assertThat(trackItem.playCount()).isEqualTo(apiTrack.getStats().getPlaybackCount());
        assertThat(trackItem.commentsCount()).isEqualTo(apiTrack.getStats().getCommentsCount());
        assertThat(trackItem.likesCount()).isEqualTo(apiTrack.getStats().getLikesCount());
        assertThat(trackItem.repostsCount()).isEqualTo(apiTrack.getStats().getRepostsCount());
        assertThat(trackItem.isSubMidTier()).isEqualTo(apiTrack.isSubMidTier().get());
        assertThat(trackItem.isSubHighTier()).isEqualTo(apiTrack.isSubHighTier().get());
        assertThat(trackItem.monetizationModel()).isEqualTo(apiTrack.getMonetizationModel().get());
        assertThat(trackItem.getImageUrlTemplate()).isEqualTo(apiTrack.getImageUrlTemplate());
        assertThat(trackItem.creatorName()).isEqualTo(apiTrack.getUserName());
        assertThat(trackItem.creatorUrn()).isEqualTo(apiTrack.getUser().getUrn());
    }

}
