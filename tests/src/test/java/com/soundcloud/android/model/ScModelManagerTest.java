package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.cache.TrackCache;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(DefaultTestRunner.class)
public class ScModelManagerTest {
    ScModelManager manager = new ScModelManager(Robolectric.application, AndroidCloudAPI.Mapper);

    @Test
    public void testShareTrack() {
        Track track = new Track();
        track.id = 1234;
        track.bpm = 120f;
        manager.cache(track);

        Track track2 = new Track();
        track2.id = 1234;
        track2.duration = 9876;
        Track t = manager.cache(track2);

        expect(t.bpm).toEqual(track.bpm);
        expect(t.duration).toEqual(track.duration);
    }

    @Test
    public void testUniqueUserMultipleTracks() throws IOException {
        TrackHolder holder = AndroidCloudAPI.Mapper.readValue(getClass().getResourceAsStream("tracks.json"), TrackHolder.class);
        expect(holder.size()).toBe(3);

        Track t1 = holder.get(0);
        Track t2 = holder.get(1);
        Track t3 = holder.get(2);
        t2.user.country = "North Korea";
        t3.user.full_name = "Kim";

        expect(t1.user.country).toBe("North Korea");
        expect(t1.user.full_name).toBe("Kim");
    }
}
