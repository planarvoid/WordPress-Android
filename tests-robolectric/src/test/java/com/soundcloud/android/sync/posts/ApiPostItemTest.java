package com.soundcloud.android.sync.posts;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class ApiPostItemTest {

    private static final Date CREATED_AT = new Date();

    @Test
    public void shouldConvertToTrackPostPropertySetIfTrackPost() throws Exception {
        ApiPost trackPost = new ApiPost(Urn.forTrack(123), CREATED_AT);
        ApiPostItem postItem = new ApiPostItem(trackPost, null, null, null);
        expect(postItem.toPropertySet()).toEqual(
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
        expect(postItem.toPropertySet()).toEqual(
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
        expect(postItem.toPropertySet()).toEqual(
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
        expect(postItem.toPropertySet()).toEqual(
                PropertySet.from(
                        PostProperty.TARGET_URN.bind(Urn.forPlaylist(123)),
                        PostProperty.CREATED_AT.bind(CREATED_AT),
                        PostProperty.IS_REPOST.bind(true)
                )
        );
    }
}
