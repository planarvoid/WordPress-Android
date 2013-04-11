package com.soundcloud.android.model;

import android.os.Parcel;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.soundcloud.android.Expect.expect;

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
        soundAssociation1.setType(SoundAssociation.Type.TRACK.type);

        SoundAssociation soundAssociation2 = new SoundAssociation();
        soundAssociation2.playable = soundAssociation1.playable;
        soundAssociation2.setType(SoundAssociation.Type.TRACK_REPOST.type);

        expect(soundAssociation1.getListItemId()).not.toEqual(soundAssociation2.getListItemId());
    }

    private void compareSoundItems(SoundAssociation soundItem, SoundAssociation soundItem2) {
        expect(soundItem2.id).toEqual(soundItem.id);
        expect(soundItem2.created_at).toEqual(soundItem.created_at);
        expect(soundItem2.associationType).toEqual(soundItem.associationType);
        expect(soundItem2.playable).toEqual(soundItem.playable);
    }
}
