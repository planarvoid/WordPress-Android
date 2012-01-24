package com.soundcloud.android.cache;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TracklistItem;
import org.junit.Test;

import java.io.IOException;

public class TrackCacheTest {
    TrackCache cache = new TrackCache();

    @Test
    public void testShareTrack() {
        Track track = new Track();
        track.id = 1234;
        track.bpm = 120f;
        cache.put(track);

        TracklistItem listItem = new TracklistItem();
        listItem.id = 1234;
        listItem.duration = 9876;
        Track t = (Track) cache.fromListItem(listItem);

        expect(t.bpm).toEqual(track.bpm);
        expect(t.duration).toEqual(track.duration);
    }

    @Test
    public void testUniqueUserMultipleTracks() throws IOException {
        // XXX what does this test do?
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