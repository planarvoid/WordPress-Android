package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.tracking.eventlogger.PlaySessionSource;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;

@RunWith(DefaultTestRunner.class)
public class PlayQueueUriTest {

    private PlaySessionSource playSessionSource = PlaySessionSource.EMPTY;

    @Test
    public void shouldBuildUri() throws Exception {
        PlayQueueUri playQueueUri = new PlayQueueUri(Content.ME_SOUND_STREAM.uri);

        expect(playQueueUri.toUri(123l, 0, 100l, playSessionSource).toString())
                .toEqual("content://com.soundcloud.android.provider.ScContentProvider/me/stream?trackId=123&playlistPos=0&seekPos=100&playSource-recommenderVersion=v1&playSource-exploreVersion=2&playSource-originUrl=1&playSource-initialTrackId=123");
    }

    @Test
    public void shouldBuildUriWithParams() throws Exception {
        PlayQueueUri playQueueUri = new PlayQueueUri(new PlayQueueUri(Content.ME_SOUND_STREAM.uri).toUri(123l, 1, 200l, playSessionSource));
        expect(playQueueUri.getPos()).toEqual(1);
        expect(playQueueUri.getSeekPos()).toEqual(200);
        expect(playQueueUri.getTrackId()).toEqual(123);
        expect(playQueueUri.getPlaySessionSource()).toEqual(playSessionSource);
    }

    @Test
    public void shouldOverwriteOldParams() throws Exception {
        Uri oldUri = new PlayQueueUri(Content.ME_SOUND_STREAM.uri).toUri(123l, 1, 200l, playSessionSource);
        Uri newUri = new PlayQueueUri(oldUri).toUri(456l, 2, 400l, playSessionSource);
        expect(newUri.toString()).toEqual("content://com.soundcloud.android.provider.ScContentProvider/me/stream?trackId=456&playlistPos=2&seekPos=400&playSource-recommenderVersion=v2&playSource-exploreVersion=4&playSource-originUrl=3&playSource-initialTrackId=321");
    }



}
