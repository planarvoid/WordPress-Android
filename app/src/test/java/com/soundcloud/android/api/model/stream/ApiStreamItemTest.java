package com.soundcloud.android.api.model.stream;

import static com.soundcloud.android.Expect.expect;

import com.google.common.base.Optional;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
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
    public void getCreatedAtWithTrackPostReturnsTrackCreatedAtDate() throws Exception {
        final ApiTrackPost apiTrackPost = ModelFixtures.create(ApiTrackPost.class);
        final ApiStreamItem streamItem = new ApiStreamItem(apiTrackPost);
        expect(streamItem.getCreatedAt()).toBe(apiTrackPost.getApiTrack().getCreatedAt());
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
        expect(streamItem.getCreatedAt()).toBe(apiTrackRepost.getCreatedAt());
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
        expect(streamItem.getCreatedAt()).toBe(apiPlaylistPost.getApiPlaylist().getCreatedAt());
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
        expect(streamItem.getCreatedAt()).toBe(apiPlaylistRepost.getCreatedAt());
    }

}