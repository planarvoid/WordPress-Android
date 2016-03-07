package com.soundcloud.android.sync.posts;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Test;

import java.util.Date;

public class ApiPostItemTest {

    private static final Date CREATED_AT = new Date();

    @Test
    public void shouldConvertToTrackPostPropertySetIfTrackPost() throws Exception {
        ApiPost trackPost = new ApiPost(Urn.forTrack(123), CREATED_AT);
        ApiPostItem postItem = new ApiPostItem(trackPost, null, null, null);
        assertThat(postItem.toPropertySet()).isEqualTo(
                PropertySet.from(
                        PostProperty.TARGET_URN.bind(Urn.forTrack(123)),
                        PostProperty.CREATED_AT.bind(CREATED_AT),
                        PostProperty.IS_REPOST.bind(false)
                )
        );
    }

    @Test
    public void shouldConvertToTrackRepostPropertySetIfTrackRepost() throws Exception {
        ApiRepost trackRepost = new ApiRepost(Urn.forTrack(123), CREATED_AT);
        ApiPostItem postItem = new ApiPostItem(null, trackRepost, null, null);
        assertThat(postItem.toPropertySet()).isEqualTo(
                PropertySet.from(
                        PostProperty.TARGET_URN.bind(Urn.forTrack(123)),
                        PostProperty.CREATED_AT.bind(CREATED_AT),
                        PostProperty.IS_REPOST.bind(true)
                )
        );
    }

    @Test
    public void shouldConvertToPlaylistPostPropertySetIfPlaylistPost() throws Exception {
        ApiPost playlistPost = new ApiPost(Urn.forPlaylist(123), CREATED_AT);
        ApiPostItem postItem = new ApiPostItem(null, null, playlistPost, null);
        assertThat(postItem.toPropertySet()).isEqualTo(
                PropertySet.from(
                        PostProperty.TARGET_URN.bind(Urn.forPlaylist(123)),
                        PostProperty.CREATED_AT.bind(CREATED_AT),
                        PostProperty.IS_REPOST.bind(false)
                )
        );
    }

    @Test
    public void shouldConvertToPlaylistRepostPropertySetIfPlaylistRepost() throws Exception {
        ApiRepost playlistRepost = new ApiRepost(Urn.forPlaylist(123), CREATED_AT);
        ApiPostItem postItem = new ApiPostItem(null, null, null, playlistRepost);
        assertThat(postItem.toPropertySet()).isEqualTo(
                PropertySet.from(
                        PostProperty.TARGET_URN.bind(Urn.forPlaylist(123)),
                        PostProperty.CREATED_AT.bind(CREATED_AT),
                        PostProperty.IS_REPOST.bind(true)
                )
        );
    }
}
