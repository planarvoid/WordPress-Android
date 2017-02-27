package com.soundcloud.android.sync.posts;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import org.junit.Test;

import java.util.Date;

public class ApiPostItemTest {

    private static final Date CREATED_AT = new Date();

    @Test
    public void shouldConvertToTrackPostItemIfTrackPost() throws Exception {
        ApiPost trackPost = ApiPost.create(Urn.forTrack(123), CREATED_AT);
        ApiPostItem postItem = new ApiPostItem(trackPost, null, null, null);
        assertThat(postItem.getPostRecord()).isEqualTo(ApiPost.create(Urn.forTrack(123), CREATED_AT));
    }

    @Test
    public void shouldConvertToTrackRepostItemIfTrackRepost() throws Exception {
        ApiRepost trackRepost = ApiRepost.create(Urn.forTrack(123), CREATED_AT);
        ApiPostItem postItem = new ApiPostItem(null, trackRepost, null, null);
        assertThat(postItem.getPostRecord()).isEqualTo(ApiRepost.create(Urn.forTrack(123), CREATED_AT));
    }

    @Test
    public void shouldConvertToPlaylistPostItemIfPlaylistPost() throws Exception {
        ApiPost playlistPost = ApiPost.create(Urn.forPlaylist(123), CREATED_AT);
        ApiPostItem postItem = new ApiPostItem(null, null, playlistPost, null);
        assertThat(postItem.getPostRecord()).isEqualTo(ApiPost.create(Urn.forPlaylist(123), CREATED_AT));
    }

    @Test
    public void shouldConvertToPlaylistRepostItemIfPlaylistRepost() throws Exception {
        ApiRepost playlistRepost = ApiRepost.create(Urn.forPlaylist(123), CREATED_AT);
        ApiPostItem postItem = new ApiPostItem(null, null, null, playlistRepost);
        assertThat(postItem.getPostRecord()).isEqualTo(ApiRepost.create(Urn.forPlaylist(123), CREATED_AT));
    }
}
