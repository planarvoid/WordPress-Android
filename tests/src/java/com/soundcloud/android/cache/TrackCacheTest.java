package com.soundcloud.android.cache;

import android.database.*;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TracklistItem;
import com.soundcloud.android.model.User;
import org.junit.Test;

import java.awt.*;
import java.awt.Cursor;

import static com.soundcloud.android.Expect.expect;
import static junit.framework.Assert.assertEquals;

@SuppressWarnings({"UnnecessaryBoxing"})
public class TrackCacheTest {
    @Test
    public void testShareTrack() {
        Track track = new Track();
        track.id = 1234;
        track.bpm = 120f;
        SoundCloudApplication.TRACK_CACHE.put(track);

        TracklistItem listItem = new TracklistItem();
        listItem.id = 1234;
        listItem.duration = 9876;
        Track t = (Track) SoundCloudApplication.TRACK_CACHE.fromListItem(listItem);

        assertEquals(t.bpm,track.bpm);
        assertEquals(t.duration,track.duration);
    }
}