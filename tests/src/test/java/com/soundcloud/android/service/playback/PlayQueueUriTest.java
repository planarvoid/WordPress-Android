package com.soundcloud.android.service.playback;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.robolectric.TestHelper.readJson;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackHolder;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class PlayQueueUriTest {

    @Test
    public void shouldBuildUri() throws Exception {
        PlayQueueUri playQueueUri = new PlayQueueUri(Content.ME_SOUND_STREAM.uri);
        expect(playQueueUri.toUri(new Track(123l), 0, 100l).toString()).toEqual("content://com.soundcloud.android.provider.ScContentProvider/me/stream?trackId=123&playlistPos=0&seekPos=100");
    }

    @Test
    public void shouldBuildUriWithParams() throws Exception {
        PlayQueueUri playQueueUri = new PlayQueueUri(new PlayQueueUri(Content.ME_SOUND_STREAM.uri).toUri(new Track(123l), 1, 200l));
        expect(playQueueUri.getPos()).toEqual(1);
        expect(playQueueUri.getSeekPos()).toEqual(200);
        expect(playQueueUri.getTrackId()).toEqual(123);
    }

    @Test
    public void shouldOverwriteOldParams() throws Exception {
        Uri oldUri = new PlayQueueUri(Content.ME_SOUND_STREAM.uri).toUri(new Track(123l), 1, 200l);
        Uri newUri = new PlayQueueUri(oldUri).toUri(new Track(456l), 2, 400l);
        expect(newUri.toString()).toEqual("content://com.soundcloud.android.provider.ScContentProvider/me/stream?trackId=456&playlistPos=2&seekPos=400");
    }

}
