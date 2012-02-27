package com.soundcloud.android.tracking;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Track;
import org.junit.Test;

public class MediaTest {
    @Test
    public void shouldGetMediaFromTrackNull() throws Exception {
        expect(Media.fromTrack(null)).toBeNull();
    }

    @Test
    public void shouldGetMediaFromEmptyTrack() throws Exception {
        Track track = new Track();
        expect(Media.fromTrack(track)).not.toBeNull();
    }

    @Test
    public void shouldGetMediaFromTrackWithDuration() throws Exception {
        Track track = new Track();
        track.duration = 4000;
        final Media media = Media.fromTrack(track);
        expect(media).not.toBeNull();
        expect(media.atParams("play")).not.toBeNull();
    }

    @Test
    public void shouldHandleUnknownActions() throws Exception {
        expect(Media.fromTrack(new Track()).atParams("unknown")).toBeNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRequireAction() throws Exception {
        Media.fromTrack(new Track()).atParams((Object)null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRequireActionEmpty() throws Exception {
        Media.fromTrack(new Track()).atParams();
    }

    @Test
    public void shouldHave5SecsMinimumRefresh() throws Exception {
        expect(Media.refresh(10000)).toEqual(5000);
    }
}
