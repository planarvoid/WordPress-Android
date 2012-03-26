package com.soundcloud.android.tracking;

import static com.soundcloud.android.Expect.expect;

import com.at.ATParams;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
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
    }

    @Test
    public void shouldAcceptDifferentMediaActionTypes() throws Exception {
        Track track = new Track();
        final Media media = Media.fromTrack(track);

        expect(media.atParams(Media.Action.Play)).not.toBeNull();
        expect(media.atParams(ATParams.mediaAction.Play)).not.toBeNull();
        expect(media.atParams("Play")).not.toBeNull();
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

    @Test
    public void shouldGenerateMedianame() throws Exception {
        Track t = new Track();
        t.user = new User();
        t.permalink = "foo";
        t.user.permalink = "user";
        expect(Media.getMediaName(t)).toEqual("user::user/foo");
    }
}
