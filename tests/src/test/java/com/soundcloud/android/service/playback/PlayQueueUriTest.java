package com.soundcloud.android.service.playback;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.tracking.eventlogger.TrackingInfo;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;

@RunWith(DefaultTestRunner.class)
public class PlayQueueUriTest {

    @Test
    public void shouldBuildUri() throws Exception {
        PlayQueueUri playQueueUri = new PlayQueueUri(Content.ME_SOUND_STREAM.uri);
        expect(playQueueUri.toUri(new Track(123l), 0, 100l, new TrackingInfo("1","2")).toString()).toEqual("content://com.soundcloud.android.provider.ScContentProvider/me/stream?trackId=123&playlistPos=0&seekPos=100&tracking-originUrl=1&tracking-exploreTag=2");
    }

    @Test
    public void shouldBuildUriWithParams() throws Exception {
        PlayQueueUri playQueueUri = new PlayQueueUri(new PlayQueueUri(Content.ME_SOUND_STREAM.uri).toUri(new Track(123l), 1, 200l, new TrackingInfo("1","2")));
        expect(playQueueUri.getPos()).toEqual(1);
        expect(playQueueUri.getSeekPos()).toEqual(200);
        expect(playQueueUri.getTrackId()).toEqual(123);
        expect(playQueueUri.getTrackingInfo()).toEqual(new TrackingInfo("1","2"));
    }

    @Test
    public void shouldOverwriteOldParams() throws Exception {
        Uri oldUri = new PlayQueueUri(Content.ME_SOUND_STREAM.uri).toUri(new Track(123l), 1, 200l, new TrackingInfo("1","2"));
        Uri newUri = new PlayQueueUri(oldUri).toUri(new Track(456l), 2, 400l, new TrackingInfo("3","4"));
        expect(newUri.toString()).toEqual("content://com.soundcloud.android.provider.ScContentProvider/me/stream?trackId=456&playlistPos=2&seekPos=400&tracking-originUrl=3&tracking-exploreTag=4");
    }

}
