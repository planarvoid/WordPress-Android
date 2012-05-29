package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

import java.util.Date;

@RunWith(DefaultTestRunner.class)
public class TrackSharingTest {
    @Test
    public void shouldBeParcelable() throws Exception {
        TrackSharing ts = new TrackSharing();
        ts.track = new Track();
        ts.sharing_note = new TrackSharing.SharingNote();
        ts.sharing_note.created_at = new Date();
        ts.sharing_note.text = "cool";

        Parcel p = Parcel.obtain();

        ts.writeToParcel(p, 0);
        TrackSharing ts2 = TrackSharing.CREATOR.createFromParcel(p);

        expect(ts.sharing_note.isEmpty()).toBeFalse();
        expect(ts.track).toEqual(ts2.track);
        expect(ts.sharing_note.text).toEqual(ts2.sharing_note.text);
        expect(ts.sharing_note.created_at).toEqual(ts2.sharing_note.created_at);
    }

    @Test
    public void shouldTestSharingNoteEmpty() throws Exception {
        TrackSharing ts = new TrackSharing();
        ts.track = new Track();
        ts.sharing_note = new TrackSharing.SharingNote();
        expect(ts.sharing_note.isEmpty()).toBeTrue();
        ts.sharing_note.text = "cool";
        expect(ts.sharing_note.isEmpty()).toBeFalse();
    }
}
