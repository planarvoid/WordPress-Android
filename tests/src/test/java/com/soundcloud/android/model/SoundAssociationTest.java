package com.soundcloud.android.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.os.Parcel;

import java.util.Date;

@RunWith(DefaultTestRunner.class)
public class SoundAssociationTest {

    @Test
    public void shouldParcelAndUnparcelCorrectly() throws Exception {
        SoundAssociation soundItem = TestHelper.getObjectMapper().readValue(
                getClass().getResourceAsStream("sound_item.json"),
                SoundAssociation.class);

        expect(soundItem.associationType).not.toBeNull();
        expect(soundItem.created_at).not.toBeNull();
        expect(soundItem.playable).not.toBeNull();

        Parcel p = Parcel.obtain();
        soundItem.writeToParcel(p, 0);

        SoundAssociation soundItem1 = new SoundAssociation(p);
        compareSoundItems(soundItem, soundItem1);
    }

    @Test
    public void shouldProvideUniqueListItemId() throws Exception {

        SoundAssociation soundAssociation1 = new SoundAssociation();
        soundAssociation1.playable = new Track(123l);
        soundAssociation1.setType(SoundAssociation.Type.TRACK);

        SoundAssociation soundAssociation2 = new SoundAssociation();
        soundAssociation2.playable = soundAssociation1.playable;
        soundAssociation2.setType(SoundAssociation.Type.TRACK_REPOST);

        expect(soundAssociation1.getListItemId()).not.toEqual(soundAssociation2.getListItemId());
    }

    @Test
    public void testEquals() {
        SoundAssociation a1 = new SoundAssociation(new Track(1), new Date(), SoundAssociation.Type.TRACK);

        SoundAssociation a2;

        a2 = new SoundAssociation(new Track(1), new Date(), SoundAssociation.Type.TRACK);
        expect(a1).toEqual(a2);

        a2 = new SoundAssociation(new Track(2), new Date(), SoundAssociation.Type.TRACK);
        expect(a1).not.toEqual(a2);

        a2 = new SoundAssociation(new Track(1), new Date(), SoundAssociation.Type.TRACK_LIKE);
        expect(a1).not.toEqual(a2);

        a2 = new SoundAssociation(new Playlist(1), new Date(), SoundAssociation.Type.TRACK);
        expect(a1).not.toEqual(a2);

        a2 = null;
        expect(a1).not.toEqual(a2);

        a2 = new SoundAssociation();
        a2.mID = 5;
        expect(a1).not.toEqual(a2);
    }

    private void compareSoundItems(SoundAssociation soundItem, SoundAssociation soundItem2) {
        expect(soundItem2.mID).toEqual(soundItem.mID);
        expect(soundItem2.created_at).toEqual(soundItem.created_at);
        expect(soundItem2.associationType).toEqual(soundItem.associationType);
        expect(soundItem2.playable).toEqual(soundItem.playable);
    }
}
