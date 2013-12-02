package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.tracking.eventlogger.PlaySessionSource;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;

@RunWith(DefaultTestRunner.class)
public class LastPlayQueueStateUriTest {

    private PlaySessionSource playSessionSource = new PlaySessionSource(Uri.parse("origin:page"), 123L, "v1.2");

    @Test
    public void shouldBuildUri() throws Exception {
        LastPlayQueueStateUri lastPlayQueueStateUri = new LastPlayQueueStateUri(Content.ME_SOUND_STREAM.uri);

        expect(lastPlayQueueStateUri.toUri(123l, 0, 100l, playSessionSource).toString())
                .toEqual("content://com.soundcloud.android.provider.ScContentProvider/me/stream?trackId=123&playlistPos=0&seekPos=100&setId=123&originUrl=origin%253Apage");
    }

    @Test
    public void shouldBuildUriWithParams() throws Exception {
        LastPlayQueueStateUri lastPlayQueueStateUri = new LastPlayQueueStateUri(new LastPlayQueueStateUri(Content.ME_SOUND_STREAM.uri).toUri(123l, 1, 200l, playSessionSource));
        expect(lastPlayQueueStateUri.getPos()).toEqual(1);
        expect(lastPlayQueueStateUri.getSeekPos()).toEqual(200);
        expect(lastPlayQueueStateUri.getTrackId()).toEqual(123);
        expect(lastPlayQueueStateUri.getPlaySessionSource().getSetId()).toEqual(playSessionSource.getSetId());
        expect(lastPlayQueueStateUri.getPlaySessionSource().getOriginPage()).toEqual(playSessionSource.getOriginPage());
    }

    @Test
    public void shouldOverwriteOldParams() throws Exception {
        Uri oldUri = new LastPlayQueueStateUri(Content.ME_SOUND_STREAM.uri).toUri(123l, 1, 200l, playSessionSource);
        Uri newUri = new LastPlayQueueStateUri(oldUri).toUri(456l, 2, 400l, new PlaySessionSource(Uri.parse("another:origin:page"), 789L, "v5.2"));
        expect(newUri.toString()).toEqual("content://com.soundcloud.android.provider.ScContentProvider/me/stream?trackId=456&playlistPos=2&seekPos=400&setId=789&originUrl=another%253Aorigin%253Apage");
    }



}
