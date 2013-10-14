package com.soundcloud.android.service.playback;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.tracking.eventlogger.PlaySourceInfo;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;

@RunWith(DefaultTestRunner.class)
public class PlayQueueUriTest {

    private PlaySourceInfo playSourceInfo = new PlaySourceInfo.Builder(1L).originUrl("1").exploreTag("2").recommenderVersion("v1").build();
    private PlaySourceInfo playSourceInfo2 = new PlaySourceInfo.Builder(2L).originUrl("3").exploreTag("4").recommenderVersion("v2").build();

    @Test
    public void shouldBuildUri() throws Exception {
        PlayQueueUri playQueueUri = new PlayQueueUri(Content.ME_SOUND_STREAM.uri);

        expect(playQueueUri.toUri(123l, 0, 100l, playSourceInfo).toString())
                .toEqual("content://com.soundcloud.android.provider.ScContentProvider/me/stream?trackId=123&playlistPos=0&seekPos=100&playSource-recommenderVersion=v1&playSource-exploreTag=2&playSource-originUrl=1&playSource-initialTrackId=1");
    }

    @Test
    public void shouldBuildUriWithParams() throws Exception {
        PlayQueueUri playQueueUri = new PlayQueueUri(new PlayQueueUri(Content.ME_SOUND_STREAM.uri).toUri(123l, 1, 200l, playSourceInfo));
        expect(playQueueUri.getPos()).toEqual(1);
        expect(playQueueUri.getSeekPos()).toEqual(200);
        expect(playQueueUri.getTrackId()).toEqual(123);
        expect(playQueueUri.getPlaySourceInfo()).toEqual(playSourceInfo);
    }

    @Test
    public void shouldOverwriteOldParams() throws Exception {
        Uri oldUri = new PlayQueueUri(Content.ME_SOUND_STREAM.uri).toUri(123l, 1, 200l, playSourceInfo);
        Uri newUri = new PlayQueueUri(oldUri).toUri(456l, 2, 400l, playSourceInfo2);
        expect(newUri.toString()).toEqual("content://com.soundcloud.android.provider.ScContentProvider/me/stream?trackId=456&playlistPos=2&seekPos=400&playSource-recommenderVersion=v2&playSource-exploreTag=4&playSource-originUrl=3&playSource-initialTrackId=2");
    }

}
