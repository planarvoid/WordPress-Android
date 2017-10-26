package com.soundcloud.android.api.model;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.testsupport.TrackFixtures;
import com.soundcloud.android.tracks.TrackItem;
import org.junit.Test;

public class ApiTrackTest {

    @Test
    public void shouldConvertToTrackItem() {
        ApiTrack apiTrack = TrackFixtures.apiTrack();
        TrackItem trackItem = TrackFixtures.trackItem(apiTrack);

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
        assertThat(trackItem.isSubMidTier()).isEqualTo(apiTrack.getIsSubMidTier().get());
        assertThat(trackItem.isSubHighTier()).isEqualTo(apiTrack.getIsSubHighTier().get());
        assertThat(trackItem.monetizationModel()).isEqualTo(apiTrack.getMonetizationModel().get());
        assertThat(trackItem.getImageUrlTemplate()).isEqualTo(apiTrack.getImageUrlTemplate());
        assertThat(trackItem.creatorName()).isEqualTo(apiTrack.getUserName());
        assertThat(trackItem.creatorUrn()).isEqualTo(apiTrack.getUser().getUrn());
    }

}
