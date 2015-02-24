package com.soundcloud.android.api.model.stream;

import static com.soundcloud.android.Expect.expect;

import com.google.common.base.Optional;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.PromotedFixtures;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class ApiStreamItemTest {

    @Test
    public void getTrackWithTrackPostReturnsTrack() throws Exception {
        final ApiTrackPost apiTrackPost = ModelFixtures.create(ApiTrackPost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiTrackPost);
        expect(streamItem.getTrack().get()).toBe(apiTrackPost.getApiTrack());
    }

    @Test
    public void getReposterWithTrackPostReturnsAbsent() throws Exception {
        final ApiTrackPost apiTrackPost = ModelFixtures.create(ApiTrackPost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiTrackPost);
        expect(streamItem.getReposter()).toBe(Optional.<ApiUser>absent());
    }

    @Test
    public void getCreatedAtWithTrackPostReturnsPostCreatedAtDate() throws Exception {
        final ApiTrackPost apiTrackPost = ModelFixtures.create(ApiTrackPost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiTrackPost);
        expect(streamItem.getCreatedAtTime()).toEqual(apiTrackPost.getCreatedAtTime());
    }

    @Test
    public void getTrackWithTrackRepostReturnsTrack() throws Exception {
        final ApiTrackRepost apiTrackPost = ModelFixtures.create(ApiTrackRepost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiTrackPost);
        expect(streamItem.getTrack().get()).toBe(apiTrackPost.getApiTrack());
    }

    @Test
    public void getReposterWithTrackRepostReturnsReposter() throws Exception {
        final ApiTrackRepost apiTrackRepost = ModelFixtures.create(ApiTrackRepost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiTrackRepost);
        expect(streamItem.getReposter().get()).toBe(apiTrackRepost.getReposter());
    }

    @Test
    public void getCreatedAtWithTrackRepostReturnsRepostingDate() throws Exception {
        final ApiTrackRepost apiTrackRepost = ModelFixtures.create(ApiTrackRepost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiTrackRepost);
        expect(streamItem.getCreatedAtTime()).toEqual(apiTrackRepost.getCreatedAtTime());
    }
    
    @Test
    public void getPlaylistWithPlaylistPostReturnsPlaylist() throws Exception {
        final ApiPlaylistPost apiPlaylistPost = ModelFixtures.create(ApiPlaylistPost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiPlaylistPost);
        expect(streamItem.getPlaylist().get()).toBe(apiPlaylistPost.getApiPlaylist());
    }

    @Test
    public void getReposterWithPlaylistPostReturnsAbsent() throws Exception {
        final ApiPlaylistPost apiPlaylistPost = ModelFixtures.create(ApiPlaylistPost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiPlaylistPost);
        expect(streamItem.getReposter()).toBe(Optional.<ApiUser>absent());
    }

    @Test
    public void getCreatedAtWithPlaylistPostReturnsPlaylistCreatedAtDate() throws Exception {
        final ApiPlaylistPost apiPlaylistPost = ModelFixtures.create(ApiPlaylistPost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiPlaylistPost);
        expect(streamItem.getCreatedAtTime()).toEqual(apiPlaylistPost.getCreatedAtTime());
    }

    @Test
    public void getPlaylistWithPlaylistRepostReturnsPlaylist() throws Exception {
        final ApiPlaylistRepost ApiPlaylistPost = ModelFixtures.create(ApiPlaylistRepost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(ApiPlaylistPost);
        expect(streamItem.getPlaylist().get()).toBe(ApiPlaylistPost.getApiPlaylist());
    }

    @Test
    public void getReposterWithPlaylistRepostReturnsReposter() throws Exception {
        final ApiPlaylistRepost ApiPlaylistRepost = ModelFixtures.create(ApiPlaylistRepost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(ApiPlaylistRepost);
        expect(streamItem.getReposter().get()).toBe(ApiPlaylistRepost.getReposter());
    }

    @Test
    public void getCreatedAtWithPlaylistRepostReturnsRepostingDate() throws Exception {
        final ApiPlaylistRepost apiPlaylistRepost = ModelFixtures.create(ApiPlaylistRepost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiPlaylistRepost);
        expect(streamItem.getCreatedAtTime()).toEqual(apiPlaylistRepost.getCreatedAtTime());
    }

    @Test
    public void getCreatedAtWithPromotedTrackReturnsMaxVaue() throws Exception {
        final ApiPromotedTrack apiPromotedTrack = PromotedFixtures.promotedStreamItemWithoutPromoter();
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedTrack);
        expect(streamItem.getCreatedAtTime()).toEqual(Long.MAX_VALUE);
    }

    @Test
    public void getPromoterWithPromotedTrackReturnsValidPromoter() throws Exception {
        final ApiUser apiUser = ModelFixtures.create(ApiUser.class);
        final ApiPromotedTrack apiPromotedTrack = PromotedFixtures.promotedStreamItemWithPromoter(apiUser);
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedTrack);
        expect(streamItem.getPromoter().get()).toEqual(apiUser);
    }

    @Test
    public void getPromoterWithPromotedTrackReturnsAbsentPromoter() throws Exception {
        final ApiPromotedTrack apiPromotedTrack = PromotedFixtures.promotedStreamItemWithoutPromoter();
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedTrack);
        expect(streamItem.getPromoter().isPresent()).toBeFalse();
    }

    @Test
    public void getPromoterWithTrackPostReturnsAbsentPromoter() throws Exception {
        final ApiStreamItem streamItem = new ApiStreamItem(ModelFixtures.create(ApiTrackPost.class));
        expect(streamItem.getPromoter().isPresent()).toBeFalse();
    }

    @Test
    public void getPromoterWithTrackRepostReturnsAbsentPromoter() throws Exception {
        final ApiStreamItem streamItem = new ApiStreamItem(ModelFixtures.create(ApiTrackRepost.class));
        expect(streamItem.getPromoter().isPresent()).toBeFalse();
    }

    @Test
    public void getPromoterWithPlaylistPostReturnsAbsentPromoter() throws Exception {
        final ApiStreamItem streamItem = new ApiStreamItem(ModelFixtures.create(ApiPlaylistPost.class));
        expect(streamItem.getPromoter().isPresent()).toBeFalse();
    }

    @Test
    public void getPromoterWithPlaylistRepostReturnsAbsentPromoter() throws Exception {
        final ApiStreamItem streamItem = new ApiStreamItem(ModelFixtures.create(ApiPlaylistRepost.class));
        expect(streamItem.getPromoter().isPresent()).toBeFalse();
    }

    @Test
    public void getUrnWithPromotedTrackReturnsUrn() throws Exception {
        final ApiPromotedTrack apiPromotedTrack = PromotedFixtures.promotedStreamItemWithoutPromoter();
        final ApiStreamItem streamItem = new ApiStreamItem(apiPromotedTrack);
        expect(streamItem.getPromotedUrn().get()).toEqual(new Urn("adswizz:ads:123"));
    }

    @Test
    public void getPromotedUrnWithTrackPostReturnsAbsentPromotedUrn() throws Exception {
        final ApiStreamItem streamItem = new ApiStreamItem(ModelFixtures.create(ApiTrackPost.class));
        expect(streamItem.getPromotedUrn().isPresent()).toBeFalse();
    }

    @Test
    public void getPromotedUrnWithTrackRepostReturnsAbsentPromotedUrn() throws Exception {
        final ApiStreamItem streamItem = new ApiStreamItem(ModelFixtures.create(ApiTrackRepost.class));
        expect(streamItem.getPromotedUrn().isPresent()).toBeFalse();
    }

    @Test
    public void getPromotedUrnWithPlaylistPostReturnsAbsentPromotedUrn() throws Exception {
        final ApiStreamItem streamItem = new ApiStreamItem(ModelFixtures.create(ApiPlaylistPost.class));
        expect(streamItem.getPromotedUrn().isPresent()).toBeFalse();
    }

    @Test
    public void getPromotedUrnWithPlaylistRepostReturnsAbsentPromotedUrn() throws Exception {
        final ApiStreamItem streamItem = new ApiStreamItem(ModelFixtures.create(ApiPlaylistRepost.class));
        expect(streamItem.getPromotedUrn().isPresent()).toBeFalse();
    }


}