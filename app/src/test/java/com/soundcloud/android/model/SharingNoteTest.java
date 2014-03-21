package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class SharingNoteTest {

    @Test
    public void shouldResolveToString() {
        expect(Sharing.PRIVATE.value).toEqual("private");
        expect(Sharing.PUBLIC.value).toEqual("public");
    }

    @Test
    public void shouldResolveFromString() {
        expect(Sharing.from("private")).toBe(Sharing.PRIVATE);
        expect(Sharing.from("public")).toBe(Sharing.PUBLIC);
        expect(Sharing.from("abc")).toBe(Sharing.UNDEFINED);
    }

    @Test
    public void shouldResolvePrivateToBoolean() {
        expect(Sharing.PRIVATE.isPrivate()).toBeTrue();
        expect(Sharing.PRIVATE.isPublic()).toBeFalse();
    }

    @Test
    public void shouldResolvePublicToBoolean() {
        expect(Sharing.PUBLIC.isPrivate()).toBeFalse();
        expect(Sharing.PUBLIC.isPublic()).toBeTrue();
    }

    @Test
    public void shouldResolveUndefinedToBoolean() {
        expect(Sharing.UNDEFINED.isPrivate()).toBeFalse();
        expect(Sharing.UNDEFINED.isPublic()).toBeFalse();
    }

    @Test
    public void shouldResolveToPublicOrPrivateFromBoolean() {
        expect(Sharing.from(true)).toBe(Sharing.PUBLIC);
        expect(Sharing.from(false)).toBe(Sharing.PRIVATE);
    }

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
