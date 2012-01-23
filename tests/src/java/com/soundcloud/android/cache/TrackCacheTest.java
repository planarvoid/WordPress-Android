package com.soundcloud.android.cache;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TracklistItem;
import org.junit.Test;

import java.io.IOException;

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

    @Test
    public void testUniqueUserMultipleTracks() throws IOException {
        ScModel.TracklistItemHolder holder = AndroidCloudAPI.Mapper.readValue(getClass().getResourceAsStream("tracks.json"), ScModel.TracklistItemHolder.class);
        expect(holder.size()).toBe(3);

        TracklistItem t1 = holder.get(0);
        TracklistItem t2 = holder.get(1);
        TracklistItem t3 = holder.get(2);
        t2.user.country = "North Korea";
        t3.user.full_name = "Kim";

        expect(t1.user.country).toBe("North Korea");
        expect(t1.user.full_name).toBe("Kim");
    }
}