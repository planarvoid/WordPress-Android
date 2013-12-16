package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

import java.util.Date;

@RunWith(DefaultTestRunner.class)
public class SharingNoteTest {

    @Test
    public void shouldParcelAndUnparcelNullCreatedAtCorrectly() throws Exception {
        SharingNote sharingNote = new SharingNote();
        sharingNote.text = "blah blah";

        Parcel p = Parcel.obtain();
        sharingNote.writeToParcel(p, 0);

        SharingNote sharingNote2 = new SharingNote(p);
        expect(sharingNote.text).toEqual(sharingNote2.text);
        expect(sharingNote.created_at).toBeNull();
        expect(sharingNote2.created_at).toBeNull();
    }

    @Test
    public void shouldParcelAndUnparcelCorrectly() throws Exception {
        SharingNote sharingNote = new SharingNote();
        sharingNote.text = "blah blah";
        sharingNote.created_at = new Date(1234567l);

        Parcel p = Parcel.obtain();
        sharingNote.writeToParcel(p, 0);

        SharingNote sharingNote2 = new SharingNote(p);
        expect(sharingNote.text).toEqual(sharingNote2.text);
        expect(sharingNote.created_at.getTime()).toEqual(sharingNote2.created_at.getTime());
    }
}
