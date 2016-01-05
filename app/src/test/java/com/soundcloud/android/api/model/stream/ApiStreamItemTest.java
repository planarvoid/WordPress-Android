package com.soundcloud.android.api.model.stream;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.PromotedFixtures;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;

public class ApiStreamItemTest {

    @Test
    public void getTrackWithTrackPostReturnsTrack() throws Exception {
        final ApiStreamTrackPost apiTrackPost = ModelFixtures.create(ApiStreamTrackPost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiTrackPost);
        assertThat(streamItem.getTrack().get()).isEqualTo(apiTrackPost.getApiTrack());
    }

    @Test
    public void getReposterWithTrackPostReturnsAbsent() throws Exception {
        final ApiStreamTrackPost apiTrackPost = ModelFixtures.create(ApiStreamTrackPost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiTrackPost);
        assertThat(streamItem.getReposter()).isEqualTo(Optional.<ApiUser>absent());
    }

    @Test
    public void getCreatedAtWithTrackPostReturnsPostCreatedAtDate() throws Exception {
        final ApiStreamTrackPost apiTrackPost = ModelFixtures.create(ApiStreamTrackPost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiTrackPost);
        assertThat(streamItem.getCreatedAtTime()).isEqualTo(apiTrackPost.getCreatedAtTime());
    }

    @Test
    public void getTrackWithTrackRepostReturnsTrack() throws Exception {
        final ApiStreamTrackRepost apiTrackPost = ModelFixtures.create(ApiStreamTrackRepost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiTrackPost);
        assertThat(streamItem.getTrack().get()).isEqualTo(apiTrackPost.getApiTrack());
    }

    @Test
    public void getReposterWithTrackRepostReturnsReposter() throws Exception {
        final ApiStreamTrackRepost apiTrackRepost = ModelFixtures.create(ApiStreamTrackRepost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiTrackRepost);
        assertThat(streamItem.getReposter().get()).isEqualTo(apiTrackRepost.getReposter());
    }

    @Test
    public void getCreatedAtWithTrackRepostReturnsRepostingDate() throws Exception {
        final ApiStreamTrackRepost apiTrackRepost = ModelFixtures.create(ApiStreamTrackRepost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiTrackRepost);
        assertThat(streamItem.getCreatedAtTime()).isEqualTo(apiTrackRepost.getCreatedAtTime());
    }

    @Test
    public void getPlaylistWithPlaylistPostReturnsPlaylist() throws Exception {
        final ApiStreamPlaylistPost apiPlaylistPost = ModelFixtures.create(ApiStreamPlaylistPost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiPlaylistPost);
        assertThat(streamItem.getPlaylist().get()).isEqualTo(apiPlaylistPost.getApiPlaylist());
    }

    @Test
    public void getReposterWithPlaylistPostReturnsAbsent() throws Exception {
        final ApiStreamPlaylistPost apiPlaylistPost = ModelFixtures.create(ApiStreamPlaylistPost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiPlaylistPost);
        assertThat(streamItem.getReposter()).isEqualTo(Optional.<ApiUser>absent());
    }

    @Test
    public void getCreatedAtWithPlaylistPostReturnsPlaylistCreatedAtDate() throws Exception {
        final ApiStreamPlaylistPost apiPlaylistPost = ModelFixtures.create(ApiStreamPlaylistPost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiPlaylistPost);
        assertThat(streamItem.getCreatedAtTime()).isEqualTo(apiPlaylistPost.getCreatedAtTime());
    }

