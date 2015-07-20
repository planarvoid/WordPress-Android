package com.soundcloud.android.api.model.stream;

import static com.soundcloud.android.Expect.expect;

import com.google.common.base.Optional;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.PromotedFixtures;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class ApiStreamItemTest {

    @Test
    public void getTrackWithTrackPostReturnsTrack() throws Exception {
        final ApiStreamTrackPost apiTrackPost = ModelFixtures.create(ApiStreamTrackPost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiTrackPost);
        expect(streamItem.getTrack().get()).toBe(apiTrackPost.getApiTrack());
    }

    @Test
    public void getReposterWithTrackPostReturnsAbsent() throws Exception {
        final ApiStreamTrackPost apiTrackPost = ModelFixtures.create(ApiStreamTrackPost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiTrackPost);
        expect(streamItem.getReposter()).toBe(Optional.<ApiUser>absent());
    }

    @Test
    public void getCreatedAtWithTrackPostReturnsPostCreatedAtDate() throws Exception {
        final ApiStreamTrackPost apiTrackPost = ModelFixtures.create(ApiStreamTrackPost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiTrackPost);
        expect(streamItem.getCreatedAtTime()).toEqual(apiTrackPost.getCreatedAtTime());
    }

    @Test
    public void getTrackWithTrackRepostReturnsTrack() throws Exception {
        final ApiStreamTrackRepost apiTrackPost = ModelFixtures.create(ApiStreamTrackRepost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiTrackPost);
        expect(streamItem.getTrack().get()).toBe(apiTrackPost.getApiTrack());
    }

    @Test
    public void getReposterWithTrackRepostReturnsReposter() throws Exception {
        final ApiStreamTrackRepost apiTrackRepost = ModelFixtures.create(ApiStreamTrackRepost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiTrackRepost);
        expect(streamItem.getReposter().get()).toBe(apiTrackRepost.getReposter());
    }

    @Test
    public void getCreatedAtWithTrackRepostReturnsRepostingDate() throws Exception {
        final ApiStreamTrackRepost apiTrackRepost = ModelFixtures.create(ApiStreamTrackRepost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiTrackRepost);
        expect(streamItem.getCreatedAtTime()).toEqual(apiTrackRepost.getCreatedAtTime());
    }

    @Test
    public void getPlaylistWithPlaylistPostReturnsPlaylist() throws Exception {
        final ApiStreamPlaylistPost apiPlaylistPost = ModelFixtures.create(ApiStreamPlaylistPost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiPlaylistPost);
        expect(streamItem.getPlaylist().get()).toBe(apiPlaylistPost.getApiPlaylist());
    }

    @Test
    public void getReposterWithPlaylistPostReturnsAbsent() throws Exception {
        final ApiStreamPlaylistPost apiPlaylistPost = ModelFixtures.create(ApiStreamPlaylistPost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiPlaylistPost);
        expect(streamItem.getReposter()).toBe(Optional.<ApiUser>absent());
    }

    @Test
    public void getCreatedAtWithPlaylistPostReturnsPlaylistCreatedAtDate() throws Exception {
        final ApiStreamPlaylistPost apiPlaylistPost = ModelFixtures.create(ApiStreamPlaylistPost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiPlaylistPost);
        expect(streamItem.getCreatedAtTime()).toEqual(apiPlaylistPost.getCreatedAtTime());
    }

    @Test
    public void getPlaylistWithPlaylistRepostReturnsPlaylist() throws Exception {
        final ApiStreamPlaylistRepost ApiPlaylistPost = ModelFixtures.create(ApiStreamPlaylistRepost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(ApiPlaylistPost);
        expect(streamItem.getPlaylist().get()).toBe(ApiPlaylistPost.getApiPlaylist());
    }

    @Test
    public void getReposterWithPlaylistRepostReturnsReposter() throws Exception {
        final ApiStreamPlaylistRepost ApiPlaylistRepost = ModelFixtures.create(ApiStreamPlaylistRepost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(ApiPlaylistRepost);
        expect(streamItem.getReposter().get()).toBe(ApiPlaylistRepost.getReposter());
    }

    @Test
    public void getCreatedAtWithPlaylistRepostReturnsRepostingDate() throws Exception {
        final ApiStreamPlaylistRepost apiPlaylistRepost = ModelFixtures.create(ApiStreamPlaylistRepost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiPlaylistRepost);
        expect(streamItem.getCreatedAtTime()).toEqual(apiPlaylistRepost.getCreatedAtTime());
    }

    @Test
    public void getCreatedAtWithPromotedTrackReturnsMaxVaue() throws Exception {
        final ApiPromotedTrack apiPromotedTrack = PromotedFixtures.promotedTrackItemWithoutPromoter();
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedTrack);
        expect(streamItem.getCreatedAtTime()).toEqual(Long.MAX_VALUE);
    }

    @Test
    public void getCreatedAtWithPromotedPlaylistReturnsMaxVaue() throws Exception {
        final ApiPromotedPlaylist apiPromotedPlaylist = PromotedFixtures.promotedPlaylistItemWithoutPromoter();
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedPlaylist);
        expect(streamItem.getCreatedAtTime()).toEqual(Long.MAX_VALUE);
    }

    @Test
    public void getPromoterWithPromotedTrackReturnsValidPromoter() throws Exception {
        final ApiUser apiUser = ModelFixtures.create(ApiUser.class);
        final ApiPromotedTrack apiPromotedTrack = PromotedFixtures.promotedTrackItemWithPromoter(apiUser);
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedTrack);
        expect(streamItem.getPromoter().get()).toEqual(apiUser);
    }

    @Test
    public void getPromoterWithPromotedPlaylistReturnsValidPromoter() throws Exception {
        final ApiUser apiUser = ModelFixtures.create(ApiUser.class);
        final ApiPromotedPlaylist apiPromotedPlaylist = PromotedFixtures.promotedPlaylistItemWithPromoter(apiUser);
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedPlaylist);
        expect(streamItem.getPromoter().get()).toEqual(apiUser);
    }

    @Test
    public void getPromoterWithPromotedTrackReturnsAbsentPromoter() throws Exception {
        final ApiPromotedTrack apiPromotedTrack = PromotedFixtures.promotedTrackItemWithoutPromoter();
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedTrack);
        expect(streamItem.getPromoter().isPresent()).toBeFalse();
    }

    @Test
    public void getPromoterWithPromotedPlaylistReturnsAbsentPromoter() throws Exception {
        final ApiPromotedPlaylist apiPromotedPlaylist = PromotedFixtures.promotedPlaylistItemWithoutPromoter();
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedPlaylist);
        expect(streamItem.getPromoter().isPresent()).toBeFalse();
    }

    @Test
    public void getPromoterWithTrackPostReturnsAbsentPromoter() throws Exception {
        final ApiStreamItem streamItem = new ApiStreamItem(ModelFixtures.create(ApiStreamTrackPost.class));
        expect(streamItem.getPromoter().isPresent()).toBeFalse();
    }

    @Test
    public void getPromoterWithTrackRepostReturnsAbsentPromoter() throws Exception {
        final ApiStreamItem streamItem = new ApiStreamItem(ModelFixtures.create(ApiStreamTrackRepost.class));
        expect(streamItem.getPromoter().isPresent()).toBeFalse();
    }

    @Test
    public void getPromoterWithPlaylistPostReturnsAbsentPromoter() throws Exception {
        final ApiStreamItem streamItem = new ApiStreamItem(ModelFixtures.create(ApiStreamPlaylistPost.class));
        expect(streamItem.getPromoter().isPresent()).toBeFalse();
    }

    @Test
    public void getPromoterWithPlaylistRepostReturnsAbsentPromoter() throws Exception {
        final ApiStreamItem streamItem = new ApiStreamItem(ModelFixtures.create(ApiStreamPlaylistRepost.class));
        expect(streamItem.getPromoter().isPresent()).toBeFalse();
    }

    @Test
    public void getUrnWithPromotedTrackReturnsUrn() throws Exception {
        final ApiPromotedTrack apiPromotedTrack = PromotedFixtures.promotedTrackItemWithoutPromoter();
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedTrack);
        expect(streamItem.getAdUrn().get()).toEqual("dfp:ads:123-4567");
    }

    @Test
    public void getUrnWithPromotedPlaylistReturnsUrn() throws Exception {
        final ApiPromotedPlaylist apiPromotedPlaylist = PromotedFixtures.promotedPlaylistItemWithoutPromoter();
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedPlaylist);
        expect(streamItem.getAdUrn().get()).toEqual("dfp:ads:678-7890");
    }

    @Test
    public void getPromotedUrnWithTrackPostReturnsAbsentPromotedUrn() throws Exception {
        final ApiStreamItem streamItem = new ApiStreamItem(ModelFixtures.create(ApiStreamTrackPost.class));
        expect(streamItem.getAdUrn().isPresent()).toBeFalse();
    }

    @Test
    public void getPromotedUrnWithTrackRepostReturnsAbsentPromotedUrn() throws Exception {
        final ApiStreamItem streamItem = new ApiStreamItem(ModelFixtures.create(ApiStreamTrackRepost.class));
        expect(streamItem.getAdUrn().isPresent()).toBeFalse();
    }

    @Test
    public void getPromotedUrnWithPlaylistPostReturnsAbsentPromotedUrn() throws Exception {
        final ApiStreamItem streamItem = new ApiStreamItem(ModelFixtures.create(ApiStreamPlaylistPost.class));
        expect(streamItem.getAdUrn().isPresent()).toBeFalse();
    }

    @Test
    public void getPromotedUrnWithPlaylistRepostReturnsAbsentPromotedUrn() throws Exception {
        final ApiStreamItem streamItem = new ApiStreamItem(ModelFixtures.create(ApiStreamPlaylistRepost.class));
        expect(streamItem.getAdUrn().isPresent()).toBeFalse();
    }
}