    @Test
    public void getPlaylistWithPlaylistRepostReturnsPlaylist() throws Exception {
        final ApiStreamPlaylistRepost ApiPlaylistPost = ModelFixtures.create(ApiStreamPlaylistRepost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(ApiPlaylistPost);
        assertThat(streamItem.getPlaylist().get()).isEqualTo(ApiPlaylistPost.getApiPlaylist());
    }

    @Test
    public void getReposterWithPlaylistRepostReturnsReposter() throws Exception {
        final ApiStreamPlaylistRepost ApiPlaylistRepost = ModelFixtures.create(ApiStreamPlaylistRepost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(ApiPlaylistRepost);
        assertThat(streamItem.getReposter().get()).isEqualTo(ApiPlaylistRepost.getReposter());
    }

    @Test
    public void getCreatedAtWithPlaylistRepostReturnsRepostingDate() throws Exception {
        final ApiStreamPlaylistRepost apiPlaylistRepost = ModelFixtures.create(ApiStreamPlaylistRepost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiPlaylistRepost);
        assertThat(streamItem.getCreatedAtTime()).isEqualTo(apiPlaylistRepost.getCreatedAtTime());
    }

    @Test
    public void getCreatedAtWithPromotedTrackReturnsMaxVaue() throws Exception {
        final ApiPromotedTrack apiPromotedTrack = PromotedFixtures.promotedTrackItemWithoutPromoter();
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedTrack);
        assertThat(streamItem.getCreatedAtTime()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    public void getCreatedAtWithPromotedPlaylistReturnsMaxVaue() throws Exception {
        final ApiPromotedPlaylist apiPromotedPlaylist = PromotedFixtures.promotedPlaylistItemWithoutPromoter();
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedPlaylist);
        assertThat(streamItem.getCreatedAtTime()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    public void getPromoterWithPromotedTrackReturnsValidPromoter() throws Exception {
        final ApiUser apiUser = ModelFixtures.create(ApiUser.class);
        final ApiPromotedTrack apiPromotedTrack = PromotedFixtures.promotedTrackItemWithPromoter(apiUser);
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedTrack);
        assertThat(streamItem.getPromoter().get()).isEqualTo(apiUser);
    }

    @Test
    public void getPromoterWithPromotedPlaylistReturnsValidPromoter() throws Exception {
        final ApiUser apiUser = ModelFixtures.create(ApiUser.class);
        final ApiPromotedPlaylist apiPromotedPlaylist = PromotedFixtures.promotedPlaylistItemWithPromoter(apiUser);
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedPlaylist);
        assertThat(streamItem.getPromoter().get()).isEqualTo(apiUser);
    }

    @Test
    public void getPromoterWithPromotedTrackReturnsAbsentPromoter() throws Exception {
        final ApiPromotedTrack apiPromotedTrack = PromotedFixtures.promotedTrackItemWithoutPromoter();
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedTrack);
        assertThat(streamItem.getPromoter().isPresent()).isFalse();
    }

    @Test
    public void getPromoterWithPromotedPlaylistReturnsAbsentPromoter() throws Exception {
        final ApiPromotedPlaylist apiPromotedPlaylist = PromotedFixtures.promotedPlaylistItemWithoutPromoter();
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedPlaylist);
        assertThat(streamItem.getPromoter().isPresent()).isFalse();
    }

    @Test
    public void getPromoterWithTrackPostReturnsAbsentPromoter() throws Exception {
        final ApiStreamItem streamItem = new ApiStreamItem(ModelFixtures.create(ApiStreamTrackPost.class));
        assertThat(streamItem.getPromoter().isPresent()).isFalse();
    }

    @Test
    public void getPromoterWithTrackRepostReturnsAbsentPromoter() throws Exception {
        final ApiStreamItem streamItem = new ApiStreamItem(ModelFixtures.create(ApiStreamTrackRepost.class));
        assertThat(streamItem.getPromoter().isPresent()).isFalse();
    }

    @Test
    public void getPromoterWithPlaylistPostReturnsAbsentPromoter() throws Exception {
        final ApiStreamItem streamItem = new ApiStreamItem(ModelFixtures.create(ApiStreamPlaylistPost.class));
        assertThat(streamItem.getPromoter().isPresent()).isFalse();
    }

    @Test
    public void getPromoterWithPlaylistRepostReturnsAbsentPromoter() throws Exception {
        final ApiStreamItem streamItem = new ApiStreamItem(ModelFixtures.create(ApiStreamPlaylistRepost.class));
        assertThat(streamItem.getPromoter().isPresent()).isFalse();
    }

    @Test
    public void getUrnWithPromotedTrackReturnsUrn() throws Exception {
        final ApiPromotedTrack apiPromotedTrack = PromotedFixtures.promotedTrackItemWithoutPromoter();
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedTrack);
        assertThat(streamItem.getAdUrn().get()).isEqualTo("dfp:ads:123-4567");
    }

    @Test
    public void getUrnWithPromotedPlaylistReturnsUrn() throws Exception {
        final ApiPromotedPlaylist apiPromotedPlaylist = PromotedFixtures.promotedPlaylistItemWithoutPromoter();
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedPlaylist);
        assertThat(streamItem.getAdUrn().get()).isEqualTo("dfp:ads:678-7890");
    }

    @Test
    public void getPromotedUrnWithTrackPostReturnsAbsentPromotedUrn() throws Exception {
        final ApiStreamItem streamItem = new ApiStreamItem(ModelFixtures.create(ApiStreamTrackPost.class));
        assertThat(streamItem.getAdUrn().isPresent()).isFalse();
    }

    @Test
    public void getPromotedUrnWithTrackRepostReturnsAbsentPromotedUrn() throws Exception {
        final ApiStreamItem streamItem = new ApiStreamItem(ModelFixtures.create(ApiStreamTrackRepost.class));
        assertThat(streamItem.getAdUrn().isPresent()).isFalse();
    }

    @Test
    public void getPromotedUrnWithPlaylistPostReturnsAbsentPromotedUrn() throws Exception {
        final ApiStreamItem streamItem = new ApiStreamItem(ModelFixtures.create(ApiStreamPlaylistPost.class));
        assertThat(streamItem.getAdUrn().isPresent()).isFalse();
    }

    @Test
    public void getPromotedUrnWithPlaylistRepostReturnsAbsentPromotedUrn() throws Exception {
        final ApiStreamItem streamItem = new ApiStreamItem(ModelFixtures.create(ApiStreamPlaylistRepost.class));
        assertThat(streamItem.getAdUrn().isPresent()).isFalse();
    }
}
